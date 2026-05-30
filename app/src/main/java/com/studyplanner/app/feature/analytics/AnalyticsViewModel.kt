package com.studyplanner.app.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.SessionEntity
import com.studyplanner.app.core.data.local.entity.SubjectEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class WeekBarData(val day: String, val minutes: Int)
data class SubjectPieData(val name: String, val colorHex: String, val minutes: Int, val percent: Float)
data class HeatmapCell(val date: Long, val minutes: Int, val intensity: Float)

data class AnalyticsUiState(
    val weeklyBars: List<WeekBarData> = emptyList(),
    val subjectPieData: List<SubjectPieData> = emptyList(),
    val heatmapCells: List<HeatmapCell> = emptyList(),
    val bestStudyHour: Int = -1,
    val avgSessionMinutes: Int = 0,
    val efficiencyScore: Int = 0,
    val monthlyConsistencyPercent: Int = 0,
    val predictedCompletionDate: Long = 0L,
    val totalStudyHours: Float = 0f,
    val isLoading: Boolean = true,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val sessionDao: SessionDao,
    private val subjectDao: SubjectDao,
    private val userDao: UserDao,
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsUiState())
    val state = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val now = System.currentTimeMillis()

            val allSessions = mutableListOf<SessionEntity>()
            sessionDao.observeByRange(uid, 0L, now).first().let { allSessions.addAll(it) }

            val completed = allSessions.filter { it.status == "COMPLETED" }
            val subjects = subjectDao.getAll(uid)

            _state.value = AnalyticsUiState(
                weeklyBars = buildWeeklyBars(completed),
                subjectPieData = buildSubjectPie(completed, subjects),
                heatmapCells = buildHeatmap(completed),
                bestStudyHour = findBestHour(completed),
                avgSessionMinutes = if (completed.isEmpty()) 0
                else completed.sumOf { it.studyMinutes } / completed.size,
                efficiencyScore = calcEfficiency(allSessions),
                monthlyConsistencyPercent = calcMonthlyConsistency(allSessions),
                predictedCompletionDate = predictCompletion(subjects, completed),
                totalStudyHours = completed.sumOf { it.studyMinutes } / 60f,
                isLoading = false,
            )
        }
    }

    private fun buildWeeklyBars(sessions: List<SessionEntity>): List<WeekBarData> {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_WEEK)
        val weekStart = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val weekSessions = sessions.filter { it.scheduledDate >= weekStart }

        return days.mapIndexed { i, day ->
            val dayOfWeek = (i + 2) % 7 + 1
            val mins = weekSessions
                .filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.scheduledDate }
                    c.get(Calendar.DAY_OF_WEEK) == dayOfWeek
                }
                .sumOf { it.studyMinutes }
            WeekBarData(day, mins)
        }
    }

    private fun buildSubjectPie(
        sessions: List<SessionEntity>,
        subjects: List<SubjectEntity>
    ): List<SubjectPieData> {
        val totalMins = sessions.sumOf { it.studyMinutes }.takeIf { it > 0 } ?: return emptyList()
        return subjects.mapNotNull { subject ->
            val mins = sessions.filter { it.subjectId == subject.id }.sumOf { it.studyMinutes }
            if (mins == 0) null
            else SubjectPieData(
                name = subject.name,
                colorHex = subject.colorHex,
                minutes = mins,
                percent = mins.toFloat() / totalMins
            )
        }.sortedByDescending { it.minutes }
    }

    private fun buildHeatmap(sessions: List<SessionEntity>): List<HeatmapCell> {
        val grouped = sessions.groupBy { it.scheduledDate }
        val maxMins = grouped.values.maxOfOrNull { list -> list.sumOf { it.studyMinutes } } ?: 1
        return grouped.map { (date, list) ->
            val mins = list.sumOf { it.studyMinutes }
            HeatmapCell(date = date, minutes = mins, intensity = mins.toFloat() / maxMins)
        }.sortedBy { it.date }
    }

    private fun findBestHour(sessions: List<SessionEntity>): Int {
        if (sessions.isEmpty()) return -1
        return sessions
            .groupBy {
                Calendar.getInstance().apply { timeInMillis = it.actualStartTime }
                    .get(Calendar.HOUR_OF_DAY)
            }
            .maxByOrNull { (_, list) -> list.size }?.key ?: -1
    }

    private fun calcEfficiency(sessions: List<SessionEntity>): Int {
        if (sessions.isEmpty()) return 0
        val completed = sessions.count { it.status == "COMPLETED" }
        return ((completed.toFloat() / sessions.size) * 100).toInt()
    }

    private fun calcMonthlyConsistency(sessions: List<SessionEntity>): Int {
        val monthAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val monthSessions = sessions.filter { it.scheduledDate >= monthAgo }
        if (monthSessions.isEmpty()) return 0
        val daysWithStudy = monthSessions.map { it.scheduledDate }.toSet().size
        return ((daysWithStudy.toFloat() / 30) * 100).toInt()
    }

    private fun predictCompletion(
        subjects: List<SubjectEntity>,
        completed: List<SessionEntity>
    ): Long {
        val totalMin = subjects.sumOf { it.estimatedTotalMinutes }
        val doneMin = subjects.sumOf { it.completedMinutes }
        val remaining = totalMin - doneMin
        if (remaining <= 0) return System.currentTimeMillis()
        val last7Days = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val recentMins = completed.filter { it.scheduledDate >= last7Days }.sumOf { it.studyMinutes }
        val dailyAvg = recentMins / 7
        if (dailyAvg == 0) return 0L
        val daysNeeded = remaining / dailyAvg
        return System.currentTimeMillis() + daysNeeded * 24 * 60 * 60 * 1000L
    }
}
