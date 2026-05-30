package com.studyplanner.app.feature.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyplanner.app.core.data.local.dao.SessionDao
import com.studyplanner.app.core.data.local.dao.SubjectDao
import com.studyplanner.app.core.data.local.dao.TopicDao
import com.studyplanner.app.core.data.local.entity.SessionEntity
import com.studyplanner.app.core.data.local.entity.SubjectEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class TimetableTab { TODAY, WEEK, MONTH }

data class SessionWithMeta(
    val session: SessionEntity,
    val subjectName: String,
    val subjectColor: String,
    val topicName: String,
)

data class DaySummary(
    val date: Long,
    val total: Int,
    val completed: Int,
    val missed: Int,
    val completionPercent: Float,
)

data class TimetableUiState(
    val tab: TimetableTab = TimetableTab.TODAY,
    val selectedDate: Long = startOfDay(System.currentTimeMillis()),
    val todaySessions: List<SessionWithMeta> = emptyList(),
    val weekSessions: Map<Long, List<SessionWithMeta>> = emptyMap(),
    val monthDays: List<DaySummary> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val sessionDao: SessionDao,
    private val subjectDao: SubjectDao,
    private val topicDao: TopicDao,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid ?: ""
    private val _tab = MutableStateFlow(TimetableTab.TODAY)
    private val _selectedDate = MutableStateFlow(startOfDay(System.currentTimeMillis()))
    private val _state = MutableStateFlow(TimetableUiState())
    val state = _state.asStateFlow()

    init {
        observeData()
    }

    fun setTab(tab: TimetableTab) {
        _tab.value = tab
        _state.update { it.copy(tab = tab) }
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.value = startOfDay(date)
        _state.update { it.copy(selectedDate = startOfDay(date)) }
        loadDaySessions(startOfDay(date))
    }

    private fun observeData() {
        viewModelScope.launch {
            val subjects = subjectDao.getAll(uid)
            _state.update { it.copy(subjects = subjects) }
            loadDaySessions(_selectedDate.value)
            loadWeekSessions()
            loadMonthSummary()
        }
    }

    private fun loadDaySessions(date: Long) {
        viewModelScope.launch {
            sessionDao.observeByDate(uid, date).collect { sessions ->
                val withMeta = sessions.map { it.toMeta() }
                _state.update { it.copy(todaySessions = withMeta, isLoading = false) }
            }
        }
    }

    private fun loadWeekSessions() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.timeInMillis = _selectedDate.value
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            val weekStart = startOfDay(cal.timeInMillis)
            val weekEnd = weekStart + 7 * 24 * 60 * 60 * 1000L

            sessionDao.observeByRange(uid, weekStart, weekEnd).collect { sessions ->
                val grouped = sessions
                    .groupBy { startOfDay(it.scheduledDate) }
                    .mapValues { (_, list) -> list.map { it.toMeta() } }
                _state.update { it.copy(weekSessions = grouped) }
            }
        }
    }

    private fun loadMonthSummary() {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply { timeInMillis = _selectedDate.value }
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = startOfDay(cal.timeInMillis)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val monthEnd = monthStart + daysInMonth * 24 * 60 * 60 * 1000L

            sessionDao.observeByRange(uid, monthStart, monthEnd).collect { sessions ->
                val grouped = sessions.groupBy { startOfDay(it.scheduledDate) }
                val days = (0 until daysInMonth).map { i ->
                    val date = monthStart + i * 24 * 60 * 60 * 1000L
                    val daySessions = grouped[date] ?: emptyList()
                    val total = daySessions.size
                    val completed = daySessions.count { it.status == "COMPLETED" }
                    val missed = daySessions.count { it.status == "MISSED" }
                    DaySummary(
                        date = date,
                        total = total,
                        completed = completed,
                        missed = missed,
                        completionPercent = if (total == 0) 0f else completed.toFloat() / total
                    )
                }
                _state.update { it.copy(monthDays = days) }
            }
        }
    }

    fun previousPeriod() {
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedDate.value }
        when (_tab.value) {
            TimetableTab.TODAY -> cal.add(Calendar.DAY_OF_YEAR, -1)
            TimetableTab.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
            TimetableTab.MONTH -> cal.add(Calendar.MONTH, -1)
        }
        _selectedDate.value = startOfDay(cal.timeInMillis)
        _state.update { it.copy(selectedDate = _selectedDate.value) }
        refreshForTab()
    }

    fun nextPeriod() {
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedDate.value }
        when (_tab.value) {
            TimetableTab.TODAY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            TimetableTab.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            TimetableTab.MONTH -> cal.add(Calendar.MONTH, 1)
        }
        _selectedDate.value = startOfDay(cal.timeInMillis)
        _state.update { it.copy(selectedDate = _selectedDate.value) }
        refreshForTab()
    }

    private fun refreshForTab() {
        when (_tab.value) {
            TimetableTab.TODAY -> loadDaySessions(_selectedDate.value)
            TimetableTab.WEEK -> loadWeekSessions()
            TimetableTab.MONTH -> loadMonthSummary()
        }
    }

    private suspend fun SessionEntity.toMeta(): SessionWithMeta {
        val subject = subjectDao.getById(subjectId)
        val topic = if (topicId > 0) topicDao.getById(topicId) else null
        return SessionWithMeta(
            session = this,
            subjectName = subject?.name ?: "Study",
            subjectColor = subject?.colorHex ?: "#1565C0",
            topicName = topic?.name ?: subject?.name ?: "Session",
        )
    }
}

fun startOfDay(ms: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
