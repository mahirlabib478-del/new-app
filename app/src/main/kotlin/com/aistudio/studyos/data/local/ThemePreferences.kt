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

    companion object {
        private const val KEY_THEME = "selected_theme_preset"
    }
}
