package com.aistudio.studyos.data.repository

data class StudyAchievement(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean
)

object GamificationCalculator {
    fun achievements(totalMinutes: Int, streakDays: Int, sessionCount: Int): List<StudyAchievement> {
        val minutes = totalMinutes.coerceAtLeast(0)
        val streak = streakDays.coerceAtLeast(0)
        val sessions = sessionCount.coerceAtLeast(0)
        return listOf(
            StudyAchievement("first_session", "First Focus", "Complete your first study session", sessions >= 1),
            StudyAchievement("one_hour", "One Hour", "Study for 60 total minutes", minutes >= 60),
            StudyAchievement("five_hours", "Five Hour Scholar", "Study for 300 total minutes", minutes >= 300),
            StudyAchievement("ten_hours", "Deep Commitment", "Study for 600 total minutes", minutes >= 600),
            StudyAchievement("three_day_streak", "3-Day Streak", "Study on 3 consecutive days", streak >= 3),
            StudyAchievement("seven_day_streak", "7-Day Streak", "Study on 7 consecutive days", streak >= 7)
        )
    }
}
