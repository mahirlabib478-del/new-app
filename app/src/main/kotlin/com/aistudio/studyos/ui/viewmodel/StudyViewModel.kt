package com.aistudio.studyos.ui.viewmodel

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.studyos.data.local.entity.ExamEntity
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.local.entity.StudyPlanItem
import com.aistudio.studyos.data.local.entity.StudyPlanItemCodec
import com.aistudio.studyos.data.local.entity.UserProfileEntity
import com.aistudio.studyos.data.repository.SessionResultCalculator
import com.aistudio.studyos.data.repository.TimerDeadlineCalculator
import com.aistudio.studyos.StudyApplication
import com.aistudio.studyos.service.StudyTimerForegroundService
import com.aistudio.studyos.data.repository.StudyRepository
import com.aistudio.studyos.data.update.AppUpdateInfo
import com.aistudio.studyos.data.update.UpdateCheckState
import com.aistudio.studyos.data.update.UpdateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch

data class FocusTimerState(
    val isRunning: Boolean = false,
    val isBreak: Boolean = false,
    val secondsRemaining: Int = 25 * 60,
    val totalBlockSeconds: Int = 25 * 60,
    val studyBlockSeconds: Int = 25 * 60,
    val breakBlockSeconds: Int = 5 * 60,
    val currentBlockIndex: Int = 0,
    val totalBlocks: Int = 4,
    val currentSubject: String = "Quick Focus",
    val currentChapter: String = "General Study",
    val planId: Long? = null,
    val mode: String = "regular",
    val isSessionCompleted: Boolean = false,
    val completedMinutes: Int = 0,
    val completedBlocks: Int = 0,
    val actualStudiedSeconds: Int = 0,
    val endAtElapsedRealtime: Long = 0L,
    val endAtWallClockMillis: Long = 0L,
    val sessionError: String? = null
)

private fun splitStudyItem(item: StudyPlanItem): List<StudyPlanItem> {
    var remaining = item.minutes.coerceIn(1, 720)
    val result = mutableListOf<StudyPlanItem>()
    while (remaining > 25) {
        result += item.copy(minutes = 25)
        remaining -= 25
    }
    result += item.copy(minutes = remaining)
    return result
}

private fun normalizeBreakMinutes(value: Int): Int = if (value >= 8) 10 else 5

class StudyViewModel(private val repository: StudyRepository) : ViewModel() {

    private var timerJob: Job? = null
    private var updateCheckJob: Job? = null
    private var currentPlanItems: List<StudyPlanItem> = emptyList()
    private val transitionMutex = Mutex()
    @Volatile private var transitionInProgress = false

    // Automatic restore is allowed only once, when this ViewModel is created.
    // Explicitly starting/resuming a session disables it so an old active-plan
    // Flow emission can never overwrite the newly created timer.
    @Volatile private var automaticRestoreEnabled = true

    // Every timer loop gets a unique generation. A cancelled/old loop is never
    // allowed to publish another countdown value.
    private var timerGeneration = 0L

    private val _currentTheme = MutableStateFlow(repository.getInitialTheme())
    val currentTheme: StateFlow<String> = _currentTheme.asStateFlow()

    private val _isWallpaperEnabled = MutableStateFlow(repository.isWallpaperEnabled())
    val isWallpaperEnabled: StateFlow<Boolean> = _isWallpaperEnabled.asStateFlow()

    private val _isFocusWallpaperEnabled = MutableStateFlow(repository.isFocusWallpaperEnabled())
    val isFocusWallpaperEnabled: StateFlow<Boolean> = _isFocusWallpaperEnabled.asStateFlow()

    private val _wallpaperOpacity = MutableStateFlow(repository.getWallpaperOpacity())
    val wallpaperOpacity: StateFlow<Float> = _wallpaperOpacity.asStateFlow()

    private val _wallpaperStyle = MutableStateFlow(repository.getThemeWallpaperStyle(repository.getInitialTheme()))
    val wallpaperStyle: StateFlow<String> = _wallpaperStyle.asStateFlow()

    private val _customWallpaperUri = MutableStateFlow(repository.getCustomWallpaperUri())
    val customWallpaperUri: StateFlow<String?> = _customWallpaperUri.asStateFlow()

    private val _customAudioUri = MutableStateFlow(repository.getCustomAudioUri())
    val customAudioUri: StateFlow<String?> = _customAudioUri.asStateFlow()

    private val _customAudioName = MutableStateFlow(repository.getCustomAudioName())
    val customAudioName: StateFlow<String?> = _customAudioName.asStateFlow()

    private val _customAudioList = MutableStateFlow<List<com.aistudio.studyos.data.local.UploadedAudio>>(repository.getCustomAudioList())
    val customAudioList: StateFlow<List<com.aistudio.studyos.data.local.UploadedAudio>> = _customAudioList.asStateFlow()

    private val _selectedAudioId = MutableStateFlow<String?>(repository.getSelectedCustomAudioId())
    val selectedAudioId: StateFlow<String?> = _selectedAudioId.asStateFlow()

    private val _isRecentLogsLoaded = MutableStateFlow(false)
    val isRecentLogsLoaded: StateFlow<Boolean> = _isRecentLogsLoaded.asStateFlow()

    private val _isAllLogsLoaded = MutableStateFlow(false)
    val isAllLogsLoaded: StateFlow<Boolean> = _isAllLogsLoaded.asStateFlow()

