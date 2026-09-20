package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.local.entity.StudyPlanItem
import com.aistudio.studyos.ui.viewmodel.StudyViewModel

private data class EditableStudyItem(
    var subject: String,
    var topic: String,
    var minutes: Int
)

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
    val title = when (mode.lowercase()) {
        "exam" -> "Exam Preparation Setup"
        "cram" -> "Next Day Exam Cram"
        else -> "Regular Study Setup"
    }

    LaunchedEffect(Unit) {
        if (items.isEmpty()) items.add(EditableStudyItem("Mathematics", "New Topic", 25))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    when (mode.lowercase()) {
                        "exam" -> "Build your exam plan with multiple subjects and topics."
                        "cram" -> "Add urgent topics and keep the full cram plan within 12 hours."
                        else -> "Add as many subjects and topics as you need."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Total: $totalMinutes / 720 min",
                    fontWeight = FontWeight.Bold,
                    color = if (totalMinutes > 720) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            itemsIndexed(items, key = { index, _ -> index }) { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Topic ${index + 1}", fontWeight = FontWeight.Bold)
                            if (items.size > 1) {
                                IconButton(onClick = { items.removeAt(index) }, modifier = Modifier.testTag("remove_topic_$index")) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove topic")
                                }
                            }
                        }
                        OutlinedTextField(
                            value = item.subject,
                            onValueChange = { item.subject = it },
                            label = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth().testTag("study_subject_$index"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = item.topic,
                            onValueChange = { item.topic = it },
                            label = { Text("Topic / Chapter") },
                            modifier = Modifier.fillMaxWidth().testTag("study_topic_$index"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = item.minutes.toString(),
                            onValueChange = { value ->
                                item.minutes = value.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 720) ?: 1
                            },
                            label = { Text("Duration (1–720 min)") },
                            modifier = Modifier.fillMaxWidth().testTag("study_duration_$index"),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        if (totalMinutes < 720) {
                            val remaining = (720 - totalMinutes).coerceAtLeast(1)
                            items.add(EditableStudyItem(items.lastOrNull()?.subject ?: "Mathematics", "New Topic", minOf(25, remaining)))
                        }
                    },
                    enabled = totalMinutes < 720,
                    modifier = Modifier.fillMaxWidth().testTag("btn_add_study_topic"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Subject / Topic")
                }
            }

            item {
                Text("Break Between Topics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 3, 5, 10, 15).forEach { mins ->
                        FilterChip(
                            selected = breakMinutes == mins,
                            onClick = { breakMinutes = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }
            }

            item {
                Text("Quick Duration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(60, 120, 180, 360, 720).forEach { mins ->
                        FilterChip(
                            selected = totalMinutes == mins,
                            onClick = {
                                if (items.isNotEmpty()) {
                                    items[0].minutes = mins
                                    for (i in 1 until items.size) items[i].minutes = 1
                                }
                            },
                            label = { Text(if (mins < 60) "${mins}m" else "${mins / 60}h", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                val valid = items.isNotEmpty() &&
                    totalMinutes in 1..720 &&
                    items.all { it.subject.isNotBlank() && it.topic.isNotBlank() && it.minutes in 1..720 }
                Button(
                    onClick = {
                        val finalItems = items.map {
                            StudyPlanItem(it.subject.trim(), it.topic.trim(), it.minutes.coerceIn(1, 720))
                        }
                        val first = finalItems.first()
                        viewModel.startNewPlan(
                            title = when (mode.lowercase()) {
                                "exam" -> "Exam Prep: ${first.subject}"
                                "cram" -> "Next Day Cram: ${first.subject}"
                                else -> "${first.subject}: ${first.topic}"
                            },
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
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("btn_start_study_plan"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Plan • $totalMinutes min", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val finalItems = items.map {
                            StudyPlanItem(it.subject.trim(), it.topic.trim(), it.minutes.coerceIn(1, 720))
                        }
                        val first = finalItems.first()
                        viewModel.saveDraftPlan(
                            title = "${first.subject}: ${first.topic} (Draft)",
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
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Draft")
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    "Maximum total study time: 720 minutes (12 hours). Each subject can contain multiple topics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
