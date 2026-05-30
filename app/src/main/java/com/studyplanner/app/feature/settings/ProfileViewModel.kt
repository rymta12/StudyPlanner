package com.studyplanner.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.UserEntity
import com.studyplanner.app.core.sync.FirestoreSyncService
import com.studyplanner.app.core.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "",
    val nickname: String = "",
    val email: String = "",
    val gender: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val examType: String = "",
    val examSubType: String = "",
    val targetDate: Long = 0L,
    val points: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val authenticityScore: Int = 100,
    val isPremium: Boolean = false,
    val isPro: Boolean = false,
    val parentCode: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    private val streakDao: StreakDao,
    private val firestoreSyncService: FirestoreSyncService,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid ?: ""

    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val currentUid = auth.currentUser?.uid ?: return@launch
            combine(
                userDao.observe(currentUid),
                streakDao.observe(currentUid),
            ) { user, streak ->
                if (user == null) return@combine ProfileUiState(isLoading = false)
                ProfileUiState(
                    name = user.name,
                    email = user.email,
                    gender = user.gender,
                    city = user.city,
                    state = user.state,
                    examType = user.examType,
                    examSubType = user.examSubType,
                    targetDate = user.targetDate,
                    points = user.points,
                    currentStreak = streak?.currentDailyStreak ?: user.currentStreak,
                    longestStreak = streak?.longestDailyStreak ?: user.longestStreak,
                    authenticityScore = user.authenticityScore,
                    isPremium = user.isPremium,
                    isPro = user.isPro,
                    parentCode = user.parentCode,
                    isLoading = false,
                )
            }.collect { _state.value = it }
        }
    }

    fun saveProfile(
        name: String, nickname: String, gender: String,
        city: String, userState: String, pincode: String, targetDate: Long
    ) {
        viewModelScope.launch {
            val current = userDao.get(uid) ?: return@launch
            val updated = current.copy(
                name = name, gender = gender, city = city,
                state = userState, targetDate = targetDate,
                updatedAt = System.currentTimeMillis()
            )
            userDao.upsert(updated)
            runCatching {
                auth.currentUser?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                )?.await()
                if (syncManager.shouldSyncNow()) firestoreSyncService.uploadAll()
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
