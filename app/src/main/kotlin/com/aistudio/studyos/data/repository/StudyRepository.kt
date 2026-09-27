package com.aistudio.studyos.data.repository

import com.aistudio.studyos.data.local.StudyDatabase
import com.aistudio.studyos.data.local.ThemeCatalog
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

data class SpinWheelReward(val slotIndex: Int, val label: String, val xp: Int = 0)
data class SpinWheelStatus(val unlocked: Boolean, val spinsUsed: Int, val remainingMs: Long)

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
    fun getRecentLogsSince30Days(): Flow<List<SessionLogEntity>> {
        val since = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
        return database.sessionLogDao().getLogsSince(since)
    }

    fun getCurrentMonthLogs(): Flow<List<SessionLogEntity>> =
        database.sessionLogDao().getCurrentMonthLogs()
    fun getRecentLogs(limit: Int = 10): Flow<List<SessionLogEntity>> = database.sessionLogDao().getRecentLogs(limit)

    fun getCurrentYearMinutes(): Flow<Int> =
        database.sessionLogDao().getCurrentYearMinutes()

    fun getCurrentYearSessionCount(): Flow<Int> =
        database.sessionLogDao().getCurrentYearSessionCount()

    fun getDistinctStudyTopicCount(): Flow<Int> = database.sessionLogDao().getValidStudySessionCount()
    fun getPeakDailyFocusMinutes(): Flow<Int> = database.sessionLogDao().getPeakDailyFocusMinutes()

    private suspend fun levelAfterMissionCheck(profile: UserProfileEntity): Int {
        val topics = database.sessionLogDao().getValidStudySessionCountOnce()
        val peak = database.sessionLogDao().getPeakDailyFocusMinutesOnce()
        val level = profile.currentLevel.coerceIn(1, 100)
        // Advance at most one level per mission check so a large accumulated
        // history cannot cause the user to skip multiple levels at once.
        return if (
            level < 100 &&
            LevelMissionCalculator.calculate(level, profile, topics, peak).allComplete
        ) {
            level + 1
        } else {
            level
        }
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
            val safeMinutes = minutesStudied.coerceAtLeast(0)
            val safeSeconds = studiedSeconds.coerceAtLeast(0)
            val updatedRows = database.studyPlanDao().completePlanEarlyIfActive(
                planId = planId,
                currentBlockIndex = nextBlockIndex ?: plan.currentBlockIndex,
                accumulatedStudiedSeconds = plan.accumulatedStudiedSeconds + safeSeconds,
                accumulatedBillableMinutes = plan.accumulatedBillableMinutes + safeMinutes
            )

            // Only the first caller may complete the plan. A stale/lifecycle
            // callback sees 0 updated rows and must not create another log
            // or increment profile totals a second time.
            if (updatedRows == 1 && safeMinutes > 0) {
                recordCompletedSession(subject, chapter, safeMinutes, mode)
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
        themePreferences.setCachedRecentSessions(currentCached.take(15))

        val currentProfile = database.userProfileDao().getProfileSync() ?: UserProfileEntity(
            id = 1,
            streakDays = 0,
            totalStudyMinutes = 0,
            totalXP = 0,
            currentLevel = 1,
            dailyGoalMinutes = 60,
            themePreset = ThemeCatalog.DEFAULT_THEME
        )
        val newTotalMinutes = currentProfile.totalStudyMinutes + durationMinutes
        val newTotalXP = currentProfile.totalXP + xpGained
        val newTotalXpEarned = currentProfile.totalXpEarned + xpGained

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
            totalXpEarned = newTotalXpEarned,
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

    fun getSpinWheelStatus(): SpinWheelStatus = SpinWheelStatus(
        unlocked = themePreferences.isSpinWheelUnlocked(),
        spinsUsed = themePreferences.getSpinWheelSpinsUsed(),
        remainingMs = themePreferences.getSpinWheelRemainingMs()
    )

    fun unlockSpinWheel(): Boolean {
        if (themePreferences.isSpinWheelUnlocked()) return false
        themePreferences.setSpinWheelUnlockedAt(System.currentTimeMillis())
        themePreferences.setSpinWheelSpinsUsed(0)
        return true
    }

    suspend fun spinWheel(): SpinWheelReward? {
        if (!themePreferences.isSpinWheelUnlocked()) return null
        val profile = database.userProfileDao().getProfileSync() ?: return null
        val spinNumber = themePreferences.getSpinWheelSpinsUsed() + 1
        val cost = 50 + (spinNumber - 1) * 20
        if (profile.totalXP < cost) return null

        val xpValues = listOf(
            listOf(5, 100, 150, 10), listOf(10, 120, 170, 20),
            listOf(15, 140, 190, 25), listOf(20, 160, 210, 30),
            listOf(25, 180, 230, 35), listOf(30, 200, 250, 40),
            listOf(35, 220, 270, 45), listOf(40, 240, 290, 50),
            listOf(45, 260, 310, 55), listOf(50, 280, 330, 60),
            listOf(55, 300, 350, 65), listOf(60, 320, 370, 70),
            listOf(65, 340, 390, 75), listOf(70, 360, 410, 80),
            listOf(75, 380, 430, 85), listOf(80, 400, 450, 90),
            listOf(85, 420, 470, 95), listOf(90, 440, 490, 100),
            listOf(95, 460, 510, 105), listOf(100, 480, 530, 110)
        )[spinNumber - 1]

        val weightedSlots = listOf(25, 18, 7, 20, 7, 10, 8, 5)
        val roll = kotlin.random.Random.nextInt(weightedSlots.sum())
        var cursor = 0
        val slot = weightedSlots.indexOfFirst { weight ->
            cursor += weight
            roll < cursor
        }

        val candidate = profile.copy(
            totalXP = profile.totalXP - cost,
            totalXpSpent = profile.totalXpSpent + cost,
            currentLevel = profile.currentLevel
        )
        val newLevel = levelAfterMissionCheck(candidate)
        database.userProfileDao().insertOrUpdate(candidate.copy(currentLevel = newLevel))

        val reward = when (slot) {
            0 -> SpinWheelReward(slot, "${xpValues[0]} XP", xpValues[0])
            1 -> SpinWheelReward(slot, "${xpValues[1]} XP", xpValues[1])
            2 -> {
                val themeKey = listOf("cyberpunk", "cyber_runner").random()
                val now = System.currentTimeMillis()
                val base = maxOf(themePreferences.getPremiumThemePassExpiresAt(themeKey), now)
                themePreferences.setPremiumThemePassExpiresAt(themeKey, base + 60 * 60 * 1000L)
                SpinWheelReward(slot, "+1h Premium Theme")
            }
            3 -> SpinWheelReward(slot, "+0 XP")
            4 -> {
                val now = System.currentTimeMillis()
                val base = maxOf(themePreferences.getCustomAudioPassExpiresAt(), now)
                themePreferences.setCustomAudioPassExpiresAt(base + 60 * 60 * 1000L)
                SpinWheelReward(slot, "+1h Custom Audio")
            }
            5 -> SpinWheelReward(slot, "${xpValues[2]} XP", xpValues[2])
            6 -> SpinWheelReward(slot, "${xpValues[3]} XP", xpValues[3])
            else -> {
                val now = System.currentTimeMillis()
                val base = maxOf(themePreferences.getCustomWallpaperPassExpiresAt(), now)
                themePreferences.setCustomWallpaperPassExpiresAt(base + 60 * 60 * 1000L)
                SpinWheelReward(slot, "+1h Custom Wallpaper")
            }
        }

        if (reward.xp > 0) addBonusXP(reward.xp)
        themePreferences.setSpinWheelSpinsUsed(spinNumber)
        return reward
    }

    fun getStreakShieldCount(): Int = themePreferences.getStreakShieldCount()

    fun isCustomWallpaperPassActive(): Boolean = themePreferences.isCustomWallpaperPassActive()
    fun getCustomWallpaperPassExpiresAt(): Long = themePreferences.getCustomWallpaperPassExpiresAt()

    fun isCustomAudioPassActive(): Boolean = themePreferences.isCustomAudioPassActive()
    fun getCustomAudioPassExpiresAt(): Long = themePreferences.getCustomAudioPassExpiresAt()

    fun isPremiumThemePassActive(themeKey: String): Boolean =
        themePreferences.isPremiumThemePassActive(themeKey)

    fun getPremiumThemePassExpiresAt(themeKey: String): Long =
        themePreferences.getPremiumThemePassExpiresAt(themeKey)

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

    suspend fun buyPremiumThemePass(themeKey: String, days: Int, xpCost: Int): Pair<Boolean, String> {
        val profile = database.userProfileDao().getProfileSync() ?: return Pair(false, "Profile not found")
        val safeDays = days.coerceIn(1, 60)
        val safeCost = xpCost.coerceAtLeast(0)
        if (profile.totalXP < safeCost) {
            return Pair(false, "Need ${safeCost - profile.totalXP} more XP to unlock this Theme Pass!")
        }

        val updatedXP = profile.totalXP - safeCost
        val updatedSpent = profile.totalXpSpent + safeCost
        val candidateProfile = profile.copy(
            totalXP = updatedXP,
            currentLevel = profile.currentLevel,
            totalXpSpent = updatedSpent
        )
        val updatedLevel = levelAfterMissionCheck(candidateProfile)
        database.userProfileDao().insertOrUpdate(candidateProfile.copy(currentLevel = updatedLevel))

        val currentExpires = themePreferences.getPremiumThemePassExpiresAt(themeKey)
        val now = System.currentTimeMillis()
        val baseTime = if (currentExpires > now) currentExpires else now
        val newExpires = baseTime + safeDays * 24L * 60L * 60L * 1000L
        themePreferences.setPremiumThemePassExpiresAt(themeKey, newExpires)

        val durationLabel = if (safeDays == 1) "24 hours" else "${safeDays} days"
        return Pair(true, "🎨 Theme Pass activated for $durationLabel!")
    }

    suspend fun buyCustomAudioPass(days: Int = 1, xpCost: Int = 300): Pair<Boolean, String> {
        val profile = database.userProfileDao().getProfileSync() ?: return Pair(false, "Profile not found")
        if (profile.totalXP < xpCost) {
            return Pair(false, "Need ${xpCost - profile.totalXP} more XP to unlock the Custom Audio Pass!")
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
            themePreset = "pitch_black"
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
            themePreset = "pitch_black"
        )
        val newTotalXP = currentProfile.totalXP + amount
        val newTotalXpEarned = currentProfile.totalXpEarned + amount
        val candidateProfile = currentProfile.copy(
            totalXP = newTotalXP,
            totalXpEarned = newTotalXpEarned,
            currentLevel = currentProfile.currentLevel
        )
        val newLevel = levelAfterMissionCheck(candidateProfile)
        database.userProfileDao().insertOrUpdate(candidateProfile.copy(currentLevel = newLevel))
    }

    suspend fun resetStats() {
        database.withTransaction {
            database.sessionLogDao().clearAll()
            database.studyPlanDao().clearAll()
        }
        themePreferences.setCachedRecentSessions(emptyList())
        val currentProfile = database.userProfileDao().getProfileSync()
        val currentTheme = ThemeCatalog.normalize(currentProfile?.themePreset)
        val currentGoal = currentProfile?.dailyGoalMinutes ?: 60
        database.userProfileDao().insertOrUpdate(
            UserProfileEntity(
                id = 1,
                streakDays = 0,
                totalStudyMinutes = 0,
                totalXP = 0,
                totalXpSpent = 0,
                currentLevel = 1,
                dailyGoalMinutes = currentGoal,
                themePreset = currentTheme
            )
        )
    }
}
