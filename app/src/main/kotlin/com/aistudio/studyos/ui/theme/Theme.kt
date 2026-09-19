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

private val MidnightColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = MidnightCard,
    onPrimaryContainer = IndigoLight,
    secondary = Color(0xFF38BDF8),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    background = MidnightBg,
    onBackground = Color(0xFFF1F5F9),
    surface = MidnightSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = MidnightCard,
    onSurfaceVariant = Color(0xFF94A3B8)
)

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

private val EspressoColorScheme = darkColorScheme(
    primary = EspressoAccent,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2A201A),
    onPrimaryContainer = EspressoAccent,
    secondary = Color(0xFFE0A96D),
    background = EspressoBg,
    onBackground = Color(0xFFF5EBE0),
    surface = EspressoSurface,
    onSurface = Color(0xFFF5EBE0),
    surfaceVariant = Color(0xFF2A201A),
    onSurfaceVariant = Color(0xFFD6C7B2)
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

private val ForestColorScheme = darkColorScheme(
    primary = ForestAccent,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF132E1B),
    onPrimaryContainer = Color(0xFF6EE7B7),
    secondary = Color(0xFF34D399),
    background = ForestBg,
    onBackground = Color(0xFFECFDF5),
    surface = ForestSurface,
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = Color(0xFF132E1B),
    onSurfaceVariant = Color(0xFFA7F3D0)
)

private val PaperColorScheme = lightColorScheme(
    primary = Color(0xFF8B5A2B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DFC8),
    onPrimaryContainer = Color(0xFF3D250C),
    secondary = Color(0xFFB45309),
    background = PaperBg,
    onBackground = Color(0xFF292524),
    surface = PaperSurface,
    onSurface = Color(0xFF292524),
    surfaceVariant = Color(0xFFE5DDD0),
    onSurfaceVariant = Color(0xFF57534E)
)

private val MintColorScheme = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    primaryContainer = MintSurface,
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF14B8A6),
    background = MintBg,
    onBackground = Color(0xFF14532D),
    surface = Color.White,
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFE0F2FE),
    onSurfaceVariant = Color(0xFF4B5563)
)

private val SunriseColorScheme = lightColorScheme(
    primary = SunriseAccent,
    onPrimary = Color.White,
    primaryContainer = SunriseSurface,
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = Color(0xFFF97316),
    background = SunriseBg,
    onBackground = Color(0xFF1C1917),
    surface = Color.White,
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFFFEDD5),
    onSurfaceVariant = Color(0xFF78716C)
)

@Composable
fun StudyOSTheme(
    preset: String = "midnight",
    content: @Composable () -> Unit
) {
    val colorScheme = when (preset) {
        "pitch_black" -> PitchBlackColorScheme
        "espresso" -> EspressoColorScheme
        "ocean" -> OceanColorScheme
        "forest" -> ForestColorScheme
        "paper" -> PaperColorScheme
        "mint" -> MintColorScheme
        "sunrise" -> SunriseColorScheme
        else -> MidnightColorScheme
    }

    val isDark = when (preset) {
        "paper", "mint", "sunrise" -> false
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
