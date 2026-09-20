package com.aistudio.studyos.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.studyos.data.local.entity.ExamEntity
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.local.entity.StudyPlanItem
import com.aistudio.studyos.data.local.entity.StudyPlanItemCodec
import com.aistudio.studyos.data.local.entity.UserProfileEntity
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
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
    val actualStudiedSeconds: Int = 0
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

    private val _currentTheme = MutableStateFlow(repository.getInitialTheme())
    val currentTheme: StateFlow<String> = _currentTheme.asStateFlow()

    val activePlan: StateFlow<StudyPlanEntity?> = repository.getActivePlan()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedPlans: StateFlow<List<StudyPlanEntity>> = repository.getSavedPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamEntity>> = repository.getAllExams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingExams: StateFlow<List<ExamEntity>> = repository.getUpcomingExams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<SessionLogEntity>> = repository.getRecentLogs(15)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayMinutes: StateFlow<Int> = repository.getTodayMinutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allLogs: StateFlow<List<SessionLogEntity>> = repository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _focusState = MutableStateFlow(FocusTimerState())
    val focusState: StateFlow<FocusTimerState> = _focusState.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    private var hasDismissedUpdateDialog: Boolean = false

    init {
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
                // Auto-restore active session if memory was empty (cold boot or process recreation)
                if (plan != null && _focusState.value.planId == null) {
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
        items: List<StudyPlanItem> = emptyList()
    ) {
        viewModelScope.launch {
            val sourceItems = if (items.isNotEmpty()) {
                items
            } else {
                List(totalBlocks.coerceIn(1, 720)) {
                    StudyPlanItem(subject, chapter, blockMinutes.coerceIn(1, 720))
                }
            }
            val normalizedItems = sourceItems
                .map { it.copy(minutes = it.minutes.coerceIn(1, 720)) }
                .filter { it.subject.isNotBlank() && it.topic.isNotBlank() }
                .flatMap(::splitStudyItem)
            val boundedItems = normalizedItems.take(720)
            val first = boundedItems.first()
            val studySec = first.minutes * 60
            val breakSec = normalizeBreakMinutes(breakMinutes) * 60
            val plan = StudyPlanEntity(
                title = title.ifBlank { "$subject - $chapter" },
                subject = first.subject,
                chapter = first.topic,
                mode = mode,
                totalBlocks = boundedItems.size,
                currentBlockIndex = 0,
                durationPerBlockMinutes = first.minutes,
                breakMinutes = normalizeBreakMinutes(breakMinutes),
                remainingSecondsInBlock = studySec,
                isBreakPhase = false,
                isCompleted = false,
                isDraft = false,
                planItems = StudyPlanItemCodec.encode(boundedItems),
                totalDurationMinutes = boundedItems.sumOf { it.minutes }
            )
            val planId = repository.savePlan(plan)
            pauseTimer()
            currentPlanItems = boundedItems
            _focusState.value = FocusTimerState(
                isRunning = false,
                isBreak = false,
                secondsRemaining = studySec,
                totalBlockSeconds = studySec,
                studyBlockSeconds = studySec,
                breakBlockSeconds = breakSec,
                currentBlockIndex = 0,
                totalBlocks = boundedItems.size,
                currentSubject = first.subject,
                currentChapter = first.topic,
                planId = planId,
                mode = mode,
                isSessionCompleted = false,
                completedMinutes = 0,
                completedBlocks = 0,
                actualStudiedSeconds = 0
            )
            if (autoStart) startTimer()
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
                    StudyPlanItem(subject, chapter, blockMinutes.coerceIn(1, 720))
                }
            }
            val normalizedItems = sourceItems
                .map { it.copy(minutes = it.minutes.coerceIn(1, 720)) }
                .filter { it.subject.isNotBlank() && it.topic.isNotBlank() }
                .flatMap(::splitStudyItem)
            val boundedItems = normalizedItems.take(720)
            val first = boundedItems.first()
            val studySec = first.minutes * 60
            val plan = StudyPlanEntity(
                title = title.ifBlank { "$subject - $chapter" },
                subject = first.subject,
                chapter = first.topic,
                mode = mode,
                totalBlocks = boundedItems.size,
                currentBlockIndex = 0,
                durationPerBlockMinutes = first.minutes,
                breakMinutes = normalizeBreakMinutes(breakMinutes),
                remainingSecondsInBlock = studySec,
                isBreakPhase = false,
                isCompleted = false,
                isDraft = true,
                planItems = StudyPlanItemCodec.encode(boundedItems),
                totalDurationMinutes = boundedItems.sumOf { it.minutes }
            )
            repository.savePlan(plan)
        }
    }

    fun resumeSavedPlan(plan: StudyPlanEntity) {
        viewModelScope.launch {
            val updated = plan.copy(isDraft = false, lastUpdated = System.currentTimeMillis())
            repository.updatePlan(updated)
            continueActiveSession(updated)
        }
    }

    fun continueActiveSession(plan: StudyPlanEntity) {
        val current = _focusState.value
        // If current in-memory state is already running/holding this active plan, preserve it!
        if (current.planId == plan.id) {
            return
        }
        pauseTimer()
        val decodedItems = StudyPlanItemCodec.decode(plan.planItems).ifEmpty {
            listOf(StudyPlanItem(plan.subject, plan.chapter, plan.durationPerBlockMinutes))
        }
        currentPlanItems = decodedItems.flatMap(::splitStudyItem).take(720)
        val currentItem = currentPlanItems.getOrElse(plan.currentBlockIndex) { currentPlanItems.last() }
        val studySec = currentItem.minutes * 60
        val breakSec = normalizeBreakMinutes(plan.breakMinutes) * 60
        val totalBlockSec = if (plan.isBreakPhase) breakSec else studySec
        val remainingSec = if (plan.remainingSecondsInBlock in 1..totalBlockSec) {
            plan.remainingSecondsInBlock
        } else {
            totalBlockSec
        }

        val priorCompletedMinutes = currentPlanItems.take(plan.currentBlockIndex).sumOf { it.minutes }

        _focusState.value = FocusTimerState(
            isRunning = false,
            isBreak = plan.isBreakPhase,
            secondsRemaining = remainingSec,
            totalBlockSeconds = totalBlockSec,
            studyBlockSeconds = studySec,
            breakBlockSeconds = breakSec,
            currentBlockIndex = plan.currentBlockIndex,
            totalBlocks = currentPlanItems.size,
            currentSubject = currentItem.subject,
            currentChapter = currentItem.topic,
            planId = plan.id,
            mode = plan.mode,
            isSessionCompleted = false,
            completedMinutes = priorCompletedMinutes,
            completedBlocks = plan.currentBlockIndex,
            actualStudiedSeconds = priorCompletedMinutes * 60
        )
    }

    fun setupFocusSession(plan: StudyPlanEntity) {
        continueActiveSession(plan)
    }

    fun finishActiveSessionEarly(): Boolean {
        val current = _focusState.value
        pauseTimer()
        val planId = current.planId
        val elapsedInCurrentBlock = if (!current.isBreak) {
            (current.totalBlockSeconds - current.secondsRemaining).coerceIn(0, current.totalBlockSeconds)
        } else 0
        val totalStudiedSec = current.actualStudiedSeconds + elapsedInCurrentBlock
        val partialMinutes = if (elapsedInCurrentBlock >= 30) (elapsedInCurrentBlock + 29) / 60 else if (elapsedInCurrentBlock > 0) 1 else 0
        val totalStudiedMin = if (totalStudiedSec >= 30) (totalStudiedSec + 29) / 60 else if (totalStudiedSec > 0) 1 else 0

        viewModelScope.launch {
            if (planId != null) {
                repository.completePlanEarly(
                    planId = planId,
                    minutesStudied = partialMinutes,
                    subject = current.currentSubject,
                    chapter = current.currentChapter,
                    mode = current.mode
                )
            } else if (partialMinutes > 0) {
                repository.recordCompletedSession(
                    subject = current.currentSubject,
                    chapter = current.currentChapter,
                    durationMinutes = partialMinutes,
                    mode = current.mode
                )
            }
        }

        _focusState.value = current.copy(
            secondsRemaining = 0,
            isRunning = false,
            isSessionCompleted = true,
            completedMinutes = totalStudiedMin,
            completedBlocks = current.currentBlockIndex + (if (elapsedInCurrentBlock > 0) 1 else 0),
            actualStudiedSeconds = totalStudiedSec,
            planId = null
        )
        return true
    }

    fun toggleTimer() {
        if (_focusState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun startTimer() {
        _focusState.value = _focusState.value.copy(isRunning = true)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var saveCounter = 0
            while (isActive && _focusState.value.isRunning && _focusState.value.secondsRemaining > 0) {
                delay(1000)
                val remaining = (_focusState.value.secondsRemaining - 1).coerceAtLeast(0)
                if (remaining <= 0) {
                    _focusState.value = _focusState.value.copy(secondsRemaining = 0)
                    onBlockFinished()
                } else {
                    _focusState.value = _focusState.value.copy(secondsRemaining = remaining)
                    saveCounter++
                    if (saveCounter >= 5) {
                        saveCounter = 0
                        val current = _focusState.value
                        if (current.planId != null) {
                            repository.updateSessionProgress(
                                current.planId,
                                remaining,
                                current.isBreak,
                                current.currentBlockIndex
                            )
                        }
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _focusState.value = _focusState.value.copy(isRunning = false)
        val current = _focusState.value
        if (current.planId != null) {
            viewModelScope.launch {
                repository.updateSessionProgress(
                    current.planId,
                    current.secondsRemaining,
                    current.isBreak,
                    current.currentBlockIndex
                )
            }
        }
    }

    private fun onBlockFinished() {
        pauseTimer()
        val current = _focusState.value

        if (!current.isBreak) {
            val blockMinutes = (current.totalBlockSeconds / 60).coerceAtLeast(1)
            val nextBlockIndex = current.currentBlockIndex + 1
            val allCompleted = nextBlockIndex >= current.totalBlocks
            val newTotalStudiedSec = current.actualStudiedSeconds + current.totalBlockSeconds
            val finalStudiedMin = (newTotalStudiedSec / 60).coerceAtLeast(1)

            viewModelScope.launch {
                repository.recordCompletedSession(
                    subject = current.currentSubject,
                    chapter = current.currentChapter,
                    durationMinutes = blockMinutes,
                    mode = current.mode
                )

                if (current.planId != null) {
                    repository.updatePlanProgress(
                        planId = current.planId,
                        blockIndex = nextBlockIndex,
                        isCompleted = allCompleted
                    )
                }

                if (allCompleted) {
                    _focusState.value = current.copy(
                        secondsRemaining = 0,
                        currentBlockIndex = nextBlockIndex,
                        isRunning = false,
                        isSessionCompleted = true,
                        completedMinutes = finalStudiedMin,
                        completedBlocks = nextBlockIndex,
                        actualStudiedSeconds = newTotalStudiedSec,
                        planId = null
                    )
                } else {
                    val breakSec = current.breakBlockSeconds.coerceAtLeast(1)
                    _focusState.value = current.copy(
                        isBreak = true,
                        secondsRemaining = breakSec,
                        totalBlockSeconds = breakSec,
                        currentBlockIndex = nextBlockIndex,
                        actualStudiedSeconds = newTotalStudiedSec,
                        isRunning = false
                    )
                    if (current.planId != null) {
                        repository.updateSessionProgress(
                            current.planId,
                            breakSec,
                            true,
                            nextBlockIndex
                        )
                    }
                    // Breaks begin automatically
                    startTimer()
                }
            }
        } else {
            val isSessionAtEnd = current.currentBlockIndex >= current.totalBlocks
            if (isSessionAtEnd) {
                val totalStudiedMin = if (current.actualStudiedSeconds >= 30) (current.actualStudiedSeconds + 29) / 60 else if (current.actualStudiedSeconds > 0) 1 else 0
                viewModelScope.launch {
                    if (current.planId != null) {
                        repository.updatePlanProgress(
                            planId = current.planId,
                            blockIndex = current.totalBlocks,
                            isCompleted = true
                        )
                    }
                }
                _focusState.value = current.copy(
                    secondsRemaining = 0,
                    isRunning = false,
                    isSessionCompleted = true,
                    completedMinutes = totalStudiedMin,
                    completedBlocks = current.totalBlocks,
                    planId = null
                )
                return
            }

            val nextItem = currentPlanItems.getOrNull(current.currentBlockIndex)
            val blockSec = (nextItem?.minutes ?: (current.studyBlockSeconds / 60).coerceAtLeast(1)) * 60
            _focusState.value = current.copy(
                isBreak = false,
                secondsRemaining = blockSec,
                totalBlockSeconds = blockSec,
                studyBlockSeconds = blockSec,
                currentSubject = nextItem?.subject ?: current.currentSubject,
                currentChapter = nextItem?.topic ?: current.currentChapter,
                isRunning = false
            )
            if (current.planId != null) {
                viewModelScope.launch {
                    repository.updateSessionProgress(
                        current.planId,
                        blockSec,
                        false,
                        current.currentBlockIndex
                    )
                }
            }
            // Next focus block begins automatically
            startTimer()
        }
    }

    fun skipCurrentBlock() {
        pauseTimer()
        val current = _focusState.value

        if (!current.isBreak) {
            // Focus block skipped: count EXACTLY the actual time studied so far in this block
            val elapsedSeconds = (current.totalBlockSeconds - current.secondsRemaining).coerceIn(0, current.totalBlockSeconds)
            val partialMinutes = if (elapsedSeconds >= 30) (elapsedSeconds + 29) / 60 else if (elapsedSeconds > 0) 1 else 0
            val newTotalStudiedSec = current.actualStudiedSeconds + elapsedSeconds
            val isLastBlock = current.currentBlockIndex >= current.totalBlocks - 1

            // If actual focus time was spent, log it to update progress, today's minutes, streak, XP!
            if (partialMinutes > 0) {
                viewModelScope.launch {
                    repository.recordCompletedSession(
                        subject = current.currentSubject,
                        chapter = current.currentChapter,
                        durationMinutes = partialMinutes,
                        mode = current.mode
                    )
                }
            }

            if (isLastBlock) {
                // Final block skipped -> session finishes, show congratulations screen!
                val totalStudiedMin = if (newTotalStudiedSec >= 30) (newTotalStudiedSec + 29) / 60 else if (newTotalStudiedSec > 0) 1 else 0
                viewModelScope.launch {
                    if (current.planId != null) {
                        repository.updatePlanProgress(
                            planId = current.planId,
                            blockIndex = current.totalBlocks,
                            isCompleted = true
                        )
                    }
                }
                _focusState.value = current.copy(
                    secondsRemaining = 0,
                    currentBlockIndex = current.totalBlocks,
                    isRunning = false,
                    isSessionCompleted = true,
                    completedMinutes = totalStudiedMin,
                    completedBlocks = current.currentBlockIndex + 1,
                    actualStudiedSeconds = newTotalStudiedSec,
                    planId = null
                )
            } else {
                // Intermediate block skipped -> proceed to break phase
                val breakSec = current.breakBlockSeconds.coerceAtLeast(1)
                _focusState.value = current.copy(
                    isBreak = true,
                    secondsRemaining = breakSec,
                    totalBlockSeconds = breakSec,
                    currentBlockIndex = current.currentBlockIndex + 1,
                    actualStudiedSeconds = newTotalStudiedSec,
                    isRunning = false
                )
                if (current.planId != null) {
                    viewModelScope.launch {
                        repository.updateSessionProgress(
                            current.planId,
                            breakSec,
                            true,
                            current.currentBlockIndex + 1
                        )
                    }
                }
                startTimer()
            }
        } else {
            // Break phase skipped (breaks are rest, so 0 study time added)
            val isSessionAtEnd = current.currentBlockIndex >= current.totalBlocks
            if (isSessionAtEnd) {
                // No more focus blocks -> session finishes, show congratulations screen!
                val totalStudiedMin = if (current.actualStudiedSeconds >= 30) (current.actualStudiedSeconds + 29) / 60 else if (current.actualStudiedSeconds > 0) 1 else 0
                viewModelScope.launch {
                    if (current.planId != null) {
                        repository.updatePlanProgress(
                            planId = current.planId,
                            blockIndex = current.totalBlocks,
                            isCompleted = true
                        )
                    }
                }
                _focusState.value = current.copy(
                    secondsRemaining = 0,
                    isRunning = false,
                    isSessionCompleted = true,
                    completedMinutes = totalStudiedMin,
                    completedBlocks = current.totalBlocks,
                    planId = null
                )
            } else {
                // Move immediately to next focus block
                val nextItem = currentPlanItems.getOrNull(current.currentBlockIndex)
                val blockSec = (nextItem?.minutes ?: (current.studyBlockSeconds / 60).coerceAtLeast(1)) * 60
                _focusState.value = current.copy(
                    isBreak = false,
                    secondsRemaining = blockSec,
                    totalBlockSeconds = blockSec,
                    studyBlockSeconds = blockSec,
                    currentSubject = nextItem?.subject ?: current.currentSubject,
                    currentChapter = nextItem?.topic ?: current.currentChapter,
                    isRunning = false
                )
                if (current.planId != null) {
                    viewModelScope.launch {
                        repository.updateSessionProgress(
                            current.planId,
                            blockSec,
                            false,
                            current.currentBlockIndex
                        )
                    }
                }
                startTimer()
            }
        }
    }

    fun dismissSessionCompletion() {
        _focusState.value = FocusTimerState()
        currentPlanItems = emptyList()
    }

    fun resetBlockTimer() {
        pauseTimer()
        _focusState.value = _focusState.value.copy(
            secondsRemaining = _focusState.value.totalBlockSeconds
        )
    }

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
        viewModelScope.launch {
            repository.updateTheme(themeKey)
        }
    }

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch {
            repository.updateDailyGoal(minutes)
        }
    }

    fun resetAllStats() {
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
