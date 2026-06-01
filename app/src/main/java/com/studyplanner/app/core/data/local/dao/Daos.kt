package com.studyplanner.app.core.data.local.dao

import androidx.room.*
import com.studyplanner.app.core.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Upsert suspend fun upsert(user: UserEntity)
    @Query("SELECT * FROM users WHERE uid = :uid") fun observe(uid: String): Flow<UserEntity?>
    @Query("SELECT * FROM users WHERE uid = :uid") suspend fun get(uid: String): UserEntity?
    @Query("DELETE FROM users WHERE uid = :uid") suspend fun delete(uid: String)
}

@Dao
interface SubjectDao {
    @Upsert suspend fun upsert(subject: SubjectEntity): Long
    @Upsert suspend fun upsertAll(subjects: List<SubjectEntity>)
    @Update suspend fun update(subject: SubjectEntity)
    @Delete suspend fun delete(subject: SubjectEntity)
    @Query("SELECT * FROM subjects WHERE userUid = :uid ORDER BY orderIndex")
    fun observeAll(uid: String): Flow<List<SubjectEntity>>
    @Query("SELECT * FROM subjects WHERE userUid = :uid ORDER BY orderIndex")
    suspend fun getAll(uid: String): List<SubjectEntity>
    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: Long): SubjectEntity?
}

