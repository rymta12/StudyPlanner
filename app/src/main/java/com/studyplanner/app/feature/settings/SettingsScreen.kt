package com.studyplanner.app.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.feature.settings.components.AboutSection
import com.studyplanner.app.feature.settings.components.AdvancedSection
import com.studyplanner.app.feature.settings.components.AlarmRow
import com.studyplanner.app.feature.settings.components.BgStyleSelector
import com.studyplanner.app.feature.settings.components.BreakSettingsEditor
import com.studyplanner.app.feature.settings.components.RoutineRow
import com.studyplanner.app.feature.settings.components.SlotRow
import com.studyplanner.app.feature.settings.components.SyncSection
import com.studyplanner.app.feature.settings.components.ThemeSelector

@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToWeeklyReview: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expandedSection by remember { mutableStateOf<String?>("appearance") }

    LaunchedEffect(state.syncSuccess) {
        if (state.syncSuccess) viewModel.clearSyncSuccess()
    }
    LaunchedEffect(state.onboardingReset) {
        if (state.onboardingReset) {
            viewModel.clearOnboardingReset()
            onNavigateToOnboarding()
        }
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

        item {
            SettingsSection(
                title = "⚙️ Advanced",
                icon = Icons.Default.Settings,
                expanded = expandedSection == "advanced",
                onToggle = { expandedSection = if (expandedSection == "advanced") null else "advanced" }
            ) {
                AdvancedSection(
                    isLoading = state.isLoading,
                    timetableSuccess = state.timetableRegenSuccess,
                    onRegenerate = { viewModel.regenerateTimetable() },
                    onRedoOnboarding = { viewModel.redoOnboarding() },
                    onClearTimetableSuccess = { viewModel.clearTimetableSuccess() }
                )
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
