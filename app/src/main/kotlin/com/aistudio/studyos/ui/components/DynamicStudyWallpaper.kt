package com.aistudio.studyos.ui.components

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.studyos.R

/**
 * Premium 9:16 mobile portrait rain wallpapers.
 * Scaled and framed specifically to look natural, aesthetic, and avoid awkward zoom/cropping on phones.
 */
enum class WallpaperStyle(
    val id: String,
    val title: String,
    val description: String,
    @DrawableRes val drawableRes: Int?
) {
    CAFE_BOKEH(
        id = "cafe_bokeh",
        title = "Cafe Rain Bokeh",
        description = "Cozy cafe window with dreamy neon & warm street bokeh lights in rain",
        drawableRes = R.drawable.img_rain_cafe_bokeh
    ),
    ZEN_GARDEN(
        id = "zen_garden",
        title = "Zen Garden Rain",
        description = "Serene Japanese garden pavilion enveloped in gentle rain",
        drawableRes = R.drawable.img_rain_zen_garden
    ),
    COZY_WINDOW(
        id = "cozy_window",
        title = "Night Window Rain",
        description = "Glistening raindrops sliding down glass with warm dim ambient streetlights",
        drawableRes = R.drawable.img_rain_cozy_window
    ),
    MISTY_WOODS(
        id = "misty_woods",
        title = "Misty Forest Rain",
        description = "Lush green pine forest enveloped in peaceful thunderstorm & mist",
        drawableRes = R.drawable.img_rain_misty_woods
    ),
    CUSTOM(
        id = "custom",
        title = "Custom Photo",
        description = "Your own aesthetic picture picked from gallery",
        drawableRes = null
    )
}

/**
 * High-definition rain wallpaper engine with optimal phone framing & soft contrast gradient.
 */
@Composable
fun DynamicStudyWallpaper(
    style: WallpaperStyle,
    themePreset: String,
    opacity: Float,
    dimOverlay: Float = 0f,
    customUri: String? = null,
    modifier: Modifier = Modifier
) {
    val isLight = remember(themePreset) {
        themePreset in listOf("light", "paper", "mint", "sunrise", "clean_indigo", "soft_mint")
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            style == WallpaperStyle.CUSTOM && !customUri.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(customUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Custom Study Wallpaper",
                    contentScale = ContentScale.Crop,
                    alpha = opacity,
                    modifier = Modifier.fillMaxSize()
                )
            }
            style.drawableRes != null -> {
                Image(
                    painter = painterResource(id = style.drawableRes),
                    contentDescription = style.title,
                    // Pre-cropped to 9:16 mobile aspect ratio so it fits seamlessly without over-zooming
                    contentScale = ContentScale.Crop,
                    alpha = opacity,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // Default fallback directly to Cafe Rain Bokeh
                Image(
                    painter = painterResource(id = R.drawable.img_rain_cafe_bokeh),
                    contentDescription = "Cafe Rain Bokeh",
                    contentScale = ContentScale.Crop,
                    alpha = opacity,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Theme-adaptive protective overlay ensuring timers, indicators, and text remain crystal clear
        val baseOverlayAlpha = if (isLight) {
            // In light themes, blend with light background color to keep dark text completely legible
            (dimOverlay + 0.15f + (1f - opacity) * 0.35f).coerceIn(0.12f, 0.88f)
        } else {
            // In dark themes, ensure deep dark backing so bright/white text never gets washed out
            (dimOverlay + 0.25f + (1f - opacity) * 0.30f).coerceIn(0.20f, 0.90f)
        }

        val overlayColor = when (themePreset) {
            "pitch_black" -> Color.Black
            "ocean" -> Color(0xFF060E18)
            "paper" -> Color(0xFFF7F4EB)
            "mint" -> Color(0xFFF0FDF4)
            "sunrise" -> Color(0xFFFFF7ED)
            "clean_indigo" -> Color(0xFFF8FAFC)
            "soft_mint" -> Color(0xFFF5FBFA)
            "light" -> Color(0xFFF8FAFC)
            "deep_teal" -> Color(0xFF071414)
            "midnight_indigo" -> Color(0xFF0B1020)
            else -> Color(0xFF0C0E17) // midnight & default
        }

        if (baseOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor.copy(alpha = baseOverlayAlpha))
            )
        }
    }
}
