package com.studyplanner.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val todaySessions: List<SessionEntity> = emptyList(),
    val upcomingSession: SessionEntity? = null,
    val currentStreak: Int = 0,
    val todayCompleted: Int = 0,
    val todayTotal: Int = 0,
    val overallProgress: Float = 0f,
    val studyDebt: Int = 0,
    val subjects: List<SubjectEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val streakDao: StreakDao,
    private val subjectDao: SubjectDao,
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid ?: ""

    // Session jo abhi start hone wala hai (alert dialog ke liye)
    private val _sessionAlert = MutableStateFlow<SessionEntity?>(null)
    val sessionAlert = _sessionAlert.asStateFlow()
    private val dismissedAlertIds = mutableSetOf<Long>()

    init {
        startSessionWatcher()
    }

    /** Har 15 sec check karta hai — agar koi session ka time aa gaya (±2 min window) to alert dikhao */
    private fun startSessionWatcher() {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val today = startOfDay()
                val sessions = runCatching {
                    sessionDao.observeByDate(currentUid, today).first()
                }.getOrDefault(emptyList())

                // Koi session ONGOING to nahi? to alert mat dikhao
                val hasOngoing = sessions.any { it.status == "ONGOING" }

                if (!hasOngoing) {
                    // 2 min pehle se start time tak window
                    val due = sessions.firstOrNull { s ->
                        s.status == "UPCOMING" &&
                                s.id !in dismissedAlertIds &&
                                now >= s.scheduledStartTime - 2 * 60 * 1000L &&
                                now <= s.scheduledStartTime + 3 * 60 * 1000L
                    }
                    _sessionAlert.value = due
                } else {
                    _sessionAlert.value = null
                }
                delay(15_000)
            }
        }
    }

    fun dismissSessionAlert() {
        _sessionAlert.value?.let { dismissedAlertIds.add(it.id) }
        _sessionAlert.value = null
    }

    fun clearAlertAfterStart() {
        _sessionAlert.value = null
    }

    val state: StateFlow<HomeUiState> = auth.currentUser?.uid?.let { currentUid ->
        combine(
            userDao.observe(currentUid),
            sessionDao.observeByDate(currentUid, startOfDay()),
            streakDao.observe(currentUid),
            subjectDao.observeAll(currentUid),
        ) { user, sessions, streak, subjects ->
            val now = System.currentTimeMillis()
            val upcoming = sessions
                .filter { it.status == "UPCOMING" && it.scheduledStartTime > now }
                .minByOrNull { it.scheduledStartTime }
            val completed = sessions.count { it.status == "COMPLETED" }
            val missed = sessions.count { it.status == "MISSED" }
            val totalProgress = if (subjects.isEmpty()) 0f else {
                val totalMin = subjects.sumOf { it.estimatedTotalMinutes }
                val completedMin = subjects.sumOf { it.completedMinutes }
                if (totalMin == 0) 0f else completedMin.toFloat() / totalMin
            }
            HomeUiState(
                userName = user?.name ?: auth.currentUser?.displayName ?: "",
                todaySessions = sessions,
                upcomingSession = upcoming,
                currentStreak = streak?.currentDailyStreak ?: 0,
                todayCompleted = completed,
                todayTotal = sessions.size,
                overallProgress = totalProgress,
                studyDebt = missed,
                subjects = subjects,
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
    } ?: MutableStateFlow(HomeUiState(isLoading = false))

    private fun startOfDay(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}