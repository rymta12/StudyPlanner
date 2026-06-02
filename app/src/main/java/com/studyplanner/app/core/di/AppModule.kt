package com.studyplanner.app.core.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyplanner.app.core.data.local.StudyPlannerDatabase
import com.studyplanner.app.core.data.local.dao.BreakSettingsDao
import com.studyplanner.app.core.data.local.dao.ChapterDao
import com.studyplanner.app.core.data.local.dao.MorningAlarmDao
import com.studyplanner.app.core.data.local.dao.PeriodSettingsDao
import com.studyplanner.app.core.data.local.dao.PersonalRoutineDao
import com.studyplanner.app.core.data.local.dao.SessionDao
import com.studyplanner.app.core.data.local.dao.StreakDao
import com.studyplanner.app.core.data.local.dao.StudySlotDao
import com.studyplanner.app.core.data.local.dao.SubjectDao
import com.studyplanner.app.core.data.local.dao.SubtopicDao
import com.studyplanner.app.core.data.local.dao.TopicDao
import com.studyplanner.app.core.data.local.dao.UserDao
import com.studyplanner.app.core.data.local.dao.VisionBoardDao
import com.studyplanner.app.core.data.repository.SessionRepository
import com.studyplanner.app.core.sync.FirestoreSyncService
import com.studyplanner.app.core.sync.SyncManager
import com.studyplanner.app.core.util.AdManager
import com.studyplanner.app.core.util.AlarmScheduler
import com.studyplanner.app.core.util.AntiCheatManager
import com.studyplanner.app.core.util.DistractionBlocker
import com.studyplanner.app.core.util.GamificationManager
import com.studyplanner.app.core.util.NotificationHelper
import com.studyplanner.app.core.util.PremiumManager
import com.studyplanner.app.core.util.ReschedulingEngine
import com.studyplanner.app.core.util.StreakManager
import com.studyplanner.app.core.util.StudyDebtBadge
import com.studyplanner.app.core.util.TimetableGenerator
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
        StudyDebtBadge(context)


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
        DistractionBlocker(context)

    @Provides @Singleton
    fun provideSyncManager(@ApplicationContext context: Context) =
        SyncManager(context)

    @Provides @Singleton
    fun provideFirestoreSyncService(
        auth: FirebaseAuth, fs: FirebaseFirestore,
        userDao: UserDao,
        subjectDao: SubjectDao,
        chapterDao: ChapterDao,
        topicDao: TopicDao,
        subtopicDao: SubtopicDao,
        studySlotDao: StudySlotDao,
        personalRoutineDao: PersonalRoutineDao,
        breakSettingsDao: BreakSettingsDao,
        morningAlarmDao: MorningAlarmDao,
        visionBoardDao: VisionBoardDao,
        periodSettingsDao: PeriodSettingsDao,
        streakDao: StreakDao,
        timetableGenerator: TimetableGenerator,
        @ApplicationContext context: Context
    ) = FirestoreSyncService(
        auth, fs, userDao, subjectDao, chapterDao, topicDao, subtopicDao,
        studySlotDao, personalRoutineDao, breakSettingsDao, morningAlarmDao,
        visionBoardDao, periodSettingsDao, streakDao, timetableGenerator,
        context
    )

    @Provides @Singleton
    fun provideStudyMusicManager(@ApplicationContext context: Context) =
        com.studyplanner.app.core.util.StudyMusicManager(context)


    @Provides @Singleton
    fun provideAntiCheatManager(@ApplicationContext context: Context) =
        AntiCheatManager(context)

    @Provides @Singleton
    fun provideAlarmScheduler(
        @ApplicationContext context: Context,
        morningAlarmDao: MorningAlarmDao,
    ) = AlarmScheduler(context, morningAlarmDao)

    @Provides @Singleton
    fun provideGamificationManager(
        auth: FirebaseAuth,
        fs: FirebaseFirestore,
        userDao: UserDao,
        sessionDao: SessionDao,
        streakDao: StreakDao,
        notificationHelper: NotificationHelper,

        ) = GamificationManager(auth, fs, userDao, sessionDao, streakDao, notificationHelper)

    @Provides @Singleton
    fun provideStreakManager(
        streakDao: StreakDao,
        sessionDao: SessionDao,
        userDao: UserDao,
    ) = StreakManager(streakDao, sessionDao, userDao)

    @Provides @Singleton
    fun provideReschedulingEngine(
        sessionDao: SessionDao,
        studySlotDao: StudySlotDao,
        personalRoutineDao: PersonalRoutineDao,
        userDao: UserDao,
    ) = ReschedulingEngine(sessionDao, studySlotDao, personalRoutineDao, userDao)

    @Provides @Singleton
    fun provideSessionRepository(
        sessionDao: SessionDao,
        auth: FirebaseAuth,
        fs: FirebaseFirestore,
        reschedulingEngine: ReschedulingEngine,
        streakManager: StreakManager,
    ) = SessionRepository(sessionDao, auth, fs, reschedulingEngine, streakManager)
}
