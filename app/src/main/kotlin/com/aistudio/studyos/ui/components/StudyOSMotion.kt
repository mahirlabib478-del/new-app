package com.aistudio.studyos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.ui.composed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import kotlin.math.roundToInt


fun Modifier.tactile3DButton(
    backgroundColor: Color,
    bottomEdgeColor: Color,
    cornerRadius: Dp = 18.dp,
    depth: Dp = 5.dp
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (pressed) depth else 0.dp,
        animationSpec = tween(115, easing = FastOutSlowInEasing),
        label = "tactile_press_offset"
    )
    val shape = RoundedCornerShape(cornerRadius)
    val extrusionDepth = if (pressed) depth * 0.22f else depth

    this
        .pointerInput(Unit) {
            while (true) {
                val down = awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                }
                val press = PressInteraction.Press(down.position)
                interactionSource.emit(press)
                val up = awaitPointerEventScope {
                    waitForUpOrCancellation()
                }
                if (up == null) {
                    interactionSource.emit(PressInteraction.Cancel(press))
                } else {
                    interactionSource.emit(PressInteraction.Release(press))
                }
            }
        }
        .graphicsLayer {
            translationY = pressOffset.toPx()
        }
        .shadow(
            elevation = depth + 3.dp,
            shape = shape,
            clip = false
        )
        .clip(shape)
        .drawWithContent {
            val depthPx = extrusionDepth.toPx()
            drawRoundRect(
                color = backgroundColor,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    cornerRadius.toPx(),
                    cornerRadius.toPx()
                )
            )
            drawContent()
            drawRect(
                color = bottomEdgeColor,
                topLeft = androidx.compose.ui.geometry.Offset(
                    0f,
                    (size.height - depthPx).coerceAtLeast(0f)
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.width,
                    depthPx.coerceAtMost(size.height)
                )
            )
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = androidx.compose.ui.geometry.Offset(cornerRadius.toPx(), 0.7.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(
                    (size.width - cornerRadius.toPx()).coerceAtLeast(cornerRadius.toPx()),
                    0.7.dp.toPx()
                ),
                strokeWidth = 0.6.dp.toPx()
            )
        }
}
@Composable
fun AnimatedReveal(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = fadeIn(tween(430, delayMillis = index * 55, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(480, delayMillis = index * 55, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 8 }
            ) +
            scaleIn(
                animationSpec = tween(480, delayMillis = index * 55, easing = FastOutSlowInEasing),
                initialScale = 0.93f
            ),
        exit = fadeOut(tween(150)) + scaleOut(tween(150))
    ) {
        content()
    }
}

@Composable
fun AnimatedCounter(
    target: Int,
    suffix: String = "",
    durationMillis: Int = 650,
    modifier: Modifier = Modifier,
    style: TextStyle = androidx.compose.ui.text.TextStyle.Default,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animated.animateTo(
            target.toFloat(),
            animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
        )
    }
    Text(
        text = animated.value.roundToInt().toString() + suffix,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
fun AnimatedSyncIndicator(
    status: String,
    modifier: Modifier = Modifier
) {
    val working = status.contains("sync", ignoreCase = true) ||
        status.contains("checking", ignoreCase = true) ||
        status.contains("retry", ignoreCase = true)
    val transition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_rotation_value"
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp).graphicsLayer {
                if (working) rotationZ = rotation
            }
        )
    }
}
