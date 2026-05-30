package com.studyplanner.app.feature.session

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.studyplanner.app.core.util.AntiCheatState
import com.studyplanner.app.core.util.PromptType
import kotlinx.coroutines.delay

@Composable
fun AntiCheatPromptDialog(
    state: AntiCheatState,
    onAnswered: (correct: Boolean) -> Unit,
    onMissed: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(60) }
    var mathInput by remember { mutableStateOf("") }

    LaunchedEffect(state.showPrompt) {
        if (!state.showPrompt) return@LaunchedEffect
        timeLeft = 60
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        onMissed()
    }

    if (!state.showPrompt) return

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👀 Still there?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { timeLeft / 60f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 3.dp,
                            color = if (timeLeft > 20) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                        Text("$timeLeft", style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                    }
                }

                when (state.promptType) {
                    PromptType.TAP -> {
                        Text("📚", fontSize = 48.sp)
                        Text("Padh raha hai na? Confirm karo!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface)
                        Button(
                            onClick = { onAnswered(true) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Haan, padh raha hoon! 👍",
                                fontWeight = FontWeight.Bold)
                        }
                    }

                    PromptType.MATH -> {
                        Text("🧮", fontSize = 40.sp)
                        Text(state.mathQuestion,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center)
                        OutlinedTextField(
                            value = mathInput,
                            onValueChange = { mathInput = it },
                            label = { Text("Answer") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                val correct = mathInput.trim().toIntOrNull() == state.mathAnswer
                                onAnswered(correct)
                                mathInput = ""
                            },
                            enabled = mathInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Submit", fontWeight = FontWeight.Bold)
                        }
                    }

                    PromptType.COMPREHENSION -> {
                        Text("💭", fontSize = 40.sp)
                        Text("Abhi kya padh raha tha?",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center)
                        Text("(Prompt confirmation)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                        Button(
                            onClick = { onAnswered(true) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Padh raha hoon ✅", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    "Authenticity: ${state.authenticityScore}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
