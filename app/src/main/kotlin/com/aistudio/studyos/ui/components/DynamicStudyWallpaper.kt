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
        themePreset in listOf("light", "paper", "mint", "sunrise")
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

        // Soft dark veil overlay ensuring all study elements, timers, tasks & charts remain crisp & readable
        val overlayAlpha = (dimOverlay + (1f - opacity) * 0.28f).coerceIn(0f, 0.85f)
        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        (if (isLight) Color(0xFF0F172A) else Color(0xFF020617)).copy(alpha = overlayAlpha)
                    )
            )
        }
    }
}
