package com.studyplanner.app.core.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun downloadAll(): Result<Unit> = runCatching {
        val userId = uid ?: throw Exception("Not logged in")
        val backup = firestore.collection("users").document(userId).collection("backup")

        backup.document("profile").get().await().toObject(UserEntity::class.java)?.let {
            userDao.upsert(it)
        }

        backup.document("subjects").get().await()
            .get("list")?.let { list ->
                (list as? List<Map<String, Any>>)?.forEach { map ->
                    subjectDao.upsert(map.toSubject())
                }
            }

        backup.document("slots").get().await()
            .get("list")?.let { list ->
                (list as? List<Map<String, Any>>)?.forEach { map ->
                    studySlotDao.upsert(map.toSlot())
                }
            }
        // chapters, topics, routines, etc. similarly restored on full implementation
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
}
