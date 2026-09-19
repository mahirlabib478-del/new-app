package com.aistudio.studyos.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.studyos.data.local.entity.ExamEntity
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.local.entity.UserProfileEntity
import com.aistudio.studyos.data.repository.StudyRepository
import com.aistudio.studyos.data.update.AppUpdateInfo
import com.aistudio.studyos.data.update.UpdateCheckState
import com.aistudio.studyos.data.update.UpdateManager
import kotlinx.coroutines.Job
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
    val mode: String = "regular"
)

class StudyViewModel(private val repository: StudyRepository) : ViewModel() {

    private var timerJob: Job? = null

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
        viewModelScope.launch {
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

        viewModelScope.launch {
            if (isManual) {
                _updateCheckState.value = UpdateCheckState.Checking
            }
            val update = UpdateManager.checkForUpdate(
                context.applicationContext,
                isManual = isManual,
                forceCheck = forceCheck
            )
            if (update != null) {
                _availableUpdate.value = update
                _updateCheckState.value = UpdateCheckState.Available(update)
            } else {
                if (isManual) {
                    val (vName, _) = UpdateManager.getCurrentVersionInfo(context.applicationContext)
                    _updateCheckState.value = UpdateCheckState.UpToDate(vName)
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
        autoStart: Boolean = true
    ) {
        viewModelScope.launch {
            val studySec = blockMinutes * 60
            val breakSec = breakMinutes * 60
            val plan = StudyPlanEntity(
                title = title.ifBlank { "$subject - $chapter" },
                subject = subject,
                chapter = chapter,
                mode = mode,
                totalBlocks = totalBlocks,
                currentBlockIndex = 0,
                durationPerBlockMinutes = blockMinutes,
                breakMinutes = breakMinutes,
                remainingSecondsInBlock = studySec,
                isBreakPhase = false,
                isCompleted = false,
                isDraft = false
            )
            val planId = repository.savePlan(plan)
            pauseTimer()
            _focusState.value = FocusTimerState(
                isRunning = false,
                isBreak = false,
                secondsRemaining = studySec,
                totalBlockSeconds = studySec,
                studyBlockSeconds = studySec,
                breakBlockSeconds = breakSec,
                currentBlockIndex = 0,
                totalBlocks = totalBlocks,
                currentSubject = subject,
                currentChapter = chapter,
                planId = planId,
                mode = mode
            )
            if (autoStart) {
                startTimer()
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
        breakMinutes: Int = 5
    ) {
        viewModelScope.launch {
            val studySec = blockMinutes * 60
            val plan = StudyPlanEntity(
                title = title.ifBlank { "$subject - $chapter" },
                subject = subject,
                chapter = chapter,
                mode = mode,
                totalBlocks = totalBlocks,
                currentBlockIndex = 0,
                durationPerBlockMinutes = blockMinutes,
                breakMinutes = breakMinutes,
                remainingSecondsInBlock = studySec,
                isBreakPhase = false,
                isCompleted = false,
                isDraft = true
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
        val studySec = plan.durationPerBlockMinutes * 60
        val breakSec = plan.breakMinutes * 60
        val totalBlockSec = if (plan.isBreakPhase) breakSec else studySec
        val remainingSec = if (plan.remainingSecondsInBlock in 1..totalBlockSec) {
            plan.remainingSecondsInBlock
        } else {
            totalBlockSec
        }

        _focusState.value = FocusTimerState(
            isRunning = false,
            isBreak = plan.isBreakPhase,
            secondsRemaining = remainingSec,
            totalBlockSeconds = totalBlockSec,
            studyBlockSeconds = studySec,
            breakBlockSeconds = breakSec,
            currentBlockIndex = plan.currentBlockIndex,
            totalBlocks = plan.totalBlocks,
            currentSubject = plan.subject,
            currentChapter = plan.chapter,
            planId = plan.id,
            mode = plan.mode
        )
    }

    fun setupFocusSession(plan: StudyPlanEntity) {
        continueActiveSession(plan)
    }

    fun finishActiveSessionEarly() {
        val current = _focusState.value
        pauseTimer()
        val planId = current.planId
        val minutesStudied = ((current.totalBlockSeconds - current.secondsRemaining) / 60).coerceAtLeast(0)

        viewModelScope.launch {
            if (planId != null) {
                repository.completePlanEarly(
                    planId = planId,
                    minutesStudied = if (!current.isBreak) minutesStudied else 0,
                    subject = current.currentSubject,
                    chapter = current.currentChapter,
                    mode = current.mode
                )
            }
            _focusState.value = FocusTimerState()
        }
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
                val remaining = _focusState.value.secondsRemaining - 1
                if (remaining <= 0) {
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
            // Completed study block! Record session log and reward XP
            val blockMinutes = current.totalBlockSeconds / 60
            val nextBlockIndex = current.currentBlockIndex + 1
            val allCompleted = nextBlockIndex >= current.totalBlocks

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
            }

            if (allCompleted) {
                // All blocks completed!
                _focusState.value = current.copy(
                    secondsRemaining = 0,
                    currentBlockIndex = nextBlockIndex,
                    isRunning = false
                )
            } else {
                // Switch to break
                val breakSec = current.breakBlockSeconds
                _focusState.value = current.copy(
                    isBreak = true,
                    secondsRemaining = breakSec,
                    totalBlockSeconds = breakSec,
                    currentBlockIndex = nextBlockIndex
                )
                if (current.planId != null) {
                    viewModelScope.launch {
                        repository.updateSessionProgress(
                            current.planId,
                            breakSec,
                            true,
                            nextBlockIndex
                        )
                    }
                }
            }
        } else {
            // Break finished, ready for next study block
            val blockSec = current.studyBlockSeconds
            _focusState.value = current.copy(
                isBreak = false,
                secondsRemaining = blockSec,
                totalBlockSeconds = blockSec
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
        }
    }

    fun skipCurrentBlock() {
        pauseTimer()
        val current = _focusState.value
        if (!current.isBreak) {
            // Switch to break
            val breakSec = current.breakBlockSeconds
            _focusState.value = current.copy(
                isBreak = true,
                secondsRemaining = breakSec,
                totalBlockSeconds = breakSec,
                currentBlockIndex = current.currentBlockIndex + 1
            )
        } else {
            // Switch to next focus block
            val blockSec = current.studyBlockSeconds
            _focusState.value = current.copy(
                isBreak = false,
                secondsRemaining = blockSec,
                totalBlockSeconds = blockSec
            )
        }
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
