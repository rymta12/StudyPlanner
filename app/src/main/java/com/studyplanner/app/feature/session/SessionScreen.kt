package com.studyplanner.app.feature.session

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.studyplanner.app.core.util.AntiCheatState
import com.studyplanner.app.ui.components.CelebrationOverlay
import com.studyplanner.app.ui.components.MilestoneToast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.ui.components.AnimatedBackground

@Composable
fun SessionScreen(
    onComplete: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val antiCheatState by viewModel.antiCheatState.collectAsStateWithLifecycle()

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
                    onEmotionChange = { viewModel.updateEmotion(it) }
                )

                SessionPhase.VISION_FLASH -> VisionFlashScreen(state = state)
                SessionPhase.STUDY -> StudyScreen(
                    state = state,
                    onMarkComplete = { viewModel.markComplete() },
                    onExtend = { viewModel.extend(it) },
                    onPushToNext = { viewModel.pushToNextSlot() },
                    onDismissExtension = { viewModel.dismissExtensionDialog() }
                )

                SessionPhase.BREAK -> BreakScreen(
                    state = state,
                    onSkipBreak = { viewModel.markComplete() })

                SessionPhase.COMPLETE -> CompleteScreen(
                    state = state,
                    onDismissMilestone = { viewModel.dismissMilestone() },
                    onDone = onComplete
                )

                SessionPhase.EXTENSION_DIALOG -> StudyScreen(
                    state = state,
                    onMarkComplete = { viewModel.markComplete() },
                    onExtend = { viewModel.extend(it) },
                    onPushToNext = { viewModel.pushToNextSlot() },
                    onDismissExtension = { viewModel.dismissExtensionDialog() }
                )
            }
            AntiCheatPromptDialog(
                state = antiCheatState,
                onAnswered = { viewModel.onPromptAnswered(it) },
                onMissed = { viewModel.onPromptMissed() }
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
}
    @Composable
    private fun PreConfirmScreen(
        state: SessionUiState,
        onStart: () -> Unit,
        onUpdate: (Int, Int, String, Boolean) -> Unit,
        onEmotionChange: (String) -> Unit
    ) {
        var studyMin by remember { mutableIntStateOf(state.studyMinutes) }
        var breakMin by remember { mutableIntStateOf(state.breakMinutes) }
        var musicUrl by remember { mutableStateOf(state.musicUrl) }
        var applyAll by remember { mutableStateOf(false) }

        AnimatedBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .systemBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Ready to study? 📚", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = Color.White
                    )
                    Text(
                        state.topicName, style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        state.subjectName, style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "How are you feeling?", style = MaterialTheme.typography.titleSmall,
                            color = Color.White, fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "😴" to "TIRED",
                                "😰" to "STRESSED",
                                "😊" to "GOOD",
                                "🔥" to "FOCUSED"
                            ).forEach { (emoji, id) ->
                                val selected = state.emotion == id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) Color.White.copy(0.3f) else Color.White.copy(
                                                0.08f
                                            )
                                        )
                                        .then(if (selected) Modifier else Modifier)
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(emoji, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Session Settings", style = MaterialTheme.typography.titleSmall,
                            color = Color.White, fontWeight = FontWeight.SemiBold
                        )

                        SessionStepperRow("Study", studyMin, "min", 25, 90, 5) {
                            studyMin = it; onUpdate(studyMin, breakMin, musicUrl, applyAll)
                        }
                        SessionStepperRow("Break", breakMin, "min", 5, 30, 5) {
                            breakMin = it; onUpdate(studyMin, breakMin, musicUrl, applyAll)
                        }

                        OutlinedTextField(
                            value = musicUrl,
                            onValueChange = {
                                musicUrl = it; onUpdate(
                                studyMin,
                                breakMin,
                                musicUrl,
                                applyAll
                            )
                            },
                            label = {
                                Text(
                                    "Background Music URL",
                                    color = Color.White.copy(0.7f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.MusicNote,
                                    null,
                                    tint = Color.White.copy(0.5f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(0.3f),
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Apply to all today's sessions", color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(checked = applyAll, onCheckedChange = {
                                applyAll = it; onUpdate(studyMin, breakMin, musicUrl, applyAll)
                            })
                        }
                    }
                }

                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Icon(
                        Icons.Default.PlayArrow, null,
                        tint = Color(0xFF1565C0), modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Start Session",
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    @Composable
    private fun VisionFlashScreen(state: SessionUiState) {
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "visionScale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(Color(0xFF1565C0), Color(0xFF060B18)))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.scale(scale).padding(32.dp)
            ) {
                Text("🎯", fontSize = 80.sp)
                Text(
                    "Yaad hai kyun padh raha hai?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White.copy(0.7f),
                    textAlign = TextAlign.Center
                )
                if (state.visionBoardDream.isNotBlank()) {
                    Text(
                        state.visionBoardDream,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    "Starting in a moment...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.4f)
                )
            }
        }
    }

    @Composable
    private fun StudyScreen(
        state: SessionUiState,
        onMarkComplete: () -> Unit,
        onExtend: (Int) -> Unit,
        onPushToNext: () -> Unit,
        onDismissExtension: () -> Unit
    ) {
        val progress = if (state.timerTotalMs > 0) {
            1f - (state.timerRemainingMs.toFloat() / state.timerTotalMs)
        } else 0f

        val remainingMin = state.timerRemainingMs / 60000
        val remainingSec = (state.timerRemainingMs % 60000) / 1000
        val is2MinAlert = state.timerRemainingMs in 1..(2 * 60 * 1000)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060B18))
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            state.subjectName, style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(0.5f)
                        )
                        Text(
                            state.topicName, style = MaterialTheme.typography.titleMedium,
                            color = Color.White, fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            null,
                            tint = Color(0xFFFF6D00),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "${state.pomodoroCount}",
                            color = Color(0xFFFF6D00),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = if (is2MinAlert) Color(0xFFFF6D00) else Color(0xFF1565C0),
                        trackColor = Color.White.copy(0.08f),
                        strokeWidth = 12.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${
                                remainingMin.toString().padStart(2, '0')
                            }:${remainingSec.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (is2MinAlert) Color(0xFFFF6D00) else Color.White
                        )
                        Text(
                            "🍅 Pomodoro ${state.pomodoroCount + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.5f)
                        )
                    }
                }

                if (is2MinAlert && !state.showExtensionDialog) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF6D00).copy(
                                0.2f
                            )
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "⏰ 2 minutes bache! Topic complete hua?",
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFFFCC80),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onMarkComplete,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Topic Complete ✅", fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onExtend(5) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text("+5 min") }
                        OutlinedButton(
                            onClick = { onExtend(10) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text("+10 min") }
                        OutlinedButton(
                            onClick = onPushToNext,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(
                                    0xFFEF9A9A
                                )
                            )
                        ) { Text("Next slot") }
                    }
                }
            }
        }
    }

    @Composable
    private fun BreakScreen(state: SessionUiState, onSkipBreak: () -> Unit) {
        val remainingMin = state.timerRemainingMs / 60000
        val remainingSec = (state.timerRemainingMs % 60000) / 1000
        val progress = if (state.timerTotalMs > 0) {
            1f - (state.timerRemainingMs.toFloat() / state.timerTotalMs)
        } else 0f
        val is2MinLeft = state.timerRemainingMs in 1..(2 * 60 * 1000)

        val breakActivities = listOf(
            Triple("🫁", "Deep Breathing", "Breathe in 4s, hold 4s, out 4s"),
            Triple("🤸", "Stretching", "Neck, shoulders, back stretch"),
            Triple("👁️", "Eye Exercise", "Look far, blink, rotate eyes"),
            Triple("🚶", "Short Walk", "Walk around for 5 minutes"),
        )
        var selectedActivity by remember { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0D1B0D), Color(0xFF1B5E20))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "☕ Break Time!", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = Color.White
                )

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF81C784),
                        trackColor = Color.White.copy(0.1f),
                        strokeWidth = 10.dp
                    )
                    Text(
                        "${remainingMin.toString().padStart(2, '0')}:${
                            remainingSec.toString().padStart(2, '0')
                        }",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Text(
                    "Break me kya karein?", style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(0.7f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    breakActivities.forEachIndexed { i, activity ->
                        val selected = selectedActivity == i
                        Card(
                            modifier = Modifier.weight(1f),
                            onClick = { selectedActivity = i },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) Color(0xFF2E7D32) else Color.White.copy(
                                    0.08f
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(activity.first, fontSize = 24.sp)
                                Text(
                                    activity.second,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                val instruction = breakActivities[selectedActivity].third
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(breakActivities[selectedActivity].first, fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            instruction,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (is2MinLeft) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1565C0).copy(
                                0.3f
                            )
                        ),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "🎯 Break khatam hone wala hai! Wapas aa jao!",
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFF90CAF9),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                TextButton(onClick = onSkipBreak, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Skip Break",
                        color = Color.White.copy(0.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    @Composable
    private fun CompleteScreen(
        state: SessionUiState,
        onDismissMilestone: () -> Unit,
        onDone: () -> Unit
    ) {
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "completeScale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(Color(0xFF1A237E), Color(0xFF060B18)))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("🎉", fontSize = 72.sp)

                Text(
                    "Session Complete!", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold, color = Color.White
                )

                Text(
                    state.topicName, style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(0.7f), textAlign = TextAlign.Center
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatBadge("⭐", "+${state.pointsEarned}", "Points")
                    StatBadge("🍅", "${state.pomodoroCount}", "Pomodoros")
                }

                state.streakMilestone?.let { milestone ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF6D00).copy(
                                0.2f
                            )
                        ),
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(milestone.emoji, fontSize = 40.sp)
                            Text(
                                milestone.message,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(onClick = onDismissMilestone) {
                                Text("Awesome! 🙌", color = Color(0xFFFFB74D))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text(
                        "Back to Dashboard",
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @Composable
    private fun StatBadge(emoji: String, value: String, label: String) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(emoji, fontSize = 24.sp)
                Text(
                    value, color = Color.White, fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    label,
                    color = Color.White.copy(0.5f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }

    @Composable
    private fun SessionStepperRow(
        label: String, value: Int, unit: String,
        min: Int, max: Int, step: Int,
        onChange: (Int) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label, color = Color.White, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (value > min) onChange(value - step) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    "$value $unit", color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.widthIn(min = 60.dp), textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = { if (value < max) onChange(value + step) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }