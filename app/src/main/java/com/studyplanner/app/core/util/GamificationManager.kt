package com.studyplanner.app.core.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyplanner.app.core.data.local.dao.SessionDao
import com.studyplanner.app.core.data.local.dao.StreakDao
import com.studyplanner.app.core.data.local.dao.UserDao
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class Badge(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val unlockedAt: Long = 0L,
    val isUnlocked: Boolean = false,
)

data class GamificationEvent(
    val type: EventType,
    val points: Int,
    val badge: Badge? = null,
    val streakMilestone: StreakMilestone? = null,
)

enum class EventType {
    SESSION_COMPLETE, SESSION_EARLY, SESSION_MISSED,
    STREAK_MILESTONE, BADGE_UNLOCKED, WEEKLY_TARGET,
    CHAPTER_COMPLETE, SUBJECT_COMPLETE, PERFECT_DAY
}

@Singleton
class GamificationManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val streakDao: StreakDao,
    private val notificationHelper: NotificationHelper,
) {
    companion object {
        const val POINTS_SESSION_COMPLETE = 10
        const val POINTS_SESSION_EARLY = 15
        const val POINTS_SESSION_MISSED = -10
        const val POINTS_WEEKLY_TARGET = 50
        const val POINTS_PERFECT_DAY = 25
        const val POINTS_CHAPTER_COMPLETE = 30
        const val POINTS_SUBJECT_COMPLETE = 100
    }

    val allBadges = listOf(
        Badge("streak_3", "💪", "3 Day Warrior", "3 din lagatar padha"),
        Badge("streak_7", "🗡️", "Week Warrior", "7 din streak"),
        Badge("streak_14", "🦁", "2 Week Beast", "14 din streak"),
        Badge("streak_30", "🌟", "Monthly Legend", "30 din streak"),
        Badge("streak_100", "👑", "UPSC Warrior", "100 din streak"),
        Badge("hours_10", "📚", "Scholar", "10 ghante padha"),
        Badge("hours_50", "🎓", "Dedicated", "50 ghante padha"),
        Badge("hours_100", "🏆", "Century", "100 ghante padha"),
        Badge("hours_500", "🔥", "Iron Will", "500 ghante padha"),
        Badge("perfect_week", "✨", "Perfect Week", "Ek hafte mein koi session miss nahi"),
        Badge("early_bird", "🌅", "Early Bird", "5 sessions time se pehle complete"),
        Badge("chapter_first", "📖", "First Chapter", "Pehla chapter complete"),
        Badge("subject_first", "🎯", "Subject Master", "Pehla subject complete"),
        Badge("debt_clear", "💳", "Debt Free", "Study debt clear kiya"),
        Badge("night_owl", "🦉", "Night Owl", "Raat 10 baje ke baad 5 sessions"),
    )

    suspend fun onSessionComplete(isEarly: Boolean): GamificationEvent {
        val uid = auth.currentUser?.uid ?: return GamificationEvent(EventType.SESSION_COMPLETE, 0)
        val points = if (isEarly) POINTS_SESSION_EARLY else POINTS_SESSION_COMPLETE
        addPoints(uid, points)
        checkAndAwardBadges(uid)
        return GamificationEvent(
            type = if (isEarly) EventType.SESSION_EARLY else EventType.SESSION_COMPLETE,
            points = points
        )
    }

    suspend fun onSessionMissed(): GamificationEvent {
        val uid = auth.currentUser?.uid ?: return GamificationEvent(EventType.SESSION_MISSED, 0)
        addPoints(uid, POINTS_SESSION_MISSED)
        return GamificationEvent(type = EventType.SESSION_MISSED, points = POINTS_SESSION_MISSED)
    }

    suspend fun onPerfectDay(): GamificationEvent {
        val uid = auth.currentUser?.uid ?: return GamificationEvent(EventType.PERFECT_DAY, 0)
        addPoints(uid, POINTS_PERFECT_DAY)
        return GamificationEvent(type = EventType.PERFECT_DAY, points = POINTS_PERFECT_DAY)
    }

    suspend fun onChapterComplete(): GamificationEvent {
        val uid = auth.currentUser?.uid ?: return GamificationEvent(EventType.CHAPTER_COMPLETE, 0)
        addPoints(uid, POINTS_CHAPTER_COMPLETE)
        val badge = checkBadge(uid, "chapter_first")
        return GamificationEvent(type = EventType.CHAPTER_COMPLETE,
            points = POINTS_CHAPTER_COMPLETE, badge = badge)
    }

    suspend fun onSubjectComplete(): GamificationEvent {
        val uid = auth.currentUser?.uid ?: return GamificationEvent(EventType.SUBJECT_COMPLETE, 0)
        addPoints(uid, POINTS_SUBJECT_COMPLETE)
        val badge = checkBadge(uid, "subject_first")
        return GamificationEvent(type = EventType.SUBJECT_COMPLETE,
            points = POINTS_SUBJECT_COMPLETE, badge = badge)
    }

    private suspend fun addPoints(uid: String, points: Int) {
        val user = userDao.get(uid) ?: return
        val newPoints = (user.points + points).coerceAtLeast(0)
        userDao.upsert(user.copy(points = newPoints, updatedAt = System.currentTimeMillis()))
        runCatching {
            firestore.collection("leaderboard").document(uid)
                .set(mapOf(
                    "uid" to uid, "name" to user.name, "points" to newPoints,
                    "city" to user.city, "state" to user.state,
                    "examType" to user.examType, "examSubType" to user.examSubType,
                    "streak" to user.currentStreak, "updatedAt" to System.currentTimeMillis()
                ), com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    private suspend fun checkAndAwardBadges(uid: String) {
        val streak = streakDao.get(uid) ?: return
        val user = userDao.get(uid) ?: return
        val today = startOfDay(System.currentTimeMillis())
        val totalMinutes = mutableListOf<Int>()
        sessionDao.observeByRange(uid, 0L, System.currentTimeMillis())

        when (streak.currentDailyStreak) {
            3 -> checkBadge(uid, "streak_3")
            7 -> checkBadge(uid, "streak_7")
            14 -> checkBadge(uid, "streak_14")
            30 -> checkBadge(uid, "streak_30")
            100 -> checkBadge(uid, "streak_100")
        }
    }

    private suspend fun checkBadge(uid: String, badgeId: String): Badge? {
        val existing = runCatching {
            firestore.collection("users").document(uid)
                .collection("badges").document(badgeId).get().await()
        }.getOrNull()

        if (existing?.exists() == true) return null

        val badge = allBadges.find { it.id == badgeId } ?: return null
        val unlockedBadge = badge.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())

        runCatching {
            firestore.collection("users").document(uid)
                .collection("badges").document(badgeId)
                .set(mapOf("id" to badgeId, "unlockedAt" to System.currentTimeMillis())).await()
        }
        // Badge unlock hone par notification dikhao
        notificationHelper.showAchievement(unlockedBadge.emoji, unlockedBadge.title, unlockedBadge.description)
        return unlockedBadge
    }

    suspend fun getUserBadges(uid: String): List<Badge> {
        val unlockedIds = runCatching {
            firestore.collection("users").document(uid)
                .collection("badges").get().await()
                .documents.map { it.id }
        }.getOrDefault(emptyList())

        return allBadges.map { badge ->
            badge.copy(isUnlocked = badge.id in unlockedIds)
        }
    }

    private fun startOfDay(ms: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
