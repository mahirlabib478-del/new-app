package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPlannerScreen(
    viewModel: StudyViewModel,
    onBack: () -> Unit,
    onStartStudySetup: (com.aistudio.studyos.data.local.entity.ExamEntity) -> Unit
) {
    val exams by viewModel.exams.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var examToDelete by remember { mutableStateOf<com.aistudio.studyos.data.local.entity.ExamEntity?>(null) }

    if (examToDelete != null) {
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            title = { Text("Delete Exam?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete ${examToDelete?.subject}? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        examToDelete?.let { viewModel.deleteExam(it) }
                        examToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { examToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exam Preparation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("fab_add_exam"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Exam")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Upcoming Exams & Tests",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track dates, confidence levels, and start targeted cram sessions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (exams.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No exams scheduled",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap the + button below to add your next test or midterm!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = exams,
                    key = { it.id }
                ) { exam ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exam_card_${exam.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (exam.isCompleted)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleExamCompleted(exam) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (exam.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Toggle Complete",
                                            tint = if (exam.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = exam.subject,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Due: ${exam.examDate} (${exam.daysRemaining}d remaining)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (exam.daysRemaining <= 1) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = exam.priority,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (exam.priority) {
                                                "High" -> Color(0xFFEF4444)
                                                "Medium" -> Color(0xFFF59E0B)
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }
                                    IconButton(
                                        onClick = { examToDelete = exam },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Exam",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (exam.syllabusTopics.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Topics: ${exam.syllabusTopics}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Confidence & Start Exam Prep CTA
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Confidence: ${exam.confidenceLevel}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Button(
                                    onClick = { onStartStudySetup(exam) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Create Study Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddDialog) {
        var subjectName by remember { mutableStateOf("") }
        var examDate by remember { mutableStateOf("In 3 days") }
        var daysLeft by remember { mutableIntStateOf(3) }
        var priority by remember { mutableStateOf("High") }
        var syllabus by remember { mutableStateOf("") }
        var confidence by remember { mutableFloatStateOf(50f) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Upcoming Exam", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject (e.g. Physics)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = examDate,
                        onValueChange = { examDate = it },
                        label = { Text("Date (e.g. Tomorrow, Monday)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = syllabus,
                        onValueChange = { syllabus = it },
                        label = { Text("Key Topics / Syllabus") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        text = "Days Remaining: $daysLeft ${if (daysLeft == 1) "day" else "days"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3, 5, 7, 14).forEach { d ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        daysLeft = d
                                        if (examDate == "In 3 days" || examDate.startsWith("In ")) {
                                            examDate = if (d == 1) "Tomorrow" else "In $d days"
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (daysLeft == d)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${d}d",
                                        fontSize = 11.sp,
                                        fontWeight = if (daysLeft == d) FontWeight.Bold else FontWeight.Normal,
                                        color = if (daysLeft == d) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("High", "Medium", "Normal").forEach { p ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { priority = p },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (priority == p)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p,
                                        fontSize = 12.sp,
                                        fontWeight = if (priority == p) FontWeight.Bold else FontWeight.Normal,
                                        color = if (priority == p) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Preparedness Confidence: ${confidence.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = confidence,
                        onValueChange = { confidence = it },
                        valueRange = 0f..100f
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subjectName.isNotBlank()) {
                            viewModel.addExam(
                                subject = subjectName,
                                examDate = examDate.ifBlank { "Soon" },
                                daysRemaining = daysLeft,
                                priority = priority,
                                topics = syllabus,
                                confidence = confidence.toInt()
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add Exam")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
