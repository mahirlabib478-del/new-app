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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.ui.viewmodel.StudyViewModel

@Composable
fun StudyHubScreen(
    viewModel: StudyViewModel,
    onOpenRegularStudy: () -> Unit,
    onOpenExamPlanner: () -> Unit,
    onOpenSavedSessions: () -> Unit,
    onOpenFocus: () -> Unit
) {
    val savedPlans by viewModel.savedPlans.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val exams by viewModel.exams.collectAsState()

    var showCramDialog by remember { mutableStateOf(false) }

    if (showCramDialog) {
        var cramSubject by remember {
            mutableStateOf(
                exams.firstOrNull { it.daysRemaining <= 2 && !it.isCompleted }?.subject
                    ?: exams.firstOrNull { !it.isCompleted }?.subject
                    ?: "Mathematics"
            )
        }
        var cramTopic by remember { mutableStateOf("Formulas & High-Yield Problems") }
        var cramBlocks by remember { mutableIntStateOf(3) }
        var cramMinutes by remember { mutableIntStateOf(20) }

        AlertDialog(
            onDismissRequest = { showCramDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Next Day Exam Cram", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Rapid blitz session designed for maximum retention right before an exam.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Urgent exam recommendations if available
                    val urgentExams = exams.filter { !it.isCompleted }
                    if (urgentExams.isNotEmpty()) {
                        Text(
                            text = "Select Upcoming Exam:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            urgentExams.take(3).forEach { exam ->
                                val isSelected = cramSubject == exam.subject
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        cramSubject = exam.subject
                                        if (exam.syllabusTopics.isNotBlank()) {
                                            cramTopic = exam.syllabusTopics
                                        }
                                    },
                                    label = {
                                        Text(
                                            "${exam.subject} (${exam.daysRemaining}d)",
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                        selectedLabelColor = Color(0xFFD97706)
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = cramSubject,
                        onValueChange = { cramSubject = it },
                        label = { Text("Exam Subject") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cram_subject"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = cramTopic,
                        onValueChange = { cramTopic = it },
                        label = { Text("High-Yield Topics / Formulas") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cram_topic"),
                        singleLine = true
                    )

                    Text(
                        text = "Cram Sprint Format:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Preset 1: Blitz 20m x 3
                        FilterChip(
                            selected = cramMinutes == 20 && cramBlocks == 3,
                            onClick = {
                                cramMinutes = 20
                                cramBlocks = 3
                            },
                            label = { Text("20m × 3 blocks", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        // Preset 2: Formula 15m x 4
                        FilterChip(
                            selected = cramMinutes == 15 && cramBlocks == 4,
                            onClick = {
                                cramMinutes = 15
                                cramBlocks = 4
                            },
                            label = { Text("15m × 4 blocks", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        // Preset 3: Deep 30m x 2
                        FilterChip(
                            selected = cramMinutes == 30 && cramBlocks == 2,
                            onClick = {
                                cramMinutes = 30
                                cramBlocks = 2
                            },
                            label = { Text("30m × 2 blocks", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                showCramDialog = false
                                onOpenExamPlanner()
                            }
                        ) {
                            Text("Open Exam Planner", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sub = cramSubject.ifBlank { "Exam Cram" }
                        val top = cramTopic.ifBlank { "High-Yield Topics" }
                        viewModel.startNewPlan(
                            title = "Next Day Cram: $sub",
                            subject = sub,
                            chapter = top,
                            mode = "cram",
                            totalBlocks = cramBlocks,
                            blockMinutes = cramMinutes,
                            breakMinutes = 3,
                            autoStart = true
                        )
                        showCramDialog = false
                        onOpenFocus()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B)
                    ),
                    modifier = Modifier.testTag("btn_confirm_start_cram")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Cram Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val sub = cramSubject.ifBlank { "Exam Cram" }
                        val top = cramTopic.ifBlank { "High-Yield Topics" }
                        viewModel.saveDraftPlan(
                            title = "Next Day Cram: $sub (Draft)",
                            subject = sub,
                            chapter = top,
                            mode = "cram",
                            totalBlocks = cramBlocks,
                            blockMinutes = cramMinutes,
                            breakMinutes = 3
                        )
                        showCramDialog = false
                    },
                    modifier = Modifier.testTag("btn_save_cram_draft")
                ) {
                    Text("Save Draft")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Study Hub",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Pick a path and get straight into studying",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active Session Shortcut if available
        if (activePlan != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.continueActiveSession(activePlan!!)
                            onOpenFocus()
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Resume Current Plan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${activePlan?.subject} • Block ${(activePlan?.currentBlockIndex ?: 0) + 1}/${activePlan?.totalBlocks}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Saved Sessions Card (Always visible and accessible)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_sessions_card")
                    .clickable { onOpenSavedSessions() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Saved Sessions",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saved Sessions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (savedPlans.isNotEmpty())
                                "${savedPlans.size} session(s) waiting • Tap to resume"
                            else
                                "No drafts saved • Tap to manage or view drafts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Study Modes",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Mode 1: Regular Study
        item {
            StudyModeCard(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "Regular Study",
                subtitle = "Plan subjects, chapters, and structured Pomodoro focus blocks.",
                iconColor = MaterialTheme.colorScheme.primary,
                testTag = "mode_regular_study",
                onClick = onOpenRegularStudy
            )
        }

        // Mode 2: Exam Preparation
        item {
            StudyModeCard(
                icon = Icons.Default.AutoAwesome,
                title = "Exam Preparation",
                subtitle = "Priority-based syllabus planner, revision tracking, and exam countdowns.",
                iconColor = Color(0xFF6366F1),
                testTag = "mode_exam_prep",
                onClick = onOpenExamPlanner
            )
        }

        // Mode 3: Next Day Cram
        item {
            StudyModeCard(
                icon = Icons.Default.Bolt,
                title = "Next Day Exam",
                subtitle = "Fast revision cram mode: brings urgent high-priority topics forward.",
                iconColor = Color(0xFFF59E0B),
                testTag = "mode_next_day_exam",
                onClick = { showCramDialog = true }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StudyModeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
