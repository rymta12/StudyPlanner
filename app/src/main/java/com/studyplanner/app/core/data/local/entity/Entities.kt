package com.studyplanner.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String,
    val gender: String,
    val city: String,
    val state: String,
    val examType: String,
    val examSubType: String,
    val targetDate: Long,
    val dailyTargetHours: Float,
    val points: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastStudyDate: Long,
    val authenticityScore: Int,
    val isPremium: Boolean,
    val isPro: Boolean,
    val parentCode: String,
    val linkedParentUid: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userUid: String,
    val name: String,
    val colorHex: String,
    val priority: String,
    val totalPages: Int,
    val readingSpeedMinPerPage: Float,
    val totalVideoMinutes: Int,
    val estimatedTotalMinutes: Int,
    val completedMinutes: Int,
    val orderIndex: Int,
    val createdAt: Long
)

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = SubjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("subjectId")]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val name: String,
    val pageStart: Int,
    val pageEnd: Int,
    val totalPages: Int,
    val estimatedMinutes: Int,
    val completedMinutes: Int,
    val status: String,
    val orderIndex: Int
)

@Entity(
    tableName = "topics",
    foreignKeys = [ForeignKey(
        entity = ChapterEntity::class,
        parentColumns = ["id"],
        childColumns = ["chapterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("chapterId")]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val name: String,
    val estimatedMinutes: Int,
    val completedMinutes: Int,
    val status: String,
    val difficultyLevel: String,
    val orderIndex: Int,
    val lastStudiedAt: Long,
    val nextRevisionAt: Long
)

@Entity(
    tableName = "subtopics",
    foreignKeys = [ForeignKey(
        entity = TopicEntity::class,
        parentColumns = ["id"],
        childColumns = ["topicId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("topicId")]
)
data class SubtopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val name: String,
    val estimatedMinutes: Int,
    val completedMinutes: Int,
    val status: String,
    val orderIndex: Int,
    val completedAt: Long,
    val nextRevisionAt: Long,
    val revisionCount: Int
)

@Entity(tableName = "study_slots")
data class StudySlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userUid: String,
    val dayOfWeek: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val slotName: String,
    val preferredSubjectId: Long,
    val isActive: Boolean
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userUid: String,
    val subjectId: Long,
    val chapterId: Long,
    val topicId: Long,
    val subtopicId: Long,
    val scheduledDate: Long,
    val scheduledStartTime: Long,
    val scheduledEndTime: Long,
    val actualStartTime: Long,
    val actualEndTime: Long,
    val studyMinutes: Int,
    val breakMinutes: Int,
    val status: String,
    val extensionCount: Int,
    val extensionMinutes: Int,
    val backgroundMusicUrl: String,
    val emotionBefore: String,
    val selfieUrl: String,
    val appUsageLog: String,
    val touchActivityScore: Int,
    val promptsAnswered: Int,
    val promptsTotal: Int,
    val authenticityScore: Int,
    val pointsEarned: Int,
    val isRescheduled: Boolean,
    val originalDate: Long,
    val createdAt: Long,
    val isManual: Boolean = false
)

@Entity(tableName = "personal_routines")
data class PersonalRoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userUid: String,
    val name: String,
    val emoji: String,
    val daysOfWeek: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val isFlexible: Boolean,
    val isActive: Boolean
)

@Entity(tableName = "period_settings")
data class PeriodSettingsEntity(
    @PrimaryKey val userUid: String,
    val expectedDayOfMonth: Int,
    val cycleDays: Int,
    val heavyPainDays: Int,
    val heavyDaySchedule: String,
    val isEnabled: Boolean
)

@Entity(tableName = "morning_alarms")
data class MorningAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userUid: String,
    val hour: Int,
    val minute: Int,
    val customMessage: String,
    val musicUrl: String,
    val wakeVerificationType: String,
    val daysOfWeek: String,
    val isEnabled: Boolean
)

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey val userUid: String,
    val currentDailyStreak: Int,
    val longestDailyStreak: Int,
    val currentWeeklyStreak: Int,
    val longestWeeklyStreak: Int,
    val todaySessionsCompleted: Int,
    val todaySessionsTotal: Int,
    val lastUpdated: Long
)

@Entity(tableName = "break_settings")
data class BreakSettingsEntity(
    @PrimaryKey val userUid: String,
    val studyMinutes: Int,
    val breakMinutes: Int,
    val longBreakAfterSessions: Int,
    val longBreakMinutes: Int
)

@Entity(tableName = "vision_board")
data class VisionBoardEntity(
    @PrimaryKey val userUid: String,
    val photoUri: String,
    val dreamPost: String,
    val inspirationalImageUri: String,
    val updatedAt: Long
)
@Entity(tableName = "reflections")
data class ReflectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userUid: String,
    val type: String,            // "NIGHT" ya "WEEKLY"
    val dateKey: String,         // night: "2026-05-30", weekly: "2026-W22"
    val mood: Int,               // 1..5 (sirf night), weekly me 0
    val wentWell: String,
    val toImprove: String,
    val tomorrowIntention: String,
    val studyMinutes: Int,
    val completionPercent: Int,
    val createdAt: Long,
)