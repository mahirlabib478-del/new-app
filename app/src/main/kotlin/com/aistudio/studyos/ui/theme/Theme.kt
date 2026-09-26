package com.aistudio.studyos.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
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
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFA0A0A0)
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
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF52525B),
    outline = Color(0xFFD4D4D8),
    outlineVariant = Color(0xFFE4E4E7)
)

private val OceanColorScheme = darkColorScheme(
    primary = OceanAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF13283E),
    onPrimaryContainer = Color(0xFF7DD3FC),
    secondary = Color(0xFF38BDF8),
    background = OceanBg,
    onBackground = Color(0xFFE0F2FE),
    surface = OceanSurface,
    onSurface = Color(0xFFE0F2FE),
    surfaceVariant = Color(0xFF13283E),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val MintColorScheme = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA7F3D0),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    background = Color(0xFFF2FBF6),
    onBackground = Color(0xFF064E3B),
    surface = Color.White,
    onSurface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFFE2F7EB),
    onSurfaceVariant = Color(0xFF065F46),
    outline = Color(0xFFA7F3D0),
    outlineVariant = Color(0xFFD1FAE5)
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
    background = Color(0xFFFFF9F5),
    onBackground = Color(0xFF292524),
    surface = Color.White,
    onSurface = Color(0xFF292524),
    surfaceVariant = Color(0xFFFFEDE0),
    onSurfaceVariant = Color(0xFF57534E),
    outline = Color(0xFFFDBA74),
    outlineVariant = Color(0xFFFFEDD5)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF94A3B8),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF273449),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = Color(0xFF60A5FA),
    background = Color(0xFF111827),
    onBackground = Color(0xFFF9FAFB),
    surface = Color(0xFF1F2937),
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = Color(0xFF273449),
    onSurfaceVariant = Color(0xFFCBD5E1)
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
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

fun isLightPreset(preset: String): Boolean = preset in setOf("mint", "sunrise", "light", "cyber_runner")

@Composable
fun StudyOSTheme(
    preset: String = "pitch_black",
    content: @Composable () -> Unit
) {
    val colorScheme = when (preset) {
        "pitch_black" -> PitchBlackColorScheme
        "cyberpunk" -> CyberpunkColorScheme
        "cyber_runner" -> CyberRunnerColorScheme
        "dark" -> DarkColorScheme
        "light" -> LightColorScheme
        "ocean" -> OceanColorScheme
        "mint" -> MintColorScheme
        "sunrise" -> SunriseColorScheme
        else -> PitchBlackColorScheme
    }

    val isDark = when (preset) {
        "mint", "sunrise", "light", "cyber_runner" -> false
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
        content = content
    )
}
