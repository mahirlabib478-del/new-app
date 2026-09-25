package com.aistudio.studyos.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.service.AdManager
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class PassType {
    WALLPAPER, AUDIO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XPShopBottomSheet(
    viewModel: StudyViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val profile by viewModel.userProfile.collectAsState()
    val totalXP = profile?.totalXP ?: 0
    val currentLevel = profile?.currentLevel ?: 1

    val streakShieldCount by viewModel.streakShieldCount.collectAsState()
    val isWallpaperPassActive by viewModel.isCustomWallpaperPassActive.collectAsState()
    val wallpaperPassRemaining by viewModel.wallpaperPassRemainingFormatted.collectAsState()
    val isAudioPassActive by viewModel.isCustomAudioPassActive.collectAsState()
    val audioPassRemaining by viewModel.audioPassRemainingFormatted.collectAsState()

    var passTypeToPurchase by remember { mutableStateOf<PassType?>(null) }

    // 30-minute cooldown for Free XP Drop
    val cooldownMs by viewModel.freeXpDropCooldownRemainingMs.collectAsState()
    val isCooldownActive = cooldownMs > 0L

    // Live ticker every second to update remaining cooldown
    androidx.compose.runtime.LaunchedEffect(isCooldownActive) {
        while (viewModel.freeXpDropCooldownRemainingMs.value > 0L) {
            viewModel.updateFreeXpDropCooldown()
            delay(1000L)
        }
    }

    val remainingMinutes = (cooldownMs / 60000L).toInt()
    val remainingSeconds = ((cooldownMs % 60000L) / 1000L).toInt()
    val cooldownFormatted = String.format("%02d:%02d", remainingMinutes, remainingSeconds)

    // 70% chance +150 XP, 30% chance +250 XP instant drop
    val offerBonusXP = remember { if (Random.nextFloat() < 0.30f) 250 else 150 }

    var showBoosterDialog by remember { mutableStateOf(false) }
    var showDirectSponsorDialog by remember { mutableStateOf(false) }

    if (showDirectSponsorDialog) {
        DirectSponsorRewardDialog(
            rewardXP = offerBonusXP,
            mode = if (offerBonusXP == 250) SponsorRewardMode.CLAIM_3X_SECRET_KEY else SponsorRewardMode.CLAIM_2X,
            onDismiss = { showDirectSponsorDialog = false },
            onRewardEarned = { secretKey ->
                viewModel.claimFreeXpDrop(offerBonusXP)
            }
        )
    }

    if (showBoosterDialog) {
        SecretCodeRewardDialog(
            bonusXP = offerBonusXP,
            multiplier = if (offerBonusXP == 250) 3 else 2,
            onDismiss = { showBoosterDialog = false },
            onClaimReward = {
                showBoosterDialog = false
                viewModel.claimFreeXpDrop(offerBonusXP)
                Toast.makeText(
                    context,
                    "🎉 +$offerBonusXP XP added to your balance!",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // Custom Days Pass Selection Dialog with 30% Lucky Deal
    passTypeToPurchase?.let { type ->
        PassDurationSelectionDialog(
            passType = type,
            totalXP = totalXP,
            onDismiss = { passTypeToPurchase = null },
            onConfirmPurchase = { days, xpCost ->
                passTypeToPurchase = null
                if (type == PassType.WALLPAPER) {
                    viewModel.buyCustomWallpaperPass(days = days, xpCost = xpCost) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.buyCustomAudioPass(days = days, xpCost = xpCost) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row - Clean layout without circular sparkle logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "XP Perks & Power-ups",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Spend XP on temporary perks & protection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_xp_shop")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // XP Wallet Balance Card - Theme-aware colors
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                            color = MaterialTheme.colorScheme.primary
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
                iconColor = MaterialTheme.colorScheme.primary,
                title = "Streak Shield",
                badgeText = "$streakShieldCount/2 Equipped",
                badgeColor = if (streakShieldCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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

            // ITEM 2: 🖼️ Custom Wallpaper Pass (Custom days)
            ShopItemCard(
                icon = Icons.Default.Image,
                iconColor = MaterialTheme.colorScheme.secondary,
                title = "Custom Wallpaper Pass",
                badgeText = if (isWallpaperPassActive) "Active • $wallpaperPassRemaining" else "Expired",
                badgeColor = if (isWallpaperPassActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                description = "Set your own photo from gallery as full-screen study background. Select duration from 1 to 30+ days.",
                costText = "From 250 XP / day",
                isButtonEnabled = true,
                buttonLabel = if (isWallpaperPassActive) "Extend Pass (Choose Days)" else "Select Duration (From 250 XP)",
                testTag = "btn_buy_wallpaper_pass",
                onAction = {
                    passTypeToPurchase = PassType.WALLPAPER
                }
            )

            // ITEM 3: 🎵 Custom Audio Pass (Custom days)
            ShopItemCard(
                icon = Icons.Default.Headphones,
                iconColor = MaterialTheme.colorScheme.tertiary,
                title = "Custom Audio Pass",
                badgeText = if (isAudioPassActive) "Active • $audioPassRemaining" else "Expired",
                badgeColor = if (isAudioPassActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                description = "Upload and play your own study playlists and audiobooks in the background. Select duration from 1 to 30+ days.",
                costText = "From 300 XP / day",
                isButtonEnabled = true,
                buttonLabel = if (isAudioPassActive) "Extend Pass (Choose Days)" else "Select Duration (From 300 XP)",
                testTag = "btn_buy_audio_pass",
                onAction = {
                    passTypeToPurchase = PassType.AUDIO
                }
            )

            // ITEM 4: ⚡ Instant Free XP Drop via Sponsor (70% +150 XP, 30% +250 XP; 30-min cooldown; 5s silent delay)
            ShopItemCard(
                icon = Icons.Default.Bolt,
                iconColor = MaterialTheme.colorScheme.primary,
                title = "+${offerBonusXP} Free Bonus XP",
                badgeText = if (isCooldownActive) "Cooldown: $cooldownFormatted" else "Available Now",
                badgeColor = if (isCooldownActive) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                description = if (isCooldownActive) {
                    "Next free XP drop available in $cooldownFormatted. Free XP drops are available every 30 minutes!"
                } else {
                    "Get an instant +${offerBonusXP} XP added directly to your wallet balance! Available every 30 minutes."
                },
                costText = "Free",
                isButtonEnabled = !isCooldownActive,
                buttonLabel = if (isCooldownActive) "Wait $cooldownFormatted" else "Claim +${offerBonusXP} XP Key",
                testTag = "btn_claim_2x_booster_key",
                onAction = {
                    if (isCooldownActive) {
                        Toast.makeText(context, "⏳ Next XP drop available in $cooldownFormatted", Toast.LENGTH_SHORT).show()
                        return@ShopItemCard
                    }
                    // 70% direct link, 30% secret code ad dialog
                    if (Random.nextFloat() < 0.70f) {
                        showDirectSponsorDialog = true
                    } else {
                        showBoosterDialog = true
                    }
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = onAction,
                    enabled = isButtonEnabled,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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

@Composable
private fun PassDurationSelectionDialog(
    passType: PassType,
    totalXP: Int,
    onDismiss: () -> Unit,
    onConfirmPurchase: (days: Int, xpCost: Int) -> Unit
) {
    val baseDailyRate = if (passType == PassType.WALLPAPER) 250 else 300
    val passTitle = if (passType == PassType.WALLPAPER) "Custom Wallpaper Pass" else "Custom Audio Pass"
    val passIcon = if (passType == PassType.WALLPAPER) Icons.Default.Image else Icons.Default.Headphones

    var selectedDays by remember { mutableIntStateOf(1) }

    // 30% chance discount is active; 70% chance regular full price
    val hasDiscount = remember { Random.nextFloat() < 0.30f }

    // Tiered bulk discounts - more days = bigger discount (only active 30% of the time)
    val discountPercent = if (hasDiscount) {
        when {
            selectedDays == 1 -> 0
            selectedDays in 2..6 -> 15
            selectedDays in 7..13 -> 25
            selectedDays in 14..29 -> 35
            else -> 50 // 30+ days
        }
    } else {
        0
    }

    val rawCost = selectedDays * baseDailyRate
    val finalCost = if (discountPercent > 0) {
        ((rawCost * (100 - discountPercent)) / 100)
    } else {
        rawCost
    }
    val savingsXP = rawCost - finalCost
    val canAfford = totalXP >= finalCost

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = passIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = passTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Select duration:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(1, 3, 7, 14, 30)
                    presets.forEach { days ->
                        val isSelected = selectedDays == days
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDays = days },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (days == 1) "1d" else "${days}d",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Stepper Row: [-] [ X Days ] [+]
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Custom Days:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { if (selectedDays > 1) selectedDays-- },
                                enabled = selectedDays > 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Days")
                            }
                            Text(
                                text = "$selectedDays ${if (selectedDays == 1) "Day" else "Days"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { if (selectedDays < 60) selectedDays++ },
                                enabled = selectedDays < 60,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Days")
                            }
                        }
                    }
                }

                // Price and Savings Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Price:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (discountPercent > 0) {
                                    Text(
                                        text = "$rawCost XP",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$finalCost XP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (discountPercent > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "-$discountPercent% DISCOUNT",
                                        color = Color(0xFF047857),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "You save $savingsXP XP!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }

                        Text(
                            text = "Your balance: $totalXP XP",
                            fontSize = 12.sp,
                            color = if (canAfford) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPurchase(selectedDays, finalCost) },
                enabled = canAfford,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (canAfford) "Buy $selectedDays Days ($finalCost XP)" else "Need ${finalCost - totalXP} More XP"
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

