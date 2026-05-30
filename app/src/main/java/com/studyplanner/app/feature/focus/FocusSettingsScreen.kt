package com.studyplanner.app.feature.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.core.util.BlockingMode
import com.studyplanner.app.ui.components.SectionHeader
import com.studyplanner.app.ui.theme.*

@Composable
fun FocusSettingsScreen(
    viewModel: FocusSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var newChannel by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.checkPermissions() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Focus Settings 🔒", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Configure app blocking during study sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            PermissionsCard(
                hasUsageStats = state.hasUsageStatsPermission,
                hasOverlay = state.hasOverlayPermission,
                onGrantUsageStats = { viewModel.openUsageStatsSettings() },
                onGrantOverlay = { viewModel.openOverlaySettings() },
                onRefresh = { viewModel.checkPermissions() }
            )
        }

        item {
            SectionHeader(title = "Blocking Mode",
                modifier = Modifier.padding(vertical = 4.dp))
        }

        item {
            BlockingModeSelector(
                selected = state.blockingMode,
                onSelect = { viewModel.setBlockingMode(it) }
            )
        }

        if (state.blockingMode == BlockingMode.YOUTUBE_WHITELIST) {
            item {
                SectionHeader(
                    title = "YouTube Whitelist",
                    modifier = Modifier.padding(vertical = 4.dp),
                    action = {
                        Text("${state.whitelistedChannels.size} channels",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            "Only these YouTube channels will be accessible during sessions. All other YouTube content will be blocked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newChannel,
                        onValueChange = { newChannel = it },
                        label = { Text("Channel name or URL") },
                        leadingIcon = { Icon(Icons.Default.PlayCircle, "YouTube") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(
                        onClick = {
                            if (newChannel.isNotBlank()) {
                                viewModel.addChannel(newChannel.trim())
                                newChannel = ""
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            if (state.whitelistedChannels.isNotEmpty()) {
                items(state.whitelistedChannels) { channel ->
                    ChannelChip(
                        channel = channel,
                        onRemove = { viewModel.removeChannel(channel) }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("📺", fontSize = 32.sp)
                            Text("No channels added yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Add study channels like Drishti IAS, Vision IAS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                        }
                    }
                }
            }

            item {
                SuggestedChannels(onAdd = { viewModel.addChannel(it) })
            }
        }

        item {
            BlockedAppsList()
        }
    }
}

@Composable
private fun PermissionsCard(
    hasUsageStats: Boolean,
    hasOverlay: Boolean,
    onGrantUsageStats: () -> Unit,
    onGrantOverlay: () -> Unit,
    onRefresh: () -> Unit
) {
    val allGranted = hasUsageStats && hasOverlay

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted) StatusDone.copy(0.1f)
            else MaterialTheme.colorScheme.errorContainer.copy(0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (allGranted) "✅ All permissions granted" else "⚠️ Permissions required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (allGranted) StatusDone else MaterialTheme.colorScheme.error
                )
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            PermissionRow(
                label = "Usage Stats Access",
                description = "Detect which app is in foreground",
                granted = hasUsageStats,
                onGrant = onGrantUsageStats
            )
            PermissionRow(
                label = "Display Over Other Apps",
                description = "Show block overlay on screen",
                granted = hasOverlay,
                onGrant = onGrantOverlay
            )
        }
    }
}

@Composable
private fun PermissionRow(
    label: String, description: String,
    granted: Boolean, onGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            if (granted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            null,
            tint = if (granted) StatusDone else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!granted) {
            TextButton(onClick = onGrant, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Grant", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BlockingModeSelector(selected: BlockingMode, onSelect: (BlockingMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BlockingMode.entries.forEach { mode ->
            val isSelected = selected == mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(mode) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
                elevation = CardDefaults.cardElevation(if (isSelected) 0.dp else 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val emoji = when (mode) {
                        BlockingMode.COMPLETE_BLOCK -> "🔴"
                        BlockingMode.YOUTUBE_BLOCK -> "📵"
                        BlockingMode.YOUTUBE_WHITELIST -> "✅"
                        BlockingMode.DISABLED -> "🔓"
                    }
                    Text(emoji, fontSize = 22.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mode.displayName, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface)
                        Text(mode.description, style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelChip(channel: String, onRemove: () -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.PlayCircle, "YouTube",
                tint = Color(0xFFFF0000), modifier = Modifier.size(18.dp))
            Text(channel, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SuggestedChannels(onAdd: (String) -> Unit) {
    val suggestions = listOf("Drishti IAS", "Vision IAS", "StudyIQ IAS",
        "Unacademy UPSC", "Khan Academy", "Adda247", "Vedantu")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Quick add", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            suggestions.take(4).forEach { ch ->
                SuggestionChip(
                    onClick = { onAdd(ch) },
                    label = { Text(ch, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            suggestions.drop(4).forEach { ch ->
                SuggestionChip(
                    onClick = { onAdd(ch) },
                    label = { Text(ch, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
private fun BlockedAppsList() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Blocked Apps", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val apps = listOf("Instagram", "Facebook", "WhatsApp", "Twitter",
                "Snapchat", "Telegram", "Reddit", "TikTok", "Pinterest", "LinkedIn")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                apps.take(5).forEach { app ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusMissed.copy(0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(app, style = MaterialTheme.typography.labelSmall, color = StatusMissed)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                apps.drop(5).forEach { app ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusMissed.copy(0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(app, style = MaterialTheme.typography.labelSmall, color = StatusMissed)
                    }
                }
            }
        }
    }
}
