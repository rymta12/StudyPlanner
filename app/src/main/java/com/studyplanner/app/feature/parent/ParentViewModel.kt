package com.studyplanner.app.feature.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ChildSummary(
    val uid: String,
    val name: String,
    val sessionStatus: String,
    val currentTopic: String,
    val todayCompleted: Int,
    val todayTotal: Int,
    val streak: Int,
    val points: Int,
    val missedCount: Int,
    val lastActive: Long,
)

data class ParentUiState(
    val children: List<ChildSummary> = emptyList(),
    val selectedChild: ChildSummary? = null,
    val isLoading: Boolean = true,
    val linkCode: String = "",
    val error: String? = null,
)

@HiltViewModel
class ParentViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    private val _state = MutableStateFlow(ParentUiState())
    val state = _state.asStateFlow()
    private var listeners = mutableListOf<ListenerRegistration>()

    init { loadChildren() }

    private fun loadChildren() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            runCatching {
                val snapshot = firestore.collection("parent_links")
                    .whereEqualTo("parentUid", uid).get().await()
                val childUids = snapshot.documents.map { it.getString("childUid") ?: "" }
                    .filter { it.isNotEmpty() }
                if (childUids.isEmpty()) {
                    _state.update { it.copy(isLoading = false) }
                    return@runCatching
                }
                childUids.forEach { childUid -> observeChild(childUid) }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun observeChild(childUid: String) {
        val listener = firestore.collection("users").document(childUid)
            .addSnapshotListener { doc, _ ->
                if (doc == null) return@addSnapshotListener
                val child = ChildSummary(
                    uid = childUid,
                    name = doc.getString("name") ?: "Child",
                    sessionStatus = doc.getString("currentSessionStatus") ?: "IDLE",
                    currentTopic = doc.getString("currentTopic") ?: "",
                    todayCompleted = (doc.getLong("todayCompleted") ?: 0).toInt(),
                    todayTotal = (doc.getLong("todayTotal") ?: 0).toInt(),
                    streak = (doc.getLong("currentStreak") ?: 0).toInt(),
                    points = (doc.getLong("points") ?: 0).toInt(),
                    missedCount = (doc.getLong("missedCount") ?: 0).toInt(),
                    lastActive = doc.getLong("lastActive") ?: 0L,
                )
                _state.update { s ->
                    val updated = s.children.toMutableList()
                    val idx = updated.indexOfFirst { it.uid == childUid }
                    if (idx >= 0) updated[idx] = child else updated.add(child)
                    s.copy(children = updated, isLoading = false)
                }
            }
        listeners.add(listener)
    }

    fun selectChild(child: ChildSummary?) = _state.update { it.copy(selectedChild = child) }

    fun linkChild(code: String) {
        viewModelScope.launch {
            val parentUid = auth.currentUser?.uid ?: return@launch
            runCatching {
                val snapshot = firestore.collection("users")
                    .whereEqualTo("parentCode", code).get().await()
                val childDoc = snapshot.documents.firstOrNull()
                    ?: throw Exception("Invalid code")
                val childUid = childDoc.id
                firestore.collection("parent_links").add(
                    mapOf("parentUid" to parentUid, "childUid" to childUid,
                        "linkedAt" to System.currentTimeMillis())
                ).await()
                observeChild(childUid)
            }.onFailure { e ->
                _state.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listeners.forEach { it.remove() }
    }
}
