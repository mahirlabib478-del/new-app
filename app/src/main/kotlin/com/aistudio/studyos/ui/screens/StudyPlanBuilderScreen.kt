package com.aistudio.studyos.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.local.entity.StudyPlanItem
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
import com.aistudio.studyos.ui.components.AnimatedReveal
import com.aistudio.studyos.ui.components.tactile3DButton

private data class EditableTopic(val name: String, val minutesText: String = "")
private data class EditableSubject(val name: String, val topics: List<EditableTopic>)

private fun parseMinutes(text: String): Int? =
    text.trim().toIntOrNull()?.takeIf { it > 0 }

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

private fun splitTopicMinutes(topicCount: Int, totalMinutes: Int): List<Int> {
    if (topicCount <= 0 || totalMinutes < topicCount) return emptyList()
    val base = totalMinutes / topicCount
    val remainder = totalMinutes % topicCount
    return List(topicCount) { index ->
        base + if (index < remainder) 1 else 0
    }
}

private fun flatten(
    subjects: List<EditableSubject>,
    totalMinutes: Int,
    blockMinutes: Int
): List<StudyPlanItem> {
    val result = mutableListOf<StudyPlanItem>()
    val safeBlock = blockMinutes.coerceIn(25, 50)
    subjects.forEach { subject ->
        subject.topics.forEach { topic ->
            var remaining = parseMinutes(topic.minutesText) ?: 0
            while (remaining > safeBlock) {
                result += StudyPlanItem(subject.name.trim(), topic.name.trim(), safeBlock)
                remaining -= safeBlock
            }
            if (remaining > 0) {
                result += StudyPlanItem(subject.name.trim(), topic.name.trim(), remaining)
            }
        }
    }
    return if (result.sumOf { it.minutes } == totalMinutes) result else emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanBuilderScreen(
    viewModel: StudyViewModel,
    mode: String,
    initialSubject: String,
    initialTopics: String,
    onBack: () -> Unit,
    onStartFocus: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var totalSessionText by remember { mutableStateOf("") }
    var focusMinutes by remember { mutableIntStateOf(25) }
    var breakMinutes by remember { mutableIntStateOf(5) }

    val initialTopicsList = remember(initialTopics) {
        initialTopics.split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
    val subjects = remember {
        mutableStateListOf(
            EditableSubject(
                initialSubject.ifBlank { "Mathematics" },
                initialTopicsList.ifEmpty { listOf("New Topic") }.map { EditableTopic(it) }
            )
        )
    }

    val totalSessionMinutes = parseMinutes(totalSessionText) ?: 0
    val allTopics = subjects.flatMap { it.topics }
    val topicCount = allTopics.size
    val topicMinutes = allTopics.map { parseMinutes(it.minutesText) }
    val allocatedTotal = topicMinutes.filterNotNull().sum()
    val allTimesEntered = topicMinutes.size == topicMinutes.count { it != null }
    val namesValid = subjects.isNotEmpty() &&
        subjects.all { it.name.trim().isNotBlank() && it.topics.isNotEmpty() &&
            it.topics.all { topic -> topic.name.trim().isNotBlank() } }
    val valid = totalSessionMinutes in 30..720 &&
        topicCount > 0 &&
        allTimesEntered &&
        namesValid &&
        allocatedTotal == totalSessionMinutes

    val draftReady = topicCount > 0 && namesValid &&
        allTimesEntered && allocatedTotal in 1..720

    fun applySplit() {
        if (totalSessionMinutes >= topicCount && topicCount > 0) {
            val times = splitTopicMinutes(topicCount, totalSessionMinutes)
            var index = 0
            subjects.indices.forEach { si ->
                val subject = subjects[si]
                subjects[si] = subject.copy(
                    topics = subject.topics.map { topic ->
                        topic.copy(minutesText = times[index++].toString())
                    }
                )
            }
        }
    }

    fun goNext() {
        when (step) {
            0 -> if (namesValid) step = 1
            1 -> if (totalSessionMinutes in 30..720) step = 2
        }
    }

    fun startPlan() {
        val finalItems = flatten(subjects, totalSessionMinutes, focusMinutes)
        if (!valid || finalItems.isEmpty()) return
        val first = finalItems.first()
        viewModel.startNewPlan(
            title = title.trim().ifBlank { "Study Session • ${formatDuration(totalSessionMinutes)}" },
            subject = first.subject,
            chapter = first.topic,
            mode = mode,
            totalBlocks = finalItems.size,
            blockMinutes = focusMinutes,
            breakMinutes = breakMinutes,
            autoStart = true,
            items = finalItems,
            expectedTotalMinutes = totalSessionMinutes,
            onReady = onStartFocus
        )
    }

    fun saveDraft() {
        val finalItems = flatten(subjects, allocatedTotal, focusMinutes)
        if (!draftReady || finalItems.isEmpty()) return
        val first = finalItems.first()
        viewModel.saveDraftPlan(
            title = title.trim().ifBlank { "Study Session • ${formatDuration(allocatedTotal)}" },
            subject = first.subject,
            chapter = first.topic,
            mode = mode,
            totalBlocks = finalItems.size,
            blockMinutes = focusMinutes,
            breakMinutes = breakMinutes,
            items = finalItems
        )
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Study Plan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        OutlinedButton(
                            onClick = { step-- },
                            modifier = Modifier.weight(0.8f).tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), 14.dp, 4.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Back") }
                    }
                    if (step < 2) {
                        Button(
                            onClick = ::goNext,
                            enabled = if (step == 0) namesValid else totalSessionMinutes in 30..720,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .tactile3DButton(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    14.dp,
                                    5.dp
                                ),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Next", maxLines = 1, softWrap = false) }
                    } else {
                        OutlinedButton(
                            onClick = ::saveDraft,
                            enabled = draftReady,
                            modifier = Modifier.weight(1f).tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), 14.dp, 4.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Save Draft") }
                        Button(
                            onClick = ::startPlan,
                            enabled = valid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .tactile3DButton(
                                    if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    if (valid) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                                    14.dp,
                                    5.dp
                                )
                                .testTag("btn_start_study_plan"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Start • ${formatDuration(totalSessionMinutes)}", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    ) { padding ->
        val horizontal = if (LocalConfiguration.current.screenWidthDp < 360) 12.dp else 18.dp
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal, 14.dp, horizontal, 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AnimatedReveal(index = step) {
                    StepHeader(step)
                }
            }

            when (step) {
                0 -> {
                    item {
                        PlanIntroCard(
                            title = "What are you studying?",
                            subtitle = "Add the subjects and topics you want to cover."
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it.take(80) },
                            label = { Text("Plan name (optional)") },
                            placeholder = { Text("Example: Biology Revision") },
                            supportingText = { Text("You can rename the plan anytime before starting.") },
                            modifier = Modifier.fillMaxWidth().testTag("study_plan_title"),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    itemsIndexed(subjects) { si, subject ->
                        SubjectEditor(
                            subject = subject,
                            index = si,
                            canDelete = subjects.size > 1,
                            onSubjectChange = { subjects[si] = it },
                            onDelete = { subjects.removeAt(si) }
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = {
                                subjects.add(
                                    EditableSubject(
                                        name = "",
                                        topics = listOf(EditableTopic(""))
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth().tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), 14.dp, 4.dp).testTag("btn_add_subject"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Subject")
                        }
                    }
                    item {
                        if (!namesValid) {
                            ValidationCard("Add a name to every subject and topic before continuing.")
                        }
                    }
                }

                1 -> {
                    item {
                        PlanIntroCard(
                            title = "How long do you want to study?",
                            subtitle = "Choose the total session time, then tune your focus and breaks."
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = totalSessionText,
                            onValueChange = { totalSessionText = it.filter(Char::isDigit).take(3) },
                            label = { Text("Total session time (minutes)") },
                            placeholder = { Text("Example: 120") },
                            supportingText = {
                                Text(
                                    if (totalSessionMinutes > 0) "Session: ${formatDuration(totalSessionMinutes)}"
                                    else "Choose between 30 minutes and 12 hours."
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("total_session_time"),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(30, 60, 90, 120, 180, 240, 300)) { minutes ->
                                FilterChip(
                                    selected = totalSessionMinutes == minutes,
                                    onClick = { totalSessionText = minutes.toString() },
                                    label = { Text(formatDuration(minutes)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                    item {
                        SettingCard(
                            title = "Focus block",
                            subtitle = "Each topic is split into blocks no longer than your selected duration.",
                            icon = Icons.Default.Timer
                        ) {
                            Spacer(Modifier.height(2.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                            ) {
                                items(listOf(25, 30, 45, 50)) { minutes ->
                                    FilterChip(
                                        selected = focusMinutes == minutes,
                                        onClick = { focusMinutes = minutes },
                                        label = { Text("${minutes}m") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            labelColor = MaterialTheme.colorScheme.onSurface,
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("focus_block_${minutes}")
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SettingCard(
                            title = "Breaks",
                            subtitle = "A break starts after each completed focus block. The final focus block ends the session.",
                            icon = Icons.Default.Schedule
                        ) {
                            Spacer(Modifier.height(2.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(listOf(5, 10)) { minutes ->
                                    FilterChip(
                                        selected = breakMinutes == minutes,
                                        onClick = { breakMinutes = minutes },
                                        label = { Text("${minutes} min") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            labelColor = MaterialTheme.colorScheme.onSurface,
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                    item {
                        if (totalSessionMinutes !in 30..720) {
                            ValidationCard("Enter a total session time between 30 minutes and 12 hours.")
                        }
                    }
                }

                else -> {
                    item {
                        PlanIntroCard(
                            title = "Review your plan",
                            subtitle = "Make sure everything is allocated before you start."
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it.take(80) },
                            label = { Text("Plan name") },
                            placeholder = { Text("Example: Biology Revision") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    item {
                        SummaryCard(
                            totalMinutes = totalSessionMinutes,
                            allocatedMinutes = allocatedTotal,
                            topicCount = topicCount,
                            focusMinutes = focusMinutes,
                            breakMinutes = breakMinutes
                        )
                    }
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Allocate your time", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                                        Text(
                                            "Balance the whole session evenly or edit each topic manually.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = ::applySplit,
                                        enabled = totalSessionMinutes >= topicCount && topicCount > 0,
                                        modifier = Modifier.tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), 12.dp, 3.dp).testTag("btn_split_topics"),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) { Text("Auto Balance") }
                                }

                                val ratio = if (totalSessionMinutes > 0) {
                                    (allocatedTotal.toFloat() / totalSessionMinutes).coerceIn(0f, 1f)
                                } else 0f
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${allocatedTotal} / ${totalSessionMinutes} min", fontWeight = FontWeight.Bold)
                                    Text(
                                        when {
                                            totalSessionMinutes == 0 -> "Set total time"
                                            allocatedTotal == totalSessionMinutes -> "✓ Fully allocated"
                                            allocatedTotal < totalSessionMinutes -> "${totalSessionMinutes - allocatedTotal} min remaining"
                                            else -> "${allocatedTotal - totalSessionMinutes} min over"
                                        },
                                        color = if (allocatedTotal == totalSessionMinutes) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    itemsIndexed(subjects) { si, subject ->
                        ReviewSubject(
                            subject = subject,
                            index = si,
                            onTopicTimeChange = { ti, value ->
                                val list = subject.topics.toMutableList()
                                list[ti] = list[ti].copy(minutesText = value.filter(Char::isDigit).take(3))
                                subjects[si] = subject.copy(topics = list)
                            }
                        )
                    }
                    item {
                        ValidationCard(
                            when {
                                !namesValid -> "Add a name to every subject and topic."
                                !allTimesEntered -> "Add a time to every topic, or use Auto Balance."
                                totalSessionMinutes !in 30..720 -> "Enter a total session time between 30 minutes and 12 hours."
                                allocatedTotal < totalSessionMinutes -> "${totalSessionMinutes - allocatedTotal} min still needs to be allocated."
                                allocatedTotal > totalSessionMinutes -> "${allocatedTotal - totalSessionMinutes} min is over the session limit."
                                else -> "Perfect — the entire session is allocated."
                            },
                            success = valid
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(step: Int) {
    val labels = listOf("Topics", "Time", "Review")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == step
            val completed = index < step
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .tactile3DButton(
                        backgroundColor = when {
                            selected -> MaterialTheme.colorScheme.primaryContainer
                            completed -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surface
                        },
                        bottomEdgeColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        cornerRadius = 14.dp,
                        depth = if (selected) 4.dp else 3.dp
                    ),
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (completed) "✓" else "${index + 1}",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}


@Composable
private fun PlanIntroCard(title: String, subtitle: String) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .tactile3DButton(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), 22.dp, 4.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SubjectEditor(
    subject: EditableSubject,
    index: Int,
    canDelete: Boolean,
    onSubjectChange: (EditableSubject) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(220)).tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f), 20.dp, 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = subject.name,
                    onValueChange = { onSubjectChange(subject.copy(name = it.take(60))) },
                    label = { Text("Subject") },
                    modifier = Modifier.weight(1f).testTag("study_subject_$index"),
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp)
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Remove subject")
                    }
                }
            }
            subject.topics.forEachIndexed { ti, topic ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Topic ${ti + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.weight(1f))
                            if (subject.topics.size > 1) {
                                IconButton(
                                    onClick = {
                                        onSubjectChange(subject.copy(
                                            topics = subject.topics.filterIndexed { i, _ -> i != ti }
                                        ))
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Remove topic", Modifier.size(18.dp))
                                }
                            }
                        }
                        OutlinedTextField(
                            value = topic.name,
                            onValueChange = { value ->
                                val list = subject.topics.toMutableList()
                                list[ti] = topic.copy(name = value.take(100))
                                onSubjectChange(subject.copy(topics = list))
                            },
                            label = { Text("Topic / Chapter") },
                            placeholder = { Text("Example: Cell structure") },
                            modifier = Modifier.fillMaxWidth().testTag("study_topic_${index}_$ti"),
                            singleLine = true,
                            shape = RoundedCornerShape(13.dp)
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = { onSubjectChange(subject.copy(topics = subject.topics + EditableTopic(""))) },
                modifier = Modifier.fillMaxWidth().tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), 13.dp, 3.dp),
                shape = RoundedCornerShape(13.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add Topic")
            }
        }
    }
}

@Composable
private fun ReviewSubject(
    subject: EditableSubject,
    index: Int,
    onTopicTimeChange: (Int, String) -> Unit
) {
    Card(
        Modifier.fillMaxWidth().tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f), 20.dp, 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(subject.name.ifBlank { "Unnamed Subject" }, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            subject.topics.forEachIndexed { ti, topic ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(topic.name.ifBlank { "Unnamed Topic" }, fontWeight = FontWeight.SemiBold)
                        Text("Topic ${ti + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedTextField(
                        value = topic.minutesText,
                        onValueChange = { onTopicTimeChange(ti, it) },
                        label = { Text("min") },
                        modifier = Modifier.width(105.dp).testTag("study_duration_${index}_$ti"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    totalMinutes: Int,
    allocatedMinutes: Int,
    topicCount: Int,
    focusMinutes: Int,
    breakMinutes: Int
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryMetric("Session", if (totalMinutes > 0) formatDuration(totalMinutes) else "—")
            SummaryMetric("Topics", topicCount.toString())
            SummaryMetric("Blocks", if (focusMinutes > 0) {
                ((allocatedMinutes + focusMinutes - 1) / focusMinutes).toString()
            } else "—")
            SummaryMetric("Break", "${breakMinutes}m")
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.ExtraBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
private fun ValidationCard(message: String, success: Boolean = false) {
    Surface(
        Modifier.fillMaxWidth().tactile3DButton(if (success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), 16.dp, 3.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (success) Icons.Default.CheckCircle else Icons.Default.Schedule,
                contentDescription = null,
                tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(9.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, fontWeight = if (success) FontWeight.Bold else FontWeight.Medium)
        }
    }
}
