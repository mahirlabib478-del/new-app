package com.aistudio.studyos.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.service.AmbientSoundManager
import com.aistudio.studyos.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    viewModel: StudyViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.focusState.collectAsState()
    val primaryColor = if (state.isBreak) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
    var showEndDialog by remember { mutableStateOf(false) }
    var showAmbientDialog by remember { mutableStateOf(false) }
    var ambientPreset by remember { mutableStateOf(AmbientSoundManager.Preset.RAIN) }
    var ambientVolume by remember { mutableStateOf(0.35f) }
    var ambientPlaying by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity

    // 🌙 Auto screen-awake: Keep screen awake while session timer is running, clear when paused or exited
    DisposableEffect(state.isRunning, state.isSessionCompleted) {
        val window = activity?.window
        val shouldKeepAwake = state.isRunning && !state.isSessionCompleted
        if (shouldKeepAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(state.isSessionCompleted) {
        if (state.isSessionCompleted) {
            AmbientSoundManager.stop()
        }
    }

    var isDismissingAfterCompletion by remember { mutableStateOf(false) }

    val cachedMinutes = remember(state.completedMinutes) {
        if (state.completedMinutes > 0) state.completedMinutes else null
    }
    val cachedBlocks = remember(state.completedBlocks) {
        if (state.completedBlocks > 0) state.completedBlocks else null
    }

    if (state.isSessionCompleted || isDismissingAfterCompletion) {
        StudySessionCompleteScreen(
            completedMinutes = cachedMinutes ?: state.completedMinutes,
            completedBlocks = (cachedBlocks ?: state.completedBlocks).coerceAtMost(state.totalBlocks.coerceAtLeast(1)),
            onDone = {
                if (!isDismissingAfterCompletion) {
                    isDismissingAfterCompletion = true
                    viewModel.dismissSessionCompletion()
                    onBack()
                }
            }
        )
        return
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End Study Session?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Your elapsed study minutes will be saved to your progress and stats.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndDialog = false
                        viewModel.finishActiveSessionEarly()
                        AmbientSoundManager.stop()
                    }
                ) {
                    Text("End Session", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("Keep Studying")
                }
            }
        )
    }

    if (showAmbientDialog) {
        AmbientSoundConfigDialog(
            preset = ambientPreset,
            volume = ambientVolume,
            isPlaying = ambientPlaying,
            onPresetChange = {
                ambientPreset = it
                if (ambientPlaying) {
                    AmbientSoundManager.play(it, ambientVolume)
                }
            },
            onVolumeChange = {
                ambientVolume = it
                AmbientSoundManager.setVolume(it)
            },
            onToggle = {
                if (ambientPlaying) {
                    AmbientSoundManager.stop()
                    ambientPlaying = false
                } else {
                    AmbientSoundManager.play(ambientPreset, ambientVolume)
                    ambientPlaying = true
                }
            },
            onDismiss = { showAmbientDialog = false }
        )
    }

    Scaffold(
        topBar = {
            FocusTopBar(
                isRunning = state.isRunning,
                onBack = {
                    AmbientSoundManager.stop()
                    onBack()
                },
                onEndSession = { showEndDialog = true }
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
            Spacer(modifier = Modifier.height(4.dp))

            if (state.sessionError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Study session needs attention",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.sessionError.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = onBack) {
                            Text("Back")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 🎯 Current Topic Indicator (e.g. English → Grammar, Topic 2 of 5)
            CurrentTopicIndicator(
                subject = state.currentSubject,
                topic = state.currentChapter,
                isBreak = state.isBreak,
                currentBlockIndex = state.currentBlockIndex,
                totalBlocks = state.totalBlocks,
                primaryColor = primaryColor
            )

            Spacer(modifier = Modifier.weight(1f))

            // 📊 Large Hero Circular Timer with smooth progress & % completed
            CircularTimerDisplay(
                secondsRemaining = state.secondsRemaining,
                totalBlockSeconds = state.totalBlockSeconds,
                isRunning = state.isRunning,
                primaryColor = primaryColor
            )

            Spacer(modifier = Modifier.weight(1f))

            // 🎵 Compact Ambient Sound Bar (Sleek, uncluttered, easily reachable)
            CompactAmbientSoundBar(
                preset = ambientPreset,
                isPlaying = ambientPlaying,
                onClick = { showAmbientDialog = true },
                onToggle = {
                    if (ambientPlaying) {
                        AmbientSoundManager.stop()
                        ambientPlaying = false
                    } else {
                        AmbientSoundManager.play(ambientPreset, ambientVolume)
                        ambientPlaying = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ⏱️ Timer Controls (Reset, Big Play/Pause, Skip)
            FocusTimerControls(
                isRunning = state.isRunning,
                primaryColor = primaryColor,
                onReset = { viewModel.resetBlockTimer() },
                onToggle = { viewModel.toggleTimer() },
                onSkip = { viewModel.skipCurrentBlock() }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusTopBar(
    isRunning: Boolean,
    onBack: () -> Unit,
    onEndSession: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant)
                )
                Text(
                    text = if (isRunning) "Active Session" else "Session Paused",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Minimize and return home")
            }
        },
        actions = {
            TextButton(onClick = onEndSession) {
                Text(
                    text = "End",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

/**
 * 🎯 Clean Current Topic Indicator
 * Displays "English → Grammar" and replaces the old "Block X of Y" with "Topic X of Y".
 */
@Composable
private fun CurrentTopicIndicator(
    subject: String,
    topic: String,
    isBreak: Boolean,
    currentBlockIndex: Int,
    totalBlocks: Int,
    primaryColor: Color
) {
    val topicDisplay = remember(subject, topic) {
        val cleanSubject = subject.trim()
        val cleanTopic = topic.trim()
        when {
            cleanTopic.isNotBlank() && !cleanTopic.equals(cleanSubject, ignoreCase = true) -> {
                "$cleanSubject → $cleanTopic"
            }
            cleanSubject.isNotBlank() -> cleanSubject
            else -> "Focus Session"
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Phase Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(primaryColor.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (isBreak) "☕ BREAK TIME" else "🎯 FOCUS INTERVAL",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = primaryColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prominent Subject → Topic
        Text(
            text = topicDisplay,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Topic X of Y
        Text(
            text = "Topic ${currentBlockIndex + 1} of $totalBlocks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Visual Topic Progress Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            repeat(totalBlocks) { index ->
                val isCompleted = index < currentBlockIndex
                val isCurrent = index == currentBlockIndex
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(if (isCurrent) 24.dp else 14.dp)
                        .clip(RoundedCornerShape(2.dp))
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

/**
 * 📊 Large Hero Circular Timer Visualizer with smooth continuous arc sweep
 * and percentage completed display.
 */
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

    val targetProgress = remember(secondsRemaining, totalBlockSeconds) {
        if (totalBlockSeconds > 0) {
            ((totalBlockSeconds - secondsRemaining).toFloat() / totalBlockSeconds).coerceIn(0f, 1f)
        } else 0f
    }

    // Smooth continuous animation for circular progress arc
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "smooth_timer_progress"
    )

    val percentCompleted = remember(targetProgress) {
        (targetProgress * 100).toInt().coerceIn(0, 100)
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .size(292.dp)
            .testTag("focus_timer_circle"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val strokeWidth = 15.dp.toPx()
                    val backgroundStroke = Stroke(width = strokeWidth)
                    val activeStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

                    onDrawBehind {
                        // Background track
                        drawCircle(
                            color = trackColor,
                            style = backgroundStroke
                        )
                        // Smooth active progress sweep
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            style = activeStroke
                        )
                    }
                }
        ) {}

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeFormatted,
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-1.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 📊 42% completed indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(primaryColor.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$percentCompleted% completed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isRunning) "Focusing" else "Paused",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 🎵 Compact Ambient Sound Bar
 * Keeps the focus screen clean without giant vertical cards.
 */
@Composable
private fun CompactAmbientSoundBar(
    preset: AmbientSoundManager.Preset,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ambient_sound_card")
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.GraphicEq else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "Ambient Sound",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPlaying) preset.label else "None (Tap to choose)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            TextButton(
                onClick = onToggle,
                modifier = Modifier.testTag("btn_toggle_ambient")
            ) {
                Text(
                    text = if (isPlaying) "Stop" else "Play",
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 🎛️ Ambient Sound Configuration Dialog
 */
@Composable
private fun AmbientSoundConfigDialog(
    preset: AmbientSoundManager.Preset,
    volume: Float,
    isPlaying: Boolean,
    onPresetChange: (AmbientSoundManager.Preset) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ambient Sound", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Background audio to block distractions:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AmbientSoundManager.Preset.values().forEach { option ->
                        val isSelected = option == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onPresetChange(option) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.label.substringBefore(" "),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Volume", style = MaterialTheme.typography.labelMedium)
                    Text("${(volume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                }

                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ambient_volume_slider")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onToggle()
                }
            ) {
                Text(if (isPlaying) "Stop Sound" else "Play Sound", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

/**
 * ⏱️ Timer Controls
 */
@Composable
private fun FocusTimerControls(
    isRunning: Boolean,
    primaryColor: Color,
    onReset: () -> Unit,
    onToggle: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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

/**
 * 🏆 Tiny Completion Celebration Screen
 * Plays a smooth spring bounce animation, celebratory +XP badge, and study recap.
 */
@Composable
private fun StudySessionCompleteScreen(
    completedMinutes: Int,
    completedBlocks: Int,
    onDone: () -> Unit
) {
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    val iconScale by animateFloatAsState(
        targetValue = if (animationTriggered) 1.0f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "celebration_icon_scale"
    )

    val earnedXP = remember(completedMinutes) {
        (completedMinutes * 3).coerceAtLeast(15)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        // Celebratory animated trophy / checkmark badge
        Box(
            modifier = Modifier
                .scale(iconScale)
                .size(112.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFF59E0B).copy(alpha = 0.25f),
                            Color(0xFFF59E0B).copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = "Celebration",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(68.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "Study Session Complete! 🎉",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Great dedication! You crushed this study plan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))

        // 🏆 Prominent +XP Celebration Badge
        Box(
            modifier = Modifier
                .scale(iconScale)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFF59E0B),
                            Color(0xFFD97706)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "✨",
                    fontSize = 18.sp
                )
                Text(
                    text = "+$earnedXP XP EARNED",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Recap Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = completedMinutes.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "minutes studied",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = completedBlocks.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "topics completed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        androidx.compose.material3.Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
}
