package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Water
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
    val accent = if (state.isBreak) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
    var showEndDialog by remember { mutableStateOf(false) }

    if (showEndDialog) AlertDialog(
        onDismissRequest = { showEndDialog = false },
        title = { Text("End Study Session?", fontWeight = FontWeight.Bold) },
        text = { Text("Your completed study time will be saved to Progress & Analytics.") },
        confirmButton = { TextButton({ showEndDialog = false; viewModel.finishActiveSessionEarly(); onBack() }) { Text("End Session", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton({ showEndDialog = false }) { Text("Keep Studying") } }
    )

    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text(if (state.isBreak) "Recovery Break" else state.currentSubject, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text(if (state.isBreak) "Reset your body and mind" else state.currentChapter, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            navigationIcon = { IconButton({ onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Minimize session") } },
            actions = { TextButton({ showEndDialog = true }) { Text("End", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(50.dp), color = accent.copy(alpha = .12f)) {
                Row(Modifier.padding(horizontal = 15.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (state.isBreak) Icons.Default.Coffee else Icons.Default.Timer, null, tint = accent, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text(if (state.isBreak) "BREAK • ${state.breakBlockSeconds / 60} MIN" else "FOCUS • MAX 25 MIN", color = accent, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
            Text("Block ${minOf(state.currentBlockIndex + 1, state.totalBlocks)} of ${state.totalBlocks}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(state.totalBlocks.coerceAtMost(12)) { i -> Box(Modifier.height(5.dp).width(if (i == state.currentBlockIndex) 25.dp else 12.dp).clip(RoundedCornerShape(4.dp)).background(if (i < state.currentBlockIndex) accent else if (i == state.currentBlockIndex) accent.copy(alpha = .9f) else MaterialTheme.colorScheme.surfaceVariant)) } }
            Spacer(Modifier.weight(1f))
            CircularTimerDisplay(state.secondsRemaining, state.totalBlockSeconds, state.isRunning, accent)
            Spacer(Modifier.height(18.dp))
            if (state.isBreak) BreakWellnessCard(accent) else FocusInfoCard(state.currentSubject, state.currentChapter, state.totalBlockSeconds / 60, accent)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 26.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton({ viewModel.resetBlockTimer() }, Modifier.size(52.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Icon(Icons.Default.Replay, "Restart block") }
                FilledIconButton({ viewModel.toggleTimer() }, Modifier.size(78.dp).testTag("btn_toggle_focus_timer"), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent)) { Icon(if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.isRunning) "Pause" else "Start", Modifier.size(38.dp), tint = MaterialTheme.colorScheme.onPrimary) }
                FilledIconButton({ viewModel.skipCurrentBlock() }, Modifier.size(52.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Icon(Icons.Default.SkipNext, "Skip block") }
            }
        }
    }
}

@Composable private fun CircularTimerDisplay(seconds: Int, total: Int, running: Boolean, accent: Color) {
    val progress = if (total > 0) ((total - seconds).toFloat() / total).coerceIn(0f, 1f) else 0f
    val mins = seconds / 60; val secs = seconds % 60
    val text = "%02d:%02d".format(mins, secs)
    Box(Modifier.size(270.dp).testTag("focus_timer_circle"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().drawWithCache { val stroke = 14.dp.toPx(); val bg = Stroke(stroke); val fg = Stroke(stroke, cap = StrokeCap.Round); onDrawBehind { drawCircle(MaterialTheme.colorScheme.surfaceVariant, style = bg); drawArc(accent, -90f, progress * 360f, false, style = fg) } }) {}
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(text, fontSize = 56.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp); Text(if (running) "In progress" else "Paused", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun FocusInfoCard(subject: String, topic: String, minutes: Int, accent: Color) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = accent.copy(alpha = .12f)) { Icon(Icons.Default.Timer, null, tint = accent, modifier = Modifier.padding(9.dp).size(20.dp)) }
            Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(subject, fontWeight = FontWeight.Bold); Text(topic, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("${minutes}m", fontWeight = FontWeight.ExtraBold, color = accent)
        }
    }
}

@Composable private fun BreakWellnessCard(accent: Color) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .72f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Take a real break", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text("Use these few minutes to reset instead of starting another task.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WellnessTip(Icons.Default.Coffee, "Rest", "Look away")
                WellnessTip(Icons.Default.Water, "Hydrate", "Drink water")
                WellnessTip(Icons.Default.Timer, "Move", "Stretch")
            }
            Text("Next: your next focus block starts automatically when the break ends.", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accent)
        }
    }
}

@Composable private fun WellnessTip(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Surface(Modifier.weight(1f), shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .55f)) {
        Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, modifier = Modifier.size(19.dp)); Spacer(Modifier.height(4.dp)); Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}