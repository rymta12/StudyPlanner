package com.studyplanner.app.core.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.studyplanner.app.R
import com.studyplanner.app.core.util.BlockingMode
import com.studyplanner.app.core.util.DistractionBlocker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class FocusMonitorService : Service() {

    @Inject lateinit var distractionBlocker: DistractionBlocker

    companion object {
        const val CHANNEL_ID = "focus_monitor_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_FOCUS"
        const val ACTION_STOP = "ACTION_STOP_FOCUS"
        const val EXTRA_MODE = "blocking_mode"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_WHITELIST = "whitelist_channels"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitorJob: Job? = null
    private var blockingMode = BlockingMode.DISABLED
    private var whitelistedChannels = listOf<String>()
    private var sessionId = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val modeId = intent.getStringExtra(EXTRA_MODE) ?: BlockingMode.DISABLED.id
                blockingMode = BlockingMode.entries.find { it.id == modeId } ?: BlockingMode.DISABLED
                whitelistedChannels = intent.getStringArrayListExtra(EXTRA_WHITELIST) ?: arrayListOf()
                sessionId = intent.getLongExtra(EXTRA_SESSION_ID, 0L)
                startForeground(NOTIFICATION_ID, buildNotification())
                startMonitoring()
            }
            ACTION_STOP -> {
                stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive && blockingMode != BlockingMode.DISABLED) {
                delay(2000)
                val foregroundPkg = distractionBlocker.getForegroundApp() ?: continue
                if (foregroundPkg == packageName) continue
                if (distractionBlocker.shouldBlock(foregroundPkg, blockingMode, whitelistedChannels)) {
                    val isPartial = distractionBlocker.isYoutubePartialBlock(foregroundPkg, blockingMode)
                    showBlockOverlay(foregroundPkg, isPartial)
                }
            }
        }
    }

    private fun stopMonitoring() { monitorJob?.cancel(); monitorJob = null }

    private fun showBlockOverlay(blockedPkg: String, isWarning: Boolean) {
        startActivity(Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BlockOverlayActivity.EXTRA_BLOCKED_PKG, blockedPkg)
            putExtra(BlockOverlayActivity.EXTRA_IS_WARNING, isWarning)
            putExtra(BlockOverlayActivity.EXTRA_SESSION_ID, sessionId)
        })
    }

    private fun buildNotification(topicName: String = "Study Session"): Notification {
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0,
            android.content.Intent(this, com.studyplanner.app.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🍅 Session Active — $topicName")
            .setContentText("Focus mode ON • Tap to open timer")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Focus Monitor", NotificationManager.IMPORTANCE_LOW)
            .apply { description = "Active study session monitoring"; setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}