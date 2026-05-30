package com.studyplanner.app.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyplanner.app.core.data.local.dao.SubjectDao
import com.studyplanner.app.core.data.local.dao.TopicDao
import com.studyplanner.app.core.data.local.dao.VisionBoardDao
import com.studyplanner.app.core.data.local.entity.SessionEntity
import com.studyplanner.app.core.data.repository.SessionRepository
import com.studyplanner.app.core.util.StreakMilestone
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SessionPhase { PRE_CONFIRM, VISION_FLASH, STUDY, BREAK, COMPLETE, EXTENSION_DIALOG }

data class SessionUiState(
    val phase: SessionPhase = SessionPhase.PRE_CONFIRM,
    val session: SessionEntity? = null,
    val topicName: String = "",
    val subjectName: String = "",
    val subjectColor: String = "#1565C0",
    val studyMinutes: Int = 50,
    val breakMinutes: Int = 10,
    val musicUrl: String = "",
    val applyToAllToday: Boolean = false,
    val emotion: String = "GOOD",
    val timerRemainingMs: Long = 0L,
    val timerTotalMs: Long = 0L,
    val isBreak: Boolean = false,
    val pomodoroCount: Int = 0,
    val streakMilestone: StreakMilestone? = null,
    val visionBoardDream: String = "",
    val visionBoardPhotoUri: String = "",
    val showExtensionDialog: Boolean = false,
    val pointsEarned: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val subjectDao: SubjectDao,
    private val topicDao: TopicDao,
    private val visionBoardDao: VisionBoardDao,
    private val auth: FirebaseAuth,
    private val antiCheatManager: com.studyplanner.app.core.util.AntiCheatManager,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle["sessionId"] ?: 0L
    private val _state = MutableStateFlow(SessionUiState())
    val state = _state.asStateFlow()
    val antiCheatState = antiCheatManager.state
    private var timerJob: Job? = null

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val session = sessionRepository.getById(sessionId) ?: run {
                _state.update { it.copy(isLoading = false, error = "Session not found") }
                return@launch
            }
            val subject = subjectDao.getById(session.subjectId)
            val topic = if (session.topicId > 0) topicDao.getById(session.topicId) else null
            val uid = auth.currentUser?.uid ?: ""
            val visionBoard = visionBoardDao.get(uid)

            _state.update {
                it.copy(
                    isLoading = false,
                    session = session,
                    topicName = topic?.name ?: "Study Session",
                    subjectName = subject?.name ?: "",
                    subjectColor = subject?.colorHex ?: "#1565C0",
                    studyMinutes = session.studyMinutes,
                    breakMinutes = session.breakMinutes,
                    musicUrl = session.backgroundMusicUrl,
                    timerRemainingMs = session.studyMinutes * 60 * 1000L,
                    timerTotalMs = session.studyMinutes * 60 * 1000L,
                    visionBoardDream = visionBoard?.dreamPost ?: "",
                    visionBoardPhotoUri = visionBoard?.photoUri ?: "",
                )
            }
        }
    }

    fun updatePreConfirm(studyMin: Int, breakMin: Int, musicUrl: String, applyAll: Boolean) {
        _state.update { it.copy(studyMinutes = studyMin, breakMinutes = breakMin,
            musicUrl = musicUrl, applyToAllToday = applyAll) }
    }

    fun updateEmotion(emotion: String) = _state.update { it.copy(emotion = emotion) }

    fun startSession() {
        viewModelScope.launch {
            val s = _state.value
            sessionRepository.startSession(
                sessionId, s.emotion, s.studyMinutes, s.breakMinutes, s.musicUrl
            )
            _state.update { it.copy(phase = SessionPhase.VISION_FLASH) }
            delay(3000)
            antiCheatManager.onSessionStart()
            _state.update { it.copy(
                phase = SessionPhase.STUDY,
                timerRemainingMs = s.studyMinutes * 60 * 1000L,
                timerTotalMs = s.studyMinutes * 60 * 1000L,
                isBreak = false
            ) }
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.timerRemainingMs > 0) {
                delay(1000)
                _state.update { it.copy(timerRemainingMs = it.timerRemainingMs - 1000) }

                val remaining = _state.value.timerRemainingMs
                val total = _state.value.timerTotalMs
                val twoMinMs = 2 * 60 * 1000L

                if (remaining == twoMinMs && !_state.value.isBreak) {
                    _state.update { it.copy(showExtensionDialog = true) }
                }
            }

            if (!_state.value.isBreak) {
                startBreak()
            } else {
                resumeStudy()
            }
        }
    }

    fun markComplete() {
        timerJob?.cancel()
        viewModelScope.launch {
            val result = sessionRepository.completeSession(sessionId)
            val milestone = null
            _state.update { it.copy(
                phase = SessionPhase.COMPLETE,
                pointsEarned = result?.pointsEarned ?: 10,
                streakMilestone = milestone,
                showExtensionDialog = false
            ) }
        }
    }

    fun extend(extraMinutes: Int) {
        viewModelScope.launch {
            sessionRepository.extendSession(sessionId, extraMinutes)
            _state.update { it.copy(
                timerRemainingMs = it.timerRemainingMs + extraMinutes * 60 * 1000L,
                timerTotalMs = it.timerTotalMs + extraMinutes * 60 * 1000L,
                showExtensionDialog = false
            ) }
            startTimer()
        }
    }

    fun pushToNextSlot() {
        timerJob?.cancel()
        viewModelScope.launch {
            sessionRepository.pushToNextSlot(sessionId)
            _state.update { it.copy(phase = SessionPhase.COMPLETE, showExtensionDialog = false) }
        }
    }

    fun dismissExtensionDialog() = _state.update { it.copy(showExtensionDialog = false) }

    fun dismissMilestone() = _state.update { it.copy(streakMilestone = null) }

    private fun startBreak() {
        val breakMs = _state.value.breakMinutes * 60 * 1000L
        _state.update { it.copy(
            isBreak = true,
            phase = SessionPhase.BREAK,
            timerRemainingMs = breakMs,
            timerTotalMs = breakMs,
            pomodoroCount = it.pomodoroCount + 1,
            showExtensionDialog = false
        ) }
        startTimer()
    }

    private fun resumeStudy() {
        val studyMs = _state.value.studyMinutes * 60 * 1000L
        _state.update { it.copy(
            isBreak = false,
            phase = SessionPhase.STUDY,
            timerRemainingMs = studyMs,
            timerTotalMs = studyMs
        ) }
        startTimer()
    }

    fun onUserTouch() = antiCheatManager.onUserTouch()
    fun onPromptAnswered(correct: Boolean) = antiCheatManager.onPromptAnswered(correct)
    fun onPromptMissed() = antiCheatManager.onPromptMissed()
    fun checkPrompt() = antiCheatManager.checkShouldShowPrompt()

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
