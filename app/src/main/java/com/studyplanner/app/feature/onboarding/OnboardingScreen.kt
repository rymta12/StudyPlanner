package com.studyplanner.app.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.studyplanner.app.core.navigation.Route
import com.studyplanner.app.ui.components.AnimatedBackground

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val ocrResult = navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<List<ChapterDraft>?>("ocr_result", null)
        ?.collectAsStateWithLifecycle()

    LaunchedEffect(ocrResult?.value) {
        ocrResult?.value?.let { chapters ->
            viewModel.addOcrChapters(chapters) // ViewModel mein add karo
            navController.currentBackStackEntry
                ?.savedStateHandle?.remove<List<ChapterDraft>>("ocr_result")
        }
    }

    BackHandler(enabled = state.currentStep > 0) { viewModel.prevStep() }

    AnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            OnboardingTopBar(
                currentStep = state.currentStep,
                totalSteps = state.totalSteps,
                onBack = { viewModel.prevStep() }
            )
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    else
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                },
                label = "step"
            ) { step ->
                when (step) {
                    0 -> StepProfile(state = state, onNext = { name, nickname, gender, city, st, pincode ->
                        viewModel.updateProfile(name, nickname, gender, city, st, pincode); viewModel.nextStep()
                    })
                    1 -> StepExamType(state = state, onNext = { examType, sub ->
                        viewModel.updateExam(examType, sub); viewModel.nextStep()
                    })
                    2 -> StepStudyMaterial(state = state, onNext = { materials ->
                        viewModel.updateStudyMaterials(materials); viewModel.nextStep()
                    })
                    3 -> StepSubjects(
                        state = state, viewModel = viewModel, onNext = { viewModel.nextStep() },
                        onScanRequest = {
                            navController.navigate(
                                Route.OcrScan.path
                            )
                        },
                        ocrResult = ocrResult?.value,
                        onClearOcrResult = {
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("ocr_result", null)
                        },
                    )
                    4 -> StepCurrentAffairs(state = state, onNext = { freq, dur ->
                        viewModel.updateCurrentAffairs(freq, dur); viewModel.nextStep()
                    })
                    5 -> StepRevision(state = state, onNext = { style ->
                        viewModel.updateRevision(style); viewModel.nextStep()
                    })
                    6 -> StepDeadline(state = state, onNext = { date ->
                        viewModel.updateDeadline(date); viewModel.nextStep()
                    })
                    7 -> StepDailySchedule(state = state, onNext = { hours, slots ->
                        viewModel.updateDailySchedule(hours, slots); viewModel.nextStep()
                    })
                    8 -> StepBreakSettings(state = state, onNext = { sMin, bMin, lAfter, lMin ->
                        viewModel.updateBreakSettings(sMin, bMin, lAfter, lMin); viewModel.nextStep()
                    })
                    9 -> StepPersonalRoutine(state = state, onNext = { routines ->
                        viewModel.updatePersonalRoutines(routines); viewModel.nextStep()
                    })
                    10 -> StepPeriodTracking(state = state, onNext = { en, day, cyc, heavy, sched ->
                        viewModel.updatePeriodSettings(en, day, cyc, heavy, sched); viewModel.nextStep()
                    })
                    11 -> StepFocusSettings(state = state, onNext = { mode, channels ->
                        viewModel.updateFocusSettings(mode, channels); viewModel.nextStep()
                    })
                    12 -> StepMorningAlarm(state = state, onNext = { h, m, msg, music, wakeType, days ->
                        viewModel.updateMorningAlarm(h, m, msg, music, wakeType, days); viewModel.nextStep()
                    })
                    13 -> StepVisionBoard(state = state, isLoading = state.isLoading, onComplete = { photo, dream, img ->
                        viewModel.updateVisionBoard(photo, dream, img)
                        viewModel.completeOnboarding(onComplete)
                    })
                }
            }
        }
    }
}

@Composable
private fun OnboardingTopBar(currentStep: Int, totalSteps: Int, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 0) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            } else {
                Spacer(Modifier.size(36.dp))
            }
            LinearProgressIndicator(
                progress = { (currentStep + 1f) / totalSteps },
                modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            Text("${currentStep + 1}/$totalSteps", style = MaterialTheme.typography.labelMedium,
                color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(onboardingStepTitle(currentStep), style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f))
    }
}

private fun onboardingStepTitle(step: Int) = when (step) {
    0 -> "Your Profile"; 1 -> "Exam Selection"; 2 -> "Study Material"
    3 -> "Subjects & Syllabus"; 4 -> "Current Affairs"; 5 -> "Revision Settings"
    6 -> "Set Your Deadline"; 7 -> "Daily Schedule"; 8 -> "Break Settings"
    9 -> "Personal Routine"; 10 -> "Period Tracking"; 11 -> "Focus & Blocking"
    12 -> "Morning Alarm"; 13 -> "Vision Board"
    else -> ""
}
