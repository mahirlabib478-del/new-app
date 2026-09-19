package com.aistudio.studyos.data.repository

import com.aistudio.studyos.data.local.StudyDatabase
import com.aistudio.studyos.data.local.entity.ExamEntity
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class StudyRepository(private val database: StudyDatabase) {

    // Study Plan
    fun getActivePlan(): Flow<StudyPlanEntity?> = database.studyPlanDao().getActivePlan()
    fun getSavedPlans(): Flow<List<StudyPlanEntity>> = database.studyPlanDao().getSavedPlans()
    suspend fun savePlan(plan: StudyPlanEntity): Long = database.studyPlanDao().insertPlan(plan)
    suspend fun updatePlan(plan: StudyPlanEntity) = database.studyPlanDao().updatePlan(plan)
    suspend fun deletePlan(plan: StudyPlanEntity) = database.studyPlanDao().deletePlan(plan)
    suspend fun deletePlanById(id: Long) = database.studyPlanDao().deletePlanById(id)

    // Exams
    fun getAllExams(): Flow<List<ExamEntity>> = database.examDao().getAllExams()
    fun getUpcomingExams(): Flow<List<ExamEntity>> = database.examDao().getUpcomingExams()
    suspend fun addExam(exam: ExamEntity): Long = database.examDao().insertExam(exam)
    suspend fun updateExam(exam: ExamEntity) = database.examDao().updateExam(exam)
    suspend fun deleteExam(exam: ExamEntity) = database.examDao().deleteExam(exam)

    // Logs & Stats
    fun getAllLogs(): Flow<List<SessionLogEntity>> = database.sessionLogDao().getAllLogs()
    fun getRecentLogs(limit: Int = 10): Flow<List<SessionLogEntity>> = database.sessionLogDao().getRecentLogs(limit)
    fun getTotalMinutes(): Flow<Int?> = database.sessionLogDao().getTotalMinutes()
    suspend fun logSession(log: SessionLogEntity): Long = database.sessionLogDao().insertLog(log)
    suspend fun clearHistory() = database.sessionLogDao().clearAll()

    // Profile & Gamification
    fun getUserProfile(): Flow<UserProfileEntity?> = database.userProfileDao().getProfile()

    suspend fun recordCompletedSession(
        subject: String,
        chapter: String,
        durationMinutes: Int,
        mode: String
    ) {
        val xpGained = durationMinutes * 3
        database.sessionLogDao().insertLog(
            SessionLogEntity(
                subject = subject,
                chapter = chapter,
                durationMinutes = durationMinutes,
                mode = mode,
                xpEarned = xpGained
            )
        )

        val currentProfile = database.userProfileDao().getProfileSync() ?: UserProfileEntity()
        val newTotalMinutes = currentProfile.totalStudyMinutes + durationMinutes
        val newTotalXP = currentProfile.totalXP + xpGained
        val newLevel = (newTotalXP / 200) + 1

        database.userProfileDao().update(
            currentProfile.copy(
                totalStudyMinutes = newTotalMinutes,
                totalXP = newTotalXP,
                currentLevel = newLevel,
                streakDays = if (currentProfile.streakDays == 0) 1 else currentProfile.streakDays
            )
        )
    }

    suspend fun updateTheme(themeKey: String) {
        val currentProfile = database.userProfileDao().getProfileSync() ?: UserProfileEntity()
        database.userProfileDao().update(currentProfile.copy(themePreset = themeKey))
    }

    suspend fun updateDailyGoal(minutes: Int) {
        val currentProfile = database.userProfileDao().getProfileSync() ?: UserProfileEntity()
        database.userProfileDao().update(currentProfile.copy(dailyGoalMinutes = minutes))
    }

    suspend fun resetStats() {
        database.sessionLogDao().clearAll()
        database.userProfileDao().update(
            UserProfileEntity(
                id = 1,
                streakDays = 1,
                totalStudyMinutes = 0,
                totalXP = 0,
                currentLevel = 1,
                dailyGoalMinutes = 60,
                themePreset = "midnight"
            )
        )
    }
}
