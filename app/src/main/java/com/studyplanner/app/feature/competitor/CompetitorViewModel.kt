package com.studyplanner.app.feature.competitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class Competitor(
    val uid: String = "",
    val name: String = "",
    val examTarget: String = "",
    val points: Int = 0,
    val streak: Int = 0,
    val todayMinutes: Int = 0,
    val weekMinutes: Int = 0,
    val rank: Int = 0,
    val isOnline: Boolean = false,
    val lastActive: Long = 0L,
)

data class CompetitorUiState(
    val competitors: List<Competitor> = emptyList(),
    val myPoints: Int = 0,
    val myStreak: Int = 0,
    val myTodayMinutes: Int = 0,
    val myRank: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchResults: List<Competitor> = emptyList(),
    val isSearching: Boolean = false,
)

@HiltViewModel
class CompetitorViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    private val _state = MutableStateFlow(CompetitorUiState())
    val state = _state.asStateFlow()

    init { loadData() }


    private fun loadData() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            runCatching {
                // 1. Load my stats
                val myDoc = firestore.collection("users").document(uid).get().await()
                val myPoints = (myDoc.getLong("points") ?: 0).toInt()
                val myStreak = (myDoc.getLong("currentStreak") ?: 0).toInt()
                val myToday = (myDoc.getLong("todayStudyMinutes") ?: 0).toInt()
                val myExamType = myDoc.getString("examType") ?: "" // User ka exam (e.g., UPSC)

                // 2. Load saved competitor UIDs
                val compSnap = firestore.collection("users").document(uid)
                    .collection("competitors").get().await()
                var compUids = compSnap.documents.map { it.id }

                // 🌟 LOGIC: Agar koi competitor nahi mila, toh AUTO-MATCH karo!
                if (compUids.isEmpty() && myExamType.isNotEmpty()) {
                    compUids = autoMatchCompetitors(uid, myExamType)
                }

                // 3. Fetch all competitors details (Aapka purana logic)
                val competitors = compUids.mapNotNull { cUid ->
                    runCatching {
                        val doc = firestore.collection("users").document(cUid).get().await()
                        Competitor(
                            uid = cUid,
                            name = doc.getString("name") ?: "Unknown",
                            examTarget = doc.getString("examTarget") ?: doc.getString("examType") ?: "",
                            points = (doc.getLong("points") ?: 0).toInt(),
                            streak = (doc.getLong("currentStreak") ?: 0).toInt(),
                            todayMinutes = (doc.getLong("todayStudyMinutes") ?: 0).toInt(),
                            weekMinutes = (doc.getLong("weekStudyMinutes") ?: 0).toInt(),
                            lastActive = doc.getLong("lastActive") ?: 0L,
                            isOnline = (System.currentTimeMillis() - (doc.getLong("lastActive") ?: 0L)) < 5 * 60 * 1000L,
                        )
                    }.getOrNull()
                }.sortedByDescending { it.points }

                val allPoints = (competitors.map { it.points } + myPoints).sortedDescending()
                val myRank = allPoints.indexOf(myPoints) + 1

                _state.update {
                    it.copy(
                        competitors = competitors,
                        myPoints = myPoints,
                        myStreak = myStreak,
                        myTodayMinutes = myToday,
                        myRank = myRank,
                        isLoading = false,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    // 🌟 NAYA FUNCTION: Jo background me automatic rivals dhoondhega aur save karega
    private suspend fun autoMatchCompetitors(myUid: String, examType: String): List<String> {
        return runCatching {
            // Same exam type ke top 10 users uthao jo khud aap nahi ho
            val batchSnap = firestore.collection("users")
                .whereEqualTo("examType", examType)
                .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()

            val autoSelectedUids = batchSnap.documents
                .map { it.id }
                .filter { it != myUid } // Apne aap ko remove kiya
                .take(5) // Top 5 rivals select kiye

            // Inhe user ke sub-collection me save kar do taaki agli baar automatic load hon
            val batch = firestore.batch()
            autoSelectedUids.forEach { competitorUid ->
                val ref = firestore.collection("users").document(myUid)
                    .collection("competitors").document(competitorUid)
                batch.set(ref, mapOf("addedAt" to System.currentTimeMillis()))
            }
            batch.commit().await()

            autoSelectedUids
        }.getOrElse { emptyList() }
    }

    fun searchUser(query: String) {
        if (query.length < 3) { _state.update { it.copy(searchResults = emptyList()) }; return }
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            runCatching {
                val myUid = auth.currentUser?.uid ?: return@launch
                val snap = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("name", query)
                    .whereLessThanOrEqualTo("name", query + "\uf8ff")
                    .limit(10).get().await()
                val results = snap.documents
                    .filter { it.id != myUid }
                    .map { doc ->
                        Competitor(
                            uid = doc.id,
                            name = doc.getString("name") ?: "Unknown",
                            examTarget = doc.getString("examTarget") ?: "",
                            points = (doc.getLong("points") ?: 0).toInt(),
                            streak = (doc.getLong("currentStreak") ?: 0).toInt(),
                        )
                    }
                _state.update { it.copy(searchResults = results, isSearching = false) }
            }.onFailure {
                _state.update { it.copy(isSearching = false) }
            }
        }
    }

    fun addCompetitor(competitor: Competitor) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            runCatching {
                firestore.collection("users").document(uid)
                    .collection("competitors").document(competitor.uid)
                    .set(mapOf("addedAt" to System.currentTimeMillis())).await()
                _state.update { s ->
                    s.copy(
                        competitors = (s.competitors + competitor).sortedByDescending { it.points },
                        searchResults = emptyList()
                    )
                }
            }
        }
    }

    fun removeCompetitor(competitorUid: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            runCatching {
                firestore.collection("users").document(uid)
                    .collection("competitors").document(competitorUid).delete().await()
                _state.update { s ->
                    s.copy(competitors = s.competitors.filter { it.uid != competitorUid })
                }
            }
        }
    }

    fun clearSearch() = _state.update { it.copy(searchResults = emptyList()) }

    fun refresh() {
        _state.update { it.copy(isLoading = true) }
        loadData()
    }
}