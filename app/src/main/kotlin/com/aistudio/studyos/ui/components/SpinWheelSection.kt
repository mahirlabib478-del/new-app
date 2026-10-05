package com.aistudio.studyos.ui.components

import com.aistudio.studyos.service.CompactToast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.service.AdManager
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
import com.aistudio.studyos.ui.components.tactile3DButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private val spinColors = listOf(
    Color(0xFF4F46E5), Color(0xFF0EA5E9), Color(0xFF8B5CF6), Color(0xFF64748B),
    Color(0xFF14B8A6), Color(0xFF7C3AED), Color(0xFFF59E0B), Color(0xFF475569)
)

@Composable
fun SpinWheelSection(viewModel: StudyViewModel) {
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isSpinning by remember { mutableStateOf(false) }
    var lastReward by remember { mutableStateOf<String?>(null) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val totalXP = profile?.totalXP ?: 0

    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val storedStatus = viewModel.getSpinWheelStatus()
    val status = storedStatus.copy(remainingMs = (storedStatus.remainingMs - (System.currentTimeMillis() - tick)).coerceAtLeast(0L))
    val active = status.unlocked && status.remainingMs > 0L && status.spinsUsed < 20
    val nextSpin = if (active) status.spinsUsed + 1 else 1
    val cost = 50 + (nextSpin - 1) * 20
    val labels = spinLabels(nextSpin)

    Card(
        modifier = Modifier.fillMaxWidth().tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f), 18.dp, 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Spin Wheel", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (active) "Spin " + nextSpin + "/20 • " + cost + " XP"
                        else "Watch an ad to unlock 20 spins",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier.tactile3DButton(
                        backgroundColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        bottomEdgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.48f),
                        cornerRadius = 22.dp,
                        depth = 3.dp
                    ),
                    shape = CircleShape,
                    color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        imageVector = if (active) Icons.Default.Refresh else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                    modifier = Modifier
                        .size(292.dp)
                        .clip(CircleShape)
                        .tactile3DButton(
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                            bottomEdgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                            cornerRadius = 138.dp,
                            depth = 6.dp
                        )
                        .padding(13.dp),
                    contentAlignment = Alignment.Center
                ) {
                Box(
                    modifier = Modifier
                        .size(266.dp)
                        .clip(CircleShape)
                        .graphicsLayer { rotationZ = rotation.value }
                        .shadow(10.dp, CircleShape)
                ) {
                    Canvas(modifier = Modifier.size(266.dp).clip(CircleShape)) {
                        val sweep = 360f / 8f
                        for (i in 0 until 8) {
                            drawArc(
                                color = spinColors[i],
                                startAngle = -90f + i * sweep,
                                sweepAngle = sweep - 1.5f,
                                useCenter = true
                            )
                        }
                    }
                    labels.forEachIndexed { index, label ->
                        SpinWheelLabel(index, label)
                    }
                }

                Text(
                    text = "▼",
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-2).dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    modifier = Modifier
                        .size(62.dp)
                        .tactile3DButton(
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            bottomEdgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            cornerRadius = 31.dp,
                            depth = 6.dp
                        ),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("SPIN", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            if (lastReward != null) {
                AnimatedReveal(index = 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "🎉 " + lastReward,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (active) {
                val minutes = status.remainingMs / 60000L
                val seconds = (status.remainingMs / 1000L) % 60L
                val timer = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                Surface(
                    modifier = Modifier.tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f), 10.dp, 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                    "20 spins • " + status.spinsUsed + "/20 used • " + timer,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Button(
                    onClick = {
                        if (isSpinning) return@Button
                        if (totalXP < cost) {
                            CompactToast.show(context, "Need " + (cost - totalXP) + " more XP")
                            return@Button
                        }
                        isSpinning = true
                        lastReward = null
                        viewModel.spinWheel { reward ->
                            if (reward == null) {
                                isSpinning = false
                                CompactToast.show(context, "Spin unavailable. Please try again.")
                                return@spinWheel
                            }
                            scope.launch {
                                val segment = 360f / 8f
                                val center = reward.slotIndex * segment + segment / 2f
                                val current = rotation.value
                                val delta = ((-center - current) % 360f + 360f) % 360f
                                rotation.animateTo(
                                    current + 5f * 360f + delta,
                                    animationSpec = tween(2800, easing = FastOutSlowInEasing)
                                )
                                lastReward = reward.label
                                isSpinning = false
                            }
                        }
                    },
                    enabled = !isSpinning && totalXP >= cost,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactile3DButton(
                            backgroundColor = if (!isSpinning && totalXP >= cost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            bottomEdgeColor = if (!isSpinning && totalXP >= cost) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                            cornerRadius = 14.dp,
                            depth = 5.dp
                        )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (isSpinning) "Spinning…" else "Spin for " + cost + " XP")
                }
            } else {
                OutlinedButton(
                    onClick = {
                        val opened = AdManager.openDirectLink(
                            context,
                            confirmationMessage = "🎡 Spin Wheel unlocked!"
                        )
                        if (opened) {
                            viewModel.unlockSpinWheel()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactile3DButton(
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                            bottomEdgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                            cornerRadius = 14.dp,
                            depth = 4.dp
                        )
                ) {
                    Text("Watch Ad to Unlock • 20 Spins")
                }
            }

            Text(
                "Rare odds • Theme 7% • Audio 7% • Wallpaper 5%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpinWheelLabel(index: Int, text: String) {
    val positions = listOf(
        0.dp to 26.dp, 54.dp to 52.dp, 70.dp to 104.dp, 52.dp to 166.dp,
        0.dp to 188.dp, (-54).dp to 166.dp, (-70).dp to 104.dp, (-54).dp to 52.dp
    )
    Box(
        modifier = Modifier.fillMaxWidth().height(250.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier.offset(x = positions[index].first, y = positions[index].second),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun spinLabels(spin: Int): List<String> {
    val values = listOf(
        listOf(5, 100, 150, 10), listOf(10, 120, 170, 20),
        listOf(15, 140, 190, 25), listOf(20, 160, 210, 30),
        listOf(25, 180, 230, 35), listOf(30, 200, 250, 40),
        listOf(35, 220, 270, 45), listOf(40, 240, 290, 50),
        listOf(45, 260, 310, 55), listOf(50, 280, 330, 60),
        listOf(55, 300, 350, 65), listOf(60, 320, 370, 70),
        listOf(65, 340, 390, 75), listOf(70, 360, 410, 80),
        listOf(75, 380, 430, 85), listOf(80, 400, 450, 90),
        listOf(85, 420, 470, 95), listOf(90, 440, 490, 100),
        listOf(95, 460, 510, 105), listOf(100, 480, 530, 110)
    )[spin.coerceIn(1, 20) - 1]
    return listOf(
        "+" + values[0], "+" + values[1], "1h Theme", "0 XP",
        "1h Audio", "+" + values[2], "+" + values[3], "1h Wall"
    )
}
