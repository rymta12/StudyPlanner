package com.studyplanner.app.core.receiver

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.studyplanner.app.MainActivity
import com.studyplanner.app.core.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SessionReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type")
        android.util.Log.d("SessionReminderReceiver", "onReceive type=$type")

        when (type) {
            "streak_reminder" -> notificationHelper.showStreakReminder(0)
            else -> {
                val sessionId = intent.getLongExtra("session_id", 0L)
                val minutesBefore = intent.getIntExtra("minutes_before", 10)
                val topicName = intent.getStringExtra("topic_name") ?: "Study Session"

                // Screen on + wake karo (screen off bhi ho to)
                wakeScreen(context)

                // Full-screen intent — lock screen pe bhi dialog dikhega
                showFullScreenAlert(context, sessionId, topicName, minutesBefore)
            }
        }
    }

    private fun wakeScreen(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "StudyPlanner:SessionAlarm"
        )
        wl.acquire(10_000L) // 10 sec ke liye screen on
    }

    private fun showFullScreenAlert(
        context: Context,
        sessionId: Long,
        topicName: String,
        minutesBefore: Int
    ) {
        // Tap karne pe session screen pe jaao
        val tapIntent = PendingIntent.getActivity(
            context,
            sessionId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("open_session_id", sessionId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full screen intent (lock screen pe bhi pop up)
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            (sessionId + 1000).toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("open_session_id", sessionId)
                putExtra("full_screen_alert", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Session in $minutesBefore min!")
            .setContentText("$topicName — Abhi taiyar ho jao! 🚀")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$topicName\n\nSession $minutesBefore minute mein shuru hoga. Taiyar ho jao! 💪"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .setFullScreenIntent(fullScreenIntent, true)  // Lock screen pe bhi dikhega
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .addAction(
                android.R.drawable.ic_media_play,
                "Start Now 🚀",
                tapIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NotificationHelper.ID_SESSION_REMINDER, notification
            )
            android.util.Log.d("SessionReminderReceiver", "Full-screen notification shown for session $sessionId")
        } catch (e: SecurityException) {
            android.util.Log.e("SessionReminderReceiver", "Permission denied: ${e.message}")
        }
    }
}