@Dao
interface ChapterDao {
    @Upsert suspend fun upsert(chapter: ChapterEntity): Long
    @Upsert suspend fun upsertAll(chapters: List<ChapterEntity>)
    @Update suspend fun update(chapter: ChapterEntity)
    @Delete suspend fun delete(chapter: ChapterEntity)
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex")
    fun observeBySubject(subjectId: Long): Flow<List<ChapterEntity>>
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex")
    suspend fun getBySubject(subjectId: Long): List<ChapterEntity>
    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getById(id: Long): ChapterEntity?
    @Query("UPDATE chapters SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}

@Dao
interface TopicDao {
    @Upsert suspend fun upsert(topic: TopicEntity): Long
    @Upsert suspend fun upsertAll(topics: List<TopicEntity>)
    @Update suspend fun update(topic: TopicEntity)
    @Delete suspend fun delete(topic: TopicEntity)
    @Query("SELECT * FROM topics WHERE chapterId = :chapterId ORDER BY orderIndex")
    fun observeByChapter(chapterId: Long): Flow<List<TopicEntity>>
    @Query("SELECT * FROM topics WHERE chapterId = :chapterId ORDER BY orderIndex")
    suspend fun getByChapter(chapterId: Long): List<TopicEntity>
    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getById(id: Long): TopicEntity?
    @Query("SELECT * FROM topics WHERE nextRevisionAt <= :now AND status = 'COMPLETED'")
    suspend fun getDueRevisions(now: Long): List<TopicEntity>
    @Query("UPDATE topics SET status = :status, completedMinutes = :minutes, lastStudiedAt = :time WHERE id = :id")
    suspend fun updateCompletion(id: Long, status: String, minutes: Int, time: Long)
}

@Dao
interface SubtopicDao {
    @Upsert suspend fun upsert(subtopic: SubtopicEntity): Long
    @Upsert suspend fun upsertAll(subtopics: List<SubtopicEntity>)
    @Update suspend fun update(subtopic: SubtopicEntity)
    @Delete suspend fun delete(subtopic: SubtopicEntity)
    @Query("SELECT * FROM subtopics WHERE topicId = :topicId ORDER BY orderIndex")
    fun observeByTopic(topicId: Long): Flow<List<SubtopicEntity>>
    @Query("SELECT * FROM subtopics WHERE topicId = :topicId ORDER BY orderIndex")
    suspend fun getByTopic(topicId: Long): List<SubtopicEntity>
    @Query("UPDATE subtopics SET status = :status, completedAt = :time, completedMinutes = :minutes WHERE id = :id")
    suspend fun updateCompletion(id: Long, status: String, time: Long, minutes: Int)
}

@Dao
interface SessionDao {
    @Upsert suspend fun upsert(session: SessionEntity): Long
    @Update suspend fun update(session: SessionEntity)
    @Query("SELECT * FROM sessions WHERE userUid = :uid AND scheduledDate = :date ORDER BY scheduledStartTime")
    fun observeByDate(uid: String, date: Long): Flow<List<SessionEntity>>
    @Query("SELECT * FROM sessions WHERE userUid = :uid AND scheduledDate BETWEEN :from AND :to ORDER BY scheduledDate, scheduledStartTime")
    fun observeByRange(uid: String, from: Long, to: Long): Flow<List<SessionEntity>>
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): SessionEntity?
    @Query("SELECT * FROM sessions WHERE userUid = :uid AND status = 'MISSED' AND isRescheduled = 0")
    suspend fun getMissedUnrescheduled(uid: String): List<SessionEntity>
    @Query("UPDATE sessions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
    @Query("SELECT COUNT(*) FROM sessions WHERE userUid = :uid AND scheduledDate = :date AND status = 'COMPLETED'")
    suspend fun countCompletedOnDate(uid: String, date: Long): Int
    @Query("SELECT COUNT(*) FROM sessions WHERE userUid = :uid AND scheduledDate = :date")
    suspend fun countTotalOnDate(uid: String, date: Long): Int
    @Query("DELETE FROM sessions WHERE userUid = :uid AND status = 'UPCOMING'")
    suspend fun deleteUpcoming(uid: String)
    @Query("SELECT * FROM sessions WHERE userUid = :uid AND status = 'ONGOING' ORDER BY actualStartTime DESC LIMIT 1")
    suspend fun getOngoing(uid: String): SessionEntity?
}

@Dao
interface StudySlotDao {
    @Upsert suspend fun upsert(slot: StudySlotEntity): Long
    @Upsert suspend fun upsertAll(slots: List<StudySlotEntity>)
    @Update suspend fun update(slot: StudySlotEntity)
    @Delete suspend fun delete(slot: StudySlotEntity)
    @Query("SELECT * FROM study_slots WHERE userUid = :uid AND isActive = 1 ORDER BY dayOfWeek, startHour, startMinute")
    fun observeAll(uid: String): Flow<List<StudySlotEntity>>
    @Query("SELECT * FROM study_slots WHERE userUid = :uid AND dayOfWeek = :day AND isActive = 1 ORDER BY startHour, startMinute")
    suspend fun getByDay(uid: String, day: Int): List<StudySlotEntity>

    @Query("SELECT * FROM study_slots WHERE userUid = :uid AND isActive = 1 ORDER BY dayOfWeek, startHour, startMinute")
    suspend fun getAll(uid: String): List<StudySlotEntity>
}

@Dao
interface PersonalRoutineDao {
    @Upsert suspend fun upsert(routine: PersonalRoutineEntity): Long
    @Upsert suspend fun upsertAll(routines: List<PersonalRoutineEntity>)
    @Update suspend fun update(routine: PersonalRoutineEntity)
    @Delete suspend fun delete(routine: PersonalRoutineEntity)
    @Query("SELECT * FROM personal_routines WHERE userUid = :uid AND isActive = 1")
    fun observeAll(uid: String): Flow<List<PersonalRoutineEntity>>
    @Query("SELECT * FROM personal_routines WHERE userUid = :uid AND isActive = 1")
    suspend fun getAll(uid: String): List<PersonalRoutineEntity>
}

@Dao
interface BreakSettingsDao {
    @Upsert suspend fun upsert(settings: BreakSettingsEntity)
    @Query("SELECT * FROM break_settings WHERE userUid = :uid")
    fun observe(uid: String): Flow<BreakSettingsEntity?>
    @Query("SELECT * FROM break_settings WHERE userUid = :uid")
    suspend fun get(uid: String): BreakSettingsEntity?
}

@Dao
interface StreakDao {
    @Upsert suspend fun upsert(streak: StreakEntity)
    @Query("SELECT * FROM streaks WHERE userUid = :uid")
    fun observe(uid: String): Flow<StreakEntity?>
    @Query("SELECT * FROM streaks WHERE userUid = :uid")
    suspend fun get(uid: String): StreakEntity?
}

@Dao
interface MorningAlarmDao {
    @Upsert suspend fun upsert(alarm: MorningAlarmEntity): Long
    @Update suspend fun update(alarm: MorningAlarmEntity)
    @Delete suspend fun delete(alarm: MorningAlarmEntity)
    @Query("SELECT * FROM morning_alarms WHERE userUid = :uid")
    fun observeAll(uid: String): Flow<List<MorningAlarmEntity>>
    @Query("SELECT * FROM morning_alarms WHERE userUid = :uid AND isEnabled = 1")
    suspend fun getEnabled(uid: String): List<MorningAlarmEntity>
}

@Dao
interface VisionBoardDao {
    @Upsert suspend fun upsert(board: VisionBoardEntity)
    @Query("SELECT * FROM vision_board WHERE userUid = :uid")
    fun observe(uid: String): Flow<VisionBoardEntity?>
    @Query("SELECT * FROM vision_board WHERE userUid = :uid")
    suspend fun get(uid: String): VisionBoardEntity?
}

@Dao
interface PeriodSettingsDao {
    @Upsert suspend fun upsert(settings: PeriodSettingsEntity)
    @Query("SELECT * FROM period_settings WHERE userUid = :uid")
    fun observe(uid: String): Flow<PeriodSettingsEntity?>
}

@Dao
interface ReflectionDao {
    @Upsert suspend fun upsert(r: ReflectionEntity): Long
    @Query("SELECT * FROM reflections WHERE userUid = :uid AND type = :type ORDER BY createdAt DESC")
    fun observeByType(uid: String, type: String): Flow<List<ReflectionEntity>>
    @Query("SELECT * FROM reflections WHERE userUid = :uid AND dateKey = :dateKey AND type = :type LIMIT 1")
    suspend fun getByDateKey(uid: String, dateKey: String, type: String): ReflectionEntity?
}