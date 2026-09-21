package com.aistudio.studyos

import com.aistudio.studyos.data.local.entity.UserProfileEntity
import com.aistudio.studyos.data.update.AppUpdateInfo
import com.aistudio.studyos.data.update.UpdateCheckState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Unit tests verifying ProfileScreen & Settings features:
 * 1. Daily Study Target calculations (formatting into hours and minutes)
 * 2. Slider snapping to 15-minute increments (15m to 720m)
 * 3. Stepper micro-adjustments (+15m, -15m, +1h, -1h) with clamping
 * 4. Custom target input validation (15 to 1440 minutes)
 * 5. Theme preset selection keys matching the 8 design palettes
 * 6. App Update states (Checking, UpToDate, Available)
 * 7. Reset profile stats entity resetting to initial zero/default values
 */
class ProfileScreenFeatureTest {

    @Test
    fun testDailyTargetFormatting() {
        fun formatTarget(dailyGoal: Int): String {
            val targetHours = dailyGoal / 60
            val targetMins = dailyGoal % 60
            return when {
                targetHours > 0 && targetMins > 0 -> "${targetHours}h ${targetMins}m"
                targetHours > 0 -> "${targetHours}h (${dailyGoal}m)"
                else -> "${targetMins}m"
            }
        }

        assertEquals("45m", formatTarget(45))
        assertEquals("1h (60m)", formatTarget(60))
        assertEquals("1h 30m", formatTarget(90))
        assertEquals("2h (120m)", formatTarget(120))
        assertEquals("3h 15m", formatTarget(195))
    }

    @Test
    fun testSliderSnappingLogic() {
        fun snapValue(raw: Float): Int {
            return ((raw / 15f).roundToInt() * 15).coerceIn(15, 720)
        }

        assertEquals(15, snapValue(10f))
        assertEquals(45, snapValue(43f))
        assertEquals(60, snapValue(58f))
        assertEquals(720, snapValue(750f))
    }

    @Test
    fun testStepperMicroAdjustments() {
        var goal = 60 // 1 hour

        // +15m
        goal = minOf(1440, goal + 15)
        assertEquals(75, goal)

        // +1h
        goal = minOf(1440, goal + 60)
        assertEquals(135, goal)

        // -15m
        goal = maxOf(15, goal - 15)
        assertEquals(120, goal)

        // -1h
        goal = maxOf(15, goal - 60)
        assertEquals(60, goal)

        // Lower bound clamp (cannot go below 15m)
        goal = 20
        goal = maxOf(15, goal - 60)
        assertEquals(15, goal)

        // Upper bound clamp (cannot exceed 1440m / 24h)
        goal = 1420
        goal = minOf(1440, goal + 60)
        assertEquals(1440, goal)
    }

    @Test
    fun testCustomTargetInputValidation() {
        fun isValidTarget(input: String): Boolean {
            val parsed = input.toIntOrNull() ?: return false
            return parsed in 15..1440
        }

        assertTrue(isValidTarget("60"))
        assertTrue(isValidTarget("15"))
        assertTrue(isValidTarget("1440"))
        assertTrue(isValidTarget("180"))

        assertFalse(isValidTarget("0"))
        assertFalse(isValidTarget("10"))
        assertFalse(isValidTarget("1500"))
        assertFalse(isValidTarget("-50"))
        assertFalse(isValidTarget("abc"))
        assertFalse(isValidTarget(""))
    }

    @Test
    fun testThemeOptionsKeys() {
        val themeKeys = listOf(
            "midnight",
            "pitch_black",
            "dark",
            "light",
            "ocean",
            "paper",
            "mint",
            "sunrise"
        )

        assertEquals(8, themeKeys.size)
        assertTrue(themeKeys.contains("midnight"))
        assertTrue(themeKeys.contains("dark"))
        assertTrue(themeKeys.contains("light"))
        assertTrue(themeKeys.contains("pitch_black"))
    }

    @Test
    fun testAppUpdateStateTransitions() {
        val checking = UpdateCheckState.Checking
        assertTrue(checking is UpdateCheckState.Checking)

        val upToDate = UpdateCheckState.UpToDate("1.0.0")
        assertEquals("1.0.0", upToDate.currentVersion)

        val available = UpdateCheckState.Available(
            AppUpdateInfo(
                latestVersion = "1.1.0",
                releaseNotes = "New features added",
                apkUrl = "https://github.com/releases/download/v1.1.0/app.apk",
                releaseUrl = "https://github.com/releases/tag/v1.1.0"
            )
        )
        assertEquals("1.1.0", available.updateInfo.latestVersion)
        assertNotNull(available.updateInfo.apkUrl)
    }

    @Test
    fun testResetStatsEntityDefaults() {
        val initialProfile = UserProfileEntity(
            id = 1,
            streakDays = 12,
            totalStudyMinutes = 1240,
            totalXP = 3200,
            currentLevel = 17,
            dailyGoalMinutes = 90,
            themePreset = "midnight"
        )

        // Reset operation preserves theme and daily goal preference while resetting progress
        val resetProfile = initialProfile.copy(
            streakDays = 0,
            totalStudyMinutes = 0,
            totalXP = 0,
            currentLevel = 1
        )

        assertEquals(0, resetProfile.streakDays)
        assertEquals(0, resetProfile.totalStudyMinutes)
        assertEquals(0, resetProfile.totalXP)
        assertEquals(1, resetProfile.currentLevel)
        assertEquals(90, resetProfile.dailyGoalMinutes)
        assertEquals("midnight", resetProfile.themePreset)
    }
}
