package com.aistudio.studyos.data.local

import android.content.Context
import android.content.SharedPreferences

import com.aistudio.studyos.data.local.entity.SessionLogEntity

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("study_os_theme_prefs", Context.MODE_PRIVATE)

    fun getThemePreset(): String {
        return prefs.getString(KEY_THEME, "pitch_black") ?: "pitch_black"
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

    fun getCustomAudioList(): List<UploadedAudio> {
        val raw = prefs.getString(KEY_CUSTOM_AUDIO_LIST, null)
        if (raw != null) {
            try {
                val array = org.json.JSONArray(raw)
                val list = mutableListOf<UploadedAudio>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        UploadedAudio(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            uri = obj.getString("uri")
                        )
                    )
                }
                return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Fallback / migrate legacy single audio if present
        val legacyUri = prefs.getString(KEY_CUSTOM_AUDIO_URI, null)
        if (legacyUri != null) {
            val legacyName = prefs.getString(KEY_CUSTOM_AUDIO_NAME, "Custom Audio") ?: "Custom Audio"
            val single = listOf(UploadedAudio("legacy_1", legacyName, legacyUri))
            saveCustomAudioList(single)
            setSelectedCustomAudioId("legacy_1")
            return single
        }
        return emptyList()
    }

    fun saveCustomAudioList(list: List<UploadedAudio>) {
        val array = org.json.JSONArray()
        for (item in list) {
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("uri", item.uri)
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_AUDIO_LIST, array.toString()).apply()
    }

    fun addCustomAudio(name: String, uri: String): UploadedAudio {
        val current = getCustomAudioList().toMutableList()
        val existing = current.find { it.uri == uri }
        if (existing != null) {
            setSelectedCustomAudioId(existing.id)
            return existing
        }
        val item = UploadedAudio(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            uri = uri
        )
        current.add(0, item)
        saveCustomAudioList(current)
        setSelectedCustomAudioId(item.id)
        // Keep legacy keys updated as well
        prefs.edit()
            .putString(KEY_CUSTOM_AUDIO_URI, uri)
            .putString(KEY_CUSTOM_AUDIO_NAME, name)
            .apply()
        return item
    }

    fun removeCustomAudio(id: String) {
        val current = getCustomAudioList().toMutableList()
        current.removeAll { it.id == id }
        saveCustomAudioList(current)
        if (getSelectedCustomAudioId() == id) {
            val nextSelected = current.firstOrNull()
            setSelectedCustomAudioId(nextSelected?.id)
            if (nextSelected != null) {
                prefs.edit()
                    .putString(KEY_CUSTOM_AUDIO_URI, nextSelected.uri)
                    .putString(KEY_CUSTOM_AUDIO_NAME, nextSelected.name)
                    .apply()
            } else {
                prefs.edit()
                    .remove(KEY_CUSTOM_AUDIO_URI)
                    .remove(KEY_CUSTOM_AUDIO_NAME)
                    .apply()
            }
        }
    }

    fun getSelectedCustomAudioId(): String? {
        return prefs.getString(KEY_SELECTED_AUDIO_ID, null)
    }

    fun setSelectedCustomAudioId(id: String?) {
        if (id == null) {
            prefs.edit().remove(KEY_SELECTED_AUDIO_ID).apply()
        } else {
            prefs.edit().putString(KEY_SELECTED_AUDIO_ID, id).apply()
            val item = getCustomAudioList().find { it.id == id }
            if (item != null) {
                prefs.edit()
                    .putString(KEY_CUSTOM_AUDIO_URI, item.uri)
                    .putString(KEY_CUSTOM_AUDIO_NAME, item.name)
                    .apply()
            }
        }
    }

    fun getCustomAudioUri(): String? {
        val selectedId = getSelectedCustomAudioId()
        if (selectedId != null) {
            val item = getCustomAudioList().find { it.id == selectedId }
            if (item != null) return item.uri
        }
        return prefs.getString(KEY_CUSTOM_AUDIO_URI, null)
    }

    fun getCustomAudioName(): String? {
        val selectedId = getSelectedCustomAudioId()
        if (selectedId != null) {
            val item = getCustomAudioList().find { it.id == selectedId }
            if (item != null) return item.name
        }
        return prefs.getString(KEY_CUSTOM_AUDIO_NAME, null)
    }

    fun setCustomAudio(uriString: String?, displayName: String?) {
        if (uriString == null) {
            prefs.edit()
                .remove(KEY_CUSTOM_AUDIO_URI)
                .remove(KEY_CUSTOM_AUDIO_NAME)
                .apply()
        } else {
            addCustomAudio(displayName ?: "Custom Audio", uriString)
        }
    }

    fun getCachedRecentSessions(): List<SessionLogEntity> {
        val raw = prefs.getString(KEY_CACHED_RECENT_SESSIONS, null) ?: return emptyList()
        return try {
            val array = org.json.JSONArray(raw)
            val list = mutableListOf<SessionLogEntity>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SessionLogEntity(
                        id = obj.optLong("id", 0L),
                        subject = obj.optString("subject", ""),
                        chapter = obj.optString("chapter", ""),
                        durationMinutes = obj.optInt("durationMinutes", 0),
                        mode = obj.optString("mode", "pomodoro"),
                        xpEarned = obj.optInt("xpEarned", 0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setCachedRecentSessions(logs: List<SessionLogEntity>) {
        try {
            val array = org.json.JSONArray()
            for (log in logs.take(10)) {
                val obj = org.json.JSONObject()
                obj.put("id", log.id)
                obj.put("subject", log.subject)
                obj.put("chapter", log.chapter)
                obj.put("durationMinutes", log.durationMinutes)
                obj.put("mode", log.mode)
                obj.put("xpEarned", log.xpEarned)
                obj.put("timestamp", log.timestamp)
                array.put(obj)
            }
            prefs.edit().putString(KEY_CACHED_RECENT_SESSIONS, array.toString()).apply()
        } catch (_: Exception) {
        }
    }

    // ==========================================
    // 🛡️ Streak Shield & Temporary Passes
    // ==========================================

    fun getStreakShieldCount(): Int {
        return prefs.getInt(KEY_STREAK_SHIELD_COUNT, 0).coerceIn(0, 2)
    }

    fun setStreakShieldCount(count: Int) {
        prefs.edit().putInt(KEY_STREAK_SHIELD_COUNT, count.coerceIn(0, 2)).apply()
    }

    fun getLastShieldSavedDate(): String? {
        return prefs.getString(KEY_LAST_SHIELD_SAVED_DATE, null)
    }

    fun setLastShieldSavedDate(dateStr: String?) {
        if (dateStr == null) {
            prefs.edit().remove(KEY_LAST_SHIELD_SAVED_DATE).apply()
        } else {
            prefs.edit().putString(KEY_LAST_SHIELD_SAVED_DATE, dateStr).apply()
        }
    }

    fun getCustomWallpaperPassExpiresAt(): Long {
        return prefs.getLong(KEY_CUSTOM_WALLPAPER_PASS_EXPIRES, 0L)
    }

    fun setCustomWallpaperPassExpiresAt(expiresAt: Long) {
        prefs.edit().putLong(KEY_CUSTOM_WALLPAPER_PASS_EXPIRES, expiresAt).apply()
    }

    fun isCustomWallpaperPassActive(): Boolean {
        return getCustomWallpaperPassExpiresAt() > System.currentTimeMillis()
    }

    fun getCustomAudioPassExpiresAt(): Long {
        return prefs.getLong(KEY_CUSTOM_AUDIO_PASS_EXPIRES, 0L)
    }

    fun setCustomAudioPassExpiresAt(expiresAt: Long) {
        prefs.edit().putLong(KEY_CUSTOM_AUDIO_PASS_EXPIRES, expiresAt).apply()
    }

    fun isCustomAudioPassActive(): Boolean {
        return getCustomAudioPassExpiresAt() > System.currentTimeMillis()
    }

    fun getDoubleXpBoosterExpiresAt(): Long {
        return prefs.getLong(KEY_DOUBLE_XP_BOOSTER_EXPIRES, 0L)
    }

    fun setDoubleXpBoosterExpiresAt(expiresAt: Long) {
        prefs.edit().putLong(KEY_DOUBLE_XP_BOOSTER_EXPIRES, expiresAt).apply()
    }

    fun isDoubleXpBoosterActive(): Boolean {
        return getDoubleXpBoosterExpiresAt() > System.currentTimeMillis()
    }

    fun getXpBoosterMultiplier(): Int {
        return prefs.getInt(KEY_XP_BOOSTER_MULTIPLIER, 2).coerceIn(2, 3)
    }

    fun setXpBoosterMultiplier(multiplier: Int) {
        prefs.edit().putInt(KEY_XP_BOOSTER_MULTIPLIER, multiplier.coerceIn(2, 3)).apply()
    }

    fun getLastFreeXpDropClaimTime(): Long {
        return prefs.getLong(KEY_LAST_FREE_XP_DROP_CLAIM_TIME, 0L)
    }

    fun setLastFreeXpDropClaimTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_FREE_XP_DROP_CLAIM_TIME, timestamp).apply()
    }

    fun getFreeXpDropRemainingCooldownMs(): Long {
        val lastClaim = getLastFreeXpDropClaimTime()
        val elapsed = System.currentTimeMillis() - lastClaim
        val cooldownTotal = 30 * 60 * 1000L
        return (cooldownTotal - elapsed).coerceAtLeast(0L)
    }

    companion object {
        private const val KEY_CACHED_RECENT_SESSIONS = "cached_recent_sessions_list"
        private const val KEY_THEME = "selected_theme_preset"
        private const val KEY_WALLPAPER_ENABLED = "wallpaper_master_enabled"
        private const val KEY_FOCUS_WALLPAPER_ENABLED = "wallpaper_focus_enabled"
        private const val KEY_WALLPAPER_OPACITY = "wallpaper_opacity_level"
        private const val KEY_STYLE_PREFIX = "wallpaper_style"
        private const val KEY_CUSTOM_WALLPAPER_URI = "wallpaper_custom_user_uri"
        private const val KEY_CUSTOM_AUDIO_URI = "ambient_custom_audio_uri"
        private const val KEY_CUSTOM_AUDIO_NAME = "ambient_custom_audio_name"
        private const val KEY_CUSTOM_AUDIO_LIST = "ambient_custom_audio_list"
        private const val KEY_SELECTED_AUDIO_ID = "ambient_selected_audio_id"

        private const val KEY_STREAK_SHIELD_COUNT = "perk_streak_shield_count"
        private const val KEY_LAST_SHIELD_SAVED_DATE = "perk_last_shield_saved_date"
        private const val KEY_CUSTOM_WALLPAPER_PASS_EXPIRES = "perk_custom_wallpaper_pass_expires"
        private const val KEY_CUSTOM_AUDIO_PASS_EXPIRES = "perk_custom_audio_pass_expires"
        private const val KEY_DOUBLE_XP_BOOSTER_EXPIRES = "perk_double_xp_booster_expires"
        private const val KEY_XP_BOOSTER_MULTIPLIER = "perk_xp_booster_multiplier"
        private const val KEY_LAST_FREE_XP_DROP_CLAIM_TIME = "perk_last_free_xp_drop_claim_time"
    }
}
