package com.studyplanner.app.core.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
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

    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createAllChannels() {
        listOf(
            NotificationChannel(CHANNEL_STUDY, "Study Sessions", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Session reminders and alerts" },
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
        ).forEach { manager.createNotificationChannel(it) }
    }

    fun showSessionReminder(sessionId: Long, topicName: String, minutesBefore: Int) {
        val intent = mainIntent()
        show(
            id = ID_SESSION_REMINDER,
            channel = CHANNEL_STUDY,
            title = "⏰ Session in $minutesBefore min!",
            body = "$topicName — ${MOTIVATIONAL_LINES.random()}",
            intent = intent,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showSessionMissed(topicName: String) {
        show(
            id = ID_SESSION_MISSED,
            channel = CHANNEL_STUDY,
            title = "⚠️ Session Missed!",
            body = "$topicName session miss hua. Rescheduling ho raha hai...",
            intent = mainIntent(),
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showBreakReminder(breakMinutes: Int) {
        show(
            id = ID_BREAK_REMINDER,
            channel = CHANNEL_BREAK,
            title = "☕ Break Time!",
            body = "${breakMinutes}min ka break lo — stretch karo, paani piyo! 💧",
            intent = mainIntent()
        )
    }

    fun showRevisionDue(topicName: String, count: Int) {
        show(
            id = ID_REVISION_DUE,
            channel = CHANNEL_REVISION,
            title = "🔄 $count Revisions Due!",
            body = "$topicName aur aur topics revise karne hain aaj",
            intent = mainIntent()
        )
    }

    fun showAchievement(emoji: String, title: String, description: String) {
        show(
            id = ID_ACHIEVEMENT,
            channel = CHANNEL_ACHIEVEMENT,
            title = "$emoji $title Unlocked!",
            body = description,
            intent = mainIntent()
        )
    }

    fun showStreakReminder(currentStreak: Int) {
        show(
            id = ID_STREAK_REMINDER,
            channel = CHANNEL_STUDY,
            title = "🔥 Streak at risk!",
            body = "Aaj padha kya? $currentStreak din ki streak toot jaayegi! Abhi padho! 💪",
            intent = mainIntent(),
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
            intent = mainIntent(),
            priority = NotificationCompat.PRIORITY_MAX,
            ongoing = true
        )
    }

    fun showNightReflection(completedSessions: Int, totalSessions: Int, streak: Int) {
        show(
            id = ID_NIGHT_REFLECTION,
            channel = CHANNEL_STUDY,
            title = "🌙 Aaj ka report",
            body = "$completedSessions/$totalSessions sessions complete • 🔥$streak day streak • So ja, kal phir mehnat karni hai! 💤",
            intent = mainIntent()
        )
    }

    fun showDailySankalp(userName: String) {
        show(
            id = ID_SANKALP,
            channel = CHANNEL_STUDY,
            title = "🎯 Aaj ka Sankalp",
            body = "\"Aaj main apna target zaroor poora karunga\" — Chalo shuru karte hain, $userName!",
            intent = mainIntent()
        )
    }

    fun dismiss(id: Int) = manager.cancel(id)

    fun buildAlarmNotification(userName: String, message: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ Good Morning, $userName!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(mainPendingIntent(), true)
            .build()
    }

    private fun show(
        id: Int, channel: String, title: String, body: String,
        intent: PendingIntent, priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        ongoing: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        }
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setOngoing(ongoing)
            .build()
        manager.notify(id, notification)
    }

    private fun mainIntent() = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun mainPendingIntent() = mainIntent()
}
