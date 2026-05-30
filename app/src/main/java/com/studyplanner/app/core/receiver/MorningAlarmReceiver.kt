package com.studyplanner.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyplanner.app.core.service.AlarmService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MorningAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_USER_NAME = "user_name"
        const val EXTRA_MESSAGE = "alarm_message"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_WAKE_TYPE = "wake_type"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_USER_NAME, intent.getStringExtra(EXTRA_USER_NAME) ?: "")
            putExtra(EXTRA_MESSAGE, intent.getStringExtra(EXTRA_MESSAGE) ?: "")
            putExtra(EXTRA_ALARM_ID, intent.getLongExtra(EXTRA_ALARM_ID, 0L))
            putExtra(EXTRA_WAKE_TYPE, intent.getStringExtra(EXTRA_WAKE_TYPE) ?: "MATH")
        }
        context.startForegroundService(serviceIntent)
    }
}
