package com.studyplanner.app.core.service

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.studyplanner.app.R
import com.studyplanner.app.core.receiver.MorningAlarmReceiver
import com.studyplanner.app.core.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {

    @Inject lateinit var notificationHelper: NotificationHelper

    private var vibrator: android.os.Vibrator? = null

    companion object {
        const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            stopAlarm(); return START_NOT_STICKY
        }

        val userName = intent?.getStringExtra(MorningAlarmReceiver.EXTRA_USER_NAME) ?: ""
        val message = intent?.getStringExtra(MorningAlarmReceiver.EXTRA_MESSAGE) ?: ""

        val notification = notificationHelper.buildAlarmNotification(userName, message)
        startForeground(NotificationHelper.ID_MORNING_ALARM, notification)

        startRingtone()
        startVibration()

        return START_STICKY
    }

    private fun startRingtone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(this, uri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        vibrator = getSystemService(Vibrator::class.java)
        val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun stopAlarm() {
        vibrator?.cancel()
        notificationHelper.dismiss(NotificationHelper.ID_MORNING_ALARM)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        vibrator?.cancel()
    }
}
