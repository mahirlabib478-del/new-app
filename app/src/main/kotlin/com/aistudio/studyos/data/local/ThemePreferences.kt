package com.aistudio.studyos.data.local

import android.content.Context
import android.content.SharedPreferences

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("study_os_theme_prefs", Context.MODE_PRIVATE)

    fun getThemePreset(): String {
        return prefs.getString(KEY_THEME, "midnight") ?: "midnight"
    }

    fun setThemePreset(themeKey: String) {
        prefs.edit().putString(KEY_THEME, themeKey).apply()
    }

    // Wallpaper configuration
    fun isWallpaperEnabled(): Boolean {
        return prefs.getBoolean(KEY_WALLPAPER_ENABLED, true)
    }

    fun setWallpaperEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WALLPAPER_ENABLED, enabled).apply()
    }

    fun isFocusWallpaperEnabled(): Boolean {
        return prefs.getBoolean(KEY_FOCUS_WALLPAPER_ENABLED, true)
    }

    fun setFocusWallpaperEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FOCUS_WALLPAPER_ENABLED, enabled).apply()
    }

    fun getWallpaperOpacity(): Float {
        return prefs.getFloat(KEY_WALLPAPER_OPACITY, 0.75f)
    }

    fun setWallpaperOpacity(opacity: Float) {
        prefs.edit().putFloat(KEY_WALLPAPER_OPACITY, opacity.coerceIn(0.1f, 1.0f)).apply()
    }

    fun getThemeWallpaperStyle(themePreset: String): String {
        val defaultStyle = "cafe_bokeh"
        val saved = prefs.getString("${KEY_STYLE_PREFIX}_$themePreset", defaultStyle) ?: defaultStyle
        val validStyles = listOf("cafe_bokeh", "zen_garden", "cozy_window", "misty_woods", "custom")
        return if (saved in validStyles) {
            saved
        } else {
            defaultStyle
        }
    }

    fun setThemeWallpaperStyle(themePreset: String, styleId: String) {
        prefs.edit().putString("${KEY_STYLE_PREFIX}_$themePreset", styleId).apply()
    }

    fun getCustomWallpaperUri(): String? {
        return prefs.getString(KEY_CUSTOM_WALLPAPER_URI, null)
    }

    fun setCustomWallpaperUri(uriString: String?) {
        if (uriString == null) {
            prefs.edit().remove(KEY_CUSTOM_WALLPAPER_URI).apply()
        } else {
            prefs.edit().putString(KEY_CUSTOM_WALLPAPER_URI, uriString).apply()
        }
    }

    fun getCustomAudioUri(): String? {
        return prefs.getString(KEY_CUSTOM_AUDIO_URI, null)
    }

    fun getCustomAudioName(): String? {
        return prefs.getString(KEY_CUSTOM_AUDIO_NAME, null)
    }

    fun setCustomAudio(uriString: String?, displayName: String?) {
        if (uriString == null) {
            prefs.edit()
                .remove(KEY_CUSTOM_AUDIO_URI)
                .remove(KEY_CUSTOM_AUDIO_NAME)
                .apply()
        } else {
            prefs.edit()
                .putString(KEY_CUSTOM_AUDIO_URI, uriString)
                .putString(KEY_CUSTOM_AUDIO_NAME, displayName ?: "Custom Audio")
                .apply()
        }
    }

    companion object {
        private const val KEY_THEME = "selected_theme_preset"
        private const val KEY_WALLPAPER_ENABLED = "wallpaper_master_enabled"
        private const val KEY_FOCUS_WALLPAPER_ENABLED = "wallpaper_focus_enabled"
        private const val KEY_WALLPAPER_OPACITY = "wallpaper_opacity_level"
        private const val KEY_STYLE_PREFIX = "wallpaper_style"
        private const val KEY_CUSTOM_WALLPAPER_URI = "wallpaper_custom_user_uri"
        private const val KEY_CUSTOM_AUDIO_URI = "ambient_custom_audio_uri"
        private const val KEY_CUSTOM_AUDIO_NAME = "ambient_custom_audio_name"
    }
}
