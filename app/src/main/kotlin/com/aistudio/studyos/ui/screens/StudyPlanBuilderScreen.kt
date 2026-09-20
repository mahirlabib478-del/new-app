package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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

private data class EditableStudyItem(
    val subject: String,
    val topic: String,
    val minutes: Int,
    val manualSplit: Boolean = false,
    val splitText: String = ""
)

private fun autoSplit(minutes: Int): List<Int> {
    var remaining = minutes.coerceAtLeast(1)
    val result = mutableListOf<Int>()
    while (remaining > 25) {
        result += 25
        remaining -= 25
    }
    result += remaining
    return result
}

private fun parseSplit(text: String): List<Int> =
    text.split(',', ' ', ';').mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }

private fun validSplit(item: EditableStudyItem): Boolean {
    val parts = if (item.manualSplit) parseSplit(item.splitText) else autoSplit(item.minutes)
    return parts.isNotEmpty() && parts.all { it in 1..25 } && parts.sum() == item.minutes
}

private fun flattenedItems(items: List<EditableStudyItem>): List<StudyPlanItem> =
    items.flatMap { item ->
        val parts = if (item.manualSplit) parseSplit(item.splitText) else autoSplit(item.minutes)
        parts.map { minutes -> StudyPlanItem(item.subject.trim(), item.topic.trim(), minutes) }
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
    val items = remember {
        mutableStateListOf<EditableStudyItem>().apply {
            val parsed = initialTopics.split(',', '\n').map { it.trim() }.filter { it.isNotBlank() }
            if (parsed.isEmpty()) {
                add(EditableStudyItem(initialSubject.ifBlank { "Mathematics" }, "New Topic", 25))
            } else {
                parsed.take(12).forEach { topic ->
                    add(EditableStudyItem(initialSubject.ifBlank { "Mathematics" }, topic, 25))
                }
            }
        }
    }
    var breakMinutes by remember { mutableStateOf(5) }
    val totalMinutes = items.sumOf { it.minutes.coerceIn(1, 720) }
    val focusBlocks = items.sumOf { if (it.manualSplit) parseSplit(it.splitText).size else autoSplit(it.minutes).size }
    val allValid = items.isNotEmpty() && totalMinutes in 1..720 &&
        items.all { it.subject.isNotBlank() && it.topic.isNotBlank() && validSplit(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Plan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Plan your focus", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Every focus session is capped at 25 minutes. Topic time is never exceeded.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryMetric("Study", formatDuration(totalMinutes))
                        SummaryMetric("Focus", focusBlocks.toString() + " blocks")
                        SummaryMetric("Max", "25 min")
                    }
                }
            }

            itemsIndexed(items, key = { index, _ -> index }) { index, item ->
                val split = if (item.manualSplit) parseSplit(item.splitText) else autoSplit(item.minutes)
                val splitOk = validSplit(item)
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text((index + 1).toString(), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Study topic", fontWeight = FontWeight.Bold)
                                Text(
                                    item.minutes.toString() + " min allocated • " + split.size + " focus " +
                                        if (split.size == 1) "block" else "blocks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (items.size > 1) {
                                IconButton(onClick = { items.removeAt(index) }, Modifier.testTag("remove_topic_" + index)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove topic")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = item.subject,
                            onValueChange = { value -> items[index] = item.copy(subject = value) },
                            label = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth().testTag("study_subject_" + index),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = item.topic,
                            onValueChange = { value -> items[index] = item.copy(topic = value) },
                            label = { Text("Topic / Chapter / Sub-topic") },
                            modifier = Modifier.fillMaxWidth().testTag("study_topic_" + index),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = item.minutes.toString(),
                            onValueChange = { value ->
                                val minutes = value.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 720) ?: 1
                                items[index] = item.copy(minutes = minutes)
                            },
                            label = { Text("Total study time (1–720 min)") },
                            modifier = Modifier.fillMaxWidth().testTag("study_duration_" + index),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.width(20.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Split into focus sessions", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            FilterChip(
                                selected = !item.manualSplit,
                                onClick = { items[index] = item.copy(manualSplit = false, splitText = "") },
                                label = { Text("Auto") }
                            )
                            Spacer(Modifier.width(6.dp))
                            FilterChip(
                                selected = item.manualSplit,
                                onClick = {
                                    items[index] = item.copy(
                                        manualSplit = true,
                                        splitText = autoSplit(item.minutes).joinToString(", ")
                                    )
                                },
                                label = { Text("Manual") }
                            )
                        }

                        if (item.manualSplit) {
                            OutlinedTextField(
                                value = item.splitText,
                                onValueChange = { value -> items[index] = item.copy(splitText = value) },
                                label = { Text("Session lengths (e.g. 25, 20, 15)") },
                                supportingText = {
                                    Text(
                                        if (splitOk) "✓ " + split.joinToString(" + ") + " = " + item.minutes + " min"
                                        else "Each session must be 1–25 min and the total must equal " + item.minutes + " min.",
                                        color = if (splitOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("study_split_" + index),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                isError = !splitOk
                            )
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            split.forEachIndexed { partIndex, minutes ->
                                Box(
                                    Modifier.clip(RoundedCornerShape(9.dp))
                                        .background(
                                            if (minutes == 25) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        )
                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                ) {
                                    Text("S" + (partIndex + 1) + "  " + minutes + "m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        if (totalMinutes < 720) {
                            val remaining = (720 - totalMinutes).coerceAtLeast(1)
                            items.add(EditableStudyItem(
                                items.lastOrNull()?.subject ?: "Mathematics",
                                "New Topic",
                                minOf(25, remaining)
                            ))
                        }
                    },
                    enabled = totalMinutes < 720,
                    Modifier.fillMaxWidth().testTag("btn_add_study_topic"),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Subject / Topic")
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Break after each focus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Breaks happen between focus blocks, but not after the final block.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(0, 3, 5, 10, 15).forEach { mins ->
                                FilterChip(
                                    selected = breakMinutes == mins,
                                    onClick = { breakMinutes = mins },
                                    label = { Text(if (mins == 0) "None" else mins.toString() + "m") }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text("Quick total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), Modifier.fillMaxWidth()) {
                    listOf(60, 120, 180, 360, 720).forEach { mins ->
                        FilterChip(
                            selected = totalMinutes == mins,
                            onClick = {
                                if (items.isNotEmpty()) {
                                    items[0] = items[0].copy(minutes = mins, manualSplit = false, splitText = "")
                                    for (i in 1 until items.size) {
                                        items[i] = items[i].copy(minutes = 1, manualSplit = false, splitText = "")
                                    }
                                }
                            },
                            label = { Text(if (mins < 60) mins.toString() + "m" else (mins / 60).toString() + "h", fontSize = 11.sp) }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val finalItems = flattenedItems(items)
                        if (finalItems.isEmpty()) return@Button
                        val first = finalItems.first()
                        viewModel.startNewPlan(
                            title = first.subject + ": " + first.topic,
                            subject = first.subject,
                            chapter = first.topic,
                            mode = mode.lowercase(),
                            totalBlocks = finalItems.size,
                            blockMinutes = first.minutes,
                            breakMinutes = breakMinutes,
                            autoStart = true,
                            items = finalItems
                        )
                        onStartFocus()
                    },
                    enabled = allValid,
                    Modifier.fillMaxWidth().height(54.dp).testTag("btn_start_study_plan"),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Study Plan  •  " + totalMinutes + " min", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val finalItems = flattenedItems(items)
                        if (finalItems.isEmpty()) return@OutlinedButton
                        val first = finalItems.first()
                        viewModel.saveDraftPlan(
                            title = first.subject + ": " + first.topic + " (Draft)",
                            subject = first.subject,
                            chapter = first.topic,
                            mode = mode.lowercase(),
                            totalBlocks = finalItems.size,
                            blockMinutes = first.minutes,
                            breakMinutes = breakMinutes,
                            items = finalItems
                        )
                        onBack()
                    },
                    enabled = allValid,
                    Modifier.fillMaxWidth().height(50.dp).testTag("btn_save_study_draft"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Draft")
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.width(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Hard limit: no focus session can exceed 25 minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

private fun formatDuration(minutes: Int): String =
    if (minutes < 60) minutes.toString() + "m"
    else (minutes / 60).toString() + "h " + (minutes % 60).toString().padStart(2, '0') + "m"
