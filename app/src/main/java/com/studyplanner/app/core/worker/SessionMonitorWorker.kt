package com.studyplanner.app.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.studyplanner.app.core.data.local.dao.SessionDao
import com.studyplanner.app.core.data.local.dao.StreakDao
import com.studyplanner.app.core.data.local.dao.UserDao
import com.studyplanner.app.core.util.AlarmScheduler
import com.studyplanner.app.core.util.NotificationHelper
import com.studyplanner.app.core.util.ReschedulingEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class SessionMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val sessionDao: SessionDao,
    private val streakDao: StreakDao,
    private val userDao: UserDao,
    private val notificationHelper: NotificationHelper,
    private val reschedulingEngine: ReschedulingEngine,
    private val alarmScheduler: AlarmScheduler,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "session_monitor"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<SessionMonitorWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueOneTime(context: Context) {
            val request = OneTimeWorkRequestBuilder<SessionMonitorWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }



    override suspend fun doWork(): Result {

        val uid = auth.currentUser?.uid ?: return Result.success()

        val now = System.currentTimeMillis()
        val gracePeriodMs = 3 * 60 * 1000L
        val today = startOfDay(now)

        val todaySessions = try {
            sessionDao.observeByDate(uid, today).first()
        } catch (_: Exception) {
            return Result.failure()
        }

        todaySessions
            .filter {
                it.status == "UPCOMING" &&
                        now > it.scheduledStartTime + gracePeriodMs
            }
            .forEach { session ->
                sessionDao.updateStatus(session.id, "MISSED")
                notificationHelper.showSessionMissed("Session")
                reschedulingEngine.reschedule(uid, session)
            }

        val streak = streakDao.get(uid)

        val completedToday = todaySessions.count {
            it.status == "COMPLETED"
        }

        val totalToday = todaySessions.size

        if (totalToday > 0 && completedToday == totalToday) {
            notificationHelper.showBreakReminder(10)
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        if (hour == 21 && completedToday < totalToday) {
            notificationHelper.showStreakReminder(
                streak?.currentDailyStreak ?: 0
            )
        }

        if (hour == 22) {
            notificationHelper.showNightReflection(
                completedToday,
                totalToday,
                streak?.currentDailyStreak ?: 0
            )
        }

        todaySessions
            .filter {
                it.status == "UPCOMING" &&
                        it.scheduledStartTime > now
            }
            .forEach { session ->

                val minutesUntil =
                    ((session.scheduledStartTime - now) / 60000).toInt()

                if (minutesUntil in 9..11) {
                    notificationHelper.showSessionReminder(
                        session.id,
                        "Study Session",
                        10
                    )
                }
            }

        return Result.success()
    }

    private fun startOfDay(ms: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
