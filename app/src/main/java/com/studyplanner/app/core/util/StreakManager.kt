package com.studyplanner.app.core.util

import com.studyplanner.app.core.data.local.dao.SessionDao
import com.studyplanner.app.core.data.local.dao.StreakDao
import com.studyplanner.app.core.data.local.dao.UserDao
import com.studyplanner.app.core.data.local.entity.StreakEntity
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class StreakMilestone(
    val type: StreakType,
    val count: Int,
    val emoji: String,
    val message: String,
)

enum class StreakType { SESSION, DAILY, WEEKLY, CHAPTER, SUBJECT }

@Singleton
class StreakManager @Inject constructor(
    private val streakDao: StreakDao,
    private val sessionDao: SessionDao,
    private val userDao: UserDao,
) {
    suspend fun onSessionComplete(uid: String): StreakMilestone? {
        val streak = streakDao.get(uid) ?: defaultStreak(uid)
        val today = startOfDay(System.currentTimeMillis())
        val completedToday = sessionDao.countCompletedOnDate(uid, today)
        val totalToday = sessionDao.countTotalOnDate(uid, today)

        val newSessionStreak = completedToday
        val milestone = sessionMilestone(newSessionStreak, totalToday)

        val updated = streak.copy(
            todaySessionsCompleted = completedToday,
            todaySessionsTotal = totalToday,
            lastUpdated = System.currentTimeMillis()
        )
        streakDao.upsert(updated)
        updateDailyStreak(uid, streak)
        return milestone
    }

    suspend fun onSessionMissed(uid: String) {
        val streak = streakDao.get(uid) ?: return
        streakDao.upsert(streak.copy(lastUpdated = System.currentTimeMillis()))
    }

    suspend fun onDayComplete(uid: String): StreakMilestone? {
        val streak = streakDao.get(uid) ?: defaultStreak(uid)
        val newDaily = streak.currentDailyStreak + 1
        val newLongest = maxOf(newDaily, streak.longestDailyStreak)
        streakDao.upsert(streak.copy(
            currentDailyStreak = newDaily,
            longestDailyStreak = newLongest,
            lastUpdated = System.currentTimeMillis()
        ))
        return dailyMilestone(newDaily)
    }

    private suspend fun updateDailyStreak(uid: String, streak: StreakEntity) {
        val today = startOfDay(System.currentTimeMillis())
        val completedToday = sessionDao.countCompletedOnDate(uid, today)
        val totalToday = sessionDao.countTotalOnDate(uid, today)
        if (completedToday >= totalToday && totalToday > 0) {
            onDayComplete(uid)
        }
    }

    private fun sessionMilestone(completed: Int, total: Int): StreakMilestone? = when {
        total > 0 && completed == total -> StreakMilestone(StreakType.SESSION, completed, "👑", "Perfect Day! Sab sessions complete! 🔥")
        completed == 1 -> StreakMilestone(StreakType.SESSION, 1, "🌱", "Shuruwat ho gayi! Pehla session complete!")
        completed == 3 -> StreakMilestone(StreakType.SESSION, 3, "⚡", "Momentum aa raha hai! 3 sessions done!")
        completed == 5 -> StreakMilestone(StreakType.SESSION, 5, "🔥", "Aaj ka din solid! 5 sessions complete!")
        else -> null
    }

    private fun dailyMilestone(days: Int): StreakMilestone? = when (days) {
        3 -> StreakMilestone(StreakType.DAILY, 3, "💪", "3 din streak! Consistency building!")
        7 -> StreakMilestone(StreakType.DAILY, 7, "🗡️", "Ek hafte ka warrior!")
        14 -> StreakMilestone(StreakType.DAILY, 14, "🦁", "2 weeks beast mode!")
        30 -> StreakMilestone(StreakType.DAILY, 30, "🌟", "30 din — LEGEND!")
        100 -> StreakMilestone(StreakType.DAILY, 100, "👑", "UPSC Warrior! 100 din streak!")
        else -> null
    }

    private fun defaultStreak(uid: String) = StreakEntity(
        userUid = uid, currentDailyStreak = 0, longestDailyStreak = 0,
        currentWeeklyStreak = 0, longestWeeklyStreak = 0,
        todaySessionsCompleted = 0, todaySessionsTotal = 0,
        lastUpdated = System.currentTimeMillis()
    )

    private fun startOfDay(ms: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
