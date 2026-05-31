package com.studyplanner.app.feature.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyplanner.app.core.data.local.dao.ReflectionDao
import com.studyplanner.app.core.data.local.dao.SessionDao
import com.studyplanner.app.core.data.local.entity.ReflectionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class NightReflectionViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val sessionDao: SessionDao,
    private val reflectionDao: ReflectionDao,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val completed: Int = 0,
        val total: Int = 0,
        val studyMinutes: Int = 0,
        val mood: Int = 0,
        val wentWell: String = "",
        val toImprove: String = "",
        val tomorrowIntention: String = "",
        val saved: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val dateKey = run {
        val c = Calendar.getInstance()
        "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val end = (start.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            }
            val sessions = sessionDao
                .observeByRange(uid, start.timeInMillis, end.timeInMillis).first()
            val existing = reflectionDao.getByDateKey(uid, dateKey, "NIGHT")

            _state.update {
                it.copy(
                    isLoading = false,
                    completed = sessions.count { s -> s.status == "COMPLETED" },
                    total = sessions.size,
                    studyMinutes = sessions.sumOf { s -> s.studyMinutes },
                    mood = existing?.mood ?: 0,
                    wentWell = existing?.wentWell ?: "",
                    toImprove = existing?.toImprove ?: "",
                    tomorrowIntention = existing?.tomorrowIntention ?: "",
                )
            }
        }
    }

    fun setMood(m: Int) = _state.update { it.copy(mood = m) }
    fun setWentWell(v: String) = _state.update { it.copy(wentWell = v) }
    fun setToImprove(v: String) = _state.update { it.copy(toImprove = v) }
    fun setTomorrow(v: String) = _state.update { it.copy(tomorrowIntention = v) }

    fun save() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val s = _state.value
            val pct = if (s.total == 0) 0 else s.completed * 100 / s.total
            val entity = ReflectionEntity(
                userUid = uid, type = "NIGHT", dateKey = dateKey, mood = s.mood,
                wentWell = s.wentWell, toImprove = s.toImprove,
                tomorrowIntention = s.tomorrowIntention,
                studyMinutes = s.studyMinutes, completionPercent = pct,
                createdAt = System.currentTimeMillis()
            )
            reflectionDao.upsert(entity)
            runCatching {
                firestore.collection("users").document(uid)
                    .collection("reflections").document(dateKey)
                    .set(
                        mapOf(
                            "type" to "NIGHT", "mood" to s.mood,
                            "wentWell" to s.wentWell, "toImprove" to s.toImprove,
                            "tomorrow" to s.tomorrowIntention,
                            "completion" to pct, "createdAt" to entity.createdAt
                        )
                    ).await()
            }
            _state.update { it.copy(saved = true) }
        }
    }
}