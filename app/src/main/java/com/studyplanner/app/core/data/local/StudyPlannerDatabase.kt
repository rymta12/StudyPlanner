package com.studyplanner.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.studyplanner.app.core.data.local.converter.Converters
import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        SubjectEntity::class,
        ChapterEntity::class,
        TopicEntity::class,
        SubtopicEntity::class,
        StudySlotEntity::class,
        SessionEntity::class,
        PersonalRoutineEntity::class,
        PeriodSettingsEntity::class,
        MorningAlarmEntity::class,
        StreakEntity::class,
        BreakSettingsEntity::class,
        VisionBoardEntity::class,
        ReflectionEntity::class,
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyPlannerDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun topicDao(): TopicDao
    abstract fun subtopicDao(): SubtopicDao
    abstract fun studySlotDao(): StudySlotDao
    abstract fun sessionDao(): SessionDao
    abstract fun personalRoutineDao(): PersonalRoutineDao
    abstract fun periodSettingsDao(): PeriodSettingsDao
    abstract fun morningAlarmDao(): MorningAlarmDao
    abstract fun streakDao(): StreakDao
    abstract fun breakSettingsDao(): BreakSettingsDao
    abstract fun visionBoardDao(): VisionBoardDao
    abstract fun reflectionDao(): ReflectionDao
}
