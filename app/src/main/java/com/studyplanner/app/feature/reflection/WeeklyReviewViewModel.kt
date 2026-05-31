package com.studyplanner.app.feature.reflection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyplanner.app.core.data.local.dao.ReflectionDao
import com.studyplanner.app.core.data.local.dao.SessionDao
import com.studyplanner.app.core.data.local.entity.ReflectionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class WeeklyReviewViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val sessionDao: SessionDao,
    private val reflectionDao: ReflectionDao,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    data class DayStat(val label: String, val completed: Int, val total: Int, val minutes: Int)

    data class UiState(
        val isLoading: Boolean = true,
        val days: List<DayStat> = emptyList(),
        val totalHours: Float = 0f,
        val completionPercent: Int = 0,
        val bestDay: String = "-",
        val missed: Int = 0,
        val wentWell: String = "",
        val toImprove: String = "",
        val nextWeekGoal: String = "",
        val saved: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val weekKey = run {
        val c = Calendar.getInstance()
        "%d-W%02d".format(c.get(Calendar.YEAR), c.get(Calendar.WEEK_OF_YEAR))
    }

    init { load() }

    private fun startOfDay(c: Calendar): Calendar = (c.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    private fun load() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val today = startOfDay(Calendar.getInstance())
            val from = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
            val to = (today.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            }
            val sessions = sessionDao.observeByRange(uid, from.timeInMillis, to.timeInMillis).first()
            val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

            val days = (0..6).map { offset ->
                val day = (from.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
                val daySessions = sessions.filter {
                    val sc = Calendar.getInstance().apply { timeInMillis = it.scheduledDate }
                    sc.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR) &&
                            sc.get(Calendar.YEAR) == day.get(Calendar.YEAR)
                }
                DayStat(
                    label = labels[(day.get(Calendar.DAY_OF_WEEK) + 5) % 7],
                    completed = daySessions.count { it.status == "COMPLETED" },
                    total = daySessions.size,
                    minutes = daySessions.sumOf { it.studyMinutes }
                )
            }

            val totalMin = days.sumOf { it.minutes }
            val totalCompleted = days.sumOf { it.completed }
            val totalSessions = days.sumOf { it.total }
            val missed = sessions.count { it.status == "MISSED" }
            val best = days.maxByOrNull { it.minutes }?.label ?: "-"

            val existing = reflectionDao.getByDateKey(uid, weekKey, "WEEKLY")

            _state.update {
                it.copy(
                    isLoading = false,
                    days = days,
                    totalHours = totalMin / 60f,
                    completionPercent = if (totalSessions == 0) 0
                    else (totalCompleted * 100 / totalSessions),
                    bestDay = best,
                    missed = missed,
                    wentWell = existing?.wentWell ?: "",
                    toImprove = existing?.toImprove ?: "",
                    nextWeekGoal = existing?.tomorrowIntention ?: "",
                )
            }
        }
    }

    fun setWentWell(v: String) = _state.update { it.copy(wentWell = v) }
    fun setToImprove(v: String) = _state.update { it.copy(toImprove = v) }
    fun setGoal(v: String) = _state.update { it.copy(nextWeekGoal = v) }

    fun save() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val s = _state.value
            val entity = ReflectionEntity(
                userUid = uid, type = "WEEKLY", dateKey = weekKey, mood = 0,
                wentWell = s.wentWell, toImprove = s.toImprove,
                tomorrowIntention = s.nextWeekGoal,
                studyMinutes = (s.totalHours * 60).toInt(),
                completionPercent = s.completionPercent,
                createdAt = System.currentTimeMillis()
            )
            reflectionDao.upsert(entity)
            runCatching {
                firestore.collection("users").document(uid)
                    .collection("reflections").document(weekKey)
                    .set(
                        mapOf(
                            "type" to "WEEKLY", "wentWell" to s.wentWell,
                            "toImprove" to s.toImprove, "goal" to s.nextWeekGoal,
                            "completion" to s.completionPercent,
                            "createdAt" to entity.createdAt
                        )
                    ).await()
            }
            _state.update { it.copy(saved = true) }
        }
    }
}
