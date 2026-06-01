package com.studyplanner.app.core.sync

import android.content.Context
import android.net.ConnectivityManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.*
import com.studyplanner.app.core.util.TimetableGenerator
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.getSystemService
import com.studyplanner.app.StudyPlannerApp
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class FirestoreSyncService @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val topicDao: TopicDao,
    private val subtopicDao: SubtopicDao,
    private val studySlotDao: StudySlotDao,
    private val personalRoutineDao: PersonalRoutineDao,
    private val breakSettingsDao: BreakSettingsDao,
    private val morningAlarmDao: MorningAlarmDao,
    private val visionBoardDao: VisionBoardDao,
    private val periodSettingsDao: PeriodSettingsDao,
    private val streakDao: StreakDao,
    private val timetableGenerator: TimetableGenerator,
    @ApplicationContext private val context: Context,
) {
    private val uid get() = auth.currentUser?.uid

    suspend fun uploadAll(): Result<Unit> = runCatching {
        val userId = uid ?: throw Exception("Not logged in")
        val batch = firestore.collection("users").document(userId).collection("backup")

        userDao.get(userId)?.let { batch.document("profile").set(it).await() }

        val subjects = subjectDao.getAll(userId)
        batch.document("subjects").set(mapOf("list" to subjects)).await()

        val allChapters = subjects.flatMap { chapterDao.getBySubject(it.id) }
        batch.document("chapters").set(mapOf("list" to allChapters)).await()

        val allTopics = allChapters.flatMap { topicDao.getByChapter(it.id) }
        batch.document("topics").set(mapOf("list" to allTopics)).await()

        batch.document("slots").set(mapOf("list" to studySlotDao.getAll(userId))).await()
        batch.document("routines").set(mapOf("list" to personalRoutineDao.getAll(userId))).await()
        breakSettingsDao.get(userId)?.let { batch.document("breakSettings").set(it).await() }
        morningAlarmDao.getEnabled(userId).let { batch.document("alarms").set(mapOf("list" to it)).await() }
        visionBoardDao.get(userId)?.let { batch.document("visionBoard").set(it).await() }
        streakDao.get(userId)?.let { batch.document("streak").set(it).await() }

        firestore.collection("users").document(userId)
            .update("lastSyncedAt", System.currentTimeMillis()).await()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun downloadAll(): Result<Unit> = runCatching {
        val userId = uid ?: throw Exception("Not logged in")
        val backup = firestore.collection("users").document(userId).collection("backup")

        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val isOnline = connectivityManager?.activeNetworkInfo?.isConnected == true
        if (!isOnline) throw Exception("No internet connection.")
        // PROFILE
        backup.document("profile").get().await().data?.let { map ->
            userDao.upsert(UserEntity(
                uid = map["uid"] as? String ?: userId,
                name = map["name"] as? String ?: "",
                email = map["email"] as? String ?: "",
                photoUrl = map["photoUrl"] as? String ?: "",
                gender = map["gender"] as? String ?: "",
                city = map["city"] as? String ?: "",
                state = map["state"] as? String ?: "",
                examType = map["examType"] as? String ?: "",
                examSubType = map["examSubType"] as? String ?: "",
                targetDate = (map["targetDate"] as? Long) ?: 0L,
                dailyTargetHours = (map["dailyTargetHours"] as? Double)?.toFloat() ?: 6f,
                points = (map["points"] as? Long)?.toInt() ?: 0,
                currentStreak = (map["currentStreak"] as? Long)?.toInt() ?: 0,
                longestStreak = (map["longestStreak"] as? Long)?.toInt() ?: 0,
                lastStudyDate = (map["lastStudyDate"] as? Long) ?: 0L,
                authenticityScore = (map["authenticityScore"] as? Long)?.toInt() ?: 100,
                isPremium = map["isPremium"] as? Boolean ?: false,
                isPro = map["isPro"] as? Boolean ?: false,
                parentCode = map["parentCode"] as? String ?: "",
                linkedParentUid = map["linkedParentUid"] as? String ?: "",
                createdAt = (map["createdAt"] as? Long) ?: 0L,
                updatedAt = (map["updatedAt"] as? Long) ?: 0L,
            ))
        }

        // SUBJECTS — old ids ko new ids se map karna padega (autoGenerate)
        val subjectIdMap = mutableMapOf<Long, Long>()
        backup.document("subjects").get().await().get("list")?.let { list ->
            (list as? List<Map<String, Any>>)?.forEach { map ->
                val oldId = (map["id"] as? Long) ?: 0L
                val newId = subjectDao.upsert(map.toSubject().copy(id = 0L))
                subjectIdMap[oldId] = newId
            }
        }

        // CHAPTERS — subjectId remap
        val chapterIdMap = mutableMapOf<Long, Long>()
        backup.document("chapters").get().await().get("list")?.let { list ->
            (list as? List<Map<String, Any>>)?.forEach { map ->
                val oldChapId = (map["id"] as? Long) ?: 0L
                val oldSubId = (map["subjectId"] as? Long) ?: 0L
                val newSubId = subjectIdMap[oldSubId] ?: return@forEach
                val newChapId = chapterDao.upsert(map.toChapter().copy(id = 0L, subjectId = newSubId))
                chapterIdMap[oldChapId] = newChapId
            }
        }

        // TOPICS — chapterId remap
        backup.document("topics").get().await().get("list")?.let { list ->
            (list as? List<Map<String, Any>>)?.forEach { map ->
                val oldChapId = (map["chapterId"] as? Long) ?: 0L
                val newChapId = chapterIdMap[oldChapId] ?: return@forEach
                topicDao.upsert(map.toTopic().copy(id = 0L, chapterId = newChapId))
            }
        }

        // SLOTS
        backup.document("slots").get().await().get("list")?.let { list ->
            (list as? List<Map<String, Any>>)?.forEach { map ->
                studySlotDao.upsert(map.toSlot().copy(id = 0L))
            }
        }

        // ROUTINES
        backup.document("routines").get().await().get("list")?.let { list ->
            (list as? List<Map<String, Any>>)?.forEach { map ->
                personalRoutineDao.upsert(map.toRoutine().copy(id = 0L))
            }
        }

        // BREAK SETTINGS
        backup.document("breakSettings").get().await().data?.let { map ->
            breakSettingsDao.upsert(BreakSettingsEntity(
                userUid = map["userUid"] as? String ?: userId,
                studyMinutes = (map["studyMinutes"] as? Long)?.toInt() ?: 50,
                breakMinutes = (map["breakMinutes"] as? Long)?.toInt() ?: 10,
                longBreakAfterSessions = (map["longBreakAfterSessions"] as? Long)?.toInt() ?: 4,
                longBreakMinutes = (map["longBreakMinutes"] as? Long)?.toInt() ?: 20,
            ))
        }

        // ALARMS
        backup.document("alarms").get().await().get("list")?.let { list ->
            (list as? List<Map<String, Any>>)?.forEach { map ->
                morningAlarmDao.upsert(map.toAlarm().copy(id = 0L))
            }
        }

        // VISION BOARD
        backup.document("visionBoard").get().await().data?.let { map ->
            visionBoardDao.upsert(VisionBoardEntity(
                userUid = map["userUid"] as? String ?: userId,
                photoUri = map["photoUri"] as? String ?: "",
                dreamPost = map["dreamPost"] as? String ?: "",
                inspirationalImageUri = map["inspirationalImageUri"] as? String ?: "",
                updatedAt = (map["updatedAt"] as? Long) ?: 0L,
            ))
        }

        // STREAK
        backup.document("streak").get().await().data?.let { map ->
            streakDao.upsert(StreakEntity(
                userUid = map["userUid"] as? String ?: userId,
                currentDailyStreak = (map["currentDailyStreak"] as? Long)?.toInt() ?: 0,
                longestDailyStreak = (map["longestDailyStreak"] as? Long)?.toInt() ?: 0,
                currentWeeklyStreak = (map["currentWeeklyStreak"] as? Long)?.toInt() ?: 0,
                longestWeeklyStreak = (map["longestWeeklyStreak"] as? Long)?.toInt() ?: 0,
                todaySessionsCompleted = (map["todaySessionsCompleted"] as? Long)?.toInt() ?: 0,
                todaySessionsTotal = (map["todaySessionsTotal"] as? Long)?.toInt() ?: 0,
                lastUpdated = (map["lastUpdated"] as? Long) ?: 0L,
            ))
        }

        // Timetable wapas generate karo (sessions local the, cloud mein nahi the)
        val user = userDao.get(userId)
        timetableGenerator.generate(userId, user?.targetDate ?: 0L)
    }

    private fun Map<String, Any>.toSubject() = SubjectEntity(
        id = (this["id"] as? Long) ?: 0L,
        userUid = this["userUid"] as? String ?: "",
        name = this["name"] as? String ?: "",
        colorHex = this["colorHex"] as? String ?: "#1565C0",
        priority = this["priority"] as? String ?: "MEDIUM",
        totalPages = (this["totalPages"] as? Long)?.toInt() ?: 0,
        readingSpeedMinPerPage = (this["readingSpeedMinPerPage"] as? Double)?.toFloat() ?: 2f,
        totalVideoMinutes = (this["totalVideoMinutes"] as? Long)?.toInt() ?: 0,
        estimatedTotalMinutes = (this["estimatedTotalMinutes"] as? Long)?.toInt() ?: 0,
        completedMinutes = (this["completedMinutes"] as? Long)?.toInt() ?: 0,
        orderIndex = (this["orderIndex"] as? Long)?.toInt() ?: 0,
        createdAt = (this["createdAt"] as? Long) ?: 0L,
    )

    private fun Map<String, Any>.toChapter() = ChapterEntity(
        id = (this["id"] as? Long) ?: 0L,
        subjectId = (this["subjectId"] as? Long) ?: 0L,
        name = this["name"] as? String ?: "",
        pageStart = (this["pageStart"] as? Long)?.toInt() ?: 0,
        pageEnd = (this["pageEnd"] as? Long)?.toInt() ?: 0,
        totalPages = (this["totalPages"] as? Long)?.toInt() ?: 0,
        estimatedMinutes = (this["estimatedMinutes"] as? Long)?.toInt() ?: 0,
        completedMinutes = (this["completedMinutes"] as? Long)?.toInt() ?: 0,
        status = this["status"] as? String ?: "PENDING",
        orderIndex = (this["orderIndex"] as? Long)?.toInt() ?: 0,
    )

    private fun Map<String, Any>.toTopic() = TopicEntity(
        id = (this["id"] as? Long) ?: 0L,
        chapterId = (this["chapterId"] as? Long) ?: 0L,
        name = this["name"] as? String ?: "",
        estimatedMinutes = (this["estimatedMinutes"] as? Long)?.toInt() ?: 0,
        completedMinutes = (this["completedMinutes"] as? Long)?.toInt() ?: 0,
        status = this["status"] as? String ?: "PENDING",
        difficultyLevel = this["difficultyLevel"] as? String ?: "MEDIUM",
        orderIndex = (this["orderIndex"] as? Long)?.toInt() ?: 0,
        lastStudiedAt = (this["lastStudiedAt"] as? Long) ?: 0L,
        nextRevisionAt = (this["nextRevisionAt"] as? Long) ?: 0L,
    )

    private fun Map<String, Any>.toSlot() = StudySlotEntity(
        id = (this["id"] as? Long) ?: 0L,
        userUid = this["userUid"] as? String ?: "",
        dayOfWeek = (this["dayOfWeek"] as? Long)?.toInt() ?: 2,
        startHour = (this["startHour"] as? Long)?.toInt() ?: 9,
        startMinute = (this["startMinute"] as? Long)?.toInt() ?: 0,
        endHour = (this["endHour"] as? Long)?.toInt() ?: 12,
        endMinute = (this["endMinute"] as? Long)?.toInt() ?: 0,
        slotName = this["slotName"] as? String ?: "",
        preferredSubjectId = (this["preferredSubjectId"] as? Long) ?: 0L,
        isActive = this["isActive"] as? Boolean ?: true,
    )

    private fun Map<String, Any>.toRoutine() = PersonalRoutineEntity(
        id = (this["id"] as? Long) ?: 0L,
        userUid = this["userUid"] as? String ?: "",
        name = this["name"] as? String ?: "",
        emoji = this["emoji"] as? String ?: "",
        daysOfWeek = this["daysOfWeek"] as? String ?: "",
        startHour = (this["startHour"] as? Long)?.toInt() ?: 0,
        startMinute = (this["startMinute"] as? Long)?.toInt() ?: 0,
        endHour = (this["endHour"] as? Long)?.toInt() ?: 0,
        endMinute = (this["endMinute"] as? Long)?.toInt() ?: 0,
        isFlexible = this["isFlexible"] as? Boolean ?: false,
        isActive = this["isActive"] as? Boolean ?: true,
    )

    private fun Map<String, Any>.toAlarm() = MorningAlarmEntity(
        id = (this["id"] as? Long) ?: 0L,
        userUid = this["userUid"] as? String ?: "",
        hour = (this["hour"] as? Long)?.toInt() ?: 6,
        minute = (this["minute"] as? Long)?.toInt() ?: 0,
        customMessage = this["customMessage"] as? String ?: "",
        musicUrl = this["musicUrl"] as? String ?: "",
        wakeVerificationType = this["wakeVerificationType"] as? String ?: "MATH",
        daysOfWeek = this["daysOfWeek"] as? String ?: "1,2,3,4,5,6,7",
        isEnabled = this["isEnabled"] as? Boolean ?: true,
    )
}