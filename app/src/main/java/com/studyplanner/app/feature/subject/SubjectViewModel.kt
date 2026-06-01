package com.studyplanner.app.feature.subject

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyplanner.app.core.data.local.dao.*
import com.studyplanner.app.core.data.local.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectDetailUiState(
    val subject: SubjectEntity? = null,
    val chapters: List<ChapterWithTopics> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
)

data class ChapterWithTopics(
    val chapter: ChapterEntity,
    val topics: List<TopicEntity>,
    val completionPercent: Float,
)

@HiltViewModel
class SubjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val topicDao: TopicDao,
) : ViewModel() {

    val subjectId: Long = savedStateHandle["subjectId"] ?: 0L
    private val uid get() = auth.currentUser?.uid ?: ""

    private val _state = MutableStateFlow(SubjectDetailUiState())
    val state = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val subject = subjectDao.getById(subjectId)
            val chapters = chapterDao.getBySubject(subjectId)
            val chaptersWithTopics = chapters.map { chapter ->
                val topics = topicDao.getByChapter(chapter.id)
                val completed = topics.count { it.status == "COMPLETED" }
                ChapterWithTopics(
                    chapter = chapter,
                    topics = topics,
                    completionPercent = if (topics.isEmpty()) 0f
                    else completed.toFloat() / topics.size
                )
            }
            _state.update {
                it.copy(subject = subject, chapters = chaptersWithTopics, isLoading = false)
            }
        }
    }

    fun saveSubject(
        name: String, colorHex: String, priority: String,
        totalPages: Int, readingSpeedMinPerPage: Float,
        totalVideoMinutes: Int,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val current = _state.value.subject ?: return@launch
            val estimated = (totalPages * readingSpeedMinPerPage).toInt() + totalVideoMinutes
            subjectDao.upsert(current.copy(
                name = name, colorHex = colorHex, priority = priority,
                totalPages = totalPages, readingSpeedMinPerPage = readingSpeedMinPerPage,
                totalVideoMinutes = totalVideoMinutes, estimatedTotalMinutes = estimated,
            ))
            _state.update { it.copy(isSaving = false, saveSuccess = true) }
            load()
        }
    }

    fun addChapter(name: String, pageStart: Int, pageEnd: Int) {
        viewModelScope.launch {
            val pages = (pageEnd - pageStart + 1).coerceAtLeast(1)
            val subject = _state.value.subject ?: return@launch
            val estMin = (pages * subject.readingSpeedMinPerPage).toInt()
            chapterDao.upsert(ChapterEntity(
                subjectId = subjectId,
                name = name,
                pageStart = pageStart,
                pageEnd = pageEnd,
                totalPages = pages,
                estimatedMinutes = estMin,
                completedMinutes = 0,
                status = "PENDING",
                orderIndex = _state.value.chapters.size,
            ))
            load()
        }
    }

    fun updateChapter(chapter: ChapterEntity, name: String, pageStart: Int, pageEnd: Int) {
        viewModelScope.launch {
            val pages = (pageEnd - pageStart + 1).coerceAtLeast(1)
            val subject = _state.value.subject ?: return@launch
            chapterDao.upsert(chapter.copy(
                name = name, pageStart = pageStart, pageEnd = pageEnd,
                totalPages = pages,
                estimatedMinutes = (pages * subject.readingSpeedMinPerPage).toInt(),
            ))
            load()
        }
    }

    fun deleteChapter(chapter: ChapterEntity) {
        viewModelScope.launch {
            chapterDao.delete(chapter)
            load()
        }
    }

    fun markTopicComplete(topic: TopicEntity) {
        viewModelScope.launch {
            topicDao.update(topic.copy(
                status = if (topic.status == "COMPLETED") "PENDING" else "COMPLETED",
                lastStudiedAt = System.currentTimeMillis()
            ))
            load()
        }
    }

    fun clearSaveSuccess() = _state.update { it.copy(saveSuccess = false) }
}
