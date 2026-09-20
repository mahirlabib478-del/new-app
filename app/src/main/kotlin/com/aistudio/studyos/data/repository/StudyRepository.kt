package com.aistudio.studyos.data.repository

import com.aistudio.studyos.data.local.StudyDatabase
import com.aistudio.studyos.data.local.ThemePreferences
import com.aistudio.studyos.data.local.entity.ExamEntity
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.local.entity.StudyPlanItemCodec
import androidx.room.withTransaction
import com.aistudio.studyos.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StudyRepository(
    private val database: StudyDatabase,
    private val themePreferences: ThemePreferences
) {

    fun getInitialTheme(): String = themePreferences.getThemePreset()

    // Study Plan
    fun getActivePlan(): Flow<StudyPlanEntity?> = database.studyPlanDao().getActivePlan()
    fun getSavedPlans(): Flow<List<StudyPlanEntity>> = database.studyPlanDao().getSavedPlans()
    suspend fun savePlan(plan: StudyPlanEntity): Long = database.studyPlanDao().insertPlan(plan)
    suspend fun updatePlan(plan: StudyPlanEntity) = database.studyPlanDao().updatePlan(plan)
    suspend fun getPlanById(id: Long): StudyPlanEntity? = database.studyPlanDao().getPlanById(id)
    suspend fun deletePlan(plan: StudyPlanEntity) = database.studyPlanDao().deletePlan(plan)
    suspend fun deletePlanById(id: Long) = database.studyPlanDao().deletePlanById(id)
    suspend fun archiveOtherActivePlans(exceptId: Long) = database.studyPlanDao().archiveOtherActivePlans(exceptId)

    // Exams
    fun getAllExams(): Flow<List<ExamEntity>> = database.examDao().getAllExams()
    fun getUpcomingExams(): Flow<List<ExamEntity>> = database.examDao().getUpcomingExams()
    suspend fun addExam(exam: ExamEntity): Long = database.examDao().insertExam(exam)
    suspend fun updateExam(exam: ExamEntity) = database.examDao().updateExam(exam)
    suspend fun deleteExam(exam: ExamEntity) = database.examDao().deleteExam(exam)

    // Logs & Stats
    fun getAllLogs(): Flow<List<SessionLogEntity>> = database.sessionLogDao().getAllLogs()
    fun getRecentLogs(limit: Int = 10): Flow<List<SessionLogEntity>> = database.sessionLogDao().getRecentLogs(limit)
    fun getTodayMinutes(): Flow<Int> = database.sessionLogDao().getTodayMinutes()
    fun getTotalMinutes(): Flow<Int?> = database.sessionLogDao().getTotalMinutes()
    suspend fun logSession(log: SessionLogEntity): Long = database.sessionLogDao().insertLog(log)
    suspend fun deleteSessionLog(log: SessionLogEntity) {
        database.sessionLogDao().deleteLog(log)
        val profile = database.userProfileDao().getProfileSync()
        if (profile != null) {
            val updatedMinutes = (profile.totalStudyMinutes - log.durationMinutes).coerceAtLeast(0)
            val updatedXP = (profile.totalXP - log.xpEarned).coerceAtLeast(0)
            val updatedLevel = (updatedXP / 200) + 1
            database.userProfileDao().insertOrUpdate(
                profile.copy(
                    totalStudyMinutes = updatedMinutes,
                    totalXP = updatedXP,
                    currentLevel = updatedLevel
                )
            )
        }
    }
    suspend fun clearHistory() = database.sessionLogDao().clearAll()

    // Profile & Gamification
    fun getUserProfile(): Flow<UserProfileEntity?> = database.userProfileDao().getProfile()

    suspend fun ensureCleanInitialData() {
        val profile = database.userProfileDao().getProfileSync()
        val currentSavedTheme = themePreferences.getThemePreset()
        if (profile == null) {
            database.userProfileDao().insertOrUpdate(
                UserProfileEntity(
                    id = 1,
                    streakDays = 0,
                    totalStudyMinutes = 0,
                    totalXP = 0,
                    currentLevel = 1,
                    dailyGoalMinutes = 60,
                    themePreset = currentSavedTheme,
                    lastActiveDate = ""
                )
            )
        } else {
            if (profile.themePreset.isNotBlank() && profile.themePreset != currentSavedTheme) {
                themePreferences.setThemePreset(profile.themePreset)
            }
            // Check if streak was broken (last active date was before yesterday)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(cal.time)

            if (profile.lastActiveDate.isNotBlank() &&
                profile.lastActiveDate != todayStr &&
                profile.lastActiveDate != yesterdayStr &&
                profile.streakDays > 0
            ) {
                // Streak broken because more than 1 day missed without studying
                database.userProfileDao().insertOrUpdate(
                    profile.copy(streakDays = 0)
                )
            }

            if (profile.streakDays == 3 && profile.totalXP == 450 && profile.totalStudyMinutes == 150) {
                // Old dummy seed cleanup if updating from prior installation
                database.userProfileDao().insertOrUpdate(
                    profile.copy(
                        streakDays = 0,
                        totalStudyMinutes = 0,
                        totalXP = 0,
                        currentLevel = 1
                    )
                )
                val oldPlan = database.studyPlanDao().getPlanById(1)
                if (oldPlan?.title == "Calculus & Linear Algebra") {
                    database.studyPlanDao().deletePlanById(1)
                }
                database.sessionLogDao().clearAll()
            }
        }
    }

    suspend fun updatePlanProgress(planId: Long, blockIndex: Int, isCompleted: Boolean) {
        val plan = database.studyPlanDao().getPlanById(planId) ?: return
        val decoded = StudyPlanItemCodec.decode(plan.planItems)
        val items = decoded.flatMap { item ->
            buildList {
                var remaining = item.minutes.coerceIn(1, 720)
                while (remaining > 25) {
                    add(item.copy(minutes = 25))
                    remaining -= 25
                }
                add(item.copy(minutes = remaining))
            }
        }
        val nextItem = items.getOrNull(blockIndex)
        val nextStudySec = (nextItem?.minutes ?: plan.durationPerBlockMinutes).coerceAtLeast(1) * 60
        database.studyPlanDao().updatePlan(
            plan.copy(
                currentBlockIndex = blockIndex,
                durationPerBlockMinutes = nextItem?.minutes ?: plan.durationPerBlockMinutes,
                remainingSecondsInBlock = nextStudySec,
                isBreakPhase = false,
                isTimerRunning = false,
                endAtElapsedRealtime = 0L,
                endAtWallClockMillis = 0L,
                isCompleted = isCompleted,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateSessionProgress(
        planId: Long,
        remainingSec: Int,
        isBreak: Boolean,
        blockIndex: Int,
        isRunning: Boolean,
        endAtElapsedRealtime: Long,
        endAtWallClockMillis: Long,
        timerBootCount: Int,
        expectedEndAtElapsedRealtime: Long = 0L
    ) {
        database.studyPlanDao().updateSessionTimer(
            planId,
            remainingSec.coerceAtLeast(0),
            isBreak,
            blockIndex,
            isRunning,
            endAtElapsedRealtime,
            endAtWallClockMillis,
            timerBootCount,
            expectedEndAtElapsedRealtime
        )
    }

    suspend fun completePlanEarly(
        planId: Long,
        minutesStudied: Int,
        studiedSeconds: Int,
        subject: String,
        chapter: String,
        mode: String = "early_finish",
        nextBlockIndex: Int? = null
    ) {
        database.withTransaction {
            val plan = database.studyPlanDao().getPlanById(planId) ?: return@withTransaction
            database.studyPlanDao().updatePlan(
                plan.copy(
                    currentBlockIndex = nextBlockIndex ?: plan.currentBlockIndex,
                    remainingSecondsInBlock = 0,
                    isTimerRunning = false,
                    endAtElapsedRealtime = 0L,
                    endAtWallClockMillis = 0L,
                    accumulatedStudiedSeconds = plan.accumulatedStudiedSeconds + studiedSeconds.coerceAtLeast(0),
                    accumulatedBillableMinutes = plan.accumulatedBillableMinutes + minutesStudied.coerceAtLeast(0),
                    isCompleted = true,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            if (minutesStudied > 0) {
                recordCompletedSession(subject, chapter, minutesStudied, mode)
            }
        }
    }

    suspend fun commitFocusBlock(
        planId: Long,
        updatedPlan: StudyPlanEntity,
        subject: String,
        chapter: String,
        minutesStudied: Int,
        mode: String
    ) {
        database.withTransaction {
            if (minutesStudied > 0) {
                recordCompletedSession(subject, chapter, minutesStudied, mode)
            }
            database.studyPlanDao().updatePlan(updatedPlan)
        }
    }

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

        val currentProfile = database.userProfileDao().getProfileSync() ?: UserProfileEntity(
            id = 1,
            streakDays = 0,
            totalStudyMinutes = 0,
            totalXP = 0,
            currentLevel = 1,
            dailyGoalMinutes = 60,
            themePreset = "midnight"
        )
        val newTotalMinutes = currentProfile.totalStudyMinutes + durationMinutes
        val newTotalXP = currentProfile.totalXP + xpGained
        val newLevel = (newTotalXP / 200) + 1

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        val updatedStreak = when {
            currentProfile.lastActiveDate == todayStr -> {
                // Already studied today, maintain existing streak
                if (currentProfile.streakDays <= 0) 1 else currentProfile.streakDays
            }
            currentProfile.lastActiveDate == yesterdayStr -> {
                // Consecutive day! Increment streak
                currentProfile.streakDays + 1
            }
            else -> {
                // First day or streak was broken, restart at 1
                1
            }
        }

        database.userProfileDao().insertOrUpdate(
            currentProfile.copy(
                totalStudyMinutes = newTotalMinutes,
                totalXP = newTotalXP,
                currentLevel = newLevel,
                streakDays = updatedStreak,
                lastActiveDate = todayStr
            )
        )
    }

    suspend fun updateTheme(themeKey: String) {
        themePreferences.setThemePreset(themeKey)
        val currentProfile = database.userProfileDao().getProfileSync() ?: UserProfileEntity(
            id = 1,
            streakDays = 0,
            totalStudyMinutes = 0,
            totalXP = 0,
            currentLevel = 1,
            dailyGoalMinutes = 60,
            themePreset = themeKey
        )
        database.userProfileDao().insertOrUpdate(currentProfile.copy(themePreset = themeKey))
    }

    suspend fun updateDailyGoal(minutes: Int) {
        val currentProfile = database.userProfileDao().getProfileSync() ?: UserProfileEntity(
            id = 1,
            streakDays = 0,
            totalStudyMinutes = 0,
            totalXP = 0,
            currentLevel = 1,
            dailyGoalMinutes = 60,
            themePreset = "midnight"
        )
        database.userProfileDao().insertOrUpdate(currentProfile.copy(dailyGoalMinutes = minutes))
    }

    suspend fun resetStats() {
        database.sessionLogDao().clearAll()
        val currentProfile = database.userProfileDao().getProfileSync()
        val currentTheme = currentProfile?.themePreset ?: "midnight"
        val currentGoal = currentProfile?.dailyGoalMinutes ?: 60
        database.userProfileDao().insertOrUpdate(
            UserProfileEntity(
                id = 1,
                streakDays = 0,
                totalStudyMinutes = 0,
                totalXP = 0,
                currentLevel = 1,
                dailyGoalMinutes = currentGoal,
                themePreset = currentTheme
            )
        )
    }
}
