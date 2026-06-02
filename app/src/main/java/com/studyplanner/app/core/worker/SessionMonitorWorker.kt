package com.studyplanner.app.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.studyplanner.app.core.data.local.dao.SessionDao
import com.studyplanner.app.core.data.local.dao.StreakDao
import com.studyplanner.app.core.data.local.dao.TopicDao
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
    private val topicDao: TopicDao,
    private val userDao: UserDao,
    private val notificationHelper: NotificationHelper,
    private val reschedulingEngine: ReschedulingEngine,
    private val alarmScheduler: AlarmScheduler,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "session_monitor"
        private const val TAG = "SessionMonitor"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<SessionMonitorWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "SessionMonitorWorker enqueued (periodic 15min)")
        }


    }

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: run {
            Log.d(TAG, "No logged-in user, skipping")
            return Result.success()
        }

        val now = System.currentTimeMillis()
        val gracePeriodMs = 3 * 60 * 1000L
        val today = startOfDay(now)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        Log.d(TAG, "doWork uid=$uid hour=$hour")

        val user = userDao.get(uid)
        val userName = user?.name?.split(" ")?.firstOrNull() ?: "Student"

        val todaySessions = try {
            sessionDao.observeByDate(uid, today).first()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load sessions: ${e.message}")
            return Result.failure()
        }

        Log.d(TAG, "Today sessions: ${todaySessions.size}")

        // 1. MISSED sessions — grace period baad mark karo + notify
        todaySessions
            .filter {
                it.status == "UPCOMING" && now > it.scheduledStartTime + gracePeriodMs
            }
            .forEach { session ->
                Log.d(TAG, "Marking missed: session ${session.id}")
                sessionDao.updateStatus(session.id, "MISSED")
                notificationHelper.showSessionMissed(session.id.toString())
                reschedulingEngine.reschedule(uid, session)
            }

        // 2. UPCOMING sessions — reminder schedule karo (AlarmManager se exact alarm)
        todaySessions
            .filter { it.status == "UPCOMING" && it.scheduledStartTime > now }
            .forEach { session ->
                val minutesUntil = ((session.scheduledStartTime - now) / 60000).toInt()
                Log.d(TAG, "Session in ${minutesUntil}min: ${session.id}")

                // 10 min pehle exact alarm schedule karo
                if (minutesUntil in 8..12) {
                    alarmScheduler.scheduleSessionReminder(session, userName, minutesUntil)
                }
                // 2 min pehle direct notification (worker yahan chal raha hai)
                if (minutesUntil in 1..3) {
                    notificationHelper.showSessionReminder(
                        session.id,
                        "Aaj ka session",
                        minutesUntil
                    )
                }
            }

        val completedToday = todaySessions.count { it.status == "COMPLETED" }
        val totalToday = todaySessions.size

        // 3. Raat 9 baje — streak reminder agar sessions miss ho rahe hain
        if (hour == 21 && completedToday < totalToday) {
            val streak = streakDao.get(uid)
            notificationHelper.showStreakReminder(streak?.currentDailyStreak ?: 0)
            Log.d(TAG, "Streak reminder sent, streak=${streak?.currentDailyStreak}")
        }

        // 4. Raat 10 baje — night reflection
        if (hour == 22) {
            val streak = streakDao.get(uid)
            notificationHelper.showNightReflection(
                completedToday, totalToday, streak?.currentDailyStreak ?: 0
            )
            // 5. Due revisions check — subah 8 baje
            if (hour == 8) {
                val dueRevisions = topicDao.getDueRevisions(now)
                if (dueRevisions.isNotEmpty()) {
                    val firstName = dueRevisions.first().name
                    notificationHelper.showRevisionDue(firstName, dueRevisions.size)
                    Log.d(TAG, "Revision due: ${dueRevisions.size} topics")
                }
            }
        }
            Log.d(TAG, "doWork complete")
            return Result.success()

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
