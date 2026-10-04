package com.aistudio.studyos.data.local

/**
 * Single source of truth for persisted theme keys.
 *
 * Removed/legacy keys are normalized here so existing installs cannot keep
 * an unsupported theme value after an app update.
 */
object ThemeCatalog {
    const val DEFAULT_THEME = "pitch_black"

    val supportedKeys: Set<String> = setOf(
        "pitch_black",
        "obsidian_gold",
        "light",
        "sky_night",
        "learning_green",
        "sunrise",
        "cyberpunk",
        "cyber_runner"
    )

    private val legacyAliases = mapOf(
        "midnight" to DEFAULT_THEME,
        "espresso" to DEFAULT_THEME,
        "forest" to DEFAULT_THEME,
        "dark" to "obsidian_gold",
        "ocean" to "sky_night",
        "mint" to "learning_green"
    )

    fun normalize(themeKey: String?): String {
        val key = themeKey?.trim().orEmpty()
        if (key in supportedKeys) return key
        return legacyAliases[key] ?: DEFAULT_THEME
    }
}
