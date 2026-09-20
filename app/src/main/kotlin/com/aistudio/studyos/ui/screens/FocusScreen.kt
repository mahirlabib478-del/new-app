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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(viewModel: StudyViewModel, onBack: () -> Unit) {
    val state by viewModel.focusState.collectAsState()
    var showEndDialog by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }

    if (state.isSessionCompleted) {
        SessionCompleteScreen(
            completedMinutes = state.completedMinutes,
            totalBlocks = state.totalBlocks,
            completedBlocks = state.completedBlocks,
            actualStudiedSeconds = state.actualStudiedSeconds,
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
            text = { Text("Your completed study time will be saved and recorded to your progress.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndDialog = false
                        viewModel.finishActiveSessionEarly()
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

    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("Skip Options", fontWeight = FontWeight.Bold) },
            text = { Text("You can skip to the next rest break or finish your entire study session right now.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSkipDialog = false
                        viewModel.finishActiveSessionEarly()
                    }
                ) {
                    Text("Finish Session", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showSkipDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            showSkipDialog = false
                            viewModel.skipCurrentBlock()
                        }
                    ) {
                        Text("Next Block", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (state.isBreak) "Break & Recovery" else state.currentSubject,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            if (state.isBreak) "Take a moment to rest" else state.currentChapter,
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
            Spacer(Modifier.height(2.dp))

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = accent.copy(alpha = .10f)
            ) {
                Row(
                    Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (state.isBreak) Icons.Default.Coffee else Icons.Default.Timer,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.isBreak) "BREAK • ${state.breakBlockSeconds / 60} MIN" else "FOCUS • 25 MIN MAX",
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = .7.sp
                    )
                }
            }

            Spacer(Modifier.height(5.dp))

            Text(
                "Block ${minOf(state.currentBlockIndex + 1, state.totalBlocks)} of ${state.totalBlocks}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(state.totalBlocks.coerceAtMost(12)) { index ->
                    Box(
                        Modifier
                            .height(4.dp)
                            .width(if (index == state.currentBlockIndex) 24.dp else 9.dp)
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

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularTimerDisplay(
                    seconds = state.secondsRemaining,
                    total = state.totalBlockSeconds,
                    running = state.isRunning,
                    accent = accent
                )

                Spacer(Modifier.height(10.dp))

                if (state.isBreak) {
                    BreakRechargeView(accent = accent, running = state.isRunning)
                } else {
                    FocusInfoCard(
                        subject = state.currentSubject,
                        topic = state.currentChapter,
                        minutes = state.totalBlockSeconds / 60,
                        accent = accent
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { viewModel.resetBlockTimer() },
                    modifier = Modifier.size(48.dp).testTag("btn_reset_focus_block"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Replay, "Restart block")
                }

                FilledIconButton(
                    onClick = { viewModel.toggleTimer() },
                    modifier = Modifier.size(74.dp).testTag("btn_toggle_focus_timer"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent)
                ) {
                    Icon(
                        if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (state.isRunning) "Pause" else "Start",
                        Modifier.size(33.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                FilledIconButton(
                    onClick = {
                        if (state.isBreak || state.currentBlockIndex >= state.totalBlocks - 1 || state.totalBlocks <= 1) {
                            viewModel.skipCurrentBlock()
                        } else {
                            showSkipDialog = true
                        }
                    },
                    modifier = Modifier.size(48.dp).testTag("btn_skip_focus_block"),
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
    val clampedSeconds = seconds.coerceIn(0, total.coerceAtLeast(1))
    val progress = if (total > 0) {
        ((total - clampedSeconds).toFloat() / total).coerceIn(0f, 1f)
    } else 0f
    val mins = clampedSeconds / 60
    val secs = clampedSeconds % 60
    val text = "%02d:%02d".format(mins, secs)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        Modifier.size(212.dp).testTag("focus_timer_circle"),
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
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
            Text(
                if (running) "Stay focused" else "Paused",
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
            Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
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

/**
 * Minimalist, elegant cardless break view.
 * Strictly no cards or bordered containers. Clean typography and serene breathing layout.
 */
@Composable
private fun BreakRechargeView(accent: Color, running: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.SelfImprovement,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Mindful Rest & Recovery",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = "Step away from the screen • Rest your eyes • Drink water",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            BreakActionPill(icon = Icons.Default.SelfImprovement, label = "Relax", tint = accent)
            BreakActionPill(icon = Icons.Default.WaterDrop, label = "Hydrate", tint = accent)
            BreakActionPill(icon = Icons.Default.Timer, label = "Stretch", tint = accent)
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = if (running) "Next focus block begins automatically when break ends" else "Break timer paused",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = accent.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BreakActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionCompleteScreen(
    completedMinutes: Int,
    totalBlocks: Int,
    completedBlocks: Int,
    actualStudiedSeconds: Int,
    onDone: () -> Unit
) {
    val formattedTime = when {
        actualStudiedSeconds <= 0 && completedMinutes <= 0 -> "0s"
        actualStudiedSeconds in 1..59 -> "${actualStudiedSeconds}s"
        actualStudiedSeconds % 60 == 0 -> "${actualStudiedSeconds / 60}m"
        else -> "${actualStudiedSeconds / 60}m ${actualStudiedSeconds % 60}s"
    }
    val effectiveMinutes = if (actualStudiedSeconds >= 30) (actualStudiedSeconds + 29) / 60 else if (actualStudiedSeconds > 0) 1 else completedMinutes
    val xpGained = maxOf(effectiveMinutes * 3, if (actualStudiedSeconds > 0) 3 else 0)

    Scaffold { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
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
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .15f)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(18.dp)
                                .size(58.dp)
                        )
                    }

                    Text(
                        "🎉 Congratulations!",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = if (actualStudiedSeconds > 0 || completedMinutes > 0)
                            "Study session finished! Your time and progress have been recorded."
                        else
                            "Session finished! Ready for your next focus round.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CompletionMetric(formattedTime, "Time Studied")
                        CompletionMetric("${completedBlocks.coerceAtMost(totalBlocks)}/$totalBlocks", "Blocks")
                        CompletionMetric("+$xpGained", "XP Earned")
                    }

                    Text(
                        "Great work! Every minute counts towards building your study habit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_session_done"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
