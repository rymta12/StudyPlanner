package com.studyplanner.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class OnboardingState(
    val currentStep: Int = 0,
    val totalSteps: Int = 14,
    val isLoading: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val nickname: String = "",
    val photoUri: String = "",
    val gender: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val examType: String = "",
    val examSubType: String = "",
    val studyMaterials: List<String> = emptyList(),
    val targetDate: Long = 0L,
    val dailyStudyHours: Float = 6f,
    val studySlots: List<StudySlotDraft> = emptyList(),
    val breakStudyMinutes: Int = 50,
    val breakMinutes: Int = 10,
    val longBreakAfterSessions: Int = 4,
    val longBreakMinutes: Int = 20,
    val personalRoutines: List<RoutineDraft> = emptyList(),
    val periodEnabled: Boolean = false,
    val periodDayOfMonth: Int = 1,
    val periodCycleDays: Int = 28,
    val periodHeavyDays: Int = 2,
    val periodHeavyDaySchedule: String = "REDUCED",
    val morningAlarmHour: Int = 6,
    val morningAlarmMinute: Int = 0,
    val morningAlarmMessage: String = "Uth ja! IAS banna hai! 🔥",
    val morningAlarmMusicUrl: String = "",
    val morningAlarmWakeType: String = "MATH",
    val morningAlarmDays: String = "1,2,3,4,5,6,7",
    val visionBoardPhotoUri: String = "",
    val visionBoardDreamPost: String = "",
    val visionBoardImageUri: String = "",
    val focusBlockingMode: String = "YOUTUBE_WHITELIST",
    val youtubeWhitelistChannels: List<String> = emptyList(),
    val currentAffairsFrequency: String = "DAILY",
    val currentAffairsDurationMin: Int = 30,
    val revisionStyle: String = "SPACED",
    val subjects: List<SubjectDraft> = emptyList(),
)

data class StudySlotDraft(
    val dayOfWeek: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val slotName: String = "",
    val preferredSubjectId: Long = 0L,
)

data class RoutineDraft(
    val name: String,
    val emoji: String,
    val daysOfWeek: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val isFlexible: Boolean,
)

data class SubjectDraft(
    val name: String,
    val colorHex: String,
    val priority: String,
    val totalPages: Int,
    val readingSpeedMinPerPage: Float,
    val totalVideoMinutes: Int,
    val chapters: List<ChapterDraft> = emptyList(),
)

