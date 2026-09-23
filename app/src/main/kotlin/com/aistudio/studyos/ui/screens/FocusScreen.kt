package com.aistudio.studyos.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.aistudio.studyos.ui.theme.isLightPreset
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Water
import com.aistudio.studyos.ui.components.DynamicStudyWallpaper
import com.aistudio.studyos.ui.components.WallpaperStyle
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
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.aistudio.studyos.service.AudioFileManager
import com.aistudio.studyos.data.local.UploadedAudio
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
    val context = LocalContext.current
    val activity = context as? Activity

    val savedCustomAudioUri by viewModel.customAudioUri.collectAsState()
    val savedCustomAudioName by viewModel.customAudioName.collectAsState()
    val customAudioList by viewModel.customAudioList.collectAsState()
    val selectedAudioId by viewModel.selectedAudioId.collectAsState()

    var ambientPreset by remember { mutableStateOf(AmbientSoundManager.getCurrentPreset()) }
    var ambientVolume by remember { mutableStateOf(AmbientSoundManager.getAmbientVolume()) }
    var isAmbientPlaying by remember { mutableStateOf(AmbientSoundManager.isAmbientPlaying()) }

    var customVolume by remember { mutableStateOf(AmbientSoundManager.getCustomAudioVolume()) }
    var isCustomAudioPlaying by remember { mutableStateOf(AmbientSoundManager.isCustomAudioPlaying()) }

    val coroutineScope = rememberCoroutineScope()

    // Synchronize saved audio into AmbientSoundManager on first load
    LaunchedEffect(savedCustomAudioUri, savedCustomAudioName) {
        if (savedCustomAudioUri != null && AmbientSoundManager.getCustomAudioUri() == null) {
            AmbientSoundManager.setCustomAudio(savedCustomAudioUri, savedCustomAudioName)
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val uploaded = AudioFileManager.copyUriToInternalStorage(context, uri)
                withContext(Dispatchers.Main) {
                    viewModel.addCustomAudio(uploaded.name, uploaded.uri)
                    AmbientSoundManager.setCustomAudio(uploaded.uri, uploaded.name)
                    AmbientSoundManager.playCustomAudio(
                        context = context,
                        uriString = uploaded.uri,
                        displayName = uploaded.name,
                        volume = customVolume
                    )
                    isCustomAudioPlaying = true
                }
            }
        }
    }

    val currentTheme by viewModel.currentTheme.collectAsState()
    val isWallpaperMasterEnabled by viewModel.isWallpaperEnabled.collectAsState()
    val isFocusWallpaperEnabled by viewModel.isFocusWallpaperEnabled.collectAsState()
    val wallpaperOpacity by viewModel.wallpaperOpacity.collectAsState()
    val wallpaperStyleId by viewModel.wallpaperStyle.collectAsState()
    val customWallpaperUri by viewModel.customWallpaperUri.collectAsState()

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
            AmbientSoundManager.stopAll()
            isAmbientPlaying = false
            isCustomAudioPlaying = false
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
                        AmbientSoundManager.stopAll()
                        isAmbientPlaying = false
                        isCustomAudioPlaying = false
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
            ambientVolume = ambientVolume,
            isAmbientPlaying = isAmbientPlaying,
            onAmbientPresetChange = {
                ambientPreset = it
                AmbientSoundManager.setAmbientPreset(it)
                if (isAmbientPlaying) {
                    AmbientSoundManager.playAmbient(it, ambientVolume)
                }
            },
            onAmbientVolumeChange = {
                ambientVolume = it
                AmbientSoundManager.setAmbientVolume(it)
            },
            onToggleAmbient = {
                if (isAmbientPlaying) {
                    AmbientSoundManager.stopAmbient()
                    isAmbientPlaying = false
                } else {
                    AmbientSoundManager.playAmbient(ambientPreset, ambientVolume)
                    isAmbientPlaying = true
                }
            },
            customAudioList = customAudioList,
            selectedAudioId = selectedAudioId,
            selectedAudioUri = savedCustomAudioUri,
            selectedAudioName = savedCustomAudioName,
            customAudioVolume = customVolume,
            isCustomAudioPlaying = isCustomAudioPlaying,
            onCustomAudioVolumeChange = {
                customVolume = it
                AmbientSoundManager.setCustomAudioVolume(it)
            },
            onToggleCustomAudio = {
                if (isCustomAudioPlaying) {
                    AmbientSoundManager.stopCustomAudio()
                    isCustomAudioPlaying = false
                } else {
                    val uri = savedCustomAudioUri
                    if (!uri.isNullOrBlank()) {
                        AmbientSoundManager.playCustomAudio(context, uri, savedCustomAudioName, customVolume)
                        isCustomAudioPlaying = true
                    } else {
                        audioPickerLauncher.launch(arrayOf("audio/*"))
                    }
                }
            },
            onSelectAudio = { audio ->
                viewModel.selectCustomAudio(audio.id)
                AmbientSoundManager.setCustomAudio(audio.uri, audio.name)
                AmbientSoundManager.playCustomAudio(context, audio.uri, audio.name, customVolume)
                isCustomAudioPlaying = true
            },
            onPickCustomAudio = {
                audioPickerLauncher.launch(arrayOf("audio/*"))
            },
            onDeleteAudio = { audio ->
                if (audio.uri == savedCustomAudioUri && isCustomAudioPlaying) {
                    AmbientSoundManager.stopCustomAudio()
                    isCustomAudioPlaying = false
                }
                viewModel.removeCustomAudio(audio.id)
            },
            onStopAll = {
                AmbientSoundManager.stopAll()
                isAmbientPlaying = false
                isCustomAudioPlaying = false
            },
            onDismiss = { showAmbientDialog = false }
        )
    }

    val isLight = remember(currentTheme) { isLightPreset(currentTheme) }
    val showWallpaper = !isLight && isWallpaperMasterEnabled && isFocusWallpaperEnabled

    Box(modifier = Modifier.fillMaxSize()) {
        if (showWallpaper) {
            val style = remember(wallpaperStyleId) {
                WallpaperStyle.entries.find { it.id == wallpaperStyleId } ?: WallpaperStyle.CAFE_BOKEH
            }
            DynamicStudyWallpaper(
                style = style,
                themePreset = currentTheme,
                opacity = wallpaperOpacity,
                customUri = customWallpaperUri
            )
        }

        Scaffold(
            containerColor = if (showWallpaper) Color.Transparent else MaterialTheme.colorScheme.background,
            topBar = {
                FocusTopBar(
                    isRunning = state.isRunning,
                    isWallpaperActive = showWallpaper,
                    isWallpaperMasterEnabled = isWallpaperMasterEnabled,
                    isLight = isLight,
                    onToggleWallpaper = { viewModel.toggleFocusWallpaperEnabled() },
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
                primaryColor = primaryColor,
                isWallpaperActive = showWallpaper,
                isLight = isLight
            )

            Spacer(modifier = Modifier.weight(1f))

            // 📊 Large Hero Circular Timer with smooth progress & % completed
            CircularTimerDisplay(
                secondsRemaining = state.secondsRemaining,
                totalBlockSeconds = state.totalBlockSeconds,
                isRunning = state.isRunning,
                primaryColor = primaryColor,
                isWallpaperActive = showWallpaper,
                isLight = isLight
            )

            Spacer(modifier = Modifier.weight(1f))

            // 🎵 Compact Ambient Sound Bar (Sleek, uncluttered, easily reachable)
            CompactAmbientSoundBar(
                preset = ambientPreset,
                isAmbientPlaying = isAmbientPlaying,
                customAudioName = savedCustomAudioName,
                isCustomAudioPlaying = isCustomAudioPlaying,
                isWallpaperActive = showWallpaper,
                isLight = isLight,
                onClick = { showAmbientDialog = true },
                onToggleAmbient = {
                    if (isAmbientPlaying) {
                        AmbientSoundManager.stopAmbient()
                        isAmbientPlaying = false
                    } else {
                        AmbientSoundManager.playAmbient(ambientPreset, ambientVolume)
                        isAmbientPlaying = true
                    }
                },
                onToggleCustomAudio = {
                    if (isCustomAudioPlaying) {
                        AmbientSoundManager.stopCustomAudio()
                        isCustomAudioPlaying = false
                    } else {
                        val uri = savedCustomAudioUri
                        if (!uri.isNullOrBlank()) {
                            AmbientSoundManager.playCustomAudio(context, uri, savedCustomAudioName, customVolume)
                            isCustomAudioPlaying = true
                        } else {
                            showAmbientDialog = true
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ⏱️ Timer Controls (Reset, Big Play/Pause, Skip)
            FocusTimerControls(
                isRunning = state.isRunning,
                primaryColor = primaryColor,
                isWallpaperActive = showWallpaper,
                isLight = isLight,
                onReset = { viewModel.resetBlockTimer() },
                onToggle = { viewModel.toggleTimer() },
                onSkip = { viewModel.skipCurrentBlock() }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusTopBar(
    isRunning: Boolean,
    isWallpaperActive: Boolean,
    isWallpaperMasterEnabled: Boolean,
    isLight: Boolean,
    onToggleWallpaper: () -> Unit,
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
            if (isWallpaperMasterEnabled && !isLight) {
                IconButton(
                    onClick = onToggleWallpaper,
                    modifier = Modifier.testTag("focus_wallpaper_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isWallpaperActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isWallpaperActive) "Hide Wallpaper" else "Show Wallpaper",
                        tint = if (isWallpaperActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onEndSession) {
                Text(
                    text = "End",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
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
    primaryColor: Color,
    isWallpaperActive: Boolean = false,
    isLight: Boolean = false
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
        val pillBgColor = if (isWallpaperActive) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        } else if (isBreak) {
            Color(0xFF10B981).copy(alpha = 0.12f)
        } else {
            primaryColor.copy(alpha = 0.10f)
        }
        val pillTextColor = if (isBreak) {
            if (isLight) Color(0xFF047857) else Color(0xFF34D399)
        } else {
            primaryColor
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(pillBgColor)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isBreak) "☕ BREAK TIME" else "🎯 FOCUS INTERVAL",
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = pillTextColor
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
            fontWeight = FontWeight.SemiBold,
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
                                else -> if (isWallpaperActive) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                                        else if (isLight) MaterialTheme.colorScheme.outlineVariant
                                        else MaterialTheme.colorScheme.surfaceVariant
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
    isWallpaperActive: Boolean = false,
    isLight: Boolean = false,
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

    val trackColor = when {
        isWallpaperActive -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
        isLight -> MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

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
            val timerColor = if (isLight) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
            }

            Text(
                text = timeFormatted,
                fontSize = 54.sp,
                fontWeight = FontWeight.SemiBold,
                color = timerColor,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 📊 42% completed indicator
            val progressBadgeBg = if (isWallpaperActive) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
            } else {
                primaryColor.copy(alpha = 0.10f)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(progressBadgeBg)
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
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 🎵 Compact Ambient Sound & Audio Bar
 * Provides independent quick controls for both Ambient Noise and Uploaded Audio.
 */
@Composable
private fun CompactAmbientSoundBar(
    preset: AmbientSoundManager.Preset,
    isAmbientPlaying: Boolean,
    customAudioName: String?,
    isCustomAudioPlaying: Boolean,
    isWallpaperActive: Boolean = false,
    isLight: Boolean = false,
    onClick: () -> Unit,
    onToggleAmbient: () -> Unit,
    onToggleCustomAudio: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ambient_sound_card")
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isWallpaperActive) Color.Transparent
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isWallpaperActive) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Channel 1: 🌧️ Ambient Noise
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAmbientPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isAmbientPlaying) Icons.Default.GraphicEq else Icons.Default.Water,
                            contentDescription = null,
                            tint = if (isAmbientPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ambient Noise",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isAmbientPlaying) "${preset.label} (Playing)" else preset.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TextButton(
                    onClick = onToggleAmbient,
                    modifier = Modifier.testTag("btn_toggle_ambient")
                ) {
                    Icon(
                        if (isAmbientPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isAmbientPlaying) "Stop Ambient" else "Play Ambient",
                        modifier = Modifier.size(16.dp),
                        tint = if (isAmbientPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAmbientPlaying) "Stop" else "Play",
                        fontWeight = FontWeight.Bold,
                        color = if (isAmbientPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.8.dp
            )

            // Channel 2: 🎧 Uploaded Audio
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCustomAudioPlaying) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isCustomAudioPlaying) Icons.Default.GraphicEq else Icons.Default.AudioFile,
                            contentDescription = null,
                            tint = if (isCustomAudioPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audiobook / Podcast / Music",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when {
                                isCustomAudioPlaying -> "${customAudioName ?: "Custom Audio"} (Playing)"
                                customAudioName != null -> customAudioName
                                else -> "None (Tap to select or upload)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TextButton(
                    onClick = onToggleCustomAudio,
                    modifier = Modifier.testTag("btn_toggle_custom_audio")
                ) {
                    Icon(
                        if (isCustomAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isCustomAudioPlaying) "Stop Audio" else "Play Audio",
                        modifier = Modifier.size(16.dp),
                        tint = if (isCustomAudioPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCustomAudioPlaying) "Stop" else "Play",
                        fontWeight = FontWeight.Bold,
                        color = if (isCustomAudioPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

/**
 * 🎛️ Dual-Channel Focus Sound & Audio Mixer Dialog
 * Allows independent Play/Pause and Volume controls for both Ambient Noise and Uploaded Audio.
 * Both can be played simultaneously!
 */
@Composable
private fun AmbientSoundConfigDialog(
    preset: AmbientSoundManager.Preset,
    ambientVolume: Float,
    isAmbientPlaying: Boolean,
    onAmbientPresetChange: (AmbientSoundManager.Preset) -> Unit,
    onAmbientVolumeChange: (Float) -> Unit,
    onToggleAmbient: () -> Unit,
    customAudioList: List<UploadedAudio>,
    selectedAudioId: String?,
    selectedAudioUri: String?,
    selectedAudioName: String?,
    customAudioVolume: Float,
    isCustomAudioPlaying: Boolean,
    onCustomAudioVolumeChange: (Float) -> Unit,
    onToggleCustomAudio: () -> Unit,
    onSelectAudio: (UploadedAudio) -> Unit,
    onPickCustomAudio: () -> Unit,
    onDeleteAudio: (UploadedAudio) -> Unit,
    onStopAll: () -> Unit,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Sound & Audio Mixer", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ambient noise and your uploaded audio can play simultaneously in the background!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // ==========================================
                // 🌧️ CHANNEL 1: Procedural Ambient Sound
                // ==========================================
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌧️ Ambient Background",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAmbientPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isAmbientPlaying) "Playing" else "Stopped",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAmbientPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Ambient Presets Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            AmbientSoundManager.Preset.RAIN,
                            AmbientSoundManager.Preset.WHITE_NOISE,
                            AmbientSoundManager.Preset.DEEP_FOCUS,
                            AmbientSoundManager.Preset.FOREST_STREAM
                        ).forEach { option ->
                            val isSelected = option == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { onAmbientPresetChange(option) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (option) {
                                        AmbientSoundManager.Preset.RAIN -> "Rain"
                                        AmbientSoundManager.Preset.WHITE_NOISE -> "White"
                                        AmbientSoundManager.Preset.DEEP_FOCUS -> "Focus"
                                        AmbientSoundManager.Preset.FOREST_STREAM -> "Stream"
                                        else -> option.label
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Ambient Controls (Play/Stop Button + Volume Level)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onToggleAmbient,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isAmbientPlaying) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (isAmbientPlaying) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("dialog_btn_ambient_toggle")
                        ) {
                            Icon(
                                if (isAmbientPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isAmbientPlaying) "Stop Ambient" else "Play Ambient")
                        }

                        Text(
                            text = "Volume: ${(ambientVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = ambientVolume,
                            onValueChange = onAmbientVolumeChange,
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ambient_volume_slider")
                        )
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ==========================================
                // 🎧 CHANNEL 2: Uploaded Custom Audio
                // ==========================================
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎧 Uploaded Audio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCustomAudioPlaying) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isCustomAudioPlaying) "Playing" else "Stopped",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCustomAudioPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Custom Audio Controls (Play/Stop Button + Volume Level)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onToggleCustomAudio,
                            enabled = !selectedAudioUri.isNullOrBlank() || customAudioList.isNotEmpty(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isCustomAudioPlaying) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (isCustomAudioPlaying) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("dialog_btn_custom_toggle")
                        ) {
                            Icon(
                                if (isCustomAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isCustomAudioPlaying) "Stop Audio" else "Play Audio")
                        }

                        Text(
                            text = "Volume: ${(customAudioVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = customAudioVolume,
                            onValueChange = onCustomAudioVolumeChange,
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_audio_volume_slider")
                        )
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Upload Button & Audio Library
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Audio Library (${customAudioList.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedButton(
                            onClick = onPickCustomAudio,
                            modifier = Modifier.testTag("btn_upload_audio")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload Audio", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (customAudioList.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.AudioFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "No custom audio uploaded yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap 'Upload Audio' to add long audiobooks, lectures, stories, or study playlists.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            customAudioList.forEach { audio ->
                                val isSelected = audio.id == selectedAudioId || audio.uri == selectedAudioUri
                                val isThisPlaying = isSelected && isCustomAudioPlaying

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSelectAudio(audio) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            if (isThisPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = audio.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isSelected) {
                                                Text(
                                                    text = if (isThisPlaying) "Playing now" else "Selected track",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }

                                        // Play / Stop Icon for this item
                                        IconButton(
                                            onClick = {
                                                if (isThisPlaying) {
                                                    onToggleCustomAudio()
                                                } else {
                                                    onSelectAudio(audio)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isThisPlaying) "Pause" else "Play",
                                                tint = if (isThisPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Delete Icon
                                        IconButton(
                                            onClick = { onDeleteAudio(audio) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete audio",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isAmbientPlaying || isCustomAudioPlaying) {
                TextButton(onClick = onStopAll) {
                    Text("Stop All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
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
    isWallpaperActive: Boolean = false,
    isLight: Boolean = false,
    onReset: () -> Unit,
    onToggle: () -> Unit,
    onSkip: () -> Unit
) {
    val secondaryBtnColor = if (isWallpaperActive) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

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
                containerColor = secondaryBtnColor
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
                containerColor = secondaryBtnColor
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
