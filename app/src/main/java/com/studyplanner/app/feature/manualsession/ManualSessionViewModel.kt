package com.studyplanner.app.feature.manualsession

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyplanner.app.core.data.local.dao.SessionDao
import com.studyplanner.app.core.data.local.dao.SubjectDao
import com.studyplanner.app.core.data.local.entity.SessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

enum class ManualScheduleType { DAILY, CUSTOM }

data class ManualSessionConfig(
    val title: String = "Extra Study",
    val startHour: Int = 20,
    val startMinute: Int = 0,
    val endHour: Int = 21,
    val endMinute: Int = 0,
    val scheduleType: ManualScheduleType = ManualScheduleType.DAILY,
    val customDays: Set<Int> = emptySet(),
    val notifyMinutesBefore: Int = 5,
)

data class ConflictInfo(
    val conflictingSession: SessionEntity,
    val subjectName: String,
    // Available free slots us din end mein (rat 12 se pahle)
    val suggestedSlots: List<Pair<Int, Int>>, // hour to hour
)

sealed class ManualSessionState {
    data object Idle : ManualSessionState()
    data class ConflictFound(val conflict: ConflictInfo) : ManualSessionState()
    data object Scheduled : ManualSessionState()
    data class Running(val remainingMs: Long, val totalMs: Long) : ManualSessionState()
    data object Done : ManualSessionState()
}

data class ManualUiState(
    val config: ManualSessionConfig = ManualSessionConfig(),
    val sessionState: ManualSessionState = ManualSessionState.Idle,
    val isChecking: Boolean = false,
)

