package com.studyplanner.app.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class ProgressPeriod { TODAY, WEEK, MONTH, ALL_TIME }

data class SubjectProgress(
    val subject: SubjectEntity,
    val chapters: List<ChapterProgress>,
    val completionPercent: Float,
    val totalMinutes: Int,
    val completedMinutes: Int,
)

data class ChapterProgress(
    val chapter: ChapterEntity,
    val topics: List<TopicEntity>,
    val completionPercent: Float,
)

data class ProgressUiState(
    val period: ProgressPeriod = ProgressPeriod.TODAY,
    val totalSessions: Int = 0,
    val completedSessions: Int = 0,
    val missedSessions: Int = 0,
    val totalStudyMinutes: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalPoints: Int = 0,
    val overallPercent: Float = 0f,
    val subjectProgress: List<SubjectProgress> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val sessionDao: SessionDao,
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val topicDao: TopicDao,
    private val streakDao: StreakDao,
    private val userDao: UserDao,
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid ?: ""
    private val _period = MutableStateFlow(ProgressPeriod.TODAY)
    private val _state = MutableStateFlow(ProgressUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun setPeriod(period: ProgressPeriod) {
        _period.value = period
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val currentUid = auth.currentUser?.uid ?: return@launch
            _state.update { it.copy(isLoading = true) }

            val (from, to) = periodRange(_period.value)
            val sessions = mutableListOf<SessionEntity>()
            sessionDao.observeByRange(currentUid, from, to).first().let { sessions.addAll(it) }

            val streak = streakDao.get(currentUid)
            val user = userDao.get(currentUid)
            val subjects = subjectDao.getAll(currentUid)

            val subjectProgressList = subjects.map { subject ->
                val chapters = chapterDao.getBySubject(subject.id)
                val chapterProgressList = chapters.map { chapter ->
                    val topics = topicDao.getByChapter(chapter.id)
                    val completedTopics = topics.count { it.status == "COMPLETED" }
                    ChapterProgress(
                        chapter = chapter,
                        topics = topics,
                        completionPercent = if (topics.isEmpty()) 0f
                        else completedTopics.toFloat() / topics.size
                    )
                }
                val subjectComplete = if (subject.estimatedTotalMinutes == 0) 0f
                else subject.completedMinutes.toFloat() / subject.estimatedTotalMinutes
                SubjectProgress(
                    subject = subject,
                    chapters = chapterProgressList,
                    completionPercent = subjectComplete,
                    totalMinutes = subject.estimatedTotalMinutes,
                    completedMinutes = subject.completedMinutes,
                )
            }

            val totalMin = subjects.sumOf { it.estimatedTotalMinutes }
            val completedMin = subjects.sumOf { it.completedMinutes }
            val overall = if (totalMin == 0) 0f else completedMin.toFloat() / totalMin

            _state.value = ProgressUiState(
                period = _period.value,
                totalSessions = sessions.size,
                completedSessions = sessions.count { it.status == "COMPLETED" },
                missedSessions = sessions.count { it.status == "MISSED" },
                totalStudyMinutes = sessions.filter { it.status == "COMPLETED" }
                    .sumOf { it.studyMinutes },
                currentStreak = streak?.currentDailyStreak ?: 0,
                longestStreak = streak?.longestDailyStreak ?: 0,
                totalPoints = user?.points ?: 0,
                overallPercent = overall,
                subjectProgress = subjectProgressList,
                isLoading = false,
            )
        }
    }

    private fun periodRange(period: ProgressPeriod): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        return when (period) {
            ProgressPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to now
            }
            ProgressPeriod.WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis to now
            }
            ProgressPeriod.MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.timeInMillis to now
            }
            ProgressPeriod.ALL_TIME -> 0L to now
        }
    }
}