    val activePlan: StateFlow<StudyPlanEntity?> = repository.getActivePlan()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val latestCompletedPlan: StateFlow<StudyPlanEntity?> = repository.getLatestCompletedPlan()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val savedPlans: StateFlow<List<StudyPlanEntity>> = repository.getSavedPlans()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val exams: StateFlow<List<ExamEntity>> = repository.getAllExams()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val upcomingExams: StateFlow<List<ExamEntity>> = repository.getUpcomingExams()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentLogs: StateFlow<List<SessionLogEntity>> = repository.getRecentLogs(15)
        .onEach { _isRecentLogsLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _todayMinutes = MutableStateFlow(0)
    val todayMinutes: StateFlow<Int> = _todayMinutes.asStateFlow()

    val allLogs: StateFlow<List<SessionLogEntity>> = repository.getAllLogs()
        .onEach { _isAllLogsLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _focusState = MutableStateFlow(FocusTimerState())
    val focusState: StateFlow<FocusTimerState> = _focusState.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    private var hasDismissedUpdateDialog: Boolean = false

    init {
        viewModelScope.launch {
            repository.getTodayMinutes().collect { minutes ->
                _todayMinutes.value = minutes
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureCleanInitialData()
            userProfile.collect { profile ->
                if (profile != null && profile.themePreset.isNotBlank()) {
                    _currentTheme.value = profile.themePreset
                }
            }
        }
        viewModelScope.launch {
            activePlan.collect { plan ->
                // Restore only on a fresh ViewModel. Do not use planId == null as
                // the restore signal: completion and explicit new-session creation
                // also temporarily have a null planId.
                if (
                    automaticRestoreEnabled &&
                    plan != null &&
                    !plan.isCompleted &&
                    !plan.isArchived &&
                    !plan.isDraft &&
                    _focusState.value.planId == null &&
                    !_focusState.value.isSessionCompleted
                ) {
                    automaticRestoreEnabled = false
                    continueActiveSession(plan)
                }
            }
        }
    }


    fun checkAppUpdate(
        context: Context,
        isManual: Boolean = false,
        forceCheck: Boolean = false
    ) {
        if (!isManual && !forceCheck && hasDismissedUpdateDialog) return

        updateCheckJob?.cancel()
        updateCheckJob = viewModelScope.launch {
            if (isManual) {
                _updateCheckState.value = UpdateCheckState.Checking
            }

            when (val result = UpdateManager.checkForUpdate(
                context.applicationContext,
                isManual = isManual,
                forceCheck = forceCheck
            )) {
                is com.aistudio.studyos.data.update.UpdateCheckResult.Available -> {
                    _availableUpdate.value = result.updateInfo
                    _updateCheckState.value = UpdateCheckState.Available(result.updateInfo)
                }
                is com.aistudio.studyos.data.update.UpdateCheckResult.UpToDate -> {
                    _availableUpdate.value = null
                    _updateCheckState.value = UpdateCheckState.UpToDate(result.currentVersion)
                }
                is com.aistudio.studyos.data.update.UpdateCheckResult.Error -> {
                    _updateCheckState.value = UpdateCheckState.Error(result.message)
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        hasDismissedUpdateDialog = true
        _availableUpdate.value = null
    }

    fun resetManualUpdateState() {
        _updateCheckState.value = UpdateCheckState.Idle
    }

    fun startNewPlan(
        title: String,
        subject: String,
        chapter: String,
        mode: String,
        totalBlocks: Int = 4,
        blockMinutes: Int = 25,
        breakMinutes: Int = 5,
        autoStart: Boolean = true,
        items: List<StudyPlanItem> = emptyList(),
        expectedTotalMinutes: Int? = null,
        onReady: (() -> Unit)? = null
    ) {
        // Disable automatic restoration immediately, before the coroutine starts.
        // This closes the race where activePlan emits an old plan while a new
        // session is being created.
        automaticRestoreEnabled = false
        stopTimerJob()

        viewModelScope.launch {
            transitionMutex.withLock {
                if (transitionInProgress) return@withLock
                transitionInProgress = true
                try {
                    val sourceItems = if (items.isNotEmpty()) {
                        items
                    } else {
                        List(totalBlocks.coerceIn(1, 720)) {
                            StudyPlanItem(subject, chapter, blockMinutes.coerceIn(1, 25))
                        }
                    }

                    val normalizedItems = normalizePlanItems(sourceItems)
                    val allocatedMinutes = normalizedItems.sumOf { it.minutes }
                    if (
                        normalizedItems.isEmpty() ||
                        normalizedItems.size > 720 ||
                        allocatedMinutes <= 0 ||
                        allocatedMinutes > 720 ||
                        (expectedTotalMinutes != null && allocatedMinutes != expectedTotalMinutes)
                    ) {
                        return@withLock
                    }

                    stopTimerJob()
                    StudyTimerForegroundService.stop(StudyApplication.instance)

                    val first = normalizedItems.first()
                    val studySec = (first.minutes * 60).coerceAtLeast(60)
                    val breakMin = normalizeBreakMinutes(breakMinutes)
                    val multiSubject = normalizedItems.map { it.subject }.distinct().size > 1
                    val plan = StudyPlanEntity(
                        title = if (multiSubject) {
                            "Multiple Subjects • ${formatPlanDuration(allocatedMinutes)}"
                        } else {
                            title.ifBlank { "$subject - $chapter" }
                        },
                        subject = if (multiSubject) "Multiple Subjects" else first.subject,
                        chapter = if (multiSubject) "Study Session" else first.topic,
                        mode = mode,
                        totalBlocks = normalizedItems.size,
                        currentBlockIndex = 0,
                        durationPerBlockMinutes = first.minutes,
                        breakMinutes = breakMin,
                        remainingSecondsInBlock = studySec,
                        isBreakPhase = false,
                        isCompleted = false,
                        isDraft = false,
                        isArchived = false,
                        isTimerRunning = false,
                        endAtElapsedRealtime = 0L,
                        endAtWallClockMillis = 0L,
                        planItems = StudyPlanItemCodec.encode(normalizedItems),
                        totalDurationMinutes = allocatedMinutes
                    )
                    val planId = repository.savePlan(plan)
                    // Keep older unfinished plans available in Saved Sessions.
                    // getActivePlan() will still expose only the most recently updated plan.
                    currentPlanItems = normalizedItems

                    // A newly created session always starts from the first block's
                    // configured duration; never inherit seconds from an older plan.
                    _focusState.value = FocusTimerState(
                        isRunning = false,
                        isBreak = false,
                        secondsRemaining = studySec,
                        totalBlockSeconds = studySec,
                        studyBlockSeconds = studySec,
                        breakBlockSeconds = breakMin * 60,
                        currentBlockIndex = 0,
                        totalBlocks = normalizedItems.size,
                        currentSubject = first.subject,
                        currentChapter = first.topic,
                        planId = planId,
                        mode = mode,
                        isSessionCompleted = false,
                        completedMinutes = 0,
                        completedBlocks = 0,
                        actualStudiedSeconds = 0,
                        endAtElapsedRealtime = 0L,
                        endAtWallClockMillis = 0L,
                        sessionError = null
                    )

                    transitionInProgress = false
                    onReady?.invoke()
                    if (autoStart) startTimer()
                } finally {
                    transitionInProgress = false
                }
            }
        }
    }
    fun saveDraftPlan(
        title: String,
        subject: String,
        chapter: String,
        mode: String,
        totalBlocks: Int = 4,
        blockMinutes: Int = 25,
        breakMinutes: Int = 5,
        items: List<StudyPlanItem> = emptyList()
    ) {
        viewModelScope.launch {
            val sourceItems = if (items.isNotEmpty()) {
                items
            } else {
                List(totalBlocks.coerceIn(1, 720)) {
                    StudyPlanItem(subject, chapter, blockMinutes.coerceIn(1, 25))
                }
            }
            val boundedItems = normalizePlanItems(sourceItems)
            if (boundedItems.isEmpty() || boundedItems.size > 720 || boundedItems.sumOf { it.minutes } > 720) {
                return@launch
            }
            val first = boundedItems.first()
            val allocatedMinutes = boundedItems.sumOf { it.minutes }
            val multiSubject = boundedItems.map { it.subject }.distinct().size > 1
            val plan = StudyPlanEntity(
                title = if (multiSubject) "Multiple Subjects • ${formatPlanDuration(allocatedMinutes)} (Draft)" else title.ifBlank { "$subject - $chapter" },
                subject = if (multiSubject) "Multiple Subjects" else first.subject,
                chapter = if (multiSubject) "Study Session" else first.topic,
                mode = mode,
                totalBlocks = boundedItems.size,
                currentBlockIndex = 0,
                durationPerBlockMinutes = first.minutes,
                breakMinutes = normalizeBreakMinutes(breakMinutes),
                remainingSecondsInBlock = first.minutes * 60,
                isBreakPhase = false,
                isCompleted = false,
                isDraft = true,
                isArchived = false,
                isTimerRunning = false,
                endAtElapsedRealtime = 0L,
                endAtWallClockMillis = 0L,
                planItems = StudyPlanItemCodec.encode(boundedItems),
                totalDurationMinutes = allocatedMinutes
            )
            repository.savePlan(plan)
        }
    }

    fun resumeSavedPlan(plan: StudyPlanEntity, onReady: (() -> Unit)? = null) {
        viewModelScope.launch {
            transitionMutex.withLock {
                if (transitionInProgress) return@withLock
                transitionInProgress = true
                try {
                    stopTimerJob()
                    val updated = plan.copy(
                        isDraft = false,
                        isArchived = false,
                        lastUpdated = System.currentTimeMillis()
                    )
                    // Resuming a saved plan must not archive the other unfinished plans.
                    // The resumed plan becomes active because its lastUpdated is refreshed.
                    repository.updatePlan(updated)
                    continueActiveSessionInternal(updated)
                    onReady?.invoke()
                } finally {
                    transitionInProgress = false
                }
            }
        }
    }

    fun continueActiveSession(plan: StudyPlanEntity) {
        viewModelScope.launch {
            transitionMutex.withLock {
                if (transitionInProgress || _focusState.value.planId == plan.id) return@withLock
                continueActiveSessionInternal(plan)
            }
        }
    }

    private suspend fun continueActiveSessionInternal(plan: StudyPlanEntity) {
        stopTimerJob()

        val decodedItems = if (plan.planItems.isBlank()) {
            List(plan.totalBlocks.coerceIn(1, 720)) {
                StudyPlanItem(plan.subject, plan.chapter, plan.durationPerBlockMinutes.coerceIn(1, 25))
            }
        } else {
            StudyPlanItemCodec.decodeStrict(plan.planItems)
        }
        if (decodedItems == null || decodedItems.isEmpty()) {
            currentPlanItems = emptyList()
            _focusState.value = FocusTimerState(
                currentSubject = plan.subject,
                currentChapter = plan.chapter,
                mode = plan.mode,
                sessionError = "This saved study plan could not be restored. Please create it again."
            )
            return
        }

        currentPlanItems = normalizePlanItems(decodedItems)
        if (currentPlanItems.isEmpty()) {
            _focusState.value = FocusTimerState(
                currentSubject = plan.subject,
                currentChapter = plan.chapter,
                mode = plan.mode,
                sessionError = "This saved study plan contains invalid topic durations."
            )
            return
        }

        if (plan.currentBlockIndex >= currentPlanItems.size) {
            val completedSeconds = plan.accumulatedStudiedSeconds.coerceAtLeast(0)
            val completedMinutes = plan.accumulatedBillableMinutes.coerceAtLeast(0)
            repository.getPlanById(plan.id)?.let { persisted ->
                if (!persisted.isCompleted) {
                    repository.updatePlan(
                        persisted.copy(
                            currentBlockIndex = currentPlanItems.size,
                            remainingSecondsInBlock = 0,
                            isBreakPhase = false,
                            isTimerRunning = false,
                            endAtElapsedRealtime = 0L,
                            endAtWallClockMillis = 0L,
                            isCompleted = true,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                }
            }
            StudyTimerForegroundService.stop(StudyApplication.instance)
            _focusState.value = FocusTimerState(
                isRunning = false,
                isBreak = false,
                secondsRemaining = 0,
                totalBlockSeconds = 0,
                studyBlockSeconds = 0,
                breakBlockSeconds = normalizeBreakMinutes(plan.breakMinutes) * 60,
                currentBlockIndex = currentPlanItems.size,
                totalBlocks = currentPlanItems.size,
                currentSubject = currentPlanItems.lastOrNull()?.subject ?: plan.subject,
                currentChapter = currentPlanItems.lastOrNull()?.topic ?: plan.chapter,
                planId = null,
                mode = plan.mode,
                isSessionCompleted = true,
                completedMinutes = completedMinutes,
                completedBlocks = currentPlanItems.size,
                actualStudiedSeconds = completedSeconds
            )
            return
        }

        val safeIndex = plan.currentBlockIndex.coerceIn(0, currentPlanItems.lastIndex)
        val currentItem = currentPlanItems[safeIndex]
        val studySec = currentItem.minutes * 60
        val breakSec = normalizeBreakMinutes(plan.breakMinutes) * 60
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val bootCount = currentBootCount()
        val elapsedValid = plan.isTimerRunning &&
            plan.timerBootCount == bootCount &&
            plan.endAtElapsedRealtime > nowElapsed
        val wallValid = plan.isTimerRunning && plan.endAtWallClockMillis > nowWall
        val isRunning = elapsedValid || wallValid
        val remainingSec = if (isRunning) {
            val totalBlockSeconds = if (plan.isBreakPhase) breakSec else studySec
            if (elapsedValid) {
                TimerDeadlineCalculator.remainingSeconds(
                    endAtElapsedRealtime = plan.endAtElapsedRealtime,
                    endAtWallClockMillis = plan.endAtWallClockMillis,
                    nowElapsedRealtime = nowElapsed,
                    nowWallClockMillis = nowWall,
                    totalBlockSeconds = totalBlockSeconds
                )
            } else {
                TimerDeadlineCalculator.remainingSeconds(
                    endAtElapsedRealtime = nowElapsed,
                    endAtWallClockMillis = plan.endAtWallClockMillis,
                    nowElapsedRealtime = nowElapsed,
                    nowWallClockMillis = nowWall,
                    totalBlockSeconds = totalBlockSeconds
                )
            }.coerceIn(1, totalBlockSeconds)
        } else {
            plan.remainingSecondsInBlock.coerceIn(1, if (plan.isBreakPhase) breakSec else studySec)
        }
        val priorCompletedMinutes = plan.accumulatedBillableMinutes.coerceAtLeast(0)
        val priorCompletedSeconds = plan.accumulatedStudiedSeconds.coerceAtLeast(0)
        val plannedFocusSeconds = plan.totalDurationMinutes.coerceAtLeast(0) * 60

        if (plan.isCompleted || (plannedFocusSeconds > 0 && priorCompletedSeconds >= plannedFocusSeconds)) {
            StudyTimerForegroundService.stop(StudyApplication.instance)
            _focusState.value = FocusTimerState(
                isRunning = false,
                isBreak = false,
                secondsRemaining = 0,
                totalBlockSeconds = 0,
                studyBlockSeconds = 0,
                breakBlockSeconds = normalizeBreakMinutes(plan.breakMinutes) * 60,
                currentBlockIndex = currentPlanItems.size,
                totalBlocks = currentPlanItems.size,
                currentSubject = currentPlanItems.lastOrNull()?.subject ?: plan.subject,
                currentChapter = currentPlanItems.lastOrNull()?.topic ?: plan.chapter,
                planId = null,
                mode = plan.mode,
                isSessionCompleted = true,
                completedMinutes = priorCompletedMinutes,
                completedBlocks = currentPlanItems.size,
                actualStudiedSeconds = priorCompletedSeconds
            )
            return
        }

        _focusState.value = FocusTimerState(
            isRunning = false,
            isBreak = plan.isBreakPhase,
            secondsRemaining = remainingSec,
            totalBlockSeconds = if (plan.isBreakPhase) breakSec else studySec,
            studyBlockSeconds = studySec,
            breakBlockSeconds = breakSec,
            currentBlockIndex = safeIndex,
            totalBlocks = currentPlanItems.size,
            currentSubject = currentItem.subject,
            currentChapter = currentItem.topic,
            planId = plan.id,
            mode = plan.mode,
            isSessionCompleted = false,
            completedMinutes = priorCompletedMinutes,
            completedBlocks = safeIndex,
            actualStudiedSeconds = priorCompletedSeconds
        )

        if (isRunning) {
            startTimerInternal()
        } else if (plan.isTimerRunning) {
            // The process may have been killed after the persisted end time.
            // Re-enter the same state machine transition instead of reviving an old countdown.
            val expiredState = _focusState.value.copy(
                isRunning = true,
                endAtElapsedRealtime = nowElapsed,
                endAtWallClockMillis = nowWall
            )
            _focusState.value = expiredState
            onBlockFinished()
        }
    }

    private suspend fun refreshTodayMinutes() {
        _todayMinutes.value = repository.getTodayMinutesNow()
    }

    fun refreshTodayStats() {
        viewModelScope.launch {
            refreshTodayMinutes()
        }
    }

    fun setupFocusSession(plan: StudyPlanEntity) = continueActiveSession(plan)

    fun finishActiveSessionEarly(): Boolean {
        if (transitionInProgress) return false
        transitionInProgress = true
        stopTimerJob()
        val current = _focusState.value
        viewModelScope.launch {
            transitionMutex.withLock {
                try {
                    val elapsedInCurrentBlock = SessionResultCalculator.studiedSecondsForEarlyFinish(
                        isBreak = current.isBreak,
                        elapsedSeconds = currentElapsedSeconds(current)
                    )
                    val totalStudiedSec = current.actualStudiedSeconds + elapsedInCurrentBlock
                    val partialMinutes = SessionResultCalculator.billableMinutes(elapsedInCurrentBlock)
                    val totalStudiedMin = current.completedMinutes + partialMinutes
                    val planId = current.planId

                    if (planId != null) {
                        repository.completePlanEarly(
                            planId = planId,
                            minutesStudied = partialMinutes,
                            studiedSeconds = elapsedInCurrentBlock,
                            subject = current.currentSubject,
                            chapter = current.currentChapter,
                            mode = current.mode
                        )
                    } else if (partialMinutes > 0) {
                        repository.recordCompletedSession(
                            current.currentSubject,
                            current.currentChapter,
                            partialMinutes,
                            current.mode
                        )
                    }

                    StudyTimerForegroundService.stop(StudyApplication.instance)
                    refreshTodayMinutes()
                    _focusState.value = current.copy(
                        secondsRemaining = 0,
                        isRunning = false,
                        isSessionCompleted = true,
                        completedMinutes = totalStudiedMin,
                        completedBlocks = current.completedBlocks + if (elapsedInCurrentBlock > 0) 1 else 0,
                        actualStudiedSeconds = totalStudiedSec,
                        planId = null,
                        endAtElapsedRealtime = 0L,
                        endAtWallClockMillis = 0L
                    )
                } catch (_: Exception) {
                    _focusState.value = current.copy(
                        isRunning = false,
                        sessionError = "The session could not be saved safely. Please try again."
                    )
                } finally {
                    transitionInProgress = false
                }
            }
        }
        return true
    }

    fun toggleTimer() {
        if (transitionInProgress) return
        if (_focusState.value.isRunning) pauseTimer() else startTimer()
    }

    fun startTimer() {
        if (transitionInProgress) return
        startTimerInternal()
    }

    private fun startTimerInternal() {
        val current = _focusState.value
        if (current.sessionError != null || current.isSessionCompleted || current.secondsRemaining <= 0) return

        stopTimerJob()
        // Starting/resuming is always a fresh countdown from the state's
        // current remaining seconds. Never reuse an old deadline here.
        // Reusing a previous deadline was the source of stale values such as 00:02
        // appearing immediately after a new session was opened.
        val previousEndElapsed = current.endAtElapsedRealtime
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val bootCount = currentBootCount()
        val durationSec = current.secondsRemaining.coerceIn(1, current.totalBlockSeconds.coerceAtLeast(1))
        val endElapsed = TimerDeadlineCalculator.deadlineMillis(nowElapsed, durationSec)
        val endWall = TimerDeadlineCalculator.deadlineMillis(nowWall, durationSec)
        _focusState.value = current.copy(
            isRunning = true,
            secondsRemaining = durationSec,
            endAtElapsedRealtime = endElapsed,
            endAtWallClockMillis = endWall
        )

        val planId = current.planId
        if (planId != null) {
            viewModelScope.launch {
                runCatching {
                    repository.updateSessionProgress(
                        planId = planId,
                        remainingSec = current.secondsRemaining,
                        isBreak = current.isBreak,
                        blockIndex = current.currentBlockIndex,
                        isRunning = true,
                        endAtElapsedRealtime = endElapsed,
                        endAtWallClockMillis = endWall,
                        timerBootCount = bootCount,
                        expectedEndAtElapsedRealtime = previousEndElapsed
                    )
                }
            }
        }

        runCatching {
            StudyTimerForegroundService.start(
                StudyApplication.instance,
                endAtWallClockMillis = endWall,
                planId = planId ?: 0L,
                isBreak = current.isBreak,
                subject = current.currentSubject
            )
        }

        val generation = ++timerGeneration
        timerJob = viewModelScope.launch {
            while (isActive && generation == timerGeneration) {
                val state = _focusState.value
                if (!state.isRunning || state.planId == null || state.isSessionCompleted) break

                val remaining = remainingFromState(state)
                if (remaining <= 0) {
                    _focusState.value = state.copy(
                        secondsRemaining = 0,
                        isRunning = false
                    )
                    onBlockFinished()
                    break
                }

                if (state.secondsRemaining != remaining) {
                    _focusState.value = state.copy(secondsRemaining = remaining)
                }
                delay(250)
            }
        }
    }

    fun pauseTimer() {
        if (transitionInProgress) return
        stopTimerJob()
        val current = _focusState.value
        val previousEndElapsed = current.endAtElapsedRealtime
        val remaining = remainingFromState(current)
        _focusState.value = current.copy(
            isRunning = false,
            secondsRemaining = remaining,
            endAtElapsedRealtime = 0L,
            endAtWallClockMillis = 0L
        )
        StudyTimerForegroundService.stop(StudyApplication.instance)
        val planId = current.planId
        if (planId != null) {
            viewModelScope.launch {
                runCatching {
                    repository.updateSessionProgress(
                        planId = planId,
                        remainingSec = remaining,
                        isBreak = current.isBreak,
                        blockIndex = current.currentBlockIndex,
                        isRunning = false,
                        endAtElapsedRealtime = 0L,
                        endAtWallClockMillis = 0L,
                        timerBootCount = currentBootCount(),
                        expectedEndAtElapsedRealtime = previousEndElapsed
                    )
                }
            }
        }
    }

    private fun onBlockFinished() {
        if (transitionInProgress) return
        transitionInProgress = true
        stopTimerJob()
        viewModelScope.launch {
            transitionMutex.withLock {
                try {
                    val current = _focusState.value
                    if (current.isBreak) finishBreakTransition(current) else finishFocusTransition(current)
                } catch (_: Exception) {
                    _focusState.value = _focusState.value.copy(
                        isRunning = false,
                        sessionError = "The session could not save this transition. Please reopen the session."
                    )
                    StudyTimerForegroundService.stop(StudyApplication.instance)
                } finally {
                    transitionInProgress = false
                }
            }
        }
    }

    private suspend fun finishFocusTransition(current: FocusTimerState) {
        // The foreground service can finalize an expired timer while this
        // ViewModel ticker is waking up at the same moment. Re-read the
        // persisted plan first so the same block can never be committed twice.
        val persistedBeforeTransition = current.planId?.let { repository.getPlanById(it) }
        if (
            persistedBeforeTransition != null &&
            (
                !persistedBeforeTransition.isTimerRunning ||
                    persistedBeforeTransition.endAtWallClockMillis != current.endAtWallClockMillis
                )
        ) {
            if (persistedBeforeTransition.isCompleted) {
                StudyTimerForegroundService.stop(StudyApplication.instance)
                _focusState.value = current.copy(
                    secondsRemaining = 0,
                    isRunning = false,
                    isSessionCompleted = true,
                    currentBlockIndex = persistedBeforeTransition.currentBlockIndex,
                    completedMinutes = persistedBeforeTransition.accumulatedBillableMinutes,
                    completedBlocks = persistedBeforeTransition.currentBlockIndex,
                    actualStudiedSeconds = persistedBeforeTransition.accumulatedStudiedSeconds,
                    planId = null,
                    endAtElapsedRealtime = 0L,
                    endAtWallClockMillis = 0L
                )
                return
            }

            if (
                persistedBeforeTransition.isBreakPhase &&
                    persistedBeforeTransition.currentBlockIndex == current.currentBlockIndex + 1
            ) {
                val breakSec = persistedBeforeTransition.remainingSecondsInBlock.coerceAtLeast(1)
                val nextItem = currentPlanItems.getOrNull(persistedBeforeTransition.currentBlockIndex)
                _focusState.value = current.copy(
                    isBreak = true,
                    secondsRemaining = breakSec,
                    totalBlockSeconds = breakSec,
                    currentBlockIndex = persistedBeforeTransition.currentBlockIndex,
                    actualStudiedSeconds = persistedBeforeTransition.accumulatedStudiedSeconds,
                    completedMinutes = persistedBeforeTransition.accumulatedBillableMinutes,
                    isRunning = false,
                    currentSubject = nextItem?.subject ?: current.currentSubject,
                    currentChapter = nextItem?.topic ?: current.currentChapter,
                    endAtElapsedRealtime = 0L,
                    endAtWallClockMillis = 0L
                )
                startTimerInternal()
                return
            }
        }

        val completedBlockMinutes = SessionResultCalculator.billableMinutes(current.totalBlockSeconds)
        val nextBlockIndex = current.currentBlockIndex + 1
        val newActualSeconds = current.actualStudiedSeconds + current.totalBlockSeconds
        val plannedFocusSeconds = currentPlanItems.sumOf { it.minutes } * 60
        val allCompleted =
            nextBlockIndex >= current.totalBlocks ||
                (plannedFocusSeconds > 0 && newActualSeconds >= plannedFocusSeconds)
        val newCompletedMinutes = current.completedMinutes + completedBlockMinutes

        if (allCompleted) {
            val planId = current.planId
            val updatedPlan = planId?.let { repository.getPlanById(it) }
            if (planId != null && updatedPlan != null) {
                repository.commitFocusBlock(
                    planId = planId,
                    updatedPlan = updatedPlan.copy(
                        currentBlockIndex = nextBlockIndex,
                        remainingSecondsInBlock = 0,
                        isBreakPhase = false,
                        isCompleted = true,
                        isTimerRunning = false,
                        endAtElapsedRealtime = 0L,
                        endAtWallClockMillis = 0L,
                        accumulatedStudiedSeconds = updatedPlan.accumulatedStudiedSeconds + current.totalBlockSeconds,
                        accumulatedBillableMinutes = updatedPlan.accumulatedBillableMinutes + completedBlockMinutes,
                        lastUpdated = System.currentTimeMillis()
                    ),
                    subject = current.currentSubject,
                    chapter = current.currentChapter,
                    minutesStudied = completedBlockMinutes,
                    mode = current.mode
                )
            } else if (completedBlockMinutes > 0) {
                repository.recordCompletedSession(current.currentSubject, current.currentChapter, completedBlockMinutes, current.mode)
            }
            StudyTimerForegroundService.stop(StudyApplication.instance)
            refreshTodayMinutes()
            _focusState.value = current.copy(
                secondsRemaining = 0,
                currentBlockIndex = nextBlockIndex,
                isRunning = false,
                isSessionCompleted = true,
                completedMinutes = newCompletedMinutes,
                completedBlocks = nextBlockIndex,
                actualStudiedSeconds = newActualSeconds,
                planId = null,
                endAtElapsedRealtime = 0L,
                endAtWallClockMillis = 0L
            )
            return
        }

        val breakSec = current.breakBlockSeconds.coerceAtLeast(1)
        val planId = current.planId
        val updatedPlan = planId?.let { repository.getPlanById(it) }
        if (planId != null && updatedPlan != null) {
            repository.commitFocusBlock(
                planId = planId,
                updatedPlan = updatedPlan.copy(
                    currentBlockIndex = nextBlockIndex,
                    durationPerBlockMinutes = currentPlanItems.getOrNull(nextBlockIndex)?.minutes ?: updatedPlan.durationPerBlockMinutes,
                    remainingSecondsInBlock = breakSec,
                    isBreakPhase = true,
                    isCompleted = false,
                    isTimerRunning = false,
                    endAtElapsedRealtime = 0L,
                    endAtWallClockMillis = 0L,
                    accumulatedStudiedSeconds = updatedPlan.accumulatedStudiedSeconds + current.totalBlockSeconds,
                    accumulatedBillableMinutes = updatedPlan.accumulatedBillableMinutes + completedBlockMinutes,
                    lastUpdated = System.currentTimeMillis()
                ),
                subject = current.currentSubject,
                chapter = current.currentChapter,
                minutesStudied = completedBlockMinutes,
                mode = current.mode
            )
        } else if (completedBlockMinutes > 0) {
            repository.recordCompletedSession(current.currentSubject, current.currentChapter, completedBlockMinutes, current.mode)
        }
        refreshTodayMinutes()

        val nextItem = currentPlanItems.getOrNull(nextBlockIndex)
        _focusState.value = current.copy(
            isBreak = true,
            secondsRemaining = breakSec,
            totalBlockSeconds = breakSec,
            currentBlockIndex = nextBlockIndex,
            actualStudiedSeconds = newActualSeconds,
            completedMinutes = newCompletedMinutes,
            isRunning = false,
            currentSubject = nextItem?.subject ?: current.currentSubject,
            currentChapter = nextItem?.topic ?: current.currentChapter,
            endAtElapsedRealtime = 0L,
            endAtWallClockMillis = 0L
        )
        startTimerInternal()
    }

    private suspend fun finishBreakTransition(current: FocusTimerState, playChime: Boolean = true) {
        val plannedFocusSeconds = currentPlanItems.sumOf { it.minutes } * 60
        if (
            current.currentBlockIndex >= current.totalBlocks ||
            (plannedFocusSeconds > 0 && current.actualStudiedSeconds >= plannedFocusSeconds)
        ) {
            StudyTimerForegroundService.stop(StudyApplication.instance)
            _focusState.value = current.copy(
                secondsRemaining = 0,
                isRunning = false,
                isSessionCompleted = true,
                completedMinutes = current.completedMinutes,
                completedBlocks = current.totalBlocks,
                planId = null,
                endAtElapsedRealtime = 0L,
                endAtWallClockMillis = 0L
            )
            return
        }

        val nextItem = currentPlanItems.getOrNull(current.currentBlockIndex)
        if (nextItem == null) {
            StudyTimerForegroundService.stop(StudyApplication.instance)
            val planId = current.planId
            val updatedPlan = planId?.let { repository.getPlanById(it) }
            if (planId != null && updatedPlan != null) {
                repository.commitFocusBlock(
                    planId = planId,
                    updatedPlan = updatedPlan.copy(
                        isCompleted = true,
                        isTimerRunning = false,
                        endAtElapsedRealtime = 0L,
                        endAtWallClockMillis = 0L,
                        lastUpdated = System.currentTimeMillis()
                    ),
                    subject = current.currentSubject,
                    chapter = current.currentChapter,
                    minutesStudied = 0,
                    mode = current.mode
                )
            }
            _focusState.value = current.copy(
                secondsRemaining = 0,
                isRunning = false,
                isSessionCompleted = true,
                completedMinutes = current.completedMinutes,
                completedBlocks = current.totalBlocks,
                planId = null,
                endAtElapsedRealtime = 0L,
                endAtWallClockMillis = 0L
            )
            return
        }
        val blockSec = nextItem.minutes * 60
        val planId = current.planId
        if (planId != null) {
            repository.updateSessionProgress(
                planId = planId,
                remainingSec = blockSec,
                isBreak = false,
                blockIndex = current.currentBlockIndex,
                isRunning = false,
                endAtElapsedRealtime = 0L,
                endAtWallClockMillis = 0L,
                timerBootCount = currentBootCount()
            )
        }
        _focusState.value = current.copy(
            isBreak = false,
            secondsRemaining = blockSec,
            totalBlockSeconds = blockSec,
            studyBlockSeconds = blockSec,
            currentSubject = nextItem.subject,
            currentChapter = nextItem.topic,
            isRunning = false,
            endAtElapsedRealtime = 0L,
            endAtWallClockMillis = 0L
        )
        startTimerInternal()
    }

    fun skipCurrentBlock() {
        if (transitionInProgress) return
        transitionInProgress = true
        stopTimerJob()
        val current = _focusState.value
        viewModelScope.launch {
            transitionMutex.withLock {
                try {
                    if (current.isBreak) {
                        finishBreakTransition(current, playChime = false)
                    } else {
                        val elapsedSeconds = currentElapsedSeconds(current)
                        val partialMinutes = SessionResultCalculator.billableMinutes(elapsedSeconds)
                        val newTotalStudiedSec = current.actualStudiedSeconds + elapsedSeconds
                        val newCompletedMinutes = current.completedMinutes + partialMinutes
                        val plannedFocusSeconds = currentPlanItems.sumOf { it.minutes } * 60
                        val isLastBlock =
                            current.currentBlockIndex >= current.totalBlocks - 1 ||
                                (plannedFocusSeconds > 0 && newTotalStudiedSec >= plannedFocusSeconds)
                        val planId = current.planId

                        if (isLastBlock) {
                            if (planId != null) {
                                repository.completePlanEarly(
                                    planId,
                                    partialMinutes,
                                    elapsedSeconds,
                                    current.currentSubject,
                                    current.currentChapter,
                                    current.mode,
                                    nextBlockIndex = current.totalBlocks
                                )
                            } else if (partialMinutes > 0) {
                                repository.recordCompletedSession(current.currentSubject, current.currentChapter, partialMinutes, current.mode)
                            }
                            StudyTimerForegroundService.stop(StudyApplication.instance)
                            refreshTodayMinutes()
                            _focusState.value = current.copy(
                                secondsRemaining = 0,
                                currentBlockIndex = current.totalBlocks,
                                isRunning = false,
                                isSessionCompleted = true,
                                completedMinutes = newCompletedMinutes,
                                completedBlocks = current.completedBlocks + if (elapsedSeconds > 0) 1 else 0,
                                actualStudiedSeconds = newTotalStudiedSec,
                                planId = null,
                                endAtElapsedRealtime = 0L,
                                endAtWallClockMillis = 0L
                            )
                        } else {
                            val breakSec = current.breakBlockSeconds.coerceAtLeast(1)
                            if (planId != null) {
                                val plan = repository.getPlanById(planId)
                                if (plan != null) {
                                    repository.commitFocusBlock(
                                        planId = planId,
                                        updatedPlan = plan.copy(
                                            currentBlockIndex = current.currentBlockIndex + 1,
                                            remainingSecondsInBlock = breakSec,
                                            isBreakPhase = true,
                                            isTimerRunning = false,
                                            endAtElapsedRealtime = 0L,
                                            endAtWallClockMillis = 0L,
                                            accumulatedStudiedSeconds = plan.accumulatedStudiedSeconds + elapsedSeconds,
                                            accumulatedBillableMinutes = plan.accumulatedBillableMinutes + partialMinutes,
                                            lastUpdated = System.currentTimeMillis()
                                        ),
                                        subject = current.currentSubject,
                                        chapter = current.currentChapter,
                                        minutesStudied = partialMinutes,
                                        mode = current.mode
                                    )
                                }
                            }
                            refreshTodayMinutes()
                            val nextItem = currentPlanItems.getOrNull(current.currentBlockIndex + 1)
                            _focusState.value = current.copy(
                                isBreak = true,
                                secondsRemaining = breakSec,
                                totalBlockSeconds = breakSec,
                                currentBlockIndex = current.currentBlockIndex + 1,
                                actualStudiedSeconds = newTotalStudiedSec,
                                completedMinutes = newCompletedMinutes,
                                isRunning = false,
                                currentSubject = nextItem?.subject ?: current.currentSubject,
                                currentChapter = nextItem?.topic ?: current.currentChapter,
                                endAtElapsedRealtime = 0L,
                                endAtWallClockMillis = 0L
                            )
                            startTimerInternal()
                        }
                    }
                } catch (_: Exception) {
                    _focusState.value = current.copy(
                        isRunning = false,
                        sessionError = "The session could not save this action safely."
                    )
                    StudyTimerForegroundService.stop(StudyApplication.instance)
                } finally {
                    transitionInProgress = false
                }
            }
        }
    }

    fun dismissSessionCompletion() {
        // Keep automatic restore disabled for the lifetime of this ViewModel.
        // A new session is created explicitly through startNewPlan().
        automaticRestoreEnabled = false
        _focusState.value = FocusTimerState()
        currentPlanItems = emptyList()
    }

    fun resetBlockTimer() {
        if (transitionInProgress) return
        transitionInProgress = true
        stopTimerJob()
        val current = _focusState.value
        // Reset discards the current block's partial progress. Only previously
        // committed blocks remain in the session totals.
        val resetSeconds = current.totalBlockSeconds

        viewModelScope.launch {
            transitionMutex.withLock {
                try {
                    val planId = current.planId
                    if (planId != null) {
                        val plan = repository.getPlanById(planId)
                        if (plan != null) {
                            repository.commitFocusBlock(
                                planId = planId,
                                updatedPlan = plan.copy(
                                    remainingSecondsInBlock = resetSeconds,
                                    isBreakPhase = current.isBreak,
                                    isTimerRunning = false,
                                    endAtElapsedRealtime = 0L,
                                    endAtWallClockMillis = 0L,
                                    accumulatedStudiedSeconds = plan.accumulatedStudiedSeconds,
                                    accumulatedBillableMinutes = plan.accumulatedBillableMinutes,
                                    lastUpdated = System.currentTimeMillis()
                                ),
                                subject = current.currentSubject,
                                chapter = current.currentChapter,
                                minutesStudied = 0,
                                mode = current.mode
                            )
                        }
                    }

                    StudyTimerForegroundService.stop(StudyApplication.instance)
                    _focusState.value = current.copy(
                        isRunning = false,
                        secondsRemaining = resetSeconds,
                        actualStudiedSeconds = current.actualStudiedSeconds,
                        completedMinutes = current.completedMinutes,
                        endAtElapsedRealtime = 0L,
                        endAtWallClockMillis = 0L
                    )
                } catch (_: Exception) {
                    _focusState.value = current.copy(
                        isRunning = false,
                        sessionError = "The block could not be restarted safely. Your saved progress was not discarded."
                    )
                } finally {
                    transitionInProgress = false
                }
            }
        }
    }

    private fun stopTimerJob() {
        timerGeneration++
        timerJob?.cancel()
        timerJob = null
    }

    private fun currentElapsedSeconds(state: FocusTimerState): Int {
        return if (!state.isRunning) {
            (state.totalBlockSeconds - state.secondsRemaining).coerceIn(0, state.totalBlockSeconds)
        } else {
            (state.totalBlockSeconds - remainingFromState(state)).coerceIn(0, state.totalBlockSeconds)
        }
    }

    private fun remainingFromState(state: FocusTimerState): Int {
        if (!state.isRunning) return state.secondsRemaining.coerceIn(0, state.totalBlockSeconds)
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        return TimerDeadlineCalculator.remainingSeconds(
            endAtElapsedRealtime = state.endAtElapsedRealtime,
            endAtWallClockMillis = state.endAtWallClockMillis,
            nowElapsedRealtime = nowElapsed,
            nowWallClockMillis = nowWall,
            totalBlockSeconds = state.totalBlockSeconds
        )
    }

    private fun currentBootCount(): Int =
        runCatching {
            Settings.Global.getInt(
                StudyApplication.instance.contentResolver,
                Settings.Global.BOOT_COUNT,
                -1
            )
        }.getOrDefault(-1)

    private fun normalizePlanItems(items: List<StudyPlanItem>): List<StudyPlanItem> {
        val normalized = items
            .filter { it.subject.isNotBlank() && it.topic.isNotBlank() && it.minutes in 1..720 }
            .flatMap(::splitStudyItem)
        return if (normalized.size <= 720) normalized else emptyList()
    }

    private fun formatPlanDuration(minutes: Int): String =
        if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"

    fun addExam(
        subject: String,
        examDate: String,
        daysRemaining: Int,
        priority: String,
        topics: String,
        confidence: Int
    ) {
        viewModelScope.launch {
            repository.addExam(
                ExamEntity(
                    subject = subject,
                    examDate = examDate,
                    daysRemaining = daysRemaining,
                    priority = priority,
                    syllabusTopics = topics,
                    confidenceLevel = confidence
                )
            )
        }
    }

    fun toggleExamCompleted(exam: ExamEntity) {
        viewModelScope.launch {
            repository.updateExam(exam.copy(isCompleted = !exam.isCompleted))
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.deleteExam(exam)
        }
    }

    fun setTheme(themeKey: String) {
        _currentTheme.value = themeKey
        _wallpaperStyle.value = repository.getThemeWallpaperStyle(themeKey)
        viewModelScope.launch {
            repository.updateTheme(themeKey)
        }
    }

    fun toggleWallpaperEnabled() {
        val next = !_isWallpaperEnabled.value
        _isWallpaperEnabled.value = next
        repository.setWallpaperEnabled(next)
        _isFocusWallpaperEnabled.value = next
        repository.setFocusWallpaperEnabled(next)
    }

    fun toggleFocusWallpaperEnabled() {
        val next = !_isFocusWallpaperEnabled.value
        _isFocusWallpaperEnabled.value = next
        repository.setFocusWallpaperEnabled(next)
    }

    fun setWallpaperOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0.1f, 1.0f)
        _wallpaperOpacity.value = clamped
        repository.setWallpaperOpacity(clamped)
    }

    fun setThemeWallpaperStyle(themeKey: String, styleId: String) {
        repository.setThemeWallpaperStyle(themeKey, styleId)
        if (_currentTheme.value == themeKey) {
            _wallpaperStyle.value = styleId
        }
    }

    fun setCustomWallpaperUri(uri: String?) {
        _customWallpaperUri.value = uri
        repository.setCustomWallpaperUri(uri)
        if (uri != null) {
            // Automatically switch style to custom
            setThemeWallpaperStyle(_currentTheme.value, "custom")
        }
    }

    fun setCustomAudio(uri: String?, displayName: String?) {
        _customAudioUri.value = uri
        _customAudioName.value = displayName
        repository.setCustomAudio(uri, displayName)
        _customAudioList.value = repository.getCustomAudioList()
        _selectedAudioId.value = repository.getSelectedCustomAudioId()
    }

    fun addCustomAudio(name: String, uri: String) {
        val added = repository.addCustomAudio(name, uri)
        _customAudioList.value = repository.getCustomAudioList()
        _selectedAudioId.value = added.id
        _customAudioUri.value = added.uri
        _customAudioName.value = added.name
    }

    fun removeCustomAudio(id: String) {
        val itemToRemove = _customAudioList.value.find { it.id == id }
        if (itemToRemove != null) {
            com.aistudio.studyos.service.AudioFileManager.deleteAudioFile(StudyApplication.instance, itemToRemove.uri)
        }
        repository.removeCustomAudio(id)
        _customAudioList.value = repository.getCustomAudioList()
        _selectedAudioId.value = repository.getSelectedCustomAudioId()
        _customAudioUri.value = repository.getCustomAudioUri()
        _customAudioName.value = repository.getCustomAudioName()
    }

    fun selectCustomAudio(id: String) {
        repository.setSelectedCustomAudioId(id)
        _selectedAudioId.value = id
        _customAudioUri.value = repository.getCustomAudioUri()
        _customAudioName.value = repository.getCustomAudioName()
    }

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch {
            repository.updateDailyGoal(minutes)
        }
    }

    fun resetAllStats() {
        automaticRestoreEnabled = false
        stopTimerJob()
        StudyTimerForegroundService.stop(StudyApplication.instance)
        _focusState.value = FocusTimerState()
        currentPlanItems = emptyList()
        viewModelScope.launch {
            repository.resetStats()
        }
    }

    fun deleteSessionLog(log: SessionLogEntity) {
        viewModelScope.launch {
            repository.deleteSessionLog(log)
        }
    }

    fun deleteSavedPlan(id: Long) {
        viewModelScope.launch {
            // Deleting the plan currently loaded in the timer must also clear
            // the in-memory focus state. Otherwise the bottom ActiveSessionMiniBar
            // can remain visible with a deleted session.
            if (_focusState.value.planId == id) {
                stopTimerJob()
                StudyTimerForegroundService.stop(StudyApplication.instance)
                _focusState.value = FocusTimerState()
            }
            repository.deletePlanById(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        updateCheckJob?.cancel()
    }
}

class StudyViewModelFactory(private val repository: StudyRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyViewModel::class.java)) {
            return StudyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}