package com.studyplanner.app.feature.competitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.studyplanner.app.core.data.local.dao.UserDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CompetitorViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
) : ViewModel() {

    data class Rival(val name: String, val points: Int, val rank: Int, val isYou: Boolean)

    data class UiState(
        val isLoading: Boolean = true,
        val myRank: Int = 0,
        val totalAspirants: Int = 0,
        val pointsToNext: Int = 0,
        val nextName: String = "",
        val nearby: List<Rival> = emptyList(),
        val examType: String = "",
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            _state.update { it.copy(isLoading = true) }
            runCatching {
                val user = userDao.get(uid)
                val examType = user?.examType ?: ""

                var q: Query = firestore.collection("leaderboard")
                    .orderBy("points", Query.Direction.DESCENDING)
                if (examType.isNotEmpty()) {
                    q = firestore.collection("leaderboard")
                        .whereEqualTo("examType", examType)
                        .orderBy("points", Query.Direction.DESCENDING)
                }
                val docs = q.limit(500).get().await().documents

                val ranked = docs.mapIndexed { idx, d ->
                    Rival(
                        name = d.getString("name") ?: "Aspirant",
                        points = (d.getLong("points") ?: 0).toInt(),
                        rank = idx + 1,
                        isYou = d.id == uid
                    )
                }
                val me = ranked.firstOrNull { it.isYou }
                val myRank = me?.rank ?: ranked.size + 1
                val nextAhead = ranked.firstOrNull { it.rank == myRank - 1 }

                // window: 2 upar + you + 2 niche
                val window = if (me != null) {
                    val i = ranked.indexOf(me)
                    ranked.subList((i - 2).coerceAtLeast(0), (i + 3).coerceAtMost(ranked.size))
                } else ranked.take(5)

                _state.update {
                    it.copy(
                        isLoading = false,
                        myRank = myRank,
                        totalAspirants = ranked.size,
                        pointsToNext = nextAhead?.let { n -> (n.points - (me?.points ?: 0)) } ?: 0,
                        nextName = nextAhead?.name ?: "",
                        nearby = window,
                        examType = examType
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}