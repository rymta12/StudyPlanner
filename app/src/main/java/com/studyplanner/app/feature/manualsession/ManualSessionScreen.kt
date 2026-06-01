package com.studyplanner.app.feature.manualsession

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Calendar

private val DAYS = listOf(
    Calendar.MONDAY to "Mon", Calendar.TUESDAY to "Tue",
    Calendar.WEDNESDAY to "Wed", Calendar.THURSDAY to "Thu",
    Calendar.FRIDAY to "Fri", Calendar.SATURDAY to "Sat", Calendar.SUNDAY to "Sun",
)

@Composable
fun ManualSessionScreen(
    onBack: () -> Unit,
    viewModel: ManualSessionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config = state.config

    // Conflict dialog
    if (state.sessionState is ManualSessionState.ConflictFound) {
        ConflictDialog(
            conflict = (state.sessionState as ManualSessionState.ConflictFound).conflict,
            onAcceptSuggested = { s, e -> viewModel.acceptSuggestedSlot(s, e) },
            onRescheduleConflict = { s, e -> viewModel.rescheduleConflict(s, e) },
            onDismiss = { viewModel.dismissConflict() }
        )
    }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF0A1628), Color(0xFF1A237E))))) {

        when (val ss = state.sessionState) {
            is ManualSessionState.Running -> RunningTimerView(
                title = config.title,
                remainingMs = ss.remainingMs,
                totalMs = ss.totalMs,
                onStop = { viewModel.stop() }
            )
            is ManualSessionState.Done -> DoneView(onBack = onBack)
            else -> ConfigView(
                config = config,
                isChecking = state.isChecking,
                isScheduled = state.sessionState is ManualSessionState.Scheduled,
                onUpdate = { viewModel.updateConfig(it) },
                onStartNow = { viewModel.startNow() },
                onSchedule = { viewModel.checkAndSchedule() },
                onBack = onBack
            )
        }
    }
}

@Composable
private fun ConfigView(
    config: ManualSessionConfig,
    isChecking: Boolean,
    isScheduled: Boolean,
    onUpdate: (ManualSessionConfig) -> Unit,
    onStartNow: () -> Unit,
    onSchedule: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Column {
                Text("Extra Study Session", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("Timetable se alag — conflict check hoga",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
            }
        }

        // Title
        OutlinedTextField(
            value = config.title,
            onValueChange = { onUpdate(config.copy(title = it)) },
            label = { Text("Session Name", color = Color.White.copy(0.7f)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(0.3f),
            )
        )

        // Time picker card
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)),
            shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("⏰ Kab se kab tak?", color = Color.White, fontWeight = FontWeight.SemiBold)

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start", color = Color.White.copy(0.7f),
                            style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(4.dp))
                        TimePickerRow(
                            hour = config.startHour,
                            minute = config.startMinute,
                            onHourChange = { onUpdate(config.copy(startHour = it)) },
                            onMinuteChange = { onUpdate(config.copy(startMinute = it)) }
                        )
                    }
                    Text("→", color = Color.White, fontSize = 20.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End", color = Color.White.copy(0.7f),
                            style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(4.dp))
                        TimePickerRow(
                            hour = config.endHour,
                            minute = config.endMinute,
                            onHourChange = { onUpdate(config.copy(endHour = it)) },
                            onMinuteChange = { onUpdate(config.copy(endMinute = it)) }
                        )
                    }
                }

                // Duration display
                val durMin = (config.endHour * 60 + config.endMinute) - (config.startHour * 60 + config.startMinute)
                if (durMin > 0) {
                    Text("Duration: ${durMin / 60}h ${durMin % 60}m",
                        color = Color.White.copy(0.7f),
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("⚠️ End time start se baad hona chahiye",
                        color = Color(0xFFFFAB40), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Schedule type
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)),
            shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("📅 Kitne din?", color = Color.White, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ManualScheduleType.entries.forEach { type ->
                        FilterChip(
                            selected = config.scheduleType == type,
                            onClick = { onUpdate(config.copy(scheduleType = type)) },
                            label = { Text(if (type == ManualScheduleType.DAILY) "Rozana" else "Custom Days") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White.copy(0.3f),
                                selectedLabelColor = Color.White, labelColor = Color.White.copy(0.6f))
                        )
                    }
                }
                if (config.scheduleType == ManualScheduleType.CUSTOM) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DAYS.forEach { (dayInt, label) ->
                            val sel = config.customDays.contains(dayInt)
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(if (sel) Color.White.copy(0.4f) else Color.White.copy(0.08f))
                                    .clickable {
                                        val days = if (sel) config.customDays - dayInt else config.customDays + dayInt
                                        onUpdate(config.copy(customDays = days))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label.first().toString(), color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }

        // Notify before
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f)),
            shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔔 Pehle notify karo", color = Color.White, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 5, 10, 15).forEach { min ->
                        FilterChip(
                            selected = config.notifyMinutesBefore == min,
                            onClick = { onUpdate(config.copy(notifyMinutesBefore = min)) },
                            label = { Text("${min}m pehle") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White.copy(0.3f),
                                selectedLabelColor = Color.White, labelColor = Color.White.copy(0.6f))
                        )
                    }
                }
            }
        }

        if (isScheduled) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(0.5f)),
                shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF81C784))
                    Text("Scheduled! Timetable mein dikhega.", color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        val durMin = (config.endHour * 60 + config.endMinute) - (config.startHour * 60 + config.startMinute)

        Button(
            onClick = onStartNow,
            enabled = durMin > 0,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF1565C0))
            Spacer(Modifier.width(8.dp))
            Text("Abhi Start Karo", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onSchedule,
            enabled = durMin > 0 && !isChecking,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp),
                    color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Conflict check ho raha hai...")
            } else {
                Icon(Icons.Default.Schedule, null)
                Spacer(Modifier.width(8.dp))
                Text("Schedule karo (timetable mein add)")
            }
        }
    }
}