data class ChapterDraft(
    val name: String,
    val pageStart: Int,
    val pageEnd: Int,
    val orderIndex: Int,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    private val studySlotDao: StudySlotDao,
    private val personalRoutineDao: PersonalRoutineDao,
    private val breakSettingsDao: BreakSettingsDao,
    private val morningAlarmDao: MorningAlarmDao,
    private val visionBoardDao: VisionBoardDao,
    private val periodSettingsDao: PeriodSettingsDao,
    private val streakDao: StreakDao,
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val syncManager: com.studyplanner.app.core.sync.SyncManager,
    private val firestoreSyncService: com.studyplanner.app.core.sync.FirestoreSyncService,
    private val timetableGenerator: com.studyplanner.app.core.util.TimetableGenerator,
    private val alarmScheduler: com.studyplanner.app.core.util.AlarmScheduler,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()

    fun nextStep() = _state.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(it.totalSteps - 1)) }
    fun prevStep() = _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    fun goToStep(step: Int) = _state.update { it.copy(currentStep = step) }

    init {
        prefillFromAuth()
    }

    private fun prefillFromAuth() {
        val authName = auth.currentUser?.displayName ?: ""
        if (authName.isNotBlank()) _state.update { it.copy(name = authName) }
    }

    fun updateProfile(name: String, nickname: String, gender: String, city: String, state: String, pincode: String) {
        _state.update { it.copy(name = name, nickname = nickname, gender = gender, city = city, state = state, pincode = pincode) }
    }

    fun updateExam(examType: String, examSubType: String) {
        _state.update { it.copy(examType = examType, examSubType = examSubType) }
    }

    fun updateStudyMaterials(materials: List<String>) {
        _state.update { it.copy(studyMaterials = materials) }
    }

    fun updateDeadline(targetDate: Long) {
        _state.update { it.copy(targetDate = targetDate) }
    }

    fun updateDailySchedule(hours: Float, slots: List<StudySlotDraft>) {
        _state.update { it.copy(dailyStudyHours = hours, studySlots = slots) }
    }

    fun updateBreakSettings(studyMin: Int, breakMin: Int, longBreakAfter: Int, longBreakMin: Int) {
        _state.update { it.copy(breakStudyMinutes = studyMin, breakMinutes = breakMin, longBreakAfterSessions = longBreakAfter, longBreakMinutes = longBreakMin) }
    }

    fun updatePersonalRoutines(routines: List<RoutineDraft>) {
        _state.update { it.copy(personalRoutines = routines) }
    }

    fun updatePeriodSettings(enabled: Boolean, day: Int, cycle: Int, heavyDays: Int, heavySchedule: String) {
        _state.update { it.copy(periodEnabled = enabled, periodDayOfMonth = day, periodCycleDays = cycle, periodHeavyDays = heavyDays, periodHeavyDaySchedule = heavySchedule) }
    }

    fun updateMorningAlarm(hour: Int, minute: Int, message: String, musicUrl: String, wakeType: String, days: String) {
        _state.update { it.copy(morningAlarmHour = hour, morningAlarmMinute = minute, morningAlarmMessage = message, morningAlarmMusicUrl = musicUrl, morningAlarmWakeType = wakeType, morningAlarmDays = days) }
    }

    fun updateVisionBoard(photoUri: String, dreamPost: String, imageUri: String) {
        _state.update { it.copy(visionBoardPhotoUri = photoUri, visionBoardDreamPost = dreamPost, visionBoardImageUri = imageUri) }
    }

    fun updateFocusSettings(blockingMode: String, channels: List<String>) {
        _state.update { it.copy(focusBlockingMode = blockingMode, youtubeWhitelistChannels = channels) }
    }

    fun updateCurrentAffairs(frequency: String, durationMin: Int) {
        _state.update { it.copy(currentAffairsFrequency = frequency, currentAffairsDurationMin = durationMin) }
    }

    fun updateRevision(style: String) {
        _state.update { it.copy(revisionStyle = style) }
    }

    fun addSubject(subject: SubjectDraft) {
        _state.update { it.copy(subjects = it.subjects + subject) }
    }

    fun removeSubject(index: Int) {
        _state.update { it.copy(subjects = it.subjects.toMutableList().also { list -> list.removeAt(index) }) }
    }

    fun updateSubject(index: Int, subject: SubjectDraft) {
        _state.update {
            it.copy(subjects = it.subjects.toMutableList().also { list -> list[index] = subject })
        }
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { saveAllData() }
                .onSuccess { onSuccess() }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.localizedMessage) } }
        }
    }

    private suspend fun saveAllData() {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val s = _state.value

        userDao.upsert(UserEntity(
            uid = uid, name = s.name, email = auth.currentUser?.email ?: "",
            photoUrl = s.photoUri, gender = s.gender, city = s.city, state = s.state,
            examType = s.examType, examSubType = s.examSubType, targetDate = s.targetDate,
            dailyTargetHours = s.dailyStudyHours, points = 0, currentStreak = 0,
            longestStreak = 0, lastStudyDate = 0L, authenticityScore = 100,
            isPremium = false, isPro = false, parentCode = generateParentCode(uid),
            linkedParentUid = "", createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ))

        studySlotDao.upsertAll(s.studySlots.mapIndexed { i, slot ->
            StudySlotEntity(userUid = uid, dayOfWeek = slot.dayOfWeek, startHour = slot.startHour,
                startMinute = slot.startMinute, endHour = slot.endHour, endMinute = slot.endMinute,
                slotName = slot.slotName, preferredSubjectId = slot.preferredSubjectId, isActive = true)
        })

        breakSettingsDao.upsert(BreakSettingsEntity(userUid = uid, studyMinutes = s.breakStudyMinutes,
            breakMinutes = s.breakMinutes, longBreakAfterSessions = s.longBreakAfterSessions,
            longBreakMinutes = s.longBreakMinutes))

        personalRoutineDao.upsertAll(s.personalRoutines.map { r ->
            PersonalRoutineEntity(userUid = uid, name = r.name, emoji = r.emoji,
                daysOfWeek = r.daysOfWeek, startHour = r.startHour, startMinute = r.startMinute,
                endHour = r.endHour, endMinute = r.endMinute, isFlexible = r.isFlexible, isActive = true)
        })

        if (s.periodEnabled) {
            periodSettingsDao.upsert(PeriodSettingsEntity(userUid = uid,
                expectedDayOfMonth = s.periodDayOfMonth, cycleDays = s.periodCycleDays,
                heavyPainDays = s.periodHeavyDays, heavyDaySchedule = s.periodHeavyDaySchedule,
                isEnabled = true))
        }

        morningAlarmDao.upsert(MorningAlarmEntity(userUid = uid, hour = s.morningAlarmHour,
            minute = s.morningAlarmMinute, customMessage = s.morningAlarmMessage,
            musicUrl = s.morningAlarmMusicUrl, wakeVerificationType = s.morningAlarmWakeType,
            daysOfWeek = s.morningAlarmDays, isEnabled = true))

        visionBoardDao.upsert(VisionBoardEntity(userUid = uid, photoUri = s.visionBoardPhotoUri,
            dreamPost = s.visionBoardDreamPost, inspirationalImageUri = s.visionBoardImageUri,
            updatedAt = System.currentTimeMillis()))

        streakDao.upsert(StreakEntity(userUid = uid, currentDailyStreak = 0, longestDailyStreak = 0,
            currentWeeklyStreak = 0, longestWeeklyStreak = 0, todaySessionsCompleted = 0,
            todaySessionsTotal = 0, lastUpdated = System.currentTimeMillis()))

        s.subjects.forEach { subjectDraft ->
            val subjectId = subjectDao.upsert(SubjectEntity(userUid = uid, name = subjectDraft.name,
                colorHex = subjectDraft.colorHex, priority = subjectDraft.priority,
                totalPages = subjectDraft.totalPages, readingSpeedMinPerPage = subjectDraft.readingSpeedMinPerPage,
                totalVideoMinutes = subjectDraft.totalVideoMinutes,
                estimatedTotalMinutes = (subjectDraft.totalPages * subjectDraft.readingSpeedMinPerPage).toInt() + subjectDraft.totalVideoMinutes,
                completedMinutes = 0, orderIndex = 0, createdAt = System.currentTimeMillis()))

            subjectDraft.chapters.forEachIndexed { idx, chapterDraft ->
                chapterDao.upsert(ChapterEntity(subjectId = subjectId, name = chapterDraft.name,
                    pageStart = chapterDraft.pageStart, pageEnd = chapterDraft.pageEnd,
                    totalPages = chapterDraft.pageEnd - chapterDraft.pageStart + 1,
                    estimatedMinutes = ((chapterDraft.pageEnd - chapterDraft.pageStart + 1) * subjectDraft.readingSpeedMinPerPage).toInt(),
                    completedMinutes = 0, status = "PENDING", orderIndex = idx))
            }
        }

        firestore.collection("users").document(uid)
            .set(mapOf("isOnboardingComplete" to true), com.google.firebase.firestore.SetOptions.merge()).await()

        timetableGenerator.generate(uid, _state.value.targetDate)

        morningAlarmDao.getEnabled(uid).forEach { alarm ->
            alarmScheduler.scheduleMorningAlarm(alarm, _state.value.name)
        }

        com.studyplanner.app.core.worker.SessionMonitorWorker.enqueue(context)

        if (syncManager.shouldSyncNow()) {
            firestoreSyncService.uploadAll()
        }
    }

    fun addOcrChapters(chapters: List<ChapterDraft>) {
        val currentSubjects = _state.value.subjects.toMutableList()
        val lastSubject = currentSubjects.lastOrNull() ?: return
        val updated = lastSubject.copy(chapters = lastSubject.chapters + chapters)
        currentSubjects[currentSubjects.lastIndex] = updated
        _state.update { it.copy(subjects = currentSubjects) }
    }
    private fun generateParentCode(uid: String): String =
        uid.take(4).uppercase() + "-" + (1000..9999).random().toString()
}
