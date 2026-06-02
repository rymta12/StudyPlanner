package com.studyplanner.app.core.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.studyplanner.app.MainActivity
import com.studyplanner.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_STUDY = "channel_study"
        const val CHANNEL_BREAK = "channel_break"
        const val CHANNEL_REVISION = "channel_revision"
        const val CHANNEL_ACHIEVEMENT = "channel_achievement"
        const val CHANNEL_ALARM = "channel_alarm"

        const val ID_SESSION_REMINDER = 2001
        const val ID_SESSION_MISSED = 2002
        const val ID_BREAK_REMINDER = 2003
        const val ID_REVISION_DUE = 2004
        const val ID_ACHIEVEMENT = 2005
        const val ID_STREAK_REMINDER = 2006
        const val ID_MORNING_ALARM = 2007
        const val ID_NIGHT_REFLECTION = 2008
        const val ID_SANKALP = 2009

        val MOTIVATIONAL_LINES = listOf(
            "Sapne woh nahi jo neend mein aate hain! 🔥",
            "Aaj ka effort kal ka result banega! 💪",
            "IAS banna hai toh focus rakh! 🎯",
            "Har session ek kadam aage! 🚀",
            "Consistency hi key hai! 🗝️",
            "Kal se nahi, abhi se! ⚡",
            "Tu kar sakta hai! Believe in yourself! 🌟",
        )
    }

    private val manager = NotificationManagerCompat.from(context)

    fun createAllChannels() {
        val nm = context.getSystemService(NotificationManager::class.java)
        listOf(
            NotificationChannel(CHANNEL_STUDY, "Study Sessions", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Session reminders and alerts"; enableVibration(true) },
            NotificationChannel(CHANNEL_BREAK, "Break Reminders", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Break time notifications" },
            NotificationChannel(CHANNEL_REVISION, "Revision Due", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Revision schedule reminders" },
            NotificationChannel(CHANNEL_ACHIEVEMENT, "Achievements", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Badges and milestone unlocked" },
            NotificationChannel(CHANNEL_ALARM, "Morning Alarm", NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    description = "Morning wake-up alarm"
                    enableVibration(true)
                    setBypassDnd(true)
                },
        ).forEach { nm.createNotificationChannel(it) }
    }

    fun showSessionReminder(sessionId: Long, topicName: String, minutesBefore: Int) {
        show(
            id = ID_SESSION_REMINDER,
            channel = CHANNEL_STUDY,
            title = "⏰ Session in $minutesBefore min!",
            body = "$topicName — ${MOTIVATIONAL_LINES.random()}",
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showSessionMissed(topicName: String) {
        show(
            id = ID_SESSION_MISSED,
            channel = CHANNEL_STUDY,
            title = "⚠️ Session Missed!",
            body = "Session miss hua — auto rescheduling ho raha hai...",
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showBreakReminder(breakMinutes: Int) {
        show(
            id = ID_BREAK_REMINDER,
            channel = CHANNEL_BREAK,
            title = "☕ Break Time!",
            body = "${breakMinutes}min ka break lo — stretch karo, paani piyo! 💧"
        )
    }

    fun showRevisionDue(topicName: String, count: Int) {
        show(
            id = ID_REVISION_DUE,
            channel = CHANNEL_REVISION,
            title = "🔄 $count Revisions Due!",
            body = "$topicName aur topics revise karne hain aaj"
        )
    }

    fun showAchievement(emoji: String, title: String, description: String) {
        show(
            id = ID_ACHIEVEMENT,
            channel = CHANNEL_ACHIEVEMENT,
            title = "$emoji $title Unlocked!",
            body = description
        )
    }

    fun showStreakReminder(currentStreak: Int) {
        show(
            id = ID_STREAK_REMINDER,
            channel = CHANNEL_STUDY,
            title = "🔥 Streak at risk!",
            body = "Aaj padha kya? $currentStreak din ki streak toot jaayegi! Abhi padho! 💪",
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showMorningAlarm(userName: String, customMessage: String) {
        val name = userName.ifBlank { "Student" }
        val msg = customMessage.ifBlank { "Uth ja $name! Aaj bhi mehnat karni hai! 🔥" }
        show(
            id = ID_MORNING_ALARM,
            channel = CHANNEL_ALARM,
            title = "⏰ Good Morning, $name!",
            body = msg,
            priority = NotificationCompat.PRIORITY_MAX,
            ongoing = true
        )
    }

    fun showNightReflection(completedSessions: Int, totalSessions: Int, streak: Int) {
        show(
            id = ID_NIGHT_REFLECTION,
            channel = CHANNEL_STUDY,
            title = "🌙 Aaj ka report",
            body = "$completedSessions/$totalSessions sessions complete • 🔥$streak day streak • So ja, kal phir mehnat! 💤"
        )
    }

    fun showDailySankalp(userName: String) {
        show(
            id = ID_SANKALP,
            channel = CHANNEL_STUDY,
            title = "🎯 Aaj ka Sankalp, $userName!",
            body = "\"Aaj main apna target zaroor poora karunga\" — Chalo shuru karte hain!"
        )
    }

    fun dismiss(id: Int) = manager.cancel(id)

    fun buildAlarmNotification(userName: String, message: String): Notification {
        val name = userName.ifBlank { "Student" }
        val msg = message.ifBlank { "Uth ja $name! IAS banna hai! 🔥" }
        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Good Morning, $name!")
            .setContentText(msg)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(mainPendingIntent(), true)
            .build()
    }

    /** Permission check + actual notify */
    private fun show(
        id: Int,
        channel: String,
        title: String,
        body: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        ongoing: Boolean = false
    ) {
        // Android 13+ permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                android.util.Log.w("NotificationHelper", "POST_NOTIFICATIONS not granted, skip id=$id")
                return
            }
        }

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)   // System icon — apna icon lag jaayega
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setContentIntent(mainPendingIntent())
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .build()

        try {
            manager.notify(id, notification)
            android.util.Log.d("NotificationHelper", "Notified id=$id title=$title")
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "SecurityException: ${e.message}")
        }
    }

    private fun mainPendingIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
