package com.studyplanner.app.feature.settings

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.studyplanner.app.core.navigation.Route
import com.studyplanner.app.ui.theme.*

@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToWeeklyReview: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expandedSection by remember { mutableStateOf<String?>("appearance") }

    LaunchedEffect(state.syncSuccess) {
        if (state.syncSuccess) viewModel.clearSyncSuccess()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SettingsHeader()
        }

        item {
            SettingsNavItem(
                icon = Icons.Default.Person,
                title = "My Profile",
                subtitle = "Name, city, exam details",
                onClick = onNavigateToProfile
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(0.15f))
        }

        item {
            SettingsSection(
                title = "🎨 Appearance",
                icon = Icons.Default.Palette,
                expanded = expandedSection == "appearance",
                onToggle = { expandedSection = if (expandedSection == "appearance") null else "appearance" }
            ) {
                ThemeSelector(
                    currentThemeId = state.currentTheme.id,
                    onThemeSelect = { viewModel.setTheme(it) }
                )
                Spacer(Modifier.height(12.dp))
                BgStyleSelector(
                    current = state.currentBgStyle,
                    onSelect = { viewModel.setBgStyle(it) }
                )
            }
        }

        item {
            SettingsSection(
                title = "⏰ Break Settings",
                icon = Icons.Default.Coffee,
                expanded = expandedSection == "break",
                onToggle = { expandedSection = if (expandedSection == "break") null else "break" }
            ) {
                BreakSettingsEditor(
                    settings = state.breakSettings,
                    onSave = { s, b, la, lb -> viewModel.updateBreakSettings(s, b, la, lb) }
                )
            }
        }

        item {
            SettingsSection(
                title = "📅 Study Slots",
                icon = Icons.Default.Schedule,
                expanded = expandedSection == "slots",
                onToggle = { expandedSection = if (expandedSection == "slots") null else "slots" }
            ) {
                if (state.studySlots.isEmpty()) {
                    Text("No slots configured", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
                } else {
                    state.studySlots.forEach { slot ->
                        SlotRow(slot = slot, onDelete = { viewModel.deleteSlot(slot) })
                    }
                }
            }
        }

        item {
            SettingsSection(
                title = "🗓️ Personal Routines",
                icon = Icons.Default.Loop,
                expanded = expandedSection == "routines",
                onToggle = { expandedSection = if (expandedSection == "routines") null else "routines" }
            ) {
                if (state.personalRoutines.isEmpty()) {
                    Text("No routines added", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
                } else {
                    state.personalRoutines.forEach { routine ->
                        RoutineRow(routine = routine, onDelete = { viewModel.deleteRoutine(routine) })
                    }
                }
            }
        }

        item {
            SettingsSection(
                title = "⏰ Morning Alarms",
                icon = Icons.Default.Alarm,
                expanded = expandedSection == "alarms",
                onToggle = { expandedSection = if (expandedSection == "alarms") null else "alarms" }
            ) {
                state.morningAlarms.forEach { alarm ->
                    AlarmRow(alarm = alarm, onToggle = { viewModel.toggleAlarm(alarm) })
                }
            }
        }

        item {
            SettingsSection(
                title = "☁️ Sync & Backup",
                icon = Icons.Default.Cloud,
                expanded = expandedSection == "sync",
                onToggle = { expandedSection = if (expandedSection == "sync") null else "sync" }
            ) {
                SyncSection(
                    autoSync = state.autoSyncEnabled,
                    isLoading = state.isLoading,
                    syncSuccess = state.syncSuccess,
                    onAutoSyncToggle = { viewModel.setAutoSync(it) },
                    onManualSync = { viewModel.manualSync() }
                )
            }
        }

        item {
            SettingsNavItem(
                icon = Icons.Default.Assessment,
                title = "Weekly Self Review 📋",
                subtitle = "Is hafte ka performance dekhein",
                onClick = onNavigateToWeeklyReview
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(0.15f))
        }

        item {
            SettingsSection(
                title = "ℹ️ About",
                icon = Icons.Default.Info,
                expanded = expandedSection == "about",
                onToggle = { expandedSection = if (expandedSection == "about") null else "about" }
            ) {
                AboutSection()
            }
        }
    }
}

@Composable
private fun SettingsHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .systemBarsPadding()
    ) {
        Text("Settings ⚙️", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f))
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String, icon: ImageVector,
    expanded: Boolean, onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp))
            }
            Text(title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(0.15f))
    }
}

@Composable
private fun ThemeSelector(currentThemeId: String, onThemeSelect: (String) -> Unit) {
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
private fun BgStyleSelector(current: BgAnimationStyle, onSelect: (BgAnimationStyle) -> Unit) {
    Text("Background Animation", style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BgAnimationStyle.entries.forEach { style ->
            val selected = style == current
            FilterChip(
                selected = selected,
                onClick = { onSelect(style) },
                label = { Text("${style.emoji} ${style.displayName}",
                    style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

@Composable
private fun BreakSettingsEditor(
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
private fun SettingsStepperRow(
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
private fun SlotRow(
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
private fun RoutineRow(
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
private fun AlarmRow(
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
private fun SyncSection(
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
private fun AboutSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AboutRow("App Version", "1.0.0")
        AboutRow("Build", "Beta")
        AboutRow("Privacy Policy", "studyplanner.app/privacy")
        AboutRow("Terms of Use", "studyplanner.app/terms")
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
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

private fun formatTime12(hour24: Int, minute: Int): String {
    val period = if (hour24 >= 12) "PM" else "AM"
    val h12 = when { hour24 == 0 -> 12; hour24 > 12 -> hour24 - 12; else -> hour24 }
    return "${h12.toString().padStart(2,'0')}:${minute.toString().padStart(2,'0')} $period"
}
