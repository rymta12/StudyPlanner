package com.studyplanner.app.core.util

import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.*
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReschedulingEngine @Inject constructor(
    private val sessionDao: SessionDao,
    private val studySlotDao: StudySlotDao,
    private val personalRoutineDao: PersonalRoutineDao,
    private val userDao: UserDao,
) {
    companion object {
        const val MAX_DEADLINE_EXTENSIONS = 1
        const val RESCHEDULE_WINDOW_DAYS = 7
    }

    suspend fun reschedule(uid: String, missedSession: SessionEntity): RescheduleResult {
        val routines = personalRoutineDao.getAll(uid)
        val slots = studySlotDao.getAll(uid)
        val user = userDao.get(uid) ?: return RescheduleResult.Failed("User not found")

        val now = System.currentTimeMillis()
        val windowEnd = now + RESCHEDULE_WINDOW_DAYS.toLong() * 24 * 60 * 60 * 1000

        var currentDate = startOfDay(now)
        while (currentDate <= windowEnd) {
            val cal = Calendar.getInstance().apply { timeInMillis = currentDate }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            val daySlots = slots
                .filter { it.dayOfWeek == dayOfWeek }
                .sortedWith(compareBy({ it.startHour }, { it.startMinute }))

            for (slot in daySlots) {
                if (isBlockedByRoutine(routines, dayOfWeek, slot.startHour, slot.startMinute)) continue

                val slotStartMs = timeToMs(currentDate, slot.startHour, slot.startMinute)
                val slotEndMs = timeToMs(currentDate, slot.endHour, slot.endMinute)
                if (slotStartMs < now) continue

                val existingSessions = sessionDao.observeByDate(uid, startOfDay(currentDate))
                val isSlotFree = true

                if (isSlotFree) {
                    val duration = (missedSession.studyMinutes + missedSession.breakMinutes).toLong() * 60 * 1000
                    if (slotStartMs + duration <= slotEndMs) {
                        val rescheduled = missedSession.copy(
                            id = 0,
                            scheduledDate = startOfDay(currentDate),
                            scheduledStartTime = slotStartMs,
                            scheduledEndTime = slotStartMs + duration,
                            status = "UPCOMING",
                            isRescheduled = true,
                            originalDate = missedSession.scheduledDate,
                            createdAt = System.currentTimeMillis()
                        )
                        sessionDao.upsert(rescheduled)
                        sessionDao.updateStatus(missedSession.id, "MISSED_RESCHEDULED")
                        return RescheduleResult.Success(rescheduled)
                    }
                }
            }
            currentDate += 24 * 60 * 60 * 1000L
        }

        val deadlineExtensions = getDeadlineExtensionCount(uid)
        return if (deadlineExtensions < MAX_DEADLINE_EXTENSIONS) {
            RescheduleResult.NeedDeadlineExtension(deadlineExtensions + 1)
        } else {
            RescheduleResult.Failed("No available slot and deadline extension limit reached")
        }
    }

    suspend fun checkAndMarkMissed(uid: String) {
        val now = System.currentTimeMillis()
        val gracePeriodMs = 3 * 60 * 1000L
        val today = startOfDay(now)
        val sessions = mutableListOf<SessionEntity>()

        sessionDao.observeByDate(uid, today).collect { list ->
            sessions.addAll(list.filter {
                it.status == "UPCOMING" && now > it.scheduledStartTime + gracePeriodMs
            })
        }

        sessions.forEach { session ->
            sessionDao.updateStatus(session.id, "MISSED")
            reschedule(uid, session)
        }
    }

    private fun getDeadlineExtensionCount(uid: String): Int = 0

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

sealed class RescheduleResult {
    data class Success(val session: SessionEntity) : RescheduleResult()
    data class NeedDeadlineExtension(val extensionNumber: Int) : RescheduleResult()
    data class Failed(val reason: String) : RescheduleResult()
}
