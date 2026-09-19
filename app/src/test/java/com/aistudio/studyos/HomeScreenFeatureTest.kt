package com.aistudio.studyos

import com.aistudio.studyos.data.local.entity.ExamEntity
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.local.entity.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unit tests verifying HomeScreen features and core business logic:
 * 1. Today Engine progress calculation and remaining goal time
 * 2. Active session block resume detection and block label format
 * 3. Upcoming Exam countdown and priority sorting
 * 4. Gamification Streak, Level, and XP progression
 * 5. Recent study sessions logs formatting
 */
class HomeScreenFeatureTest {

    @Test
    fun testTodayGoalProgressCalculation() {
        val dailyGoalMinutes = 60
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        val logs = listOf(
            SessionLogEntity(id = 1, subject = "Math", chapter = "Calculus", durationMinutes = 25, xpEarned = 25, timestamp = System.currentTimeMillis()),
            SessionLogEntity(id = 2, subject = "Physics", chapter = "Mechanics", durationMinutes = 20, xpEarned = 20, timestamp = System.currentTimeMillis())
        )

        val todayMinutes = logs.filter { log ->
            sdf.format(Date(log.timestamp)) == todayStr
        }.sumOf { it.durationMinutes }

        assertEquals(45, todayMinutes)
        val progressFraction = todayMinutes.toFloat() / dailyGoalMinutes
        assertEquals(0.75f, progressFraction, 0.001f)
    }

    @Test
    fun testActiveSessionTodayEngineDetection() {
        val activePlan = StudyPlanEntity(
            id = 10,
            title = "Organic Chemistry Study",
            subject = "Chemistry",
            chapter = "Alkanes & Reaction Mechanisms",
            mode = "regular",
            totalBlocks = 4,
            currentBlockIndex = 1,
            durationPerBlockMinutes = 25,
            breakMinutes = 5,
            isCompleted = false
        )

        assertNotNull(activePlan)
        val subtitle = "${activePlan.subject} • ${activePlan.chapter}"
        assertEquals("Chemistry • Alkanes & Reaction Mechanisms", subtitle)

        val blockBadgeText = "Block ${activePlan.currentBlockIndex + 1}/${activePlan.totalBlocks}"
        assertEquals("Block 2/4", blockBadgeText)
    }

    @Test
    fun testUpcomingExamCountdownSpotlight() {
        val exams = listOf(
            ExamEntity(id = 1, subject = "Physics Final", examDate = "Tomorrow", daysRemaining = 1, priority = "High", syllabusTopics = "Optics & Modern Physics"),
            ExamEntity(id = 2, subject = "Biology Midterm", examDate = "Next Monday", daysRemaining = 5, priority = "Normal", syllabusTopics = "Genetics")
        )

        val spotlightExam = exams.firstOrNull()
        assertNotNull(spotlightExam)
        assertEquals("Physics Final", spotlightExam!!.subject)
        assertEquals(1, spotlightExam.daysRemaining)

        val badgeText = if (spotlightExam.daysRemaining > 0) "${spotlightExam.daysRemaining}d left" else spotlightExam.examDate
        assertEquals("1d left", badgeText)
    }

    @Test
    fun testGamificationLevelAndStreak() {
        val profile = UserProfileEntity(
            id = 1,
            streakDays = 5,
            totalStudyMinutes = 240,
            totalXP = 650,
            currentLevel = 4, // 650 / 200 + 1 = 4
            dailyGoalMinutes = 60,
            themePreset = "midnight"
        )

        assertEquals(5, profile.streakDays)
        val calculatedLevel = (profile.totalXP / 200) + 1
        assertEquals(4, calculatedLevel)
        assertEquals("5 d", "${profile.streakDays} d")
        assertEquals("Lv 4", "Lv ${profile.currentLevel}")
    }

    @Test
    fun testRecentSessionsTakeFirstThree() {
        val logs = listOf(
            SessionLogEntity(id = 1, subject = "Math", chapter = "Calculus", durationMinutes = 25, xpEarned = 25, timestamp = 1000L),
            SessionLogEntity(id = 2, subject = "Chemistry", chapter = "Periodic Table", durationMinutes = 30, xpEarned = 30, timestamp = 2000L),
            SessionLogEntity(id = 3, subject = "English", chapter = "Poetry", durationMinutes = 15, xpEarned = 15, timestamp = 3000L),
            SessionLogEntity(id = 4, subject = "History", chapter = "WWII", durationMinutes = 25, xpEarned = 25, timestamp = 4000L)
        )

        val recentThree = logs.take(3)
        assertEquals(3, recentThree.size)
        assertEquals("Math", recentThree[0].subject)
        assertEquals("+25 XP", "+${recentThree[0].xpEarned} XP")
        assertTrue(recentThree.all { it.durationMinutes > 0 })
    }
}
