package com.aistudio.studyos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.studyos.audio.AmbientSound
import com.aistudio.studyos.audio.AmbientSoundSynthesizer
import com.aistudio.studyos.data.local.entity.ExamEntity
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.local.entity.UserProfileEntity
import com.aistudio.studyos.data.repository.StudyRepository
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
    val currentSubject: String = "Mathematics",
    val currentChapter: String = "Linear Algebra",
    val activeSound: AmbientSound = AmbientSound.NONE
)

class StudyViewModel(private val repository: StudyRepository) : ViewModel() {

    private val soundSynthesizer = AmbientSoundSynthesizer()
    private var timerJob: Job? = null

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
            activeSound = _focusState.value.activeSound
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
            viewModelScope.launch {
                repository.recordCompletedSession(
                    subject = current.currentSubject,
                    chapter = current.currentChapter,
                    durationMinutes = blockMinutes,
                    mode = "focus"
                )
            }

            val nextBlockIndex = current.currentBlockIndex + 1
            if (nextBlockIndex >= current.totalBlocks) {
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

    fun setAmbientSound(sound: AmbientSound) {
        soundSynthesizer.play(sound)
        _focusState.value = _focusState.value.copy(activeSound = sound)
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
        soundSynthesizer.stop()
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
