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

    fun isWallpaperEnabled(): Boolean = themePreferences.isWallpaperEnabled()
    fun setWallpaperEnabled(enabled: Boolean) = themePreferences.setWallpaperEnabled(enabled)

    fun isFocusWallpaperEnabled(): Boolean = themePreferences.isFocusWallpaperEnabled()
    fun setFocusWallpaperEnabled(enabled: Boolean) = themePreferences.setFocusWallpaperEnabled(enabled)

    fun getWallpaperOpacity(): Float = themePreferences.getWallpaperOpacity()
    fun setWallpaperOpacity(opacity: Float) = themePreferences.setWallpaperOpacity(opacity)

    fun getThemeWallpaperStyle(themeKey: String): String = themePreferences.getThemeWallpaperStyle(themeKey)
    fun setThemeWallpaperStyle(themeKey: String, styleId: String) = themePreferences.setThemeWallpaperStyle(themeKey, styleId)

    fun getCustomWallpaperUri(): String? = themePreferences.getCustomWallpaperUri()
    fun setCustomWallpaperUri(uri: String?) = themePreferences.setCustomWallpaperUri(uri)

    fun getCustomAudioUri(): String? = themePreferences.getCustomAudioUri()
    fun getCustomAudioName(): String? = themePreferences.getCustomAudioName()
    fun setCustomAudio(uri: String?, displayName: String?) = themePreferences.setCustomAudio(uri, displayName)
    fun getCustomAudioList(): List<com.aistudio.studyos.data.local.UploadedAudio> = themePreferences.getCustomAudioList()
    fun addCustomAudio(name: String, uri: String): com.aistudio.studyos.data.local.UploadedAudio = themePreferences.addCustomAudio(name, uri)
    fun removeCustomAudio(id: String) = themePreferences.removeCustomAudio(id)
    fun getSelectedCustomAudioId(): String? = themePreferences.getSelectedCustomAudioId()
    fun setSelectedCustomAudioId(id: String?) = themePreferences.setSelectedCustomAudioId(id)

    // Study Plan
    fun getActivePlan(): Flow<StudyPlanEntity?> = database.studyPlanDao().getActivePlan()
    fun getSavedPlans(): Flow<List<StudyPlanEntity>> = database.studyPlanDao().getSavedPlans()
    fun getLatestCompletedPlan(): Flow<StudyPlanEntity?> = database.studyPlanDao().getLatestCompletedPlan()
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

    // In-memory cache for ultra-fast instant startup without waiting for Room SQLite
    @Volatile
    private var inMemoryCachedLogs: List<SessionLogEntity>? = null

    // Logs & Stats
    fun getAllLogs(): Flow<List<SessionLogEntity>> {
        val since = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
        return database.sessionLogDao().getLogsSince(since)
    }
    fun getRecentLogs(limit: Int = 10): Flow<List<SessionLogEntity>> = database.sessionLogDao().getRecentLogs(limit)

    fun getCurrentYearMinutes(): Flow<Int> {
        val yearStart = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        return database.sessionLogDao().getMinutesBetween(yearStart.timeInMillis, now + 1L)
    }

    fun getCurrentYearSessionCount(): Flow<Int> {
        val yearStart = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        return database.sessionLogDao().getSessionCountBetween(yearStart.timeInMillis, now + 1L)
    }

    fun getDistinctStudyTopicCount(): Flow<Int> = database.sessionLogDao().getDistinctSubjectCount()
    fun getPeakDailyFocusMinutes(): Flow<Int> = database.sessionLogDao().getPeakDailyFocusMinutes()

    private suspend fun levelAfterMissionCheck(profile: UserProfileEntity): Int {
        val topics = database.sessionLogDao().getDistinctSubjectCountOnce()
        val peak = database.sessionLogDao().getPeakDailyFocusMinutesOnce()
        var level = profile.currentLevel.coerceIn(1, 100)
        while (
            level < 100 &&
            LevelMissionCalculator.calculate(level, profile, topics, peak).allComplete
        ) {
            level++
        }
        return level
    }
    
    fun getCachedRecentLogs(): List<SessionLogEntity> {
        inMemoryCachedLogs?.let { if (it.isNotEmpty()) return it }
        val loaded = themePreferences.getCachedRecentSessions()
        if (loaded.isNotEmpty()) {
            inMemoryCachedLogs = loaded
        }
        return loaded
    }
    
    fun cacheRecentLogs(logs: List<SessionLogEntity>) {
        inMemoryCachedLogs = logs
        themePreferences.setCachedRecentSessions(logs)
    }

    fun getTodayMinutes(): Flow<Int> {
        val range = TodayMinutesCalculator.currentLocalDayRange()
        val startOfDayMillis = range.startMillis
        val startOfNextDayMillis = range.endMillis
        return database.sessionLogDao().getTodayMinutes(startOfDayMillis, startOfNextDayMillis)
    }

    suspend fun getTodayMinutesNow(): Int {
        val range = TodayMinutesCalculator.currentLocalDayRange()
        return database.sessionLogDao().getTodayMinutesOnce(range.startMillis, range.endMillis)
    }
    fun getTotalMinutes(): Flow<Int?> = database.sessionLogDao().getTotalMinutes()
    suspend fun logSession(log: SessionLogEntity): Long {
        val id = database.sessionLogDao().insertLog(log)
        val currentCached = (inMemoryCachedLogs ?: themePreferences.getCachedRecentSessions()).toMutableList()
        currentCached.add(0, log.copy(id = id))
        val trimmed = currentCached.take(15)
        inMemoryCachedLogs = trimmed
        themePreferences.setCachedRecentSessions(trimmed)
        return id
    }
    suspend fun deleteSessionLog(log: SessionLogEntity) {
        database.sessionLogDao().deleteLog(log)
        val currentCached = (inMemoryCachedLogs ?: themePreferences.getCachedRecentSessions()).toMutableList()
        currentCached.removeAll { it.id == log.id }
        inMemoryCachedLogs = currentCached
        themePreferences.setCachedRecentSessions(currentCached)
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
    suspend fun clearHistory() {
        database.sessionLogDao().clearAll()
        inMemoryCachedLogs = emptyList()
        themePreferences.setCachedRecentSessions(emptyList())
    }

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
                // Check if user has an active Streak Shield
                val currentShields = themePreferences.getStreakShieldCount()
                if (currentShields > 0) {
                    // Consume 1 streak shield and protect the streak!
                    themePreferences.setStreakShieldCount(currentShields - 1)
                    themePreferences.setLastShieldSavedDate(todayStr)
                    // Keep streak intact, advance lastActiveDate to yesterday so it won't break again today
                    database.userProfileDao().insertOrUpdate(
                        profile.copy(lastActiveDate = yesterdayStr)
                    )
                } else {
                    // Streak broken because more than 1 day missed without studying and no shield
                    database.userProfileDao().insertOrUpdate(
                        profile.copy(streakDays = 0)
                    )
                }
            }

            // Do not infer that real user data is test/seed data from numeric values.
            // Older builds used a dummy-data cleanup heuristic here; that could erase
            // legitimate progress after an update if a user happened to match those values.
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

    /**
     * Finalizes an expired foreground timer using the persisted plan as the
     * single source of truth. The expected deadline makes this operation
     * idempotent and prevents stale service commands from advancing a newer
     * timer.
     */
    suspend fun expireRunningPlanIfNeeded(
        planId: Long,
        expectedEndAtWallClockMillis: Long
    ): Boolean {
        if (planId <= 0L || expectedEndAtWallClockMillis <= 0L) return false

        return database.withTransaction {
            val plan = database.studyPlanDao().getPlanById(planId) ?: return@withTransaction false
            val now = System.currentTimeMillis()

            if (
                plan.isCompleted ||
                !plan.isTimerRunning ||
                plan.endAtWallClockMillis != expectedEndAtWallClockMillis ||
                expectedEndAtWallClockMillis > now
            ) {
                return@withTransaction false
            }

            val items = if (plan.planItems.isBlank()) {
                List(plan.totalBlocks.coerceIn(1, 720)) {
                    com.aistudio.studyos.data.local.entity.StudyPlanItem(
                        plan.subject,
                        plan.chapter,
                        plan.durationPerBlockMinutes.coerceIn(1, 25)
                    )
                }
            } else {
                StudyPlanItemCodec.decodeStrict(plan.planItems) ?: return@withTransaction false
            }

            val safeItems = items.flatMap { item ->
                buildList {
                    var remaining = item.minutes.coerceIn(1, 720)
                    while (remaining > 25) {
                        add(item.copy(minutes = 25))
                        remaining -= 25
                    }
                    add(item.copy(minutes = remaining))
                }
            }.takeIf { it.isNotEmpty() } ?: return@withTransaction false

            val index = plan.currentBlockIndex.coerceIn(0, safeItems.lastIndex)
            val currentItem = safeItems[index]

            if (plan.isBreakPhase) {
                // Break expiry only opens the next focus block. It never adds
                // break time to studied totals.
                val blockSeconds = safeItems[index].minutes * 60
                database.studyPlanDao().updatePlan(
                    plan.copy(
                        remainingSecondsInBlock = blockSeconds,
                        durationPerBlockMinutes = safeItems[index].minutes,
                        isBreakPhase = false,
                        isTimerRunning = false,
                        endAtElapsedRealtime = 0L,
                        endAtWallClockMillis = 0L,
                        lastUpdated = now
                    )
                )
                false
            } else {
                val completedMinutes = SessionResultCalculator.billableMinutes(currentItem.minutes * 60)
                val newStudiedSeconds = plan.accumulatedStudiedSeconds + currentItem.minutes * 60
                val newCompletedMinutes = plan.accumulatedBillableMinutes + completedMinutes
                val nextIndex = index + 1
                val sessionComplete =
                    nextIndex >= safeItems.size ||
                        newStudiedSeconds >= plan.totalDurationMinutes.coerceAtLeast(0) * 60

                if (sessionComplete) {
                    database.studyPlanDao().updatePlan(
                        plan.copy(
                            currentBlockIndex = nextIndex,
                            remainingSecondsInBlock = 0,
                            isBreakPhase = false,
                            isTimerRunning = false,
                            endAtElapsedRealtime = 0L,
                            endAtWallClockMillis = 0L,
                            isCompleted = true,
                            accumulatedStudiedSeconds = newStudiedSeconds,
                            accumulatedBillableMinutes = newCompletedMinutes,
                            lastUpdated = now
                        )
                    )
                    if (completedMinutes > 0) {
                        recordCompletedSession(
                            currentItem.subject,
                            currentItem.topic,
                            completedMinutes,
                            plan.mode
                        )
                    }
                    true
                } else {
                    val breakSeconds = plan.breakMinutes.coerceIn(0, 120) * 60
                    database.studyPlanDao().updatePlan(
                        plan.copy(
                            currentBlockIndex = nextIndex,
                            durationPerBlockMinutes = safeItems[nextIndex].minutes,
                            remainingSecondsInBlock = breakSeconds.coerceAtLeast(1),
                            isBreakPhase = true,
                            isTimerRunning = false,
                            endAtElapsedRealtime = 0L,
                            endAtWallClockMillis = 0L,
                            isCompleted = false,
                            accumulatedStudiedSeconds = newStudiedSeconds,
                            accumulatedBillableMinutes = newCompletedMinutes,
                            lastUpdated = now
                        )
                    )
                    if (completedMinutes > 0) {
                        recordCompletedSession(
                            currentItem.subject,
                            currentItem.topic,
                            completedMinutes,
                            plan.mode
                        )
                    }
                    false
                }
            }
        }
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
        val isBoosterActive = themePreferences.isDoubleXpBoosterActive()
        val boosterMultiplier = if (isBoosterActive) themePreferences.getXpBoosterMultiplier() else 1
        val baseXP = durationMinutes * 3
        val xpGained = baseXP * boosterMultiplier

        val log = SessionLogEntity(
            subject = subject,
            chapter = chapter,
            durationMinutes = durationMinutes,
            mode = mode,
            xpEarned = xpGained
        )
        val insertedId = database.sessionLogDao().insertLog(log)
        val currentCached = themePreferences.getCachedRecentSessions().toMutableList()
        currentCached.add(0, log.copy(id = insertedId))
        themePreferences.setCachedRecentSessions(currentCached.take(10))

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

        val candidateProfile = currentProfile.copy(
            totalStudyMinutes = newTotalMinutes,
            totalXP = newTotalXP,
            currentLevel = currentProfile.currentLevel,
            totalXpSpent = currentProfile.totalXpSpent,
            streakDays = updatedStreak,
            lastActiveDate = todayStr
        )
        val newLevel = levelAfterMissionCheck(candidateProfile)
        database.userProfileDao().insertOrUpdate(candidateProfile.copy(currentLevel = newLevel))
    }

    // ==========================================
    // 🏪 XP Perks & Power-ups Shop Actions
    // ==========================================

    fun getStreakShieldCount(): Int = themePreferences.getStreakShieldCount()

    fun isCustomWallpaperPassActive(): Boolean = themePreferences.isCustomWallpaperPassActive()
    fun getCustomWallpaperPassExpiresAt(): Long = themePreferences.getCustomWallpaperPassExpiresAt()

    fun isCustomAudioPassActive(): Boolean = themePreferences.isCustomAudioPassActive()
    fun getCustomAudioPassExpiresAt(): Long = themePreferences.getCustomAudioPassExpiresAt()

    fun isDoubleXpBoosterActive(): Boolean = themePreferences.isDoubleXpBoosterActive()
    fun getDoubleXpBoosterExpiresAt(): Long = themePreferences.getDoubleXpBoosterExpiresAt()

    fun getLastShieldSavedDate(): String? = themePreferences.getLastShieldSavedDate()
    fun clearLastShieldSavedDate() = themePreferences.setLastShieldSavedDate(null)

    suspend fun buyStreakShield(): Pair<Boolean, String> {
        val profile = database.userProfileDao().getProfileSync() ?: return Pair(false, "Profile not found")
        val currentShields = themePreferences.getStreakShieldCount()
        if (currentShields >= 2) {
            return Pair(false, "Inventory full! You already have max 2 Streak Shields equipped.")
        }
        val cost = 500
        if (profile.totalXP < cost) {
            return Pair(false, "Need ${cost - profile.totalXP} more XP to purchase a Streak Shield!")
        }

        val updatedXP = profile.totalXP - cost
        val updatedSpent = profile.totalXpSpent + cost
        val candidateProfile = profile.copy(
            totalXP = updatedXP,
            currentLevel = profile.currentLevel,
            totalXpSpent = updatedSpent
        )
        val updatedLevel = levelAfterMissionCheck(candidateProfile)
        database.userProfileDao().insertOrUpdate(candidateProfile.copy(currentLevel = updatedLevel))
        themePreferences.setStreakShieldCount(currentShields + 1)
        return Pair(true, "🛡️ Streak Shield equipped! (Total: ${currentShields + 1}/2)")
    }

    suspend fun buyCustomWallpaperPass(days: Int = 1, xpCost: Int = 250): Pair<Boolean, String> {
        val profile = database.userProfileDao().getProfileSync() ?: return Pair(false, "Profile not found")
        if (profile.totalXP < xpCost) {
            return Pair(false, "Need ${xpCost - profile.totalXP} more XP to unlock the Custom Wallpaper Pass!")
        }

        val updatedXP = profile.totalXP - xpCost
        val updatedSpent = profile.totalXpSpent + xpCost
        val candidateProfile = profile.copy(
            totalXP = updatedXP,
            currentLevel = profile.currentLevel,
            totalXpSpent = updatedSpent
        )
        val updatedLevel = levelAfterMissionCheck(candidateProfile)
        database.userProfileDao().insertOrUpdate(candidateProfile.copy(currentLevel = updatedLevel))

        val currentExpires = themePreferences.getCustomWallpaperPassExpiresAt()
        val now = System.currentTimeMillis()
        val baseTime = if (currentExpires > now) currentExpires else now
        val newExpires = baseTime + (days * 24L * 3600 * 1000L)
        themePreferences.setCustomWallpaperPassExpiresAt(newExpires)

        val durationLabel = if (days == 1) "24 hours" else "$days days"
        return Pair(true, "🖼️ Custom Wallpaper Pass activated for $durationLabel!")
    }

    suspend fun buyCustomAudioPass(days: Int = 1, xpCost: Int = 300): Pair<Boolean, String> {
        val profile = database.userProfileDao().getProfileSync() ?: return Pair(false, "Profile not found")
        if (profile.totalXP < xpCost) {
            return Pair(false, "Need ${xpCost - profile.totalXP} more XP to unlock the Custom Audio Pass!")
        }

        val updatedXP = profile.totalXP - xpCost
        val updatedLevel = (updatedXP / 200) + 1
        database.userProfileDao().insertOrUpdate(
            profile.copy(
                totalXP = updatedXP,
                currentLevel = updatedLevel,
                totalXpSpent = updatedSpent
            )
        )

        val currentExpires = themePreferences.getCustomAudioPassExpiresAt()
        val now = System.currentTimeMillis()
        val baseTime = if (currentExpires > now) currentExpires else now
        val newExpires = baseTime + (days * 24L * 3600 * 1000L)
        themePreferences.setCustomAudioPassExpiresAt(newExpires)

        val durationLabel = if (days == 1) "24 hours" else "$days days"
        return Pair(true, "🎵 Custom Audio Pass activated for $durationLabel!")
    }

    fun activateDoubleXpBooster(minutes: Int = 60, multiplier: Int = 2) {
        val currentExpires = themePreferences.getDoubleXpBoosterExpiresAt()
        val now = System.currentTimeMillis()
        val baseTime = if (currentExpires > now) currentExpires else now
        val newExpires = baseTime + (minutes * 60 * 1000L)
        themePreferences.setDoubleXpBoosterExpiresAt(newExpires)
        themePreferences.setXpBoosterMultiplier(multiplier)
    }

    fun getXpBoosterMultiplier(): Int = themePreferences.getXpBoosterMultiplier()

    fun getLastFreeXpDropClaimTime(): Long = themePreferences.getLastFreeXpDropClaimTime()
    fun setLastFreeXpDropClaimTime(timestamp: Long) = themePreferences.setLastFreeXpDropClaimTime(timestamp)
    fun getFreeXpDropRemainingCooldownMs(): Long = themePreferences.getFreeXpDropRemainingCooldownMs()

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

    suspend fun addBonusXP(amount: Int) {
        if (amount <= 0) return
        val currentProfile = database.userProfileDao().getProfileSync() ?: UserProfileEntity(
            id = 1,
            streakDays = 0,
            totalStudyMinutes = 0,
            totalXP = 0,
            currentLevel = 1,
            dailyGoalMinutes = 60,
            themePreset = "midnight"
        )
        val newTotalXP = currentProfile.totalXP + amount
        val newLevel = (newTotalXP / 200) + 1
        database.userProfileDao().insertOrUpdate(
            currentProfile.copy(
                totalXP = newTotalXP,
                currentLevel = newLevel
            )
        )
    }

    suspend fun resetStats() {
        database.withTransaction {
            database.sessionLogDao().clearAll()
            database.studyPlanDao().clearAll()
        }
        themePreferences.setCachedRecentSessions(emptyList())
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
