package com.studyplanner.app.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App icon par study-debt badge (pending/missed sessions count).
 * ShortcutBadger ki jagah modern notification-badge approach — har launcher par chalta hai,
 * koi extra dependency nahi. POST_NOTIFICATIONS permission manifest me already hai.
 */
@Singleton
class StudyDebtBadge @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val channelId = "study_debt"
    private val notifId = 9100

    private fun ensureChannel() {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) == null) {
            val ch = NotificationChannel(
                channelId, "Study Debt", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Pending/missed study sessions ki yaad"
                setShowBadge(true)
            }
            nm.createNotificationChannel(ch)
        }
    }

    /** debtCount > 0 -> badge dikhao, 0 ya kam -> hata do */
    fun update(debtCount: Int) {
        if (debtCount <= 0) { clear(); return }
        ensureChannel()
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📚 $debtCount session pending")
            .setContentText("Missed slots reschedule kar lo!")
            .setNumber(debtCount)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notifId, notif)
        } catch (_: SecurityException) { /* notif permission nahi mili */ }
    }

    fun clear() = NotificationManagerCompat.from(context).cancel(notifId)
}