@HiltViewModel
class ManualSessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val sessionDao: SessionDao,
    private val subjectDao: SubjectDao,
) : ViewModel() {

    private val _state = MutableStateFlow(ManualUiState())
    val state = _state.asStateFlow()
    private var timerJob: Job? = null
    private val notifId = 9001

    fun updateConfig(config: ManualSessionConfig) =
        _state.update { it.copy(config = config) }

    // User ne time set kiya — conflict check karo
    fun checkAndSchedule() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val config = _state.value.config
            _state.update { it.copy(isChecking = true) }

            val today = getTodayMidnight()
            val startMs = today + (config.startHour * 60 + config.startMinute) * 60 * 1000L
            val endMs = today + (config.endHour * 60 + config.endMinute) * 60 * 1000L

            val conflicts = sessionDao.getConflicting(uid, today, startMs, endMs)
            _state.update { it.copy(isChecking = false) }

            if (conflicts.isEmpty()) {
                // No conflict — directly schedule
                saveManualSession(config)
                _state.update { it.copy(sessionState = ManualSessionState.Scheduled) }
            } else {
                // Conflict mila — available slots dhundo
                val allSessions = sessionDao.getForDay(uid, today)
                val freeSlotsAfter = findFreeSlots(allSessions, config.endHour, config.endMinute)
                val subject = subjectDao.getById(conflicts.first().subjectId)
                _state.update {
                    it.copy(sessionState = ManualSessionState.ConflictFound(
                        ConflictInfo(
                            conflictingSession = conflicts.first(),
                            subjectName = subject?.name ?: "Study Session",
                            suggestedSlots = freeSlotsAfter
                        )
                    ))
                }
            }
        }
    }

    // User ne "Change manual session time" choose kiya — suggest kiya slot accept karo
    fun acceptSuggestedSlot(startHour: Int, endHour: Int) {
        val config = _state.value.config.copy(startHour = startHour, startMinute = 0, endHour = endHour, endMinute = 0)
        viewModelScope.launch {
            saveManualSession(config)
            _state.update { it.copy(config = config, sessionState = ManualSessionState.Scheduled) }
        }
    }

    // User ne "Study session change karo" choose kiya — existing session reschedule
    fun rescheduleConflict(newStartHour: Int, newEndHour: Int) {
        val conflictState = _state.value.sessionState as? ManualSessionState.ConflictFound ?: return
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val today = getTodayMidnight()
            val session = conflictState.conflict.conflictingSession
            val updatedSession = session.copy(
                scheduledStartTime = today + newStartHour * 3600 * 1000L,
                scheduledEndTime = today + newEndHour * 3600 * 1000L,
            )
            sessionDao.update(updatedSession)
            saveManualSession(_state.value.config)
            _state.update { it.copy(sessionState = ManualSessionState.Scheduled) }
        }
    }

    fun dismissConflict() = _state.update { it.copy(sessionState = ManualSessionState.Idle) }

    fun startNow() {
        val config = _state.value.config
        val durationMs = ((config.endHour * 60 + config.endMinute) - (config.startHour * 60 + config.startMinute)) * 60 * 1000L
        val safeMs = if (durationMs > 0) durationMs else 30 * 60 * 1000L

        _state.update { it.copy(sessionState = ManualSessionState.Running(safeMs, safeMs)) }
        showRunningNotification(config.title, safeMs)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = safeMs
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _state.update { it.copy(sessionState = ManualSessionState.Running(remaining, safeMs)) }
                updateNotificationProgress(config.title, remaining, safeMs)
            }
            onTimerDone(config.title)
        }
    }

    fun stop() {
        timerJob?.cancel()
        cancelNotification()
        _state.update { it.copy(sessionState = ManualSessionState.Idle) }
    }

    private suspend fun saveManualSession(config: ManualSessionConfig) {
        val uid = auth.currentUser?.uid ?: return
        val today = getTodayMidnight()
        val startMs = today + (config.startHour * 60 + config.startMinute) * 60 * 1000L
        val endMs = today + (config.endHour * 60 + config.endMinute) * 60 * 1000L
        val durationMin = ((endMs - startMs) / 60000).toInt().coerceAtLeast(5)

        sessionDao.upsert(SessionEntity(
            userUid = uid,
            subjectId = 0L,
            chapterId = 0L,
            topicId = 0L,
            subtopicId = 0L,
            scheduledDate = today,
            scheduledStartTime = startMs,
            scheduledEndTime = endMs,
            actualStartTime = 0L,
            actualEndTime = 0L,
            studyMinutes = durationMin,
            breakMinutes = 0,
            status = "UPCOMING",
            extensionCount = 0,
            extensionMinutes = 0,
            backgroundMusicUrl = "",
            emotionBefore = "",
            selfieUrl = "",
            appUsageLog = "",
            touchActivityScore = 0,
            promptsAnswered = 0,
            promptsTotal = 0,
            authenticityScore = 0,
            pointsEarned = 0,
            isRescheduled = false,
            originalDate = today,
            createdAt = System.currentTimeMillis(),
            isManual = true,
        ))
    }

    // Free slots dhundo — existing sessions ke baad, rat 12 se pahle
    private fun findFreeSlots(sessions: List<SessionEntity>, afterHour: Int, afterMin: Int): List<Pair<Int, Int>> {
        val slots = mutableListOf<Pair<Int, Int>>()
        var currentHour = afterHour
        var currentMin = afterMin

        // Sessions ko sort karo
        val sorted = sessions.sortedBy { it.scheduledStartTime }

        for (session in sorted) {
            val cal = Calendar.getInstance().apply { timeInMillis = session.scheduledStartTime }
            val sH = cal.get(Calendar.HOUR_OF_DAY)
            val eCal = Calendar.getInstance().apply { timeInMillis = session.scheduledEndTime }
            val eH = eCal.get(Calendar.HOUR_OF_DAY)

            if (sH > currentHour || (sH == currentHour && cal.get(Calendar.MINUTE) > currentMin)) {
                // Gap mila
                slots.add(Pair(currentHour, sH))
            }
            currentHour = eH
        }

        // Rat 12 baje tak
        if (currentHour < 24) slots.add(Pair(currentHour, 24))
        return slots.filter { it.second - it.first >= 1 } // Kam se kam 1 ghanta
    }

    private fun getTodayMidnight(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun showRunningNotification(title: String, totalMs: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, NotificationCompat.Builder(context, "study_sessions")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("📚 $title")
            .setContentText("${totalMs / 60000} min session chal raha hai")
            .setOngoing(true).setProgress(100, 0, false).build())
    }

    private fun updateNotificationProgress(title: String, remaining: Long, total: Long) {
        val progress = if (total > 0) ((1f - remaining.toFloat() / total) * 100).toInt() else 0
        val min = remaining / 60000
        val sec = (remaining % 60000) / 1000
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, NotificationCompat.Builder(context, "study_sessions")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("📚 $title")
            .setContentText("$min:${sec.toString().padStart(2, '0')} bacha hai")
            .setOngoing(true).setProgress(100, progress, false).build())
    }

    private fun onTimerDone(title: String) {
        _state.update { it.copy(sessionState = ManualSessionState.Done) }
        cancelNotification()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId + 1, NotificationCompat.Builder(context, "study_sessions")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("✅ $title complete!")
            .setAutoCancel(true).build())
    }

    private fun cancelNotification() {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}