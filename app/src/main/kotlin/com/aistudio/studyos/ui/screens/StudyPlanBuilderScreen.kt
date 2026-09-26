package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.local.entity.StudyPlanItem
import com.aistudio.studyos.ui.viewmodel.StudyViewModel

private data class EditableTopic(
    val name: String,
    val minutesText: String = ""
)

private data class EditableSubject(
    val name: String,
    val topics: List<EditableTopic>
)

private fun parseMinutes(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it > 0 }

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
    return List(topicCount) { index -> base + if (index < remainder) 1 else 0 }
}

private fun flatten(
    subjects: List<EditableSubject>,
    totalMinutes: Int
): List<StudyPlanItem> {
    val result = mutableListOf<StudyPlanItem>()
    subjects.forEach { subject ->
        subject.topics.forEach { topic ->
            val minutes = parseMinutes(topic.minutesText) ?: 0
            var remaining = minutes
            while (remaining > 25) {
                result += StudyPlanItem(subject.name.trim(), topic.name.trim(), 25)
                remaining -= 25
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
    var totalSessionText by remember { mutableStateOf("") }
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
    val topicCount = subjects.sumOf { it.topics.size }
    val topicMinutes = subjects.flatMap { it.topics }.mapNotNull { parseMinutes(it.minutesText) }
    val allocatedTotal = topicMinutes.sum()
    val allTimesEntered = topicMinutes.size == topicCount
    val namesValid = subjects.isNotEmpty() &&
        subjects.all { it.name.isNotBlank() && it.topics.isNotEmpty() && it.topics.all { t -> t.name.isNotBlank() } }
    val valid = totalSessionMinutes in 30..720 &&
        topicCount > 0 &&
        allTimesEntered &&
        namesValid &&
        allocatedTotal == totalSessionMinutes

    fun applySplit() {
        if (totalSessionMinutes >= topicCount && topicCount > 0) {
            val times = splitTopicMinutes(topicCount, totalSessionMinutes)
            var index = 0
            for (si in subjects.indices) {
                val subject = subjects[si]
                val updatedTopics = subject.topics.map { topic ->
                    topic.copy(minutesText = times[index++].toString())
                }
                subjects[si] = subject.copy(topics = updatedTopics)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Plan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = .12f))
                                    .padding(11.dp)
                            ) {
                                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Plan your study session", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "Set the total time first, then divide it across every topic.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        OutlinedTextField(
                            value = totalSessionText,
                            onValueChange = { value ->
                                totalSessionText = value.filter(Char::isDigit).take(3)
                            },
                            label = { Text("Total session time") },
                            placeholder = { Text("Example: 300") },
                            supportingText = {
                                Text(
                                    if (totalSessionMinutes > 0) "Session: ${formatDuration(totalSessionMinutes)}"
                                    else "Enter total minutes, e.g. 300 = 5 hours"
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("total_session_time"),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 4.dp)
                        ) {
                            items(listOf(60, 120, 180, 300, 360)) { minutes ->
                                FilterChip(
                                    selected = totalSessionMinutes == minutes,
                                    onClick = { totalSessionText = minutes.toString() },
                                    label = { Text(formatDuration(minutes)) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Metric("Session", if (totalSessionMinutes > 0) formatDuration(totalSessionMinutes) else "—")
                        Metric("Topics", topicCount.toString())
                        Metric("Allocated", if (allocatedTotal > 0) formatDuration(allocatedTotal) else "—")
                        Metric("Max block", "25m")
                    }
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .55f))
                ) {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Split total time across topics", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text(
                                    "One tap divides the full session time as evenly as possible among all topics.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = ::applySplit,
                                enabled = totalSessionMinutes >= topicCount && topicCount > 0,
                                modifier = Modifier.testTag("btn_split_topics"),
                                contentPadding = PaddingValues(horizontal = 15.dp)
                            ) {
                                Text("Split")
                            }
                        }
                    }
                }
            }

            item {
                Text("Subjects & Topics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Add as many subjects and topics as you need. Set each topic time manually or use Split.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            itemsIndexed(subjects) { si, subject ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = subject.name,
                                onValueChange = { subjects[si] = subject.copy(name = it) },
                                label = { Text("Subject") },
                                modifier = Modifier.weight(1f).testTag("study_subject_$si"),
                                singleLine = true,
                                shape = RoundedCornerShape(13.dp)
                            )
                            if (subjects.size > 1) {
                                IconButton(onClick = { subjects.removeAt(si) }) {
                                    Icon(Icons.Default.Delete, "Remove subject")
                                }
                            }
                        }

                        subject.topics.forEachIndexed { ti, topic ->
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(17.dp),
                                colors = CardDefaults.cardColors(
                                    MaterialTheme.colorScheme.surface.copy(alpha = .92f)
                                )
                            ) {
                                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Topic ${ti + 1}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.weight(1f))
                                        if (subject.topics.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    subjects[si] = subject.copy(
                                                        topics = subject.topics.filterIndexed { i, _ -> i != ti }
                                                    )
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
                                            list[ti] = topic.copy(name = value)
                                            subjects[si] = subject.copy(topics = list)
                                        },
                                        label = { Text("Topic / Chapter / Sub-topic") },
                                        placeholder = { Text("Example: Cell structure") },
                                        modifier = Modifier.fillMaxWidth().testTag("study_topic_${si}_$ti"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(13.dp)
                                    )

                                    OutlinedTextField(
                                        value = topic.minutesText,
                                        onValueChange = { value ->
                                            val list = subject.topics.toMutableList()
                                            list[ti] = topic.copy(
                                                minutesText = value.filter(Char::isDigit).take(3)
                                            )
                                            subjects[si] = subject.copy(topics = list)
                                        },
                                        label = { Text("Topic time (minutes)") },
                                        placeholder = { Text("Example: 45") },
                                        supportingText = {
                                            Text("Each focus block is automatically capped at 25 minutes.")
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("study_duration_${si}_$ti"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(13.dp)
                                    )

                                    val minutes = parseMinutes(topic.minutesText)
                                    if (minutes != null) {
                                        val blocks = (minutes + 24) / 25
                                        Surface(
                                            Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = .08f)
                                        ) {
                                            Row(
                                                Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Schedule, null, Modifier.size(17.dp))
                                                Spacer(Modifier.width(7.dp))
                                                Text(
                                                    "Allocated: ${formatDuration(minutes)}",
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    "$blocks focus block${if (blocks == 1) "" else "s"}",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                subjects[si] = subject.copy(
                                    topics = subject.topics + EditableTopic("New Topic")
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Topic")
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        subjects.add(
                            EditableSubject(
                                "New Subject",
                                listOf(EditableTopic("New Topic"))
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_add_subject"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Subject")
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(19.dp),
                    colors = CardDefaults.cardColors(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
                    )
                ) {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Mandatory breaks", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            "After every completed focus block, a break starts automatically. The final focus block ends the session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10).forEach { minutes ->
                                FilterChip(
                                    selected = breakMinutes == minutes,
                                    onClick = { breakMinutes = minutes },
                                    label = { Text("$minutes min") }
                                )
                            }
                        }
                    }
                }
            }

            item {
                val difference = totalSessionMinutes - allocatedTotal
                val completeSetup = totalSessionText.isNotBlank() && allocatedTotal > 0 && allTimesEntered && namesValid
                val invalidSetup = completeSetup && (
                    totalSessionMinutes !in 30..720 || difference != 0
                )
                val message = when {
                    valid -> "Perfect — the entire session is allocated."
                    totalSessionText.isBlank() -> "Enter your total session time."
                    allocatedTotal == 0 -> "Set topic times manually or press Split."
                    !allTimesEntered -> "Add a time to every topic, or press Split."
                    !namesValid -> "Add a name to every subject and topic."
                    difference > 0 -> "${formatDuration(difference)} still needs to be allocated."
                    difference < 0 -> "${formatDuration(-difference)} is over the session limit."
                    else -> "Check the session time and topic allocation."
                }
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        when {
                            valid -> MaterialTheme.colorScheme.primaryContainer
                            invalidSetup -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (valid) Icons.Default.CheckCircle else Icons.Default.Timer,
                            null
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(message, fontWeight = FontWeight.Bold)
                            Text(
                                "Allocated ${formatDuration(allocatedTotal)} / Session ${if (totalSessionMinutes > 0) formatDuration(totalSessionMinutes) else "—"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val finalItems = flatten(subjects, totalSessionMinutes)
                        if (finalItems.isNotEmpty()) {
                            val first = finalItems.first()
                            viewModel.startNewPlan(
                                "Study Session • ${formatDuration(totalSessionMinutes)}",
                                first.subject,
                                first.topic,
                                mode.lowercase(),
                                finalItems.size,
                                first.minutes,
                                breakMinutes,
                                true,
                                finalItems,
                                expectedTotalMinutes = totalSessionMinutes,
                                onReady = onStartFocus
                            )
                        }
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("btn_start_study_plan"),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start ${formatDuration(totalSessionMinutes)} Study Session", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val finalItems = flatten(subjects, totalSessionMinutes)
                        if (finalItems.isNotEmpty()) {
                            val first = finalItems.first()
                            viewModel.saveDraftPlan(
                                "Study Session • ${formatDuration(totalSessionMinutes)} (Draft)",
                                first.subject,
                                first.topic,
                                mode.lowercase(),
                                finalItems.size,
                                first.minutes,
                                breakMinutes,
                                finalItems
                            )
                            onBack()
                        }
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_save_study_draft"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Bookmark, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Draft")
                }
            }

            item {
                Text(
                    "Rules: topic times must equal total session time • focus blocks are max 25 minutes • breaks are 5 or 10 minutes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}
