package com.aistudio.studyos.ui.components

import com.aistudio.studyos.service.CompactToast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.service.AdManager
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
import com.aistudio.studyos.ui.components.AnimatedReveal
import kotlinx.coroutines.delay
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var shopEntryStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shopEntryStarted = true }
    val shopEntryScale by animateFloatAsState(
        targetValue = if (shopEntryStarted) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shop_entry_scale"
    )

    val profile by viewModel.userProfile.collectAsState()
    val totalXP = profile?.totalXP ?: 0
    val currentLevel = profile?.currentLevel ?: 1

    val streakShieldCount by viewModel.streakShieldCount.collectAsState()
    val isWallpaperPassActive by viewModel.isCustomWallpaperPassActive.collectAsState()
    val wallpaperPassRemaining by viewModel.wallpaperPassRemainingFormatted.collectAsState()
    val isAudioPassActive by viewModel.isCustomAudioPassActive.collectAsState()
    val audioPassRemaining by viewModel.audioPassRemainingFormatted.collectAsState()
    val passTimeTick by viewModel.passTimeTick.collectAsState()

    var passTypeToPurchase by remember { mutableStateOf<PassType?>(null) }
    var themeKeyToPurchase by remember { mutableStateOf<String?>(null) }

    themeKeyToPurchase?.let { themeKey ->
        ThemePassDurationSelectionDialog(
            viewModel = viewModel,
            themeKey = themeKey,
            totalXP = totalXP,
            onDismiss = { themeKeyToPurchase = null }
        )
    }

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
                        CompactToast.show(context, msg)
                    }
                } else {
                    viewModel.buyCustomAudioPass(days = days, xpCost = xpCost) { success, msg ->
                        CompactToast.show(context, msg)
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
        val horizontalContentPadding = if (LocalConfiguration.current.screenWidthDp < 360) 12.dp else 20.dp
        AnimatedReveal(
            index = 0,
            modifier = Modifier.graphicsLayer {
                scaleX = shopEntryScale
                scaleY = shopEntryScale
            }
        ) {
            Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = horizontalContentPadding, vertical = 6.dp)
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
                modifier = Modifier.fillMaxWidth().tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f), 16.dp, 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
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
                description = "Each shield protects 1 missed day. Shields activate automatically when they cover the full gap. Max 2 stored.",
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
                        CompactToast.show(context, msg)
                    }
                }
            )

            // ITEM 2: 🖼️ Custom Wallpaper Pass (Custom days)
            ShopItemCard(
                icon = Icons.Default.Image,
                iconColor = MaterialTheme.colorScheme.primary,
                title = "Custom Wallpaper Pass",
                badgeText = if (isWallpaperPassActive) "Active • $wallpaperPassRemaining" else "Expired",
                badgeColor = if (isWallpaperPassActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                description = "Set your own photo from gallery as full-screen study background. 24 hours is available here; buy longer passes in the XP Shop.",
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
                iconColor = MaterialTheme.colorScheme.primary,
                title = "Custom Audio Pass",
                badgeText = if (isAudioPassActive) "Active • $audioPassRemaining" else "Expired",
                badgeColor = if (isAudioPassActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                description = "Upload and play your own study playlists and audiobooks in the background. 24 hours is available here; buy longer passes in the XP Shop.",
                costText = "From 300 XP / day",
                isButtonEnabled = true,
                buttonLabel = if (isAudioPassActive) "Extend Pass (Choose Days)" else "Select Duration (From 300 XP)",
                testTag = "btn_buy_audio_pass",
                onAction = {
                    passTypeToPurchase = PassType.AUDIO
                }
            )

            // Premium Theme Passes — grouped compact section
            PremiumThemeSection(
                viewModel = viewModel,
                passTimeTick = passTimeTick,
                onSelectTheme = { themeKeyToPurchase = it }
            )

            // 🎡 Spin Wheel
            SpinWheelSection(viewModel = viewModel)

            Spacer(modifier = Modifier.height(10.dp))

            // ITEM 6: ⚡ Instant Free XP Drop via Sponsor (70% +150 XP, 30% +250 XP; 30-min cooldown; 5s silent delay)
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
                buttonLabel = if (isCooldownActive) "Wait $cooldownFormatted" else "Claim +${offerBonusXP} XP",
                testTag = "btn_claim_2x_booster_key",
                onAction = {
                    if (isCooldownActive) {
                        CompactToast.show(context, "⏳ Next XP drop available in $cooldownFormatted")
                        return@ShopItemCard
                    }
                    val opened = AdManager.openDirectLink(
                        context,
                        confirmationMessage = "🎉 +$offerBonusXP XP Added!"
                    )
                    if (opened) {
                        viewModel.claimFreeXpDrop(offerBonusXP)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

}

@Composable
private fun PremiumThemeSection(
    viewModel: StudyViewModel,
    passTimeTick: Long,
    onSelectTheme: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(220)).tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f), 18.dp, 5.dp).testTag("premium_theme_section"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Premium Themes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Choose a theme and set the pass duration", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PremiumThemeRow(
                title = "Cyberpunk / Synthwave 80s",
                price = "400 XP/day",
                active = viewModel.isPremiumThemePassActive("cyberpunk"),
                remaining = viewModel.premiumThemePassRemaining("cyberpunk"),
                testTag = "btn_buy_cyberpunk_theme_pass",
                onClick = { onSelectTheme("cyberpunk") }
            )
            PremiumThemeRow(
                title = "Mirror's Edge / Cyber Runner",
                price = "500 XP/day",
                active = viewModel.isPremiumThemePassActive("cyber_runner"),
                remaining = viewModel.premiumThemePassRemaining("cyber_runner"),
                testTag = "btn_buy_cyber_runner_theme_pass",
                onClick = { onSelectTheme("cyber_runner") }
            )
        }
    }
}

