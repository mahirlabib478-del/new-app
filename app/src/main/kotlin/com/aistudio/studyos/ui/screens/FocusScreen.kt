package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(viewModel: StudyViewModel, onBack: () -> Unit) {
    val state by viewModel.focusState.collectAsState()
    var showEndDialog by remember { mutableStateOf(false) }

    if (state.isSessionCompleted) {
        SessionCompleteScreen(
            completedMinutes = state.completedMinutes,
            totalBlocks = state.totalBlocks,
            onDone = {
                viewModel.dismissSessionCompletion()
                onBack()
            }
        )
        return
    }

    val accent = if (state.isBreak) Color(0xFF10B981) else MaterialTheme.colorScheme.primary

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End Study Session?", fontWeight = FontWeight.Bold) },
            text = { Text("Your completed focus time will be saved to Progress & Analytics.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndDialog = false
                        viewModel.finishActiveSessionEarly()
                        onBack()
                    }
                ) {
                    Text("End Session", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) { Text("Keep Studying") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (state.isBreak) "Recovery Break" else state.currentSubject,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            if (state.isBreak) "Rest before the next focus block" else state.currentChapter,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Minimize session")
                    }
                },
                actions = {
                    TextButton(onClick = { showEndDialog = true }) {
                        Text("End", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = accent.copy(alpha = .12f)
            ) {
                Row(
                    Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (state.isBreak) Icons.Default.Coffee else Icons.Default.Timer,
                        null,
                        tint = accent,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (state.isBreak) "BREAK • ${state.breakBlockSeconds / 60} MIN" else "FOCUS • MAX 25 MIN",
                        color = accent,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Block ${minOf(state.currentBlockIndex + 1, state.totalBlocks)} of ${state.totalBlocks}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(7.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(state.totalBlocks.coerceAtMost(12)) { index ->
                    Box(
                        Modifier
                            .height(5.dp)
                            .width(if (index == state.currentBlockIndex) 27.dp else 11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    index < state.currentBlockIndex -> accent
                                    index == state.currentBlockIndex -> accent.copy(alpha = .9f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                    )
                }
            }

            Spacer(Modifier.height(15.dp))

            CircularTimerDisplay(
                seconds = state.secondsRemaining,
                total = state.totalBlockSeconds,
                running = state.isRunning,
                accent = accent
            )

            Spacer(Modifier.height(14.dp))

            if (state.isBreak) {
                BreakWellnessCard(accent, state.isRunning)
            } else {
                FocusInfoCard(
                    subject = state.currentSubject,
                    topic = state.currentChapter,
                    minutes = state.totalBlockSeconds / 60,
                    accent = accent
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { viewModel.resetBlockTimer() },
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Replay, "Restart block")
                }

                FilledIconButton(
                    onClick = { viewModel.toggleTimer() },
                    modifier = Modifier.size(76.dp).testTag("btn_toggle_focus_timer"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent)
                ) {
                    Icon(
                        if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (state.isRunning) "Pause" else "Start",
                        Modifier.size(35.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                FilledIconButton(
                    onClick = { viewModel.skipCurrentBlock() },
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.SkipNext, "Skip block")
                }
            }
        }
    }
}

@Composable
private fun CircularTimerDisplay(
    seconds: Int,
    total: Int,
    running: Boolean,
    accent: Color
) {
    val progress = if (total > 0) {
        ((total - seconds).toFloat() / total).coerceIn(0f, 1f)
    } else 0f
    val mins = seconds / 60
    val secs = seconds % 60
    val text = "%02d:%02d".format(mins, secs)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        Modifier.size(230.dp).testTag("focus_timer_circle"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            Modifier.fillMaxSize().drawWithCache {
                val stroke = 13.dp.toPx()
                val bg = Stroke(stroke)
                val fg = Stroke(stroke, cap = StrokeCap.Round)
                onDrawBehind {
                    drawCircle(trackColor, style = bg)
                    drawArc(accent, -90f, progress * 360f, false, style = fg)
                }
            }
        ) {}

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text,
                fontSize = 50.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
            Text(
                if (running) "In progress" else "Paused",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FocusInfoCard(
    subject: String,
    topic: String,
    minutes: Int,
    accent: Color
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
        )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = accent.copy(alpha = .12f)) {
                Icon(
                    Icons.Default.Timer,
                    null,
                    tint = accent,
                    modifier = Modifier.padding(9.dp).size(20.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(subject, fontWeight = FontWeight.Bold)
                Text(
                    topic,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("${minutes}m", fontWeight = FontWeight.ExtraBold, color = accent)
        }
    }
}

@Composable
private fun BreakWellnessCard(accent: Color, running: Boolean) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .72f)
        )
    ) {
        Column(
            Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Coffee, null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Recovery time", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Step away from the screen for a few minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                WellnessTip(Modifier.weight(1f), Icons.Default.SelfImprovement, "Rest", "Look away")
                WellnessTip(Modifier.weight(1f), Icons.Default.WaterDrop, "Hydrate", "Drink water")
                WellnessTip(Modifier.weight(1f), Icons.Default.Timer, "Move", "Stretch")
            }

            Text(
                if (running) "Next focus block starts automatically when this break ends." else "Break timer is paused.",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
    }
}

@Composable
private fun WellnessTip(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .55f)
    ) {
        Column(
            Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(3.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionCompleteScreen(
    completedMinutes: Int,
    totalBlocks: Int,
    onDone: () -> Unit
) {
    Scaffold {
        Box(
            Modifier.fillMaxSize().padding(it).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(18.dp).size(58.dp)
                        )
                    }
                    Text("Study session complete!", fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text(
                        "You finished the entire plan.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CompletionMetric("${completedMinutes}m", "Focused")
                        CompletionMetric(totalBlocks.toString(), "Blocks")
                    }

                    Text(
                        "Great work. Take a moment before starting another session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 23.sp, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
