package com.studyplanner.app.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyplanner.app.core.data.local.dao.SubjectDao
import com.studyplanner.app.core.data.local.dao.TopicDao
import com.studyplanner.app.core.data.local.dao.VisionBoardDao
import com.studyplanner.app.core.data.local.entity.SessionEntity
import com.studyplanner.app.core.data.repository.SessionRepository
import com.studyplanner.app.core.util.StreakMilestone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SessionPhase { PRE_CONFIRM, VISION_FLASH, STUDY, BREAK, COMPLETE }

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
    val showTimeUpDialog: Boolean = false,
    val pointsEarned: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val celebrationDismissed: Boolean = false,
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
    private val musicManager: com.studyplanner.app.core.util.StudyMusicManager,
    private val notificationHelper: com.studyplanner.app.core.util.NotificationHelper,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle["sessionId"] ?: 0L
    private val _state = MutableStateFlow(SessionUiState())
    val state = _state.asStateFlow()
    val antiCheatState = antiCheatManager.state
    val musicState = musicManager.state
    private var timerJob: Job? = null
    private var selectedTrackId = "lofi_1"

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
                    topicName = topic?.name ?: subject?.name ?: "Study Session",
                    subjectName = subject?.name ?: "",
                    subjectColor = subject?.colorHex ?: "#1565C0",
                    studyMinutes = session.studyMinutes,
                    breakMinutes = session.breakMinutes,
                    musicUrl = session.backgroundMusicUrl,
                    visionBoardDream = visionBoard?.dreamPost ?: "",
                    visionBoardPhotoUri = visionBoard?.photoUri ?: "",
                )
            }

            // ALREADY ONGOING? → resume from elapsed time (back press resilience)
            if (session.status == "ONGOING" && session.actualStartTime > 0) {
                resumeOngoing(session)
            } else if (session.status == "UPCOMING") {
                _state.update {
                    it.copy(
                        phase = SessionPhase.PRE_CONFIRM,
                        timerRemainingMs = session.studyMinutes * 60 * 1000L,
                        timerTotalMs = session.studyMinutes * 60 * 1000L,
                    )
                }
            }
        }
    }

    /** Back press ke baad wapas aane par — elapsed time se resume karo */
    private fun resumeOngoing(session: SessionEntity) {
        val now = System.currentTimeMillis()
        val studyMs = session.studyMinutes * 60 * 1000L
        val elapsed = now - session.actualStartTime
        val remaining = studyMs - elapsed

        antiCheatManager.onSessionStart()

        if (remaining <= 0) {
            // Time already up → show "topic complete?" dialog
            _state.update {
                it.copy(
                    phase = SessionPhase.STUDY,
                    timerRemainingMs = 0L,
                    timerTotalMs = studyMs,
                    isBreak = false,
                    showTimeUpDialog = true
                )
            }
        } else {
            _state.update {
                it.copy(
                    phase = SessionPhase.STUDY,
                    timerRemainingMs = remaining,
                    timerTotalMs = studyMs,
                    isBreak = false
                )
            }
            startTimer()
        }
    }

    fun updatePreConfirm(studyMin: Int, breakMin: Int, musicUrl: String, applyAll: Boolean) {
        _state.update {
            it.copy(studyMinutes = studyMin, breakMinutes = breakMin,
                musicUrl = musicUrl, applyToAllToday = applyAll,
                timerRemainingMs = studyMin * 60 * 1000L,
                timerTotalMs = studyMin * 60 * 1000L)
        }
    }

    fun updateEmotion(emotion: String) = _state.update { it.copy(emotion = emotion) }

    fun startSession() {
        viewModelScope.launch {
            val s = _state.value
            // GUARD: agar koi aur session ONGOING hai to use complete/miss karo
            sessionRepository.startSession(
                sessionId, s.emotion, s.studyMinutes, s.breakMinutes, s.musicUrl
            )
            _state.update { it.copy(phase = SessionPhase.VISION_FLASH) }
            delay(3000)
            antiCheatManager.onSessionStart()
            // Music start — selected track ya custom URL
            if (s.musicUrl.isNotBlank()) {
                musicManager.play("custom", s.musicUrl)
            } else {
                musicManager.play(selectedTrackId)
            }

            // FocusMonitorService start karo — lock screen notification + app blocking
            val serviceIntent = android.content.Intent(context, com.studyplanner.app.core.service.FocusMonitorService::class.java).apply {
                action = com.studyplanner.app.core.service.FocusMonitorService.ACTION_START
                putExtra(com.studyplanner.app.core.service.FocusMonitorService.EXTRA_SESSION_ID, sessionId)
                putExtra(com.studyplanner.app.core.service.FocusMonitorService.EXTRA_MODE, "YOUTUBE_WHITELIST")
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            _state.update {
                it.copy(
                    phase = SessionPhase.STUDY,
                    timerRemainingMs = s.studyMinutes * 60 * 1000L,
                    timerTotalMs = s.studyMinutes * 60 * 1000L,
                    isBreak = false
                )
            }
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.timerRemainingMs > 0) {
                delay(1000)
                _state.update { it.copy(timerRemainingMs = (it.timerRemainingMs - 1000).coerceAtLeast(0)) }

                val remaining = _state.value.timerRemainingMs
                val twoMinMs = 2 * 60 * 1000L
                if (remaining in 1..twoMinMs && remaining > twoMinMs - 1000 && !_state.value.isBreak) {
                    _state.update { it.copy(showExtensionDialog = true) }
                }
            }
            // Timer hit zero
            if (!_state.value.isBreak) {
                // Study time up → ASK user (auto-complete na karo)
                _state.update { it.copy(showTimeUpDialog = true, showExtensionDialog = false) }
            } else {
                resumeStudy()
            }
        }
    }

    fun markComplete() {
        timerJob?.cancel()
        musicManager.stop()
        context.startService(android.content.Intent(context, com.studyplanner.app.core.service.FocusMonitorService::class.java).apply {
            action = com.studyplanner.app.core.service.FocusMonitorService.ACTION_STOP
        })
        viewModelScope.launch {
            val result = sessionRepository.completeSession(sessionId)
            _state.update {
                it.copy(
                    phase = SessionPhase.COMPLETE,
                    pointsEarned = result?.pointsEarned ?: 10,
                    showExtensionDialog = false,
                    showTimeUpDialog = false,
                    celebrationDismissed = false
                )
            }
        }
    }

    fun extend(extraMinutes: Int) {
        viewModelScope.launch {
            sessionRepository.extendSession(sessionId, extraMinutes)
            _state.update {
                it.copy(
                    timerRemainingMs = it.timerRemainingMs + extraMinutes * 60 * 1000L,
                    timerTotalMs = it.timerTotalMs + extraMinutes * 60 * 1000L,
                    showExtensionDialog = false,
                    showTimeUpDialog = false
                )
            }
            startTimer()
        }
    }

    fun pushToNextSlot() {
        timerJob?.cancel()
        viewModelScope.launch {
            sessionRepository.pushToNextSlot(sessionId)
            _state.update { it.copy(phase = SessionPhase.COMPLETE, showExtensionDialog = false, showTimeUpDialog = false) }
        }
    }

    fun dismissExtensionDialog() = _state.update { it.copy(showExtensionDialog = false) }
    fun dismissTimeUpDialog() = _state.update { it.copy(showTimeUpDialog = false) }
    fun dismissCelebration() = _state.update { it.copy(celebrationDismissed = true) }
    fun dismissMilestone() = _state.update { it.copy(streakMilestone = null) }

    /** Break start — time up dialog se "Complete" dabaane ke baad agar aur sessions hain */
    fun startBreakManually() {
        _state.update { it.copy(showTimeUpDialog = false) }
        startBreak()
    }

    private fun startBreak() {
        musicManager.pause()
        val breakMs = _state.value.breakMinutes * 60 * 1000L
        // Break start notification
        notificationHelper.showBreakReminder(_state.value.breakMinutes)
        _state.update {
            it.copy(
                isBreak = true,
                phase = SessionPhase.BREAK,
                timerRemainingMs = breakMs,
                timerTotalMs = breakMs,
                pomodoroCount = it.pomodoroCount + 1,
                showExtensionDialog = false,
                showTimeUpDialog = false
            )
        }
        startTimer()
    }

    private fun resumeStudy() {
        musicManager.resume()  // break ke baad resume
        val studyMs = _state.value.studyMinutes * 60 * 1000L
        _state.update {
            it.copy(
                isBreak = false,
                phase = SessionPhase.STUDY,
                timerRemainingMs = studyMs,
                timerTotalMs = studyMs
            )
        }
        startTimer()
    }

    fun skipBreak() {
        timerJob?.cancel()
        markComplete()
    }

    fun onUserTouch() = antiCheatManager.onUserTouch()
    fun onPromptAnswered(correct: Boolean) = antiCheatManager.onPromptAnswered(correct)
    fun onPromptMissed() = antiCheatManager.onPromptMissed()
    fun checkPrompt() = antiCheatManager.checkShouldShowPrompt()

    fun selectTrack(trackId: String) { selectedTrackId = trackId }
    fun stopMusic() = musicManager.stop()

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        musicManager.stop()
    }
}