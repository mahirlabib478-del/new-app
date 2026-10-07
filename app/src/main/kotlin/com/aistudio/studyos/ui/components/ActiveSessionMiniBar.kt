package com.aistudio.studyos.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.ui.viewmodel.FocusTimerState

@Composable
fun ActiveSessionMiniBar(
    focusState: FocusTimerState,
    onOpenFocus: () -> Unit,
    onToggleTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (focusState.planId == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val minutes = focusState.secondsRemaining / 60
    val seconds = focusState.secondsRemaining % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val accentColor = if (focusState.isBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    // Keep the mini bar readable across pitch-black, light and accent themes.
    // The previous tertiary/surface split could become too low-contrast in some themes.
    val miniSurface = MaterialTheme.colorScheme.surfaceVariant
    val miniOnSurface = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
             .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .tactile3DButton(
                backgroundColor = miniSurface,
                bottomEdgeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                cornerRadius = 18.dp,
                depth = 5.dp,
                shadowExtra = 0.dp
            )
            .clickable { onOpenFocus() }
            .testTag("active_session_mini_bar"),
        shape = RoundedCornerShape(18.dp),
        color = miniSurface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Pulsing Active Indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer {
                            val scaleValue = if (focusState.isRunning) pulseScale else 1f
                            scaleX = scaleValue
                            scaleY = scaleValue
                        }
                        .clip(CircleShape)
                        .background(accentColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = focusState.currentSubject,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = miniOnSurface
                    )
                    Text(
                        text = if (focusState.isBreak) {
                            "☕ Break Time"
                        } else {
                            "🎯 Block ${focusState.currentBlockIndex + 1}/${focusState.totalBlocks} • ${focusState.currentChapter}"
                        },
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = miniOnSurface.copy(alpha = 0.88f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Time and Play/Pause button
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = timeFormatted,
                    label = "mini_bar_timer",
                ) { animatedTime ->
                    Text(
                        text = animatedTime,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = onToggleTimer,
                    modifier = Modifier
                        .size(34.dp)
                        .tactile3DButton(
                            backgroundColor = if (focusState.isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            bottomEdgeColor = accentColor.copy(alpha = 0.42f),
                            cornerRadius = 50.dp,
                            depth = 4.dp
                        )
                        .testTag("mini_bar_toggle_timer"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (focusState.isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = if (focusState.isRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    AnimatedContent(
                        targetState = focusState.isRunning,
                        label = "mini_bar_timer_toggle",
                    ) { isRunning ->
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Play",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
