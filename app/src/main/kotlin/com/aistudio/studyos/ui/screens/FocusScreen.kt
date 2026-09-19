package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun FocusScreen(
    viewModel: StudyViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.focusState.collectAsState()
    val primaryColor = if (state.isBreak) Color(0xFF10B981) else MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            FocusTopBar(
                subject = state.currentSubject,
                chapter = state.currentChapter,
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Mode & Block indicator (only updates when block/break changes)
            FocusBlockIndicator(
                isBreak = state.isBreak,
                currentBlockIndex = state.currentBlockIndex,
                totalBlocks = state.totalBlocks,
                primaryColor = primaryColor
            )

            Spacer(modifier = Modifier.weight(1f))

            // Circular Focus Timer Visualizer (highly optimized draw phase, no GC churn)
            CircularTimerDisplay(
                secondsRemaining = state.secondsRemaining,
                totalBlockSeconds = state.totalBlockSeconds,
                isRunning = state.isRunning,
                primaryColor = primaryColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Session Info Card (only updates when interval, phase or running state changes)
            FocusSessionInfoCard(
                totalBlockSeconds = state.totalBlockSeconds,
                isBreak = state.isBreak,
                isRunning = state.isRunning,
                primaryColor = primaryColor
            )

            Spacer(modifier = Modifier.weight(1f))

            // Timer Controls (only updates when isRunning changes)
            FocusTimerControls(
                isRunning = state.isRunning,
                primaryColor = primaryColor,
                onReset = { viewModel.resetBlockTimer() },
                onToggle = { viewModel.toggleTimer() },
                onSkip = { viewModel.skipCurrentBlock() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusTopBar(
    subject: String,
    chapter: String,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = subject,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = chapter,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun FocusBlockIndicator(
    isBreak: Boolean,
    currentBlockIndex: Int,
    totalBlocks: Int,
    primaryColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(primaryColor.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isBreak) "BREAK TIME" else "FOCUS INTERVAL",
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = primaryColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Block ${currentBlockIndex + 1} of $totalBlocks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Visual Block Progress Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            repeat(totalBlocks) { index ->
                val isCompleted = index < currentBlockIndex
                val isCurrent = index == currentBlockIndex
                Box(
                    modifier = Modifier
                        .height(5.dp)
                        .width(if (isCurrent) 28.dp else 16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when {
                                isCompleted -> primaryColor
                                isCurrent -> primaryColor.copy(alpha = 0.9f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun CircularTimerDisplay(
    secondsRemaining: Int,
    totalBlockSeconds: Int,
    isRunning: Boolean,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val mins = secondsRemaining / 60
    val secs = secondsRemaining % 60
    val timeFormatted = remember(mins, secs) {
        val m = if (mins < 10) "0$mins" else "$mins"
        val s = if (secs < 10) "0$secs" else "$secs"
        "$m:$s"
    }

    val progress = remember(secondsRemaining, totalBlockSeconds) {
        if (totalBlockSeconds > 0) {
            ((totalBlockSeconds - secondsRemaining).toFloat() / totalBlockSeconds).coerceIn(0f, 1f)
        } else 0f
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .size(260.dp)
            .testTag("focus_timer_circle"),
        contentAlignment = Alignment.Center
    ) {
        // High-performance Canvas using drawWithCache to reuse Stroke objects and eliminate allocations
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val strokeWidth = 14.dp.toPx()
                    val backgroundStroke = Stroke(width = strokeWidth)
                    val activeStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

                    onDrawBehind {
                        // Background track
                        drawCircle(
                            color = trackColor,
                            style = backgroundStroke
                        )
                        // Active progress sweep
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = activeStroke
                        )
                    }
                }
        ) {}

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeFormatted,
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-1).sp
            )
            Text(
                text = if (isRunning) "In Progress" else "Paused",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FocusSessionInfoCard(
    totalBlockSeconds: Int,
    isBreak: Boolean,
    isRunning: Boolean,
    primaryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Interval",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${totalBlockSeconds / 60} min",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Next Phase",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isBreak) "Focus Interval" else "Short Break",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isRunning) "Active" else "Paused",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRunning) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FocusTimerControls(
    isRunning: Boolean,
    primaryColor: Color,
    onReset: () -> Unit,
    onToggle: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            onClick = onReset,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "Restart Block",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        FilledIconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(76.dp)
                .testTag("btn_toggle_focus_timer"),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = primaryColor
            )
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(38.dp)
            )
        }

        FilledIconButton(
            onClick = onSkip,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Skip Block",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
