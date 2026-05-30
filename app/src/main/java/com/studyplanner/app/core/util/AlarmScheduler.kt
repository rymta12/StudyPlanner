package com.studyplanner.app.core.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.studyplanner.app.core.data.local.dao.MorningAlarmDao
import com.studyplanner.app.core.data.local.dao.SessionDao
import com.studyplanner.app.core.data.local.entity.MorningAlarmEntity
import com.studyplanner.app.core.data.local.entity.SessionEntity
import com.studyplanner.app.core.receiver.MorningAlarmReceiver
import com.studyplanner.app.core.receiver.SessionReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val morningAlarmDao: MorningAlarmDao,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleSessionReminder(session: SessionEntity, userName: String, minutesBefore: Int = 10) {
        val triggerAt = session.scheduledStartTime - minutesBefore * 60 * 1000L
        if (triggerAt < System.currentTimeMillis()) return

        val intent = Intent(context, SessionReminderReceiver::class.java).apply {
            putExtra("session_id", session.id)
            putExtra("user_name", userName)
            putExtra("minutes_before", minutesBefore)
        }
        val pi = PendingIntent.getBroadcast(
            context, session.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    fun cancelSessionReminder(sessionId: Long) {
        val intent = Intent(context, SessionReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, sessionId.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { alarmManager.cancel(it) }
    }

    fun scheduleMorningAlarm(alarm: MorningAlarmEntity, userName: String) {
        if (!alarm.isEnabled) return
        val triggerAt = nextAlarmTime(alarm.hour, alarm.minute, alarm.daysOfWeek)

        val intent = Intent(context, MorningAlarmReceiver::class.java).apply {
            putExtra(MorningAlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(MorningAlarmReceiver.EXTRA_USER_NAME, userName)
            putExtra(MorningAlarmReceiver.EXTRA_MESSAGE, alarm.customMessage)
            putExtra(MorningAlarmReceiver.EXTRA_WAKE_TYPE, alarm.wakeVerificationType)
        }
        val pi = PendingIntent.getBroadcast(
            context, alarm.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    fun cancelMorningAlarm(alarmId: Long) {
        val intent = Intent(context, MorningAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, alarmId.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { alarmManager.cancel(it) }
    }

    fun scheduleStreakReminder(uid: String) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(context, SessionReminderReceiver::class.java).apply {
            putExtra("type", "streak_reminder")
            putExtra("uid", uid)
        }
        val pi = PendingIntent.getBroadcast(
            context, 9999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }

    private fun nextAlarmTime(hour: Int, minute: Int, daysOfWeek: String): Long {
        val enabledDays = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        repeat(7) {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek in enabledDays) return cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
