package com.studyplanner.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.studyplanner.app.core.worker.DailySankalpWorker
import com.studyplanner.app.core.worker.SessionMonitorWorker


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {

            // 🌟 FIXED: Boot hone par unique periodic loop ko trigger/reschedule karega
            try {
                SessionMonitorWorker.enqueue(context)
                Log.d("BOOT_RECEIVER", "SessionMonitorWorker periodic chain validated.")
            } catch (e: Exception) {
                Log.e("BOOT_RECEIVER", "Failed: ${e.localizedMessage}")
            }

            try {
                DailySankalpWorker.enqueue(context)
            } catch (e: Exception) {
                Log.e("BOOT_RECEIVER", "Failed DailySankalp: ${e.localizedMessage}")
            }
        }
    }
}