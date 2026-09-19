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
    val currentBlockIndex: Int = 0,
    val totalBlocks: Int = 4,
    val currentSubject: String = "Quick Focus",
    val currentChapter: String = "General Study",
    val planId: Long? = null
)

class StudyViewModel(private val repository: StudyRepository) : ViewModel() {

    private var timerJob: Job? = null

    private val _currentTheme = MutableStateFlow(repository.getInitialTheme())
    val currentTheme: StateFlow<String> = _currentTheme.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureCleanInitialData()
            repository.getUserProfile().collect { profile ->
                if (profile != null && profile.themePreset.isNotBlank()) {
                    _currentTheme.value = profile.themePreset
                }
            }
        }
    }

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

    val userProfile: StateFlow<UserProfileEntity?> = repository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _focusState = MutableStateFlow(FocusTimerState())
    val focusState: StateFlow<FocusTimerState> = _focusState.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    private var hasDismissedUpdateDialog: Boolean = false

    fun checkAppUpdate(context: Context, isManual: Boolean = false) {
        if (!isManual && hasDismissedUpdateDialog) return

        viewModelScope.launch {
            if (isManual) {
                _updateCheckState.value = UpdateCheckState.Checking
            }
            val update = UpdateManager.checkForUpdate(context.applicationContext, isManual)
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
        breakMinutes: Int = 5
    ) {
        viewModelScope.launch {
            val plan = StudyPlanEntity(
                title = title.ifBlank { "$subject - $chapter" },
                subject = subject,
                chapter = chapter,
                mode = mode,
                totalBlocks = totalBlocks,
                currentBlockIndex = 0,
                durationPerBlockMinutes = blockMinutes,
                breakMinutes = breakMinutes,
                isCompleted = false,
                isDraft = false
            )
            val planId = repository.savePlan(plan)
            setupFocusSession(plan.copy(id = planId))
        }
    }

    fun setupFocusSession(plan: StudyPlanEntity) {
        pauseTimer()
        _focusState.value = FocusTimerState(
            isRunning = false,
            isBreak = false,
            secondsRemaining = plan.durationPerBlockMinutes * 60,
            totalBlockSeconds = plan.durationPerBlockMinutes * 60,
            currentBlockIndex = plan.currentBlockIndex,
            totalBlocks = plan.totalBlocks,
            currentSubject = plan.subject,
            currentChapter = plan.chapter,
            planId = plan.id
        )
    }

    fun toggleTimer() {
        if (_focusState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        _focusState.value = _focusState.value.copy(isRunning = true)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _focusState.value.isRunning && _focusState.value.secondsRemaining > 0) {
                delay(1000)
                val remaining = _focusState.value.secondsRemaining - 1
                if (remaining <= 0) {
                    onBlockFinished()
                } else {
                    _focusState.value = _focusState.value.copy(secondsRemaining = remaining)
                }
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _focusState.value = _focusState.value.copy(isRunning = false)
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
                    mode = "focus"
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
                val breakSec = 5 * 60
                _focusState.value = current.copy(
                    isBreak = true,
                    secondsRemaining = breakSec,
                    totalBlockSeconds = breakSec,
                    currentBlockIndex = nextBlockIndex
                )
            }
        } else {
            // Break finished, ready for next study block
            val planMinutes = 25
            _focusState.value = current.copy(
                isBreak = false,
                secondsRemaining = planMinutes * 60,
                totalBlockSeconds = planMinutes * 60
            )
        }
    }

    fun skipCurrentBlock() {
        pauseTimer()
        val current = _focusState.value
        if (!current.isBreak) {
            // Switch to break
            val breakSec = 5 * 60
            _focusState.value = current.copy(
                isBreak = true,
                secondsRemaining = breakSec,
                totalBlockSeconds = breakSec,
                currentBlockIndex = current.currentBlockIndex + 1
            )
        } else {
            // Switch to next focus block
            val blockSec = 25 * 60
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
