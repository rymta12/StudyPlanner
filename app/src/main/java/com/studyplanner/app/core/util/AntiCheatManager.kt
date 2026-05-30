package com.studyplanner.app.core.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class AntiCheatState(
    val showPrompt: Boolean = false,
    val promptType: PromptType = PromptType.TAP,
    val mathQuestion: String = "",
    val mathAnswer: Int = 0,
    val promptsAnswered: Int = 0,
    val promptsMissed: Int = 0,
    val lastTouchTime: Long = 0L,
    val authenticityScore: Int = 100,
)

enum class PromptType { TAP, MATH, COMPREHENSION }

@Singleton
class AntiCheatManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _state = MutableStateFlow(AntiCheatState())
    val state = _state.asStateFlow()

    private var sessionStartTime = 0L
    private var lastPromptTime = 0L
    private val promptIntervalMs = 8 * 60 * 1000L

    fun onSessionStart() {
        sessionStartTime = System.currentTimeMillis()
        lastPromptTime = sessionStartTime
        _state.value = AntiCheatState()
    }

    fun onUserTouch() {
        _state.update { it.copy(lastTouchTime = System.currentTimeMillis()) }
    }

    fun checkShouldShowPrompt(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastPromptTime < promptIntervalMs) return false
        if (_state.value.showPrompt) return false
        lastPromptTime = now
        showRandomPrompt()
        return true
    }

    private fun showRandomPrompt() {
        val type = PromptType.entries.random()
        val (question, answer) = if (type == PromptType.MATH) generateMath() else "" to 0
        _state.update {
            it.copy(showPrompt = true, promptType = type,
                mathQuestion = question, mathAnswer = answer)
        }
    }

    fun onPromptAnswered(correct: Boolean) {
        _state.update { s ->
            val answered = s.promptsAnswered + if (correct) 1 else 0
            val missed = s.promptsMissed + if (correct) 0 else 1
            val total = answered + missed
            val score = if (total == 0) 100 else ((answered.toFloat() / total) * 100).toInt()
            s.copy(showPrompt = false, promptsAnswered = answered,
                promptsMissed = missed, authenticityScore = score)
        }
    }

    fun onPromptMissed() {
        _state.update { s ->
            val missed = s.promptsMissed + 1
            val total = s.promptsAnswered + missed
            val score = if (total == 0) 100 else ((s.promptsAnswered.toFloat() / total) * 100).toInt()
            s.copy(showPrompt = false, promptsMissed = missed, authenticityScore = score)
        }
    }

    fun isInactive(): Boolean {
        val s = _state.value
        if (s.lastTouchTime == 0L) return false
        return System.currentTimeMillis() - s.lastTouchTime > 5 * 60 * 1000L
    }

    fun getAuthenticityScore() = _state.value.authenticityScore

    private fun generateMath(): Pair<String, Int> {
        val a = (1..20).random()
        val b = (1..20).random()
        val op = listOf("+", "-", "×").random()
        val answer = when (op) {
            "+" -> a + b
            "-" -> a - b
            else -> a * b
        }
        return "$a $op $b = ?" to answer
    }
}
