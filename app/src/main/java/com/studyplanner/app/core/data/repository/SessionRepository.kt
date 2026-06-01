package com.studyplanner.app.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.SessionEntity
import com.studyplanner.app.core.util.ReschedulingEngine
import com.studyplanner.app.core.util.StreakManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val reschedulingEngine: ReschedulingEngine,
    private val streakManager: StreakManager,
) {
    private val uid get() = auth.currentUser?.uid ?: ""

    fun observeToday(): Flow<List<SessionEntity>> {
        val today = startOfDay(System.currentTimeMillis())
        return sessionDao.observeByDate(uid, today)
    }

    fun observeByRange(from: Long, to: Long): Flow<List<SessionEntity>> =
        sessionDao.observeByRange(uid, from, to)

    suspend fun getById(id: Long): SessionEntity? = sessionDao.getById(id)

    suspend fun startSession(id: Long, emotionBefore: String, studyMinutes: Int, breakMinutes: Int, musicUrl: String): SessionEntity? {
        // GUARD: Pehle se koi ONGOING session hai (aur ye nahi) to use MISSED kar do
        val existingOngoing = sessionDao.getOngoing(uid)
        if (existingOngoing != null && existingOngoing.id != id) {
            sessionDao.updateStatus(existingOngoing.id, "UPCOMING")
        }
        val session = sessionDao.getById(id) ?: return null
        val updated = session.copy(
            actualStartTime = System.currentTimeMillis(),
            status = "ONGOING",
            emotionBefore = emotionBefore,
            studyMinutes = studyMinutes,
            breakMinutes = breakMinutes,
            backgroundMusicUrl = musicUrl,
        )
        sessionDao.update(updated)
        runCatching {
            firestore.collection("users").document(uid)
                .update(mapOf(
                    "currentSessionStatus" to "ONGOING",
                    "currentTopic" to "Session #$id",
                    "lastActive" to System.currentTimeMillis()
                )).await()
        }
        return updated
    }

    suspend fun completeSession(id: Long): SessionEntity? {
        val session = sessionDao.getById(id) ?: return null
        val now = System.currentTimeMillis()
        val isEarly = now < session.scheduledEndTime - 2 * 60 * 1000
        val points = if (isEarly) 15 else 10
        val updated = session.copy(
            actualEndTime = now, status = "COMPLETED", pointsEarned = points)
        sessionDao.update(updated)
        streakManager.onSessionComplete(uid)
        val today = startOfDay(now)
        val todaySessions = mutableListOf<com.studyplanner.app.core.data.local.entity.SessionEntity>()
        sessionDao.observeByDate(uid, today).collect { todaySessions.addAll(it); return@collect }
        val completed = todaySessions.count { it.status == "COMPLETED" }
        runCatching {
            firestore.collection("users").document(uid)
                .update(mapOf(
                    "currentSessionStatus" to "COMPLETED",
                    "currentTopic" to "",
                    "todayCompleted" to completed,
                    "todayTotal" to todaySessions.size,
                    "lastActive" to now
                )).await()
        }
        return updated
    }

    suspend fun extendSession(id: Long, extraMinutes: Int): SessionEntity? {
        val session = sessionDao.getById(id) ?: return null
        val updated = session.copy(
            scheduledEndTime = session.scheduledEndTime + extraMinutes * 60 * 1000L,
            studyMinutes = session.studyMinutes + extraMinutes,
            extensionCount = session.extensionCount + 1,
            extensionMinutes = session.extensionMinutes + extraMinutes,
        )
        sessionDao.update(updated)
        return updated
    }

    suspend fun pushToNextSlot(id: Long) {
        val session = sessionDao.getById(id) ?: return
        sessionDao.updateStatus(id, "MISSED")
        reschedulingEngine.reschedule(uid, session)
    }

    suspend fun missSession(id: Long) {
        val session = sessionDao.getById(id) ?: return
        sessionDao.updateStatus(id, "MISSED")
        streakManager.onSessionMissed(uid)
        reschedulingEngine.reschedule(uid, session)
    }

    suspend fun recordSelfie(id: Long, selfieUrl: String) {
        val session = sessionDao.getById(id) ?: return
        sessionDao.update(session.copy(selfieUrl = selfieUrl))
    }

    suspend fun recordAppUsage(id: Long, appLog: String) {
        val session = sessionDao.getById(id) ?: return
        sessionDao.update(session.copy(appUsageLog = appLog))
    }

    suspend fun recordPromptAnswer(id: Long, answered: Boolean) {
        val session = sessionDao.getById(id) ?: return
        val newAnswered = if (answered) session.promptsAnswered + 1 else session.promptsAnswered
        val newTotal = session.promptsTotal + 1
        val score = ((newAnswered.toFloat() / newTotal) * 100).toInt()
        sessionDao.update(session.copy(
            promptsAnswered = newAnswered,
            promptsTotal = newTotal,
            authenticityScore = score
        ))
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