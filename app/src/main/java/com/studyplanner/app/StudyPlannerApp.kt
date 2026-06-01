package com.studyplanner.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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

        // Pure app mein kahin se bhi context lene ke liye functions
        fun getContext(): android.content.Context {
            return instance.applicationContext
        }
    }
    override val workManagerConfiguration: Configuration
        get() {
            Log.d("WORKER_DEBUG", "WorkerFactory = $workerFactory")

            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()

        instance = this

        notificationHelper.createAllChannels()
        Log.d("APP_TEST", "Application Started")

//        SessionMonitorWorker.enqueue(this)
        val request = OneTimeWorkRequestBuilder<SessionMonitorWorker>()
            .build()

        WorkManager.getInstance(this)
            .enqueue(request)

        WorkManager.getInstance(this)
            .getWorkInfoByIdLiveData(request.id)
            .observeForever {
                Log.d("WORK_INFO", "State = ${it?.state}")
            }
        DailySankalpWorker.enqueue(this)
    }
}
