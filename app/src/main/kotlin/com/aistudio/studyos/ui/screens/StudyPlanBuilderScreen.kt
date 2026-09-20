package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    val manualTime: Boolean = false,
    val minutes: Int = 25,
    val manualSplit: Boolean = false,
    val splitText: String = ""
)

private data class EditableSubject(val name: String, val topics: List<EditableTopic>)

private fun parseSplit(text: String): List<Int> = text.split(",", ";", " ").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }

private fun autoSplit(minutes: Int): List<Int> {
    var remaining = minutes.coerceAtLeast(1)
    val result = mutableListOf<Int>()
    while (remaining > 25) { result += 25; remaining -= 25 }
    result += remaining
    return result
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when { h > 0 && m > 0 -> "${h}h ${m}m"; h > 0 -> "${h}h"; else -> "${m}m" }
}

private fun allocateTimes(subjects: List<EditableSubject>, total: Int): Map<Pair<Int, Int>, Int> {
    val result = mutableMapOf<Pair<Int, Int>, Int>()
    val auto = mutableListOf<Pair<Int, Int>>()
    var manualTotal = 0
    subjects.forEachIndexed { si, s -> s.topics.forEachIndexed { ti, t ->
        if (t.manualTime) { val m = t.minutes.coerceAtLeast(1); result[si to ti] = m; manualTotal += m }
        else auto += si to ti
    } }
    val remaining = total - manualTotal
    if (remaining >= 0 && auto.isNotEmpty()) {
        val each = remaining / auto.size
        var extra = remaining % auto.size
        auto.forEach { key -> result[key] = each + if (extra-- > 0) 1 else 0 }
    }
    return result
}

private fun splitValid(topic: EditableTopic, minutes: Int): Boolean {
    if (minutes !in 1..720) return false
    val parts = if (topic.manualSplit) parseSplit(topic.splitText) else autoSplit(minutes)
    return parts.isNotEmpty() && parts.all { it in 1..25 } && parts.sum() == minutes
}

