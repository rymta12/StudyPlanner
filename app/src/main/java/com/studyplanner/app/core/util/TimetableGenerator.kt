package com.studyplanner.app.core.util

import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.*
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimetableGenerator @Inject constructor(
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val topicDao: TopicDao,
    private val sessionDao: SessionDao,
    private val studySlotDao: StudySlotDao,
    private val personalRoutineDao: PersonalRoutineDao,
    private val breakSettingsDao: BreakSettingsDao,
) {
    suspend fun generate(uid: String, targetDate: Long) {
        val subjects = subjectDao.getAll(uid)
        val slots = studySlotDao.getAll(uid)
        val routines = personalRoutineDao.getAll(uid)
        val breakSettings = breakSettingsDao.get(uid) ?: defaultBreakSettings(uid)
        val block = breakSettings.studyMinutes.coerceAtLeast(10)

        val today = startOfDay(System.currentTimeMillis())
        val sessions = mutableListOf<SessionEntity>()

        subjects.forEach { subject ->
            val chapters = chapterDao.getBySubject(subject.id)

            if (chapters.isEmpty()) {
                // Subject me chapters nahi → poore subject ke estimated time ko blocks me todo
                val mins = subject.estimatedTotalMinutes.takeIf { it > 0 } ?: block
                repeat(blockCount(mins, block)) {
                    sessions.add(buildSession(uid, subject.id, 0L, 0L, breakSettings))
                }
            } else {
                chapters.forEach { chapter ->
                    val topics = topicDao.getByChapter(chapter.id).filter { it.status == "PENDING" }
                    if (topics.isNotEmpty()) {
                        topics.forEach { topic ->
                            val mins = topic.estimatedMinutes.takeIf { it > 0 } ?: block
                            repeat(blockCount(mins, block)) {
                                sessions.add(buildSession(uid, subject.id, chapter.id, topic.id, breakSettings))
                            }
                        }
                    } else {
                        // Chapter me topics nahi → chapter ke estimated time ko blocks me todo
                        val mins = chapter.estimatedMinutes.takeIf { it > 0 } ?: block
                        repeat(blockCount(mins, block)) {
                            sessions.add(buildSession(uid, subject.id, chapter.id, 0L, breakSettings))
                        }
                    }
                }
            }
        }

        // Purana auto-generated plan hata do, fir naya daalo (re-generate safe)
        sessionDao.deleteUpcoming(uid)

        val scheduled = distributeSessionsToSlots(
            sessions = sessions,
            slots = slots,
            routines = routines,
            startDate = today,
            endDate = if (targetDate > today) targetDate else today + 30L * 24 * 60 * 60 * 1000,
            uid = uid
        )

        scheduled.forEach { sessionDao.upsert(it) }
    }

    /** kitne study-blocks chahiye for `mins` minutes (har block = `block` min) */
    private fun blockCount(mins: Int, block: Int): Int =
        ((mins + block - 1) / block).coerceAtLeast(1)

    private fun distributeSessionsToSlots(
        sessions: List<SessionEntity>,
        slots: List<StudySlotEntity>,
        routines: List<PersonalRoutineEntity>,
        startDate: Long,
        endDate: Long,
        uid: String
    ): List<SessionEntity> {
        if (sessions.isEmpty() || slots.isEmpty()) return emptyList()

        val result = mutableListOf<SessionEntity>()
        val sessionQueue = ArrayDeque(sessions)
        var currentDate = startDate

        while (sessionQueue.isNotEmpty() && currentDate <= endDate) {
            val cal = Calendar.getInstance().apply { timeInMillis = currentDate }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            val daySlots = slots
                .filter { it.dayOfWeek == dayOfWeek }
                .sortedWith(compareBy({ it.startHour }, { it.startMinute }))

            daySlots.forEach { slot ->
                if (sessionQueue.isEmpty()) return@forEach
                if (isBlockedByRoutine(routines, dayOfWeek, slot.startHour, slot.startMinute)) return@forEach

                val slotStartMs = timeToMs(currentDate, slot.startHour, slot.startMinute)
                val slotEndMs = timeToMs(currentDate, slot.endHour, slot.endMinute)

                var cursor = slotStartMs
                while (cursor < slotEndMs && sessionQueue.isNotEmpty()) {
                    val session = sessionQueue.removeFirst()
                    val duration = (session.studyMinutes + session.breakMinutes).toLong() * 60 * 1000
                    if (cursor + duration <= slotEndMs) {
                        result.add(session.copy(
                            scheduledDate = startOfDay(currentDate),
                            scheduledStartTime = cursor,
                            scheduledEndTime = cursor + duration,
                            status = "UPCOMING"
                        ))
                        cursor += duration
                    } else {
                        sessionQueue.addFirst(session)
                        break
                    }
                }
            }

            currentDate += 24 * 60 * 60 * 1000L
        }

        return result
    }

    private fun isBlockedByRoutine(
        routines: List<PersonalRoutineEntity>,
        dayOfWeek: Int,
        startHour: Int,
        startMinute: Int
    ): Boolean = routines.any { r ->
        if (r.isFlexible) return@any false
        val days = r.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (dayOfWeek !in days) return@any false
        val slotMins = startHour * 60 + startMinute
        val routineStart = r.startHour * 60 + r.startMinute
        val routineEnd = r.endHour * 60 + r.endMinute
        slotMins in routineStart until routineEnd
    }

    private fun buildSession(
        uid: String,
        subjectId: Long,
        chapterId: Long,
        topicId: Long,
        breakSettings: BreakSettingsEntity
    ) = SessionEntity(
        userUid = uid,
        subjectId = subjectId,
        chapterId = chapterId,
        topicId = topicId,
        subtopicId = 0L,
        scheduledDate = 0L,
        scheduledStartTime = 0L,
        scheduledEndTime = 0L,
        actualStartTime = 0L,
        actualEndTime = 0L,
        studyMinutes = breakSettings.studyMinutes,
        breakMinutes = breakSettings.breakMinutes,
        status = "PENDING",
        extensionCount = 0,
        extensionMinutes = 0,
        backgroundMusicUrl = "",
        emotionBefore = "",
        selfieUrl = "",
        appUsageLog = "",
        touchActivityScore = 100,
        promptsAnswered = 0,
        promptsTotal = 0,
        authenticityScore = 100,
        pointsEarned = 0,
        isRescheduled = false,
        originalDate = 0L,
        createdAt = System.currentTimeMillis()
    )

    private fun defaultBreakSettings(uid: String) = BreakSettingsEntity(
        userUid = uid, studyMinutes = 50, breakMinutes = 10,
        longBreakAfterSessions = 4, longBreakMinutes = 20
    )

    private fun timeToMs(date: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun startOfDay(ms: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}