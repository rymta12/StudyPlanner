package com.studyplanner.app.feature.session

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.feature.session.compo.BreakScreen
import com.studyplanner.app.feature.session.compo.CompleteScreen
import com.studyplanner.app.feature.session.compo.PreConfirmScreen
import com.studyplanner.app.feature.session.compo.StudyScreen
import com.studyplanner.app.feature.session.compo.TimeUpDialog
import com.studyplanner.app.feature.session.compo.VisionFlashScreen
import com.studyplanner.app.ui.components.CelebrationOverlay
import com.studyplanner.app.ui.components.MilestoneToast

@Composable
fun SessionScreen(
    onComplete: () -> Unit,
    onNightReflection: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val antiCheatState by viewModel.antiCheatState.collectAsStateWithLifecycle()

    androidx.activity.compose.BackHandler(enabled = true) {
    }

    LaunchedEffect(state.phase) {
        if (state.phase == SessionPhase.STUDY) {
            while (true) {
                kotlinx.coroutines.delay(60_000)
                viewModel.checkPrompt()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { viewModel.onUserTouch() }
            }
    ) {
        AnimatedContent(
            targetState = state.phase,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "phase"
        ) { phase ->
            when (phase) {
                SessionPhase.PRE_CONFIRM -> PreConfirmScreen(
                    state = state,
                    onStart = { viewModel.startSession() },
                    onUpdate = { sMin, bMin, music, all ->
                        viewModel.updatePreConfirm(sMin, bMin, music, all)
                    },
                    onEmotionChange = { viewModel.updateEmotion(it) },
                    onTrackSelected = { viewModel.selectTrack(it) }
                )

                SessionPhase.VISION_FLASH -> VisionFlashScreen(state = state)
                SessionPhase.STUDY -> {
                    val musicState by viewModel.musicState.collectAsStateWithLifecycle()
                    StudyScreen(
                        state = state,
                        onMarkComplete = { viewModel.markComplete() },
                        onExtend = { viewModel.extend(it) },
                        onPushToNext = { viewModel.pushToNextSlot() },
                        onDismissExtension = { viewModel.dismissExtensionDialog() },
                        onStopMusic = { viewModel.stopMusic() },
                        isMusicPlaying = musicState.isPlaying
                    )
                }

                SessionPhase.BREAK -> BreakScreen(
                    state = state,
                    onSkipBreak = { viewModel.skipBreak() })

                SessionPhase.COMPLETE -> CompleteScreen(
                    state = state,
                    onDismissMilestone = { viewModel.dismissMilestone() },
                    onDone = onComplete,
                    onNightReflection = onNightReflection
                )
            }
        }

        // Overlays — AnimatedContent ke BAHAR, Box ke andar (correct placement)
        AntiCheatPromptDialog(
            state = antiCheatState,
            onAnswered = { viewModel.onPromptAnswered(it) },
            onMissed = { viewModel.onPromptMissed() }
        )
        if (state.showTimeUpDialog) {
            TimeUpDialog(
                onComplete = { viewModel.markComplete() },
                onExtend5 = { viewModel.extend(5) },
                onExtend10 = { viewModel.extend(10) },
                onBreak = { viewModel.startBreakManually() },
                onNextSlot = { viewModel.pushToNextSlot() }
            )
        }
        // Celebration overlay — session complete hone par
        CelebrationOverlay(
            show = state.phase == SessionPhase.COMPLETE && state.pointsEarned > 0 && !state.celebrationDismissed,
            emoji = "🎉",
            title = "Session Complete!",
            subtitle = "+${state.pointsEarned} points earned\n${state.pomodoroCount} pomodoros done 🍅",
            onDismiss = { viewModel.dismissCelebration() }
        )
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            state.streakMilestone?.let { milestone ->
                MilestoneToast(
                    show = true,
                    emoji = milestone.emoji,
                    message = milestone.message,
                    onDismiss = { viewModel.dismissMilestone() }
                )
            }
        }
    }
}
