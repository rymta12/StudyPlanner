package com.studyplanner.app

import android.app.Application
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createAllChannels()
        SessionMonitorWorker.enqueue(this)
        DailySankalpWorker.enqueue(this)
    }
}