@Composable
private fun TimePickerRow(
    hour: Int, minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(0.12f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Hour
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { onHourChange((hour + 1).coerceAtMost(23)) },
                modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text(hour.toString().padStart(2, '0'), color = Color.White,
                fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { onHourChange((hour - 1).coerceAtLeast(0)) },
                modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Text(":", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        // Minute
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { onMinuteChange(if (minute >= 45) 0 else minute + 15) },
                modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text(minute.toString().padStart(2, '0'), color = Color.White,
                fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { onMinuteChange(if (minute == 0) 45 else minute - 15) },
                modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ConflictDialog(
    conflict: ConflictInfo,
    onAcceptSuggested: (Int, Int) -> Unit,
    onRescheduleConflict: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚠️", fontSize = 20.sp)
                Text("Time Conflict!", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Is time pe already '${conflict.subjectName}' ka session hai.")
                Text("Kya karna chahoge?", fontWeight = FontWeight.SemiBold)

                if (conflict.suggestedSlots.isNotEmpty()) {
                    Text("Available slots aaj ke liye:", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    conflict.suggestedSlots.take(3).forEach { (s, e) ->
                        OutlinedButton(
                            onClick = { onAcceptSuggested(s, e) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📚 Extra session ${s}:00 – ${e}:00 pe karo")
                        }
                    }
                }

                HorizontalDivider()

                if (conflict.suggestedSlots.isNotEmpty()) {
                    val slot = conflict.suggestedSlots.first()
                    OutlinedButton(
                        onClick = { onRescheduleConflict(slot.first, slot.second) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🔄 Study session ko ${slot.first}:00 pe shift karo")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RunningTimerView(
    title: String, remainingMs: Long, totalMs: Long, onStop: () -> Unit
) {
    val progress = if (totalMs > 0) 1f - remainingMs.toFloat() / totalMs else 0f
    val min = remainingMs / 60000
    val sec = (remainingMs % 60000) / 1000
    val pulse by rememberInfiniteTransition(label = "p").animateFloat(
        1f, 1.05f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "ps"
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)) {
            Text("📚", fontSize = (72 * pulse).sp)
            Text(title, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp, color = Color.White, trackColor = Color.White.copy(0.15f))
                Text("$min:${sec.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Text("Notification mein bhi dikh raha hai",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.5f))
            TextButton(onClick = onStop) {
                Icon(Icons.Default.Stop, null, tint = Color.White.copy(0.5f))
                Spacer(Modifier.width(8.dp))
                Text("Band karo", color = Color.White.copy(0.5f))
            }
        }
    }
}

@Composable
private fun DoneView(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Text("🎉", fontSize = 72.sp)
            Text("Extra session complete!", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Button(onClick = onBack, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                Text("Back to Home", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
            }
        }
    }
}