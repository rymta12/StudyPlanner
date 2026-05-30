package com.studyplanner.app.feature.home

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.core.data.local.entity.SessionEntity
import com.studyplanner.app.ui.components.*
import com.studyplanner.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeDashboardScreen(
    onSessionClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) { LoadingScreen(); return }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { DashboardHeader(state = state) }

        if (state.studyDebt > 0) {
            item {
                DebtBanner(debt = state.studyDebt, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }

        state.upcomingSession?.let { session ->
            item {
                UpcomingSessionCard(
                    session = session,
                    onClick = { onSessionClick(session.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            SectionHeader(
                title = "Today's Schedule",
                modifier = Modifier.padding(horizontal = 16.dp),
                action = {
                    Text("${state.todayCompleted}/${state.todayTotal}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold)
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        if (state.todaySessions.isEmpty()) {
            item {
                EmptyState(emoji = "📅", title = "No sessions today",
                    subtitle = "Your timetable will appear here",
                    modifier = Modifier.fillMaxWidth())
            }
        } else {
            items(state.todaySessions) { session ->
                SessionCard(
                    session = session,
                    onClick = { if (session.status == "UPCOMING") onSessionClick(session.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            SectionHeader(title = "Subjects Progress",
                modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))
        }

        items(state.subjects) { subject ->
            val progress = if (subject.estimatedTotalMinutes == 0) 0f
            else subject.completedMinutes.toFloat() / subject.estimatedTotalMinutes
            LinearProgressWithLabel(
                progress = progress,
                label = subject.name,
                color = runCatching { Color(android.graphics.Color.parseColor(subject.colorHex)) }
                    .getOrDefault(MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun DashboardHeader(state: HomeUiState) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(theme.gradients.backgroundStart, theme.gradients.backgroundMid)
                )
            )
    ) {
        Column(modifier = Modifier.padding(20.dp).systemBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(greeting(), style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.7f))
                    Text(state.userName.ifBlank { "Student" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.15f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🔥", fontSize = 16.sp)
                            Text("${state.currentStreak}",
                                color = Color.White, fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("📊", "Overall", "${(state.overallProgress * 100).toInt()}%", Modifier.weight(1f))
                StatCard("✅", "Today", "${state.todayCompleted}/${state.todayTotal}", Modifier.weight(1f))
                StatCard("🔥", "Streak", "${state.currentStreak}d", Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            LinearProgressIndicator(
                progress = { state.overallProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(0.2f)
            )
            Spacer(Modifier.height(4.dp))
            Text("Overall syllabus: ${(state.overallProgress * 100).toInt()}% complete",
                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatCard(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.12f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium)
        Text(label, color = Color.White.copy(0.6f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DebtBanner(debt: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828).copy(0.1f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFC62828).copy(0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Study Debt: $debt sessions pending",
                    color = Color(0xFFEF5350), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium)
                Text("Complete makeup sessions to clear debt",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun UpcomingSessionCard(
    session: SessionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val theme = LocalAppTheme.current

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(theme.gradients.primaryGradientStart, theme.gradients.primaryGradientEnd)
                    )
                )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("⏰ Next Session", color = Color.White.copy(0.8f),
                        style = MaterialTheme.typography.labelMedium)
                    Text(timeFormat.format(Date(session.scheduledStartTime)),
                        color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium)
                }
                Text("Tap to start", color = Color.White,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${session.studyMinutes} min study",
                        color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                    Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val (statusColor, statusLabel) = when (session.status) {
        "COMPLETED" -> StatusDone to "Done ✅"
        "ONGOING" -> StatusOngoing to "Ongoing 🔵"
        "UPCOMING" -> StatusUpcoming to "Upcoming"
        "MISSED" -> StatusMissed to "Missed ❌"
        "MISSED_RESCHEDULED" -> StatusRescheduled to "Rescheduled 🔄"
        else -> StatusUpcoming to session.status
    }

    SurfaceCard(modifier = modifier, onClick = if (session.status == "UPCOMING") onClick else null) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(4.dp, 40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(statusColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${timeFormat.format(Date(session.scheduledStartTime))} → ${timeFormat.format(Date(session.scheduledEndTime))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Session #${session.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            StatusChip(text = statusLabel, color = statusColor)
        }
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning 🌅"
        hour < 17 -> "Good Afternoon ☀️"
        hour < 21 -> "Good Evening 🌆"
        else -> "Good Night 🌙"
    }
}
