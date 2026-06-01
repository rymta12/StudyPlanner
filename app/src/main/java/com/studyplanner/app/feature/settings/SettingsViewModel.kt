package com.studyplanner.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.*
import com.studyplanner.app.core.sync.FirestoreSyncService
import com.studyplanner.app.core.sync.SyncManager
import kotlinx.coroutines.tasks.await
import com.studyplanner.app.ui.theme.AppTheme
import com.studyplanner.app.ui.theme.AppThemes
import com.studyplanner.app.ui.theme.BgAnimationStyle
import com.studyplanner.app.ui.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val breakSettings: BreakSettingsEntity? = null,
    val morningAlarms: List<MorningAlarmEntity> = emptyList(),
    val studySlots: List<StudySlotEntity> = emptyList(),
    val personalRoutines: List<PersonalRoutineEntity> = emptyList(),
    val periodSettings: PeriodSettingsEntity? = null,
    val currentTheme: AppTheme = AppThemes.default,
    val currentBgStyle: BgAnimationStyle = BgAnimationStyle.GRADIENT_FLOW,
    val autoSyncEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val sessionReminderMinutes: Int = 10,
    val isLoading: Boolean = false,
    val syncSuccess: Boolean = false,
    val timetableRegenSuccess: Boolean = false,
    val onboardingReset: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val breakSettingsDao: BreakSettingsDao,
    private val morningAlarmDao: MorningAlarmDao,
    private val studySlotDao: StudySlotDao,
    private val personalRoutineDao: PersonalRoutineDao,
    private val periodSettingsDao: PeriodSettingsDao,
    private val themeManager: ThemeManager,
    private val syncManager: SyncManager,
    private val firestoreSyncService: FirestoreSyncService,
    private val timetableGenerator: com.studyplanner.app.core.util.TimetableGenerator,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val userDao: UserDao,
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid ?: ""
    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        observeAll()
    }

    private fun observeAll() {
        viewModelScope.launch {
            combine(
                breakSettingsDao.observe(uid),
                morningAlarmDao.observeAll(uid),
                studySlotDao.observeAll(uid),
                personalRoutineDao.observeAll(uid),
                themeManager.currentTheme,
            ) { break_, alarms, slots, routines, theme ->
                _state.update {
                    it.copy(
                        breakSettings = break_,
                        morningAlarms = alarms,
                        studySlots = slots,
                        personalRoutines = routines,
                        currentTheme = theme,
                    )
                }
            }.collect()
        }
        viewModelScope.launch {
            themeManager.currentBgStyle.collect { bg ->
                _state.update { it.copy(currentBgStyle = bg) }
            }
        }
        viewModelScope.launch {
            syncManager.isAutoSyncEnabled.collect { enabled ->
                _state.update { it.copy(autoSyncEnabled = enabled) }
            }
        }
    }

    fun setTheme(themeId: String) {
        viewModelScope.launch { themeManager.setTheme(themeId) }
    }

    fun setBgStyle(style: BgAnimationStyle) {
        viewModelScope.launch { themeManager.setBgStyle(style) }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch { syncManager.setAutoSync(enabled) }
    }

    fun manualSync() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, syncSuccess = false) }
            val result = firestoreSyncService.uploadAll()
            result.onSuccess {
                android.util.Log.d("ManualSync", "uploadAll SUCCESS")
            }.onFailure {
                android.util.Log.e("ManualSync", "uploadAll FAILED: ${it.message}", it)
            }
            _state.update { it.copy(isLoading = false, syncSuccess = result.isSuccess) }
        }
    }

    fun updateBreakSettings(studyMin: Int, breakMin: Int, longAfter: Int, longMin: Int) {
        viewModelScope.launch {
            breakSettingsDao.upsert(BreakSettingsEntity(
                userUid = uid, studyMinutes = studyMin, breakMinutes = breakMin,
                longBreakAfterSessions = longAfter, longBreakMinutes = longMin
            ))
        }
    }

    fun toggleAlarm(alarm: MorningAlarmEntity) {
        viewModelScope.launch {
            morningAlarmDao.update(alarm.copy(isEnabled = !alarm.isEnabled))
        }
    }

    fun deleteRoutine(routine: PersonalRoutineEntity) {
        viewModelScope.launch { personalRoutineDao.delete(routine) }
    }

    fun deleteSlot(slot: StudySlotEntity) {
        viewModelScope.launch { studySlotDao.delete(slot) }
    }

    fun clearSyncSuccess() = _state.update { it.copy(syncSuccess = false) }

    fun regenerateTimetable() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                val user = userDao.get(uid) ?: throw Exception("User not found")
                timetableGenerator.generate(uid, user.targetDate)
            }.onSuccess {
                _state.update { it.copy(isLoading = false, timetableRegenSuccess = true) }
            }.onFailure { e ->
                android.util.Log.e("Settings", "Timetable regen failed: ${e.message}")
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun redoOnboarding() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                firestore.collection("users").document(uid)
                    .update("isOnboardingComplete", false)
                    .await()
            }.onSuccess {
                _state.update { it.copy(isLoading = false, onboardingReset = true) }
            }.onFailure {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearTimetableSuccess() = _state.update { it.copy(timetableRegenSuccess = false) }
    fun clearOnboardingReset() = _state.update { it.copy(onboardingReset = false) }
}