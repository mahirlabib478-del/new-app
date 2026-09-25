package com.aistudio.studyos.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XPShopBottomSheet(
    viewModel: StudyViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val profile by viewModel.userProfile.collectAsState()
    val totalXP = profile?.totalXP ?: 0
    val currentLevel = profile?.currentLevel ?: 1

    val streakShieldCount by viewModel.streakShieldCount.collectAsState()
    val isWallpaperPassActive by viewModel.isCustomWallpaperPassActive.collectAsState()
    val wallpaperPassRemaining by viewModel.wallpaperPassRemainingFormatted.collectAsState()
    val isAudioPassActive by viewModel.isCustomAudioPassActive.collectAsState()
    val audioPassRemaining by viewModel.audioPassRemainingFormatted.collectAsState()
    val isBoosterActive by viewModel.isDoubleXpBoosterActive.collectAsState()
    val boosterRemaining by viewModel.boosterRemainingFormatted.collectAsState()

    var showBoosterDialog by remember { mutableStateOf(false) }

    if (showBoosterDialog) {
        SecretCodeRewardDialog(
            bonusXP = 150,
            onDismiss = { showBoosterDialog = false },
            onClaimReward = {
                showBoosterDialog = false
                viewModel.claimBonusXP(150)
                viewModel.activateDoubleXpBooster(60)
                Toast.makeText(
                    context,
                    "⚡ 2X XP Booster Activated for 1 Hour! (+150 XP)",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "XP Perks & Power-ups",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Spend XP on temporary perks & protection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_xp_shop")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // XP Wallet Balance Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT XP BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$totalXP XP",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Scholar Lv $currentLevel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ITEM 1: 🛡️ Streak Shield
            ShopItemCard(
                icon = Icons.Default.Shield,
                iconColor = Color(0xFF3B82F6),
                title = "Streak Shield",
                badgeText = "$streakShieldCount/2 Equipped",
                badgeColor = if (streakShieldCount > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant,
                description = "Automatically preserves your streak if you miss a study day. Max 2 stored in inventory.",
                costText = "500 XP",
                isButtonEnabled = streakShieldCount < 2 && totalXP >= 500,
                buttonLabel = when {
                    streakShieldCount >= 2 -> "Max Equipped (2/2)"
                    totalXP < 500 -> "Need ${500 - totalXP} XP"
                    else -> "Buy Shield (500 XP)"
                },
                testTag = "btn_buy_streak_shield",
                onAction = {
                    viewModel.buyStreakShield { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // ITEM 2: 🖼️ 24h Custom Wallpaper Pass
            ShopItemCard(
                icon = Icons.Default.Image,
                iconColor = Color(0xFF8B5CF6),
                title = "Custom Wallpaper Pass",
                badgeText = if (isWallpaperPassActive) "Active • $wallpaperPassRemaining" else "Expired",
                badgeColor = if (isWallpaperPassActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                description = "Set your own personal aesthetic photo from your phone gallery as the full-screen study background for 24 hours.",
                costText = "250 XP / 24h",
                isButtonEnabled = totalXP >= 250,
                buttonLabel = when {
                    totalXP < 250 -> "Need ${250 - totalXP} XP"
                    isWallpaperPassActive -> "Extend +24h (250 XP)"
                    else -> "Unlock 24h Pass (250 XP)"
                },
                testTag = "btn_buy_wallpaper_pass",
                onAction = {
                    viewModel.buyCustomWallpaperPass(24) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // ITEM 3: 🎵 24h Custom Audio Pass
            ShopItemCard(
                icon = Icons.Default.Headphones,
                iconColor = Color(0xFFEC4899),
                title = "Custom Audio Pass",
                badgeText = if (isAudioPassActive) "Active • $audioPassRemaining" else "Expired",
                badgeColor = if (isAudioPassActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                description = "Upload and play your own study playlists, lofi beats, or lecture audiobooks in the background for 24 hours.",
                costText = "300 XP / 24h",
                isButtonEnabled = totalXP >= 300,
                buttonLabel = when {
                    totalXP < 300 -> "Need ${300 - totalXP} XP"
                    isAudioPassActive -> "Extend +24h (300 XP)"
                    else -> "Unlock 24h Pass (300 XP)"
                },
                testTag = "btn_buy_audio_pass",
                onAction = {
                    viewModel.buyCustomAudioPass(24) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // ITEM 4: ⚡ 2X XP Multiplier Booster (1 Hour)
            ShopItemCard(
                icon = Icons.Default.Bolt,
                iconColor = Color(0xFFEAB308),
                title = "2X XP Multiplier (1 Hour)",
                badgeText = if (isBoosterActive) "Active • $boosterRemaining" else "Free via Sponsor",
                badgeColor = if (isBoosterActive) Color(0xFF10B981) else Color(0xFFEAB308),
                description = "Doubles all XP earned from completed study sessions! Tap to visit sponsor page and get your secret key code.",
                costText = "FREE (Key)",
                isButtonEnabled = true,
                buttonLabel = if (isBoosterActive) "Boost Active (Get Key)" else "Claim 2X Key",
                testTag = "btn_claim_2x_booster_key",
                onAction = {
                    showBoosterDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ShopItemCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    badgeText: String,
    badgeColor: Color,
    description: String,
    costText: String,
    isButtonEnabled: Boolean,
    buttonLabel: String,
    testTag: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Icon, Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Row 2: Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            // Row 3: Cost and Purchase Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cost",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = costText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF59E0B)
                    )
                }

                Button(
                    onClick = onAction,
                    enabled = isButtonEnabled,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag(testTag)
                ) {
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
