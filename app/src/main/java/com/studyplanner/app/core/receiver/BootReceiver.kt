package com.studyplanner.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.studyplanner.app.core.worker.DailySankalpWorker
import com.studyplanner.app.core.worker.SessionMonitorWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        SessionMonitorWorker.enqueue(context)
        DailySankalpWorker.enqueue(context)
    }
}
