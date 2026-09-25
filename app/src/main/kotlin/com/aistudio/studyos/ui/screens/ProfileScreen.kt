package com.aistudio.studyos.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.OpenInNew
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
import android.os.Build
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.aistudio.studyos.service.StudyReminderScheduler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.update.UpdateCheckState
import com.aistudio.studyos.data.update.UpdateManager
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
import kotlin.math.roundToInt

data class ThemeOption(
    val key: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

private val THEME_OPTIONS = listOf(
    ThemeOption("midnight", "Midnight", Icons.Default.NightsStay, Color(0xFF6366F1)),
    ThemeOption("pitch_black", "Pitch Black", Icons.Default.DarkMode, Color(0xFF00E5FF)),
    ThemeOption("dark", "Dark", Icons.Default.DarkMode, Color(0xFF64748B)),
    ThemeOption("light", "Light", Icons.Default.WbSunny, Color(0xFF2563EB)),
    ThemeOption("ocean", "Ocean Dark", Icons.Default.Water, Color(0xFF0284C7)),
    ThemeOption("paper", "Paper Sepia", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF8B5A2B)),
    ThemeOption("mint", "Mint Fresh", Icons.Default.Spa, Color(0xFF0D9488)),
    ThemeOption("sunrise", "Sunrise", Icons.Default.WbSunny, Color(0xFFEA580C))
)

private fun formatDailyGoal(minutes: Int): String = when {
    minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60}h"
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: StudyViewModel
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
    var showWallpaperPassPrompt by remember { mutableStateOf(false) }

    val currentTheme by viewModel.currentTheme.collectAsState()
    val focusState by viewModel.focusState.collectAsState()
    val bottomListPadding = if (focusState.planId != null) 150.dp else 96.dp
    val dailyGoal = profile?.dailyGoalMinutes ?: 60

    val totalXP = profile?.totalXP ?: 0
    val streakShieldCount by viewModel.streakShieldCount.collectAsState()
    val isWallpaperPassActive by viewModel.isCustomWallpaperPassActive.collectAsState()
    val wallpaperPassRemaining by viewModel.wallpaperPassRemainingFormatted.collectAsState()
    val isAudioPassActive by viewModel.isCustomAudioPassActive.collectAsState()
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

    if (showWallpaperPassPrompt) {
        AlertDialog(
            onDismissRequest = { showWallpaperPassPrompt = false },
            icon = { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Unlock Custom Wallpaper Pass", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Setting your own aesthetic photo from your phone gallery requires a 24-Hour Custom Wallpaper Pass.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pass Cost:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("250 XP (24 Hours)", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 14.sp)
                        }
                    }
                    Text(
                        "Your balance: $totalXP XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWallpaperPassPrompt = false
                        viewModel.buyCustomWallpaperPass(24) { success, msg ->
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            if (success) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        }
                    },
                    enabled = totalXP >= 250,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (totalXP >= 250) "Unlock Now (250 XP)" else "Need ${250 - totalXP} More XP")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showWallpaperPassPrompt = false
                        showXPShop = true
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Open XP Shop")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomListPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Settings & Profile",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Themes, study goals, and local storage",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

        // Theme Palette Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Color Theme & Aesthetics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = THEME_OPTIONS,
                            key = { it.key }
                        ) { option ->
                            val isSelected = currentTheme == option.key
                            Card(
                                modifier = Modifier
                                    .clickable { viewModel.setTheme(option.key) }
                                    .testTag("theme_card_${option.key}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = null,
                                        tint = option.color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = option.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                                    text = "Wallpapers are disabled in Light themes to ensure maximum text sharpness and timer visibility. Switch to a Dark theme (Midnight, Pitch Black, Dark, Ocean) to use wallpapers.",
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
                                            fontSize = 10.sp,
                                            maxLines = 2,
                                            lineHeight = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Custom Picture from Gallery Option (24-Hour Pass)
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
                                        text = if (isWallpaperPassActive) "✨ Pass Active • $wallpaperPassRemaining" else "🔒 24h Pass (250 XP)",
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
                                            showWallpaperPassPrompt = true
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
                                            !isWallpaperPassActive -> "Unlock 24h Pass (250 XP)"
                                            customWallpaperUri != null -> "Change Photo"
                                            else -> "Pick from Gallery"
                                        },
                                        fontSize = 12.sp
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
                        Text("Set Daily Target Manually")
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
                            )
                        )
                    }
                }
            }
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

            Spacer(modifier = Modifier.height(80.dp))
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
                Text("This will clear your study session logs, XP, and streaks back to zero. This cannot be undone.")
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
                    Text("Save Target")
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
