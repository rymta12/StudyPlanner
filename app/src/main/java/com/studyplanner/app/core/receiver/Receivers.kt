package com.studyplanner.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyplanner.app.core.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SessionReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type")
        when (type) {
            "streak_reminder" -> {
                notificationHelper.showStreakReminder(0)
            }
            else -> {
                val sessionId = intent.getLongExtra("session_id", 0L)
                val minutesBefore = intent.getIntExtra("minutes_before", 10)
                notificationHelper.showSessionReminder(sessionId, "Study Session", minutesBefore)
            }
        }
    }
}
