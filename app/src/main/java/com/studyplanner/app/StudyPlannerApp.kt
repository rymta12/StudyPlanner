package com.studyplanner.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.studyplanner.app.core.util.NotificationHelper
import com.studyplanner.app.core.worker.DailySankalpWorker
import com.studyplanner.app.core.worker.SessionMonitorWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StudyPlannerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper

    companion object {
        private lateinit var instance: StudyPlannerApp
        fun getContext(): android.content.Context = instance.applicationContext
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()
        instance = this
        notificationHelper.createAllChannels()

        // Periodic worker — har 15 min session monitor karo
        SessionMonitorWorker.enqueue(this)
        DailySankalpWorker.enqueue(this)

        Log.d("StudyPlannerApp", "App started, workers enqueued")
    }
}
