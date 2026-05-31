package com.studyplanner.app.core.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyplanner.app.core.data.local.StudyPlannerDatabase
import com.studyplanner.app.core.util.AdManager
import com.studyplanner.app.core.util.PremiumManager
import com.studyplanner.app.ui.theme.ThemeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StudyPlannerDatabase =
        Room.databaseBuilder(context, StudyPlannerDatabase::class.java, "study_planner.db")
            .fallbackToDestructiveMigration().build()

    @Provides fun provideUserDao(db: StudyPlannerDatabase) = db.userDao()
    @Provides fun provideSubjectDao(db: StudyPlannerDatabase) = db.subjectDao()
    @Provides fun provideChapterDao(db: StudyPlannerDatabase) = db.chapterDao()
    @Provides fun provideTopicDao(db: StudyPlannerDatabase) = db.topicDao()
    @Provides fun provideSubtopicDao(db: StudyPlannerDatabase) = db.subtopicDao()
    @Provides fun provideStudySlotDao(db: StudyPlannerDatabase) = db.studySlotDao()
    @Provides fun provideSessionDao(db: StudyPlannerDatabase) = db.sessionDao()
    @Provides fun providePersonalRoutineDao(db: StudyPlannerDatabase) = db.personalRoutineDao()
    @Provides fun providePeriodSettingsDao(db: StudyPlannerDatabase) = db.periodSettingsDao()
    @Provides fun provideMorningAlarmDao(db: StudyPlannerDatabase) = db.morningAlarmDao()
    @Provides fun provideStreakDao(db: StudyPlannerDatabase) = db.streakDao()
    @Provides fun provideBreakSettingsDao(db: StudyPlannerDatabase) = db.breakSettingsDao()
    @Provides fun provideVisionBoardDao(db: StudyPlannerDatabase) = db.visionBoardDao()

    @Provides fun provideReflectionDao(db: StudyPlannerDatabase) = db.reflectionDao()

    @Provides @Singleton
    fun provideStudyDebtBadge(@ApplicationContext context: Context) =
        com.studyplanner.app.core.util.StudyDebtBadge(context)


    @Provides @Singleton fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    @Provides @Singleton fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideThemeManager(@ApplicationContext context: Context) = ThemeManager(context)

    @Provides @Singleton
    fun providePremiumManager(auth: FirebaseAuth, fs: FirebaseFirestore) = PremiumManager(auth, fs)

    @Provides @Singleton
    fun provideAdManager(pm: PremiumManager) = AdManager(pm)

    @Provides @Singleton
    fun provideDistractionBlocker(@ApplicationContext context: Context) =
        com.studyplanner.app.core.util.DistractionBlocker(context)

    @Provides @Singleton
    fun provideSyncManager(@ApplicationContext context: Context) =
        com.studyplanner.app.core.sync.SyncManager(context)

    @Provides @Singleton
    fun provideFirestoreSyncService(
        auth: FirebaseAuth, fs: FirebaseFirestore,
        userDao: com.studyplanner.app.core.data.local.dao.UserDao,
        subjectDao: com.studyplanner.app.core.data.local.dao.SubjectDao,
        chapterDao: com.studyplanner.app.core.data.local.dao.ChapterDao,
        topicDao: com.studyplanner.app.core.data.local.dao.TopicDao,
        subtopicDao: com.studyplanner.app.core.data.local.dao.SubtopicDao,
        studySlotDao: com.studyplanner.app.core.data.local.dao.StudySlotDao,
        personalRoutineDao: com.studyplanner.app.core.data.local.dao.PersonalRoutineDao,
        breakSettingsDao: com.studyplanner.app.core.data.local.dao.BreakSettingsDao,
        morningAlarmDao: com.studyplanner.app.core.data.local.dao.MorningAlarmDao,
        visionBoardDao: com.studyplanner.app.core.data.local.dao.VisionBoardDao,
        periodSettingsDao: com.studyplanner.app.core.data.local.dao.PeriodSettingsDao,
        streakDao: com.studyplanner.app.core.data.local.dao.StreakDao,
    ) = com.studyplanner.app.core.sync.FirestoreSyncService(
        auth, fs, userDao, subjectDao, chapterDao, topicDao, subtopicDao,
        studySlotDao, personalRoutineDao, breakSettingsDao, morningAlarmDao,
        visionBoardDao, periodSettingsDao, streakDao
    )

    @Provides @Singleton
    fun provideAntiCheatManager(@ApplicationContext context: Context) =
        com.studyplanner.app.core.util.AntiCheatManager(context)

    @Provides @Singleton
    fun provideAlarmScheduler(
        @ApplicationContext context: Context,
        morningAlarmDao: com.studyplanner.app.core.data.local.dao.MorningAlarmDao,
    ) = com.studyplanner.app.core.util.AlarmScheduler(context, morningAlarmDao)

    @Provides @Singleton
    fun provideGamificationManager(
        auth: FirebaseAuth,
        fs: FirebaseFirestore,
        userDao: com.studyplanner.app.core.data.local.dao.UserDao,
        sessionDao: com.studyplanner.app.core.data.local.dao.SessionDao,
        streakDao: com.studyplanner.app.core.data.local.dao.StreakDao,
    ) = com.studyplanner.app.core.util.GamificationManager(auth, fs, userDao, sessionDao, streakDao)

    @Provides @Singleton
    fun provideStreakManager(
        streakDao: com.studyplanner.app.core.data.local.dao.StreakDao,
        sessionDao: com.studyplanner.app.core.data.local.dao.SessionDao,
        userDao: com.studyplanner.app.core.data.local.dao.UserDao,
    ) = com.studyplanner.app.core.util.StreakManager(streakDao, sessionDao, userDao)

    @Provides @Singleton
    fun provideReschedulingEngine(
        sessionDao: com.studyplanner.app.core.data.local.dao.SessionDao,
        studySlotDao: com.studyplanner.app.core.data.local.dao.StudySlotDao,
        personalRoutineDao: com.studyplanner.app.core.data.local.dao.PersonalRoutineDao,
        userDao: com.studyplanner.app.core.data.local.dao.UserDao,
    ) = com.studyplanner.app.core.util.ReschedulingEngine(sessionDao, studySlotDao, personalRoutineDao, userDao)

    @Provides @Singleton
    fun provideSessionRepository(
        sessionDao: com.studyplanner.app.core.data.local.dao.SessionDao,
        auth: FirebaseAuth,
        fs: FirebaseFirestore,
        reschedulingEngine: com.studyplanner.app.core.util.ReschedulingEngine,
        streakManager: com.studyplanner.app.core.util.StreakManager,
    ) = com.studyplanner.app.core.data.repository.SessionRepository(sessionDao, auth, fs, reschedulingEngine, streakManager)
}