@Composable
private fun PremiumThemeRow(
    title: String,
    price: String,
    active: Boolean,
    remaining: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f), 14.dp, 3.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (active) "Active • $remaining" else "Premium • $price",
                    fontSize = 11.sp,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                modifier = Modifier
                    .tactile3DButton(
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        bottomEdgeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                        cornerRadius = 10.dp,
                        depth = 4.dp
                    )
                    .testTag(testTag)
            ) {
                Text("Select", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .tactile3DButton(
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                bottomEdgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.46f),
                cornerRadius = 18.dp,
                depth = 5.dp
            )
            .animateContentSize(animationSpec = tween(220)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                            .tactile3DButton(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), 10.dp, 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                    ),
                    modifier = Modifier
                        .height(40.dp)
                        .widthIn(min = 118.dp, max = 170.dp)
                        .tactile3DButton(
                            backgroundColor = if (isButtonEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
                            bottomEdgeColor = if (isButtonEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.48f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                            cornerRadius = 12.dp,
                            depth = 4.dp
                        )
                        .testTag(testTag)
                ) {
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
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

    var dialogAnimationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { dialogAnimationStarted = true }
    val dialogScale by animateFloatAsState(
        if (dialogAnimationStarted) 1f else 0.90f,
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pass_dialog_scale"
    )
    val dialogAlpha by animateFloatAsState(if (dialogAnimationStarted) 1f else 0f, tween(300), label = "pass_dialog_alpha")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
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
                    .verticalScroll(rememberScrollState())
                    .graphicsLayer {
                        scaleX = dialogScale
                        scaleY = dialogScale
                        alpha = dialogAlpha
                    },
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
                                .clickable { selectedDays = days }
                                .tactile3DButton(
                                    backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    bottomEdgeColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.48f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                                    cornerRadius = 10.dp,
                                    depth = if (isSelected) 4.dp else 2.dp
                                ),
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
                    modifier = Modifier.fillMaxWidth().tactile3DButton(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f), MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), 14.dp, 4.dp)
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
                                modifier = Modifier.size(36.dp).tactile3DButton(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), 18.dp, 3.dp)
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
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Price:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
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
                                    fontSize = 16.sp,
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
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "-$discountPercent% DISCOUNT",
                                        color = MaterialTheme.colorScheme.primary,
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
                            fontSize = 11.sp,
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (canAfford) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(40.dp)
                    .tactile3DButton(
                    backgroundColor = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    bottomEdgeColor = if (canAfford) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                    cornerRadius = 10.dp,
                    depth = 4.dp
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (canAfford) "Buy $selectedDays Days ($finalCost XP)" else "Need ${finalCost - totalXP} More XP",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(40.dp)
                    .tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), 10.dp, 3.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
            }
        }
    )
}

@Composable
private fun ThemePassDurationSelectionDialog(
    viewModel: StudyViewModel,
    themeKey: String,
    totalXP: Int,
    onDismiss: () -> Unit
) {
    val title = if (themeKey == "cyberpunk") "Cyberpunk / Synthwave 80s" else "Mirror's Edge / Cyber Runner"
    val baseDailyRate = if (themeKey == "cyberpunk") 400 else 500
    var selectedDays by remember { mutableIntStateOf(3) }

    val hasDiscount = remember { Random.nextFloat() < 0.30f }
    val discountPercent = if (hasDiscount) {
        when {
            selectedDays in 2..6 -> 15
            selectedDays in 7..13 -> 25
            selectedDays in 14..29 -> 35
            else -> 50
        }
    } else 0

    val rawCost = selectedDays * baseDailyRate
    val finalCost = if (discountPercent > 0) rawCost * (100 - discountPercent) / 100 else rawCost
    val savingsXP = rawCost - finalCost
    val canAfford = totalXP >= finalCost

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Select a longer pass:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3, 7, 14, 30).forEach { days ->
                        val selected = selectedDays == days
                        Surface(
                            Modifier.weight(1f).tactile3DButton(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), 10.dp, 3.dp).clickable { selectedDays = days },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(days.toString() + "d", fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Custom Days:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { if (selectedDays > 1) selectedDays-- },
                                enabled = selectedDays > 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Days")
                            }
                            Text(selectedDays.toString() + if (selectedDays == 1) " Day" else " Days", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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

                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Price:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (discountPercent > 0) {
                                    Text(rawCost.toString() + " XP", style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(finalCost.toString() + " XP", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (discountPercent > 0) {
                            Text("-" + discountPercent + "% DISCOUNT • You save " + savingsXP + " XP!", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF047857))
                        }
                        Text("Your balance: " + totalXP + " XP", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.buyPremiumThemePass(themeKey, selectedDays, finalCost) { _, _ -> onDismiss() }
                },
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (canAfford) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(40.dp)
                    .tactile3DButton(
                    backgroundColor = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    bottomEdgeColor = if (canAfford) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.40f),
                    cornerRadius = 10.dp,
                    depth = if (canAfford) 4.dp else 2.dp
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (canAfford) "Buy " + selectedDays + " Days" else "Need " + (finalCost - totalXP) + " More XP",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        },
        dismissButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            modifier = Modifier
                .height(40.dp)
                .tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), 10.dp, 3.dp),
            shape = RoundedCornerShape(10.dp)
        ) { Text("Cancel", maxLines = 1, softWrap = false, textAlign = TextAlign.Center) }
        }
    )
}