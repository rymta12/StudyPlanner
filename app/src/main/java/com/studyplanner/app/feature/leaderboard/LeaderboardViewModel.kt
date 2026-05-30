package com.studyplanner.app.feature.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.studyplanner.app.core.data.local.dao.UserDao
import com.studyplanner.app.core.util.GamificationManager
import com.studyplanner.app.core.util.Badge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class LeaderboardScope { NATIONAL, STATE, CITY, EXAM }

data class LeaderboardEntry(
    val uid: String = "",
    val name: String = "",
    val points: Int = 0,
    val streak: Int = 0,
    val city: String = "",
    val state: String = "",
    val examType: String = "",
    val examSubType: String = "",
    val rank: Int = 0,
    val isCurrentUser: Boolean = false,
)

data class LeaderboardUiState(
    val scope: LeaderboardScope = LeaderboardScope.NATIONAL,
    val entries: List<LeaderboardEntry> = emptyList(),
    val myRank: Int = 0,
    val myEntry: LeaderboardEntry? = null,
    val badges: List<Badge> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    private val gamificationManager: GamificationManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LeaderboardUiState())
    val state = _state.asStateFlow()

    init {
        load(LeaderboardScope.NATIONAL)
        loadBadges()
    }

    fun setScope(scope: LeaderboardScope) {
        _state.update { it.copy(scope = scope, isLoading = true) }
        load(scope)
    }

    private fun load(scope: LeaderboardScope) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val user = userDao.get(uid)

            runCatching {
                var query = firestore.collection("leaderboard")
                    .orderBy("points", Query.Direction.DESCENDING)

                query = when (scope) {
                    LeaderboardScope.NATIONAL -> query.limit(100)
                    LeaderboardScope.STATE -> query
                        .whereEqualTo("state", user?.state ?: "").limit(50)
                    LeaderboardScope.CITY -> query
                        .whereEqualTo("city", user?.city ?: "").limit(50)
                    LeaderboardScope.EXAM -> query
                        .whereEqualTo("examType", user?.examType ?: "").limit(50)
                }

                val docs = query.get().await()
                val entries = docs.documents.mapIndexed { index, doc ->
                    LeaderboardEntry(
                        uid = doc.getString("uid") ?: "",
                        name = doc.getString("name") ?: "Anonymous",
                        points = (doc.getLong("points") ?: 0).toInt(),
                        streak = (doc.getLong("streak") ?: 0).toInt(),
                        city = doc.getString("city") ?: "",
                        state = doc.getString("state") ?: "",
                        examType = doc.getString("examType") ?: "",
                        examSubType = doc.getString("examSubType") ?: "",
                        rank = index + 1,
                        isCurrentUser = doc.getString("uid") == uid
                    )
                }

                val myRank = entries.indexOfFirst { it.isCurrentUser }.let {
                    if (it >= 0) it + 1 else entries.size + 1
                }

                _state.update {
                    it.copy(
                        entries = entries,
                        myRank = myRank,
                        myEntry = entries.find { e -> e.isCurrentUser },
                        isLoading = false
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun loadBadges() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val badges = gamificationManager.getUserBadges(uid)
            _state.update { it.copy(badges = badges) }
        }
    }
}