private fun flatten(subjects: List<EditableSubject>, allocation: Map<Pair<Int, Int>, Int>): List<StudyPlanItem> =
    subjects.flatMapIndexed { si, subject -> subject.topics.flatMapIndexed { ti, topic ->
        val minutes = allocation[si to ti] ?: 0
        val parts = if (topic.manualSplit) parseSplit(topic.splitText) else autoSplit(minutes)
        parts.map { StudyPlanItem(subject.name.trim(), topic.name.trim(), it) }
    } }

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
    var totalSessionMinutes by remember { mutableIntStateOf(300) }
    var breakMinutes by remember { mutableIntStateOf(5) }
    val initialTopicsList = remember(initialTopics) { initialTopics.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() } }
    val subjects = remember { mutableStateListOf(EditableSubject(initialSubject.ifBlank { "Mathematics" }, initialTopicsList.ifEmpty { listOf("New Topic") }.map { EditableTopic(it) })) }
    val allocation = allocateTimes(subjects, totalSessionMinutes)
    val topicCount = subjects.sumOf { it.topics.size }
    val manualTotal = subjects.flatMap { it.topics }.filter { it.manualTime }.sumOf { it.minutes.coerceAtLeast(1) }
    val autoCount = topicCount - subjects.flatMap { it.topics }.count { it.manualTime }
    val allocatedTotal = allocation.values.sum()
    val valid = subjects.isNotEmpty() && manualTotal <= totalSessionMinutes && allocatedTotal == totalSessionMinutes &&
        subjects.all { it.name.isNotBlank() && it.topics.isNotEmpty() && it.topics.all { t -> t.name.isNotBlank() } } &&
        subjects.indices.all { si -> subjects[si].topics.indices.all { ti -> splitValid(subjects[si].topics[ti], allocation[si to ti] ?: 0) } }

    Scaffold(topBar = { TopAppBar(title = { Text("Study Plan", fontWeight = FontWeight.Bold) }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = .12f)).padding(11.dp)) { Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary) }
                            Spacer(Modifier.width(12.dp)); Column { Text("Build your study session", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold); Text("Set total time first. Subjects and topics must fit inside it.", style = MaterialTheme.typography.bodySmall) }
                        }
                        OutlinedTextField(
                            value = totalSessionMinutes.toString(),
                            onValueChange = { it.filter(Char::isDigit).toIntOrNull()?.let { v -> totalSessionMinutes = v.coerceIn(30, 720) } },
                            label = { Text("Total session time (minutes)") },
                            supportingText = { Text("Hard limit: ${formatDuration(totalSessionMinutes)}") },
                            modifier = Modifier.fillMaxWidth().testTag("total_session_time"), singleLine = true, shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f))) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        Metric("Session", formatDuration(totalSessionMinutes)); Metric("Topics", topicCount.toString()); Metric("Blocks", flatten(subjects, allocation).size.toString()); Metric("Max", "25m")
                    }
                }
            }
            item { Text("Subjects & Topics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text("Add custom subjects, then add multiple topics under each one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            itemsIndexed(subjects) { si, subject ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp))
                            OutlinedTextField(subject.name, { subjects[si] = subject.copy(name = it) }, label = { Text("Subject") }, modifier = Modifier.weight(1f).testTag("study_subject_$si"), singleLine = true, shape = RoundedCornerShape(13.dp))
                            if (subjects.size > 1) IconButton({ subjects.removeAt(si) }) { Icon(Icons.Default.Delete, "Remove subject") }
                        }
                        subject.topics.forEachIndexed { ti, topic ->
                            val minutes = allocation[si to ti] ?: 0
                            val split = if (topic.manualSplit) parseSplit(topic.splitText) else autoSplit(minutes)
                            val splitOk = splitValid(topic, minutes)
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f))) {
                                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Topic ${ti + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.weight(1f)); if (subject.topics.size > 1) IconButton({ subjects[si] = subject.copy(topics = subject.topics.filterIndexed { i, _ -> i != ti }) }, Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Remove topic", Modifier.size(18.dp)) } }
                                    OutlinedTextField(topic.name, { value -> val list = subject.topics.toMutableList(); list[ti] = topic.copy(name = value); subjects[si] = subject.copy(topics = list) }, label = { Text("Topic / Chapter / Sub-topic") }, modifier = Modifier.fillMaxWidth().testTag("study_topic_${si}_$ti"), singleLine = true, shape = RoundedCornerShape(13.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Time allocation", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); FilterChip(!topic.manualTime, { val list = subject.topics.toMutableList(); list[ti] = topic.copy(manualTime = false); subjects[si] = subject.copy(topics = list) }, label = { Text("Auto") }); Spacer(Modifier.width(6.dp)); FilterChip(topic.manualTime, { val list = subject.topics.toMutableList(); list[ti] = topic.copy(manualTime = true); subjects[si] = subject.copy(topics = list) }, label = { Text("Manual") }) }
                                    if (topic.manualTime) OutlinedTextField(topic.minutes.toString(), { value -> val list = subject.topics.toMutableList(); list[ti] = topic.copy(minutes = value.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1); subjects[si] = subject.copy(topics = list) }, label = { Text("Topic time (minutes)") }, supportingText = { Text("Manual total: ${formatDuration(manualTotal)} / ${formatDuration(totalSessionMinutes)}") }, modifier = Modifier.fillMaxWidth().testTag("study_duration_${si}_$ti"), singleLine = true, shape = RoundedCornerShape(13.dp))
                                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .08f)) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Schedule, null, Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text(if (topic.manualTime) "Allocated: ${formatDuration(minutes)}" else "Auto allocated: ${formatDuration(minutes)}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("${split.size} blocks", style = MaterialTheme.typography.labelSmall) } }
                                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Session split", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); FilterChip(!topic.manualSplit, { val list = subject.topics.toMutableList(); list[ti] = topic.copy(manualSplit = false, splitText = ""); subjects[si] = subject.copy(topics = list) }, label = { Text("Auto ≤25m") }); Spacer(Modifier.width(5.dp)); FilterChip(topic.manualSplit, { val list = subject.topics.toMutableList(); list[ti] = topic.copy(manualSplit = true, splitText = autoSplit(minutes.coerceAtLeast(1)).joinToString(", ")); subjects[si] = subject.copy(topics = list) }, label = { Text("Manual") }) }
                                    if (topic.manualSplit) OutlinedTextField(topic.splitText, { value -> val list = subject.topics.toMutableList(); list[ti] = topic.copy(splitText = value); subjects[si] = subject.copy(topics = list) }, label = { Text("Block lengths: 25, 20, 15") }, supportingText = { Text(if (splitOk) "✓ ${split.joinToString(" + ")} = $minutes min" else "Every block must be 1–25 min and total exactly ${minutes} min.", color = if (splitOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }, modifier = Modifier.fillMaxWidth().testTag("study_split_${si}_$ti"), singleLine = true, shape = RoundedCornerShape(13.dp), isError = !splitOk)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) { split.forEachIndexed { i, m -> Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (m == 25) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 8.dp, vertical = 5.dp)) { Text("S${i + 1} ${m}m", fontSize = 10.sp, fontWeight = FontWeight.Bold) } } }
                                }
                            }
                        }
                        OutlinedButton({ subjects[si] = subject.copy(topics = subject.topics + EditableTopic("New Topic")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add Topic") }
                    }
                }
            }
            item { OutlinedButton({ subjects.add(EditableSubject("New Subject", listOf(EditableTopic("New Topic")))) }, modifier = Modifier.fillMaxWidth().testTag("btn_add_subject"), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add Subject") } }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))) {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Auto allocation", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        Text("Auto topics share the remaining session time equally after Manual topics. This guarantees the plan stays inside the total session time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf(60, 120, 180, 300, 360, 480).forEach { m -> FilterChip(totalSessionMinutes == m, { totalSessionMinutes = m }, label = { Text(formatDuration(m)) }) } }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))) {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Mandatory break", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        Text("A break is required after every focus block. The final block has no extra break after it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(5, 10).forEach { m -> FilterChip(breakMinutes == m, { breakMinutes = m }, label = { Text("$m min") }) } }
                    }
                }
            }
            item {
                val remaining = totalSessionMinutes - allocatedTotal
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(if (valid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (valid) Icons.Default.CheckCircle else Icons.Default.Timer, null); Spacer(Modifier.width(10.dp)); Column { Text(if (valid) "Perfect — ${formatDuration(totalSessionMinutes)} allocated" else if (remaining > 0) "${formatDuration(remaining)} still needs allocation" else "${formatDuration(-remaining)} over the limit", fontWeight = FontWeight.Bold); Text("Manual: ${formatDuration(manualTotal)} • Auto topics: $autoCount", style = MaterialTheme.typography.bodySmall) } }
                }
            }
            item {
                Button({ val finalItems = flatten(subjects, allocation); if (finalItems.isNotEmpty()) { val first = finalItems.first(); viewModel.startNewPlan("Study Session • ${formatDuration(totalSessionMinutes)}", first.subject, first.topic, mode.lowercase(), finalItems.size, first.minutes, breakMinutes, true, finalItems); onStartFocus() } }, enabled = valid, modifier = Modifier.fillMaxWidth().height(56.dp).testTag("btn_start_study_plan"), shape = RoundedCornerShape(17.dp)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Start ${formatDuration(totalSessionMinutes)} Study Session", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton({ val finalItems = flatten(subjects, allocation); if (finalItems.isNotEmpty()) { val first = finalItems.first(); viewModel.saveDraftPlan("Study Session • ${formatDuration(totalSessionMinutes)} (Draft)", first.subject, first.topic, mode.lowercase(), finalItems.size, first.minutes, breakMinutes, finalItems); onBack() } }, enabled = valid, modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_save_study_draft"), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Bookmark, null); Spacer(Modifier.width(8.dp)); Text("Save Draft") }
            }
            item { Text("Hard rule: total topic time = total session time • every focus block ≤ 25 minutes • break = 5 or 10 minutes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 24.dp)) }
        }
    }
}

@Composable private fun Metric(label: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(3.dp)); Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) } }