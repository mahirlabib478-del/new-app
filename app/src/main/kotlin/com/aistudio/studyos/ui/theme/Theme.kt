package com.aistudio.studyos.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PitchBlackColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF141414),
    onPrimaryContainer = CyanAccent,
    secondary = Color(0xFF38BDF8),
    background = PitchBlackBg,
    onBackground = Color.White,
    surface = PitchBlackSurface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF202020),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF343434),
    outlineVariant = Color(0xFF252525)
)

private val CyberpunkColorScheme = darkColorScheme(
    primary = CyberpunkPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1230),
    onPrimaryContainer = Color(0xFFFF8FBD),
    secondary = CyberpunkCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF10343B),
    onSecondaryContainer = Color(0xFF8EFAFF),
    background = CyberpunkBg,
    onBackground = Color(0xFFF9F5FF),
    surface = CyberpunkSurface,
    onSurface = Color(0xFFF9F5FF),
    surfaceVariant = Color(0xFF261844),
    onSurfaceVariant = Color(0xFFB9A9CC),
    outline = Color(0xFF6C3A78)
)

private val CyberRunnerColorScheme = lightColorScheme(
    primary = CyberRunnerRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEE2E2),
    onPrimaryContainer = Color(0xFF7F1D1D),
    secondary = CyberRunnerCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    background = CyberRunnerBg,
    onBackground = CyberRunnerCarbon,
    surface = CyberRunnerSurface,
    onSurface = CyberRunnerCarbon,
    surfaceVariant = CyberRunnerSurface,
    onSurfaceVariant = Color(0xFF44444B),
    outline = Color(0xFFD4D4D8),
    outlineVariant = Color(0xFFE4E4E7)
)

private val SkyNightColorScheme = darkColorScheme(
    primary = SkyNightAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF102A43),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFF38BDF8),
    background = SkyNightBg,
    onBackground = Color(0xFFF0F9FF),
    surface = SkyNightSurface,
    onSurface = Color(0xFFF0F9FF),
    surfaceVariant = Color(0xFF163653),
    onSurfaceVariant = Color(0xFFA9B9C9),
    outline = Color(0xFF2C4B67),
    outlineVariant = Color(0xFF1B334B)
)

private val LearningGreenColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFF0D3),
    onPrimaryContainer = Color(0xFF214D15),
    secondary = Color(0xFF72B84A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7F1DF),
    onSecondaryContainer = Color(0xFF31502A),
    tertiary = Color(0xFFFFC800),
    onTertiary = Color(0xFF3D3000),
    background = Color(0xFFEFF2E8),
    onBackground = Color(0xFF293026),
    surface = Color(0xFFF5F7F0),
    onSurface = Color(0xFF293026),
    surfaceVariant = Color(0xFFECEFE6),
    onSurfaceVariant = Color(0xFF3F493C),
    outline = Color(0xFFB7C5AD),
    outlineVariant = Color(0xFFD2DCCB)
)

private val SunriseColorScheme = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFED7AA),
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = Color(0xFFEA580C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFF9A3412),
    background = SunriseBg,
    onBackground = Color(0xFF292524),
    surface = SunriseSurface,
    onSurface = Color(0xFF292524),
    surfaceVariant = SunriseSurface,
    onSurfaceVariant = Color(0xFF49433D),
    outline = Color(0xFFE0C4A8),
    outlineVariant = Color(0xFFE9DED1)
)

private val ObsidianGoldColorScheme = darkColorScheme(
    primary = Color(0xFFFFD60A),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF111111),
    onPrimaryContainer = Color(0xFFFFD60A),
    secondary = Color(0xFFFFC107),
    onSecondary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = Color(0xFFA3A3A3),
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF1F1F1F)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF3B82F6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF1E1B4B),
    background = Color(0xFFF0F2EC),
    onBackground = Color(0xFF242820),
    surface = Color(0xFFF8F9F5),
    onSurface = Color(0xFF242820),
    surfaceVariant = Color(0xFFF8F9F5),
    onSurfaceVariant = Color(0xFF454C42),
    outline = Color(0xFFD0D6CB),
    outlineVariant = Color(0xFFDEE3D9)
)

private val StudyOSShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp)
)

fun isLightPreset(preset: String): Boolean = preset in setOf("learning_green", "sunrise", "light", "cyber_runner")

@Composable
fun StudyOSTheme(
    preset: String = "pitch_black",
    content: @Composable () -> Unit
) {
    val colorScheme = when (preset) {
        "pitch_black" -> PitchBlackColorScheme
        "cyberpunk" -> CyberpunkColorScheme
        "cyber_runner" -> CyberRunnerColorScheme
        "obsidian_gold" -> ObsidianGoldColorScheme
        "light" -> LightColorScheme
        "sky_night" -> SkyNightColorScheme
        "learning_green" -> LearningGreenColorScheme
        "sunrise" -> SunriseColorScheme
        else -> PitchBlackColorScheme
    }

    val isDark = when (preset) {
        "learning_green", "sunrise", "light", "cyber_runner" -> false
        else -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StudyOSShapes,
        content = content
    )
}
