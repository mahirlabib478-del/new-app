package com.aistudio.studyos.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Water
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.aistudio.studyos.ui.components.XPShopBottomSheet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.os.Build
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.aistudio.studyos.service.StudyReminderScheduler
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.update.UpdateCheckState
import com.aistudio.studyos.data.update.UpdateManager
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
import com.aistudio.studyos.ui.components.AnimatedReveal
import com.aistudio.studyos.ui.components.AnimatedSyncIndicator
import com.aistudio.studyos.data.repository.FirebaseAccountRepository
import kotlin.math.roundToInt

data class ThemeOption(
    val key: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

private val THEME_OPTIONS = listOf(
    ThemeOption("pitch_black", "Pitch Black", Icons.Default.DarkMode, Color(0xFF00E5FF)),
    ThemeOption("obsidian_gold", "Obsidian Gold", Icons.Default.DarkMode, Color(0xFFFFD60A)),
    ThemeOption("light", "Light", Icons.Default.WbSunny, Color(0xFF2563EB)),
    ThemeOption("sky_night", "Sky Night", Icons.Default.NightsStay, Color(0xFF38BDF8)),
    ThemeOption("learning_green", "Learning Green", Icons.Default.Eco, Color(0xFF4CAF00)),
    ThemeOption("sunrise", "Sunrise", Icons.Default.WbSunny, Color(0xFFEA580C)),
    ThemeOption("cyberpunk", "Cyberpunk / Synthwave 80s", Icons.Default.Bolt, Color(0xFFFF2A85)),
    ThemeOption("cyber_runner", "Mirror's Edge / Cyber Runner", Icons.Default.Bolt, Color(0xFFEF4444)),
)

@Composable
private fun ThemeOptionCard(option: ThemeOption, isSelected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = tween(260),
        label = "theme_icon_scale"
    )
    Card(
        modifier = Modifier.width(104.dp).animateContentSize(animationSpec = tween(220)).clickable(onClick = onClick).testTag("theme_card_" + option.key),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(option.icon, contentDescription = null, tint = option.color, modifier = Modifier.size(22.dp).scale(iconScale))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                option.name,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                when {
                    isSelected -> "Applied"
                    locked -> "🔒 Locked"
                    else -> "Free"
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (locked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileSectionHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDailyGoal(minutes: Int): String = when {
    minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60}h"
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: StudyViewModel,
    onOpenAccount: () -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val updateState by viewModel.updateCheckState.collectAsState()
    val (versionName, versionCode) = remember { UpdateManager.getCurrentVersionInfo(context) }

    var showResetDialog by remember { mutableStateOf(false) }
    var showCustomGoalDialog by remember { mutableStateOf(false) }
    var customGoalInput by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(StudyReminderScheduler.isEnabled(context)) }
    var reminderHour by remember { mutableStateOf(StudyReminderScheduler.getHour(context)) }
    var reminderMinute by remember { mutableStateOf(StudyReminderScheduler.getMinute(context)) }
    var showReminderTimeDialog by remember { mutableStateOf(false) }
    var isCleaningCache by remember { mutableStateOf(false) }
    var cacheCleanedSuccess by remember { mutableStateOf(false) }
    var showXPShop by remember { mutableStateOf(false) }
    val accountRepository = remember { FirebaseAccountRepository() }
    val profileAccountUser = accountRepository.currentUser
    val profileAccountEmail = profileAccountUser?.email.orEmpty()
    val profileAccountVerified = profileAccountUser?.isEmailVerified == true
    val profileAvatarInitial = profileAccountEmail.firstOrNull()?.uppercaseChar()?.toString()
    val activeAccountUid = profileAccountUser?.takeIf { it.isEmailVerified }?.uid
    val accountCloudSync = remember(activeAccountUid) {
        activeAccountUid?.let { (context.applicationContext as com.aistudio.studyos.StudyApplication).cloudSyncFor(it) }
    }
    val accountCloudSyncState = accountCloudSync?.syncStatus?.collectAsState(initial = "Checking cloud sync…")
    val accountCloudSyncStatus = accountCloudSyncState?.value ?: "Sign in with a verified account to sync progress"

    val currentTheme by viewModel.currentTheme.collectAsState()
    val focusState by viewModel.focusState.collectAsState()
    val bottomListPadding = if (focusState.planId != null) 150.dp else 96.dp
    val dailyGoal = profile?.dailyGoalMinutes ?: 60

    val streakShieldCount by viewModel.streakShieldCount.collectAsState()
    val isWallpaperPassActive by viewModel.isCustomWallpaperPassActive.collectAsState()
    val wallpaperPassRemaining by viewModel.wallpaperPassRemainingFormatted.collectAsState()
    val isAudioPassActive by viewModel.isCustomAudioPassActive.collectAsState()
    val passTimeTick by viewModel.passTimeTick.collectAsState()
    val isBoosterActive by viewModel.isDoubleXpBoosterActive.collectAsState()
    val activeBoosterMultiplier by viewModel.xpBoosterMultiplier.collectAsState()

    // Photo picker launcher (complies with Google Play permissions policy)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val targetFile = java.io.File(context.filesDir, "custom_study_wallpaper.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val localUriString = android.net.Uri.fromFile(targetFile).toString()
                viewModel.setCustomWallpaperUri(localUriString)
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.setCustomWallpaperUri(uri.toString())
            }
        }
    }

    if (showXPShop) {
        XPShopBottomSheet(
            viewModel = viewModel,
            onDismiss = { showXPShop = false }
        )
    }


    val horizontalContentPadding = if (LocalConfiguration.current.screenWidthDp < 360) 12.dp else 20.dp

    AnimatedReveal(index = 0) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = horizontalContentPadding, end = horizontalContentPadding, top = 12.dp, bottom = bottomListPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Profile & Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Account, rewards, preferences, and support",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ProfileSectionHeader("Account", "Identity and cloud sync")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenAccount).testTag("profile_account_entry"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (profileAvatarInitial != null) {
                                Text(
                                    profileAvatarInitial,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Guest profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (profileAccountUser == null) "Your account" else profileAccountEmail.substringBefore("@").ifBlank { "StudyOS member" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = when {
                                profileAccountVerified -> profileAccountEmail
                                profileAccountUser != null -> "Email verification pending"
                                else -> "Guest mode · progress saved on this device"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AnimatedSyncIndicator(
                                status = accountCloudSyncStatus,
                                modifier = Modifier.size(18.dp)
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (accountCloudSyncStatus.startsWith("Synced")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(7.dp)
                            ) {}
                            Text(
                                text = if (profileAccountVerified) accountCloudSyncStatus else if (profileAccountUser == null) "Set up account & security" else "Verify email to enable cloud sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (accountCloudSyncStatus.startsWith("Synced")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open account settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        item {
            ProfileSectionHeader("Rewards", "XP perks and power-ups")
        }

        // XP Perks & Power-ups Shop Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showXPShop = true }
                    .testTag("profile_xp_perks_shop_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "XP Perks & Power-ups Shop",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Streak shields, aesthetic passes & XP boost",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showXPShop = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("btn_open_shop_from_profile")
                        ) {
                            Text(
                                text = "Open Shop",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Inventory summary chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Shields: $streakShieldCount/2",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Free XP Drop",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            ProfileSectionHeader("Appearance", "Choose your theme and personalize the study space")
        }

        // Theme Palette Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Color Theme & Aesthetics", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Text("FREE THEMES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(items = THEME_OPTIONS.filter { !viewModel.isPremiumTheme(it.key) }, key = { it.key }) { option ->
                            val isSelected = currentTheme == option.key
                            ThemeOptionCard(option, isSelected, false) { viewModel.setTheme(option.key) }
                        }
                    }

                    Text("PREMIUM THEMES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        THEME_OPTIONS.filter { viewModel.isPremiumTheme(it.key) }.forEach { option ->
                            val isSelected = currentTheme == option.key
                            val active = viewModel.isPremiumThemePassActive(option.key)
                            val remaining = viewModel.premiumThemePassRemaining(option.key)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (active) viewModel.setTheme(option.key) else showXPShop = true
                                    }
                                    .testTag("profile_premium_theme_" + option.key),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(option.icon, contentDescription = null, tint = option.color, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(option.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                        Text(
                                            if (active) "Active • $remaining" else "Available in XP Shop",
                                            fontSize = 12.sp,
                                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        if (isSelected) "Applied" else if (active) "Use" else "Unlock",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🌧️ Theme Dynamic Wallpaper & Ambience Settings
        item {
            val isWallpaperEnabled by viewModel.isWallpaperEnabled.collectAsState()
            val wallpaperOpacity by viewModel.wallpaperOpacity.collectAsState()
            val currentStyleId by viewModel.wallpaperStyle.collectAsState()
            val customWallpaperUri by viewModel.customWallpaperUri.collectAsState()

            val isLight = remember(currentTheme) { com.aistudio.studyos.ui.theme.isLightPreset(currentTheme) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallpaper_settings_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Focus Session Wallpaper",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isLight) "Disabled in Light themes for optimal clarity" else "Atmospheric background exclusively during focus sessions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isWallpaperEnabled && !isLight,
                            enabled = !isLight,
                            onCheckedChange = { viewModel.toggleWallpaperEnabled() },
                            modifier = Modifier.testTag("wallpaper_master_switch")
                        )
                    }

                    if (isLight) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Wallpapers are disabled in Light themes to ensure maximum text sharpness and timer visibility. Switch to a supported Dark theme to use wallpapers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (isWallpaperEnabled && !isLight) {
                        // Live Miniature Wallpaper Preview Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp)
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            val style = remember(currentStyleId) {
                                com.aistudio.studyos.ui.components.WallpaperStyle.entries.find { it.id == currentStyleId }
                                    ?: com.aistudio.studyos.ui.components.WallpaperStyle.CAFE_BOKEH
                            }
                            com.aistudio.studyos.ui.components.DynamicStudyWallpaper(
                                style = style,
                                themePreset = currentTheme,
                                opacity = wallpaperOpacity,
                                dimOverlay = 0f,
                                customUri = customWallpaperUri
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Current: ${style.title}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Style selector chips with descriptions
                        Text(
                            text = "Rain Ambience Styles",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = com.aistudio.studyos.ui.components.WallpaperStyle.entries.filter { it != com.aistudio.studyos.ui.components.WallpaperStyle.CUSTOM },
                                key = { it.id }
                            ) { style ->
                                val isSelected = currentStyleId == style.id
                                Card(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable { viewModel.setThemeWallpaperStyle(currentTheme, style.id) }
                                        .testTag("wallpaper_style_${style.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = style.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = style.description,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            lineHeight = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Custom Picture from Gallery Option
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Custom Gallery Photo",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isWallpaperPassActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (isWallpaperPassActive) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFF59E0B).copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = if (isWallpaperPassActive) "✨ Pass Active • $wallpaperPassRemaining" else "🔒 Available in XP Shop",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWallpaperPassActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (isWallpaperPassActive) {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        } else {
                                            showXPShop = true
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pick_wallpaper_from_gallery_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            !isWallpaperPassActive -> "Open XP Shop"
                                            customWallpaperUri != null -> "Change Photo"
                                            else -> "Pick from Gallery"
                                        },
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                if (customWallpaperUri != null) {
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                val targetFile = java.io.File(context.filesDir, "custom_study_wallpaper.jpg")
                                                if (targetFile.exists()) targetFile.delete()
                                            }
                                            viewModel.setCustomWallpaperUri(null)
                                            viewModel.setThemeWallpaperStyle(currentTheme, "cafe_bokeh")
                                        },
                                        modifier = Modifier.testTag("clear_custom_wallpaper_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Clear custom wallpaper",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        // Opacity Adjustment Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Visibility & Contrast",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(wallpaperOpacity * 100).roundToInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = wallpaperOpacity,
                                onValueChange = { viewModel.setWallpaperOpacity(it) },
                                valueRange = 0.1f..1.0f,
                                modifier = Modifier.testTag("wallpaper_opacity_slider")
                            )
                        }
                    }
                }
            }
        }

        item {
            ProfileSectionHeader("Study Routine", "Set your daily target and reminder")
        }

        // Daily Study Target — intentionally simple: one manual value.
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("daily_goal_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Daily Study Target", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Set the amount of study time you want to complete each day.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = formatDailyGoal(dailyGoal),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            customGoalInput = dailyGoal.toString()
                            showCustomGoalDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_edit_custom_goal"),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Set Daily Target Manually", maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // Daily study reminder
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("study_reminder_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Study Reminder", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Get a daily reminder even when the app is closed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                reminderEnabled = enabled
                                StudyReminderScheduler.setReminder(context, enabled, reminderHour, reminderMinute)
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        ActivityCompat.requestPermissions(
                                            context as android.app.Activity,
                                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                            5204
                                        )
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                        !context.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()
                                    ) {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                    Uri.parse("package:" + context.packageName)
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.testTag("study_reminder_switch")
                        )
                    }

                    OutlinedButton(
                        enabled = reminderEnabled,
                        onClick = { showReminderTimeDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("study_reminder_time"),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            String.format(
                                java.util.Locale.getDefault(),
                                "Every day at %02d:%02d",
                                reminderHour,
                                reminderMinute
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            ProfileSectionHeader("App & Data", "Updates, privacy, and local storage")
        }

        // Offline-First Privacy Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "100% Offline-First",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "All exams, sessions, and streaks are safely persisted locally on your device with Room SQLite.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // App Version & Updates Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("app_updates_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "App Updates & Releases",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Installed: v$versionName (Build $versionCode)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                UpdateManager.openUpdateLink(
                                    context,
                                    "https://github.com/mahirlabib478-del/new-app/releases"
                                )
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "View GitHub Releases",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = updateState,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(150)) togetherWith
                                fadeOut(animationSpec = tween(150))
                        },
                        label = "update_state_transition"
                    ) { state ->
                    when (state) {
                        is UpdateCheckState.Checking -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Checking GitHub for new releases...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is UpdateCheckState.UpToDate -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "You're on the latest version (v${state.currentVersion})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        viewModel.checkAppUpdate(context, isManual = true)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Check Again", fontSize = 12.sp)
                                }
                            }
                        }
                        is UpdateCheckState.Available -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "New update v${state.updateInfo.latestVersion} is ready to install!",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val targetUrl = state.updateInfo.apkUrl.ifBlank { state.updateInfo.releaseUrl }
                                        UpdateManager.openUpdateLink(context, targetUrl)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download Update Now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        is UpdateCheckState.Error -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(text = state.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.checkAppUpdate(context, isManual = true) },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Try Again", fontSize = 12.sp)
                                }
                            }
                        }
                        is UpdateCheckState.Idle -> {
                            Button(
                                onClick = {
                                    viewModel.checkAppUpdate(context, isManual = true)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("manual_check_update_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Check for Updates",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    }
                }
            }
        }

        // Storage & Cache Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("storage_cache_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Storage & Cache Clean", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Removes old update APKs and temporary audio cache without affecting your study records.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (!isCleaningCache) {
                                isCleaningCache = true
                                cacheCleanedSuccess = false
                                viewModel.cleanAppCache {
                                    isCleaningCache = false
                                    cacheCleanedSuccess = true
                                }
                            }
                        },
                        enabled = !isCleaningCache,
                        modifier = Modifier.fillMaxWidth().testTag("btn_clean_cache"),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        if (isCleaningCache) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Cleaning...")
                        } else if (cacheCleanedSuccess) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Cache Cleaned Successfully!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Clear Temporary Cache & Free Storage")
                        }
                    }
                }
            }
        }

        item {
            ProfileSectionHeader("Danger Zone", "Irreversible actions")
        }

        // Reset Data Action
        item {
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_reset_stats"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reset Study Statistics & History",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Contact Developer — placed at the very end of Profile
        item {
            ProfileSectionHeader("Support")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_contact_developer_card"),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Need a hand?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "We're here to help with StudyOS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val messageText = "Hello Developer! I need help with the StudyOS app."
                                val whatsappUrl = "https://wa.me/8801339871504?text=" + Uri.encode(messageText)
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)))
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Could not open WhatsApp. Please try again.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            .testTag("btn_contact_developer_whatsapp"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Canvas(modifier = Modifier.size(46.dp)) {
                                val green = Color(0xFF25D366)
                                drawCircle(color = green)
                                val handset = Path().apply {
                                    moveTo(size.width * 0.31f, size.height * 0.25f)
                                    cubicTo(size.width * 0.26f, size.height * 0.25f, size.width * 0.23f, size.height * 0.31f, size.width * 0.25f, size.height * 0.37f)
                                    cubicTo(size.width * 0.32f, size.height * 0.58f, size.width * 0.45f, size.height * 0.71f, size.width * 0.65f, size.height * 0.76f)
                                    cubicTo(size.width * 0.72f, size.height * 0.78f, size.width * 0.77f, size.height * 0.72f, size.width * 0.77f, size.height * 0.66f)
                                    lineTo(size.width * 0.76f, size.height * 0.59f)
                                    cubicTo(size.width * 0.75f, size.height * 0.55f, size.width * 0.70f, size.height * 0.54f, size.width * 0.66f, size.height * 0.56f)
                                    lineTo(size.width * 0.60f, size.height * 0.60f)
                                    cubicTo(size.width * 0.51f, size.height * 0.56f, size.width * 0.44f, size.height * 0.49f, size.width * 0.40f, size.height * 0.40f)
                                    lineTo(size.width * 0.44f, size.height * 0.34f)
                                    cubicTo(size.width * 0.46f, size.height * 0.30f, size.width * 0.44f, size.height * 0.26f, size.width * 0.40f, size.height * 0.25f)
                                    close()
                                }
                                drawPath(
                                    path = handset,
                                    color = Color.White,
                                    style = Stroke(width = size.width * 0.065f, cap = StrokeCap.Round)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Contact Developer",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Get support directly on WhatsApp",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open WhatsApp",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "Usually the quickest way to get help or share feedback.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

    }

    if (showReminderTimeDialog) {
        val timePickerState = androidx.compose.material3.rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showReminderTimeDialog = false },
            title = {
                Text("Study Reminder Time", fontWeight = FontWeight.Bold)
            },
            text = {
                AnimatedReveal(index = 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    Text(
                        "Choose your daily reminder time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.TimePicker(
                            state = timePickerState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reminderHour = timePickerState.hour
                        reminderMinute = timePickerState.minute
                        StudyReminderScheduler.setReminder(
                            context,
                            true,
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        showReminderTimeDialog = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Statistics?", fontWeight = FontWeight.Bold) },
            text = {
                AnimatedReveal(index = 0) {
                    Text("This will clear your study session logs, XP, and streaks back to zero. This cannot be undone.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllStats()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("btn_confirm_reset_everything")
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCustomGoalDialog) {
        val parsedMinutes = customGoalInput.toIntOrNull() ?: dailyGoal
        val calcHours = parsedMinutes / 60
        val calcMins = parsedMinutes % 60
        val previewFormatted = when {
            calcHours > 0 && calcMins > 0 -> "${calcHours}h ${calcMins}m"
            calcHours > 0 -> "${calcHours} hours"
            else -> "${calcMins} minutes"
        }

        AlertDialog(
            onDismissRequest = { showCustomGoalDialog = false },
            title = { Text("Set Custom Daily Target", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter your desired daily study target in minutes (e.g. 180 for 3 hours, 360 for 6 hours, or up to 1440 for all-day prep):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = customGoalInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                customGoalInput = input
                            }
                        },
                        label = { Text("Target Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("custom_goal_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (parsedMinutes in 15..1440) {
                        Text(
                            text = "Equivalent to: $previewFormatted ($parsedMinutes minutes)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "Please enter a value between 15 and 1440 minutes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalMins = (customGoalInput.toIntOrNull() ?: dailyGoal).coerceIn(15, 1440)
                        viewModel.setDailyGoal(finalMins)
                        showCustomGoalDialog = false
                    },
                    enabled = (customGoalInput.toIntOrNull() ?: 0) in 15..1440,
                    modifier = Modifier.testTag("save_custom_goal_btn")
                ) {
                    Text("Save Target", maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

}