package com.studyplanner.app.feature.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyplanner.app.ui.theme.AppThemes
import com.studyplanner.app.ui.theme.BgAnimationStyle
import com.studyplanner.app.ui.theme.StatusDone


@Composable
  fun ThemeSelector(currentThemeId: String, onThemeSelect: (String) -> Unit) {
    Text("App Theme", style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppThemes.all.forEach { theme ->
            val selected = theme.id == currentThemeId
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        if (selected) 2.dp else 0.dp,
                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onThemeSelect(theme.id) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(theme.emoji, fontSize = 20.sp)
                Text(theme.displayName, style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
  fun BgStyleSelector(
    current: BgAnimationStyle,
    onSelect: (BgAnimationStyle) -> Unit
) {
    Text(
        text = "Background Animation",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BgAnimationStyle.entries.forEach { style ->
            val selected = style == current
            FilterChip(
                selected = selected,
                onClick = { onSelect(style) },
                label = {
                    Text(
                        text = "${style.emoji} ${style.displayName}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

@Composable
  fun BreakSettingsEditor(
    settings: com.studyplanner.app.core.data.local.entity.BreakSettingsEntity?,
    onSave: (Int, Int, Int, Int) -> Unit
) {
    var studyMin by remember(settings) { mutableIntStateOf(settings?.studyMinutes ?: 50) }
    var breakMin by remember(settings) { mutableIntStateOf(settings?.breakMinutes ?: 10) }
    var longAfter by remember(settings) { mutableIntStateOf(settings?.longBreakAfterSessions ?: 4) }
    var longMin by remember(settings) { mutableIntStateOf(settings?.longBreakMinutes ?: 20) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsStepperRow("Study", studyMin, "min", 25, 90, 5) { studyMin = it }
        SettingsStepperRow("Short Break", breakMin, "min", 5, 30, 5) { breakMin = it }
        SettingsStepperRow("Long Break After", longAfter, "sessions", 2, 8, 1) { longAfter = it }
        SettingsStepperRow("Long Break", longMin, "min", 15, 60, 5) { longMin = it }
        Button(
            onClick = { onSave(studyMin, breakMin, longAfter, longMin) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) { Text("Save Break Settings") }
    }
}

@Composable
  fun SettingsStepperRow(
    label: String, value: Int, unit: String, min: Int, max: Int, step: Int, onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > min) onChange(value - step) },
                modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
            }
            Text("$value $unit", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.widthIn(min = 60.dp))
            IconButton(onClick = { if (value < max) onChange(value + step) },
                modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
  fun SlotRow(
    slot: com.studyplanner.app.core.data.local.entity.StudySlotEntity,
    onDelete: () -> Unit
) {
    val days = listOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val day = days.getOrNull(slot.dayOfWeek) ?: ""
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary)
        Text("$day  ${formatTime12(slot.startHour, slot.startMinute)} - ${formatTime12(slot.endHour, slot.endMinute)}",
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
  fun RoutineRow(
    routine: com.studyplanner.app.core.data.local.entity.PersonalRoutineEntity,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(routine.emoji, fontSize = 18.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(routine.name, style = MaterialTheme.typography.bodyMedium)
            Text("${formatTime12(routine.startHour, routine.startMinute)} - ${formatTime12(routine.endHour, routine.endMinute)} • ${if (routine.isFlexible) "Flexible" else "Fixed"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
  fun AlarmRow(
    alarm: com.studyplanner.app.core.data.local.entity.MorningAlarmEntity,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Alarm, null, modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(formatTime12(alarm.hour, alarm.minute), style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold)
            Text(alarm.customMessage.take(40), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = alarm.isEnabled, onCheckedChange = { onToggle() })
    }
}

@Composable
  fun SyncSection(
    autoSync: Boolean, isLoading: Boolean, syncSuccess: Boolean,
    onAutoSyncToggle: (Boolean) -> Unit, onManualSync: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Auto Sync", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("Har save pe Firestore sync", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = autoSync, onCheckedChange = onAutoSyncToggle)
    }
    Button(
        onClick = onManualSync,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp),
                color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sync Now")
    }
    if (syncSuccess) {
        Text("✅ Sync complete!", color = StatusDone,
            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
  fun AboutSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AboutRow("App Version", "1.0.0")
        AboutRow("Build", "Beta")
        AboutRow("Privacy Policy", "studyplanner.app/privacy")
        AboutRow("Terms of Use", "studyplanner.app/terms")
    }
}

@Composable
  fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

  fun formatTime12(hour24: Int, minute: Int): String {
    val period = if (hour24 >= 12) "PM" else "AM"
    val h12 = when { hour24 == 0 -> 12; hour24 > 12 -> hour24 - 12; else -> hour24 }
    return "${h12.toString().padStart(2,'0')}:${minute.toString().padStart(2,'0')} $period"
}

@Composable
  fun AdvancedSection(
    isLoading: Boolean,
    timetableSuccess: Boolean,
    onRegenerate: () -> Unit,
    onRedoOnboarding: () -> Unit,
    onClearTimetableSuccess: () -> Unit,
) {
    var showRedoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(timetableSuccess) {
        if (timetableSuccess) {
            kotlinx.coroutines.delay(3000)
            onClearTimetableSuccess()
        }
    }

    if (showRedoDialog) {
        AlertDialog(
            onDismissRequest = { showRedoDialog = false },
            title = { Text("Redo Onboarding?") },
            text = { Text("Poora onboarding dobara hoga. Subjects, slots, schedule sab fir se set karna hoga. Sure ho?") },
            confirmButton = {
                Button(
                    onClick = { showRedoDialog = false; onRedoOnboarding() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Haan, Reset Karo") }
            },
            dismissButton = { TextButton(onClick = { showRedoDialog = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Regenerate Timetable
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🗓️ Regenerate Timetable",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
            Text("Subjects/slots change kiye hain? Timetable naya banao.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (timetableSuccess) {
                Text("✅ Timetable regenerated!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onRegenerate,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("🔄 Regenerate Timetable", fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

        // Redo Onboarding
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🔁 Redo Onboarding",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
            Text("Exam, subjects, schedule sab fir se set karo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = { showRedoDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text("🔁 Redo Onboarding", fontWeight = FontWeight.Bold)
            }
        }
    }
}