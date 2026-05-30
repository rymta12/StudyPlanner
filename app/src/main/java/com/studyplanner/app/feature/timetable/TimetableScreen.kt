package com.studyplanner.app.feature.timetable

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.core.data.local.entity.SessionEntity
import com.studyplanner.app.ui.components.EmptyState
import com.studyplanner.app.ui.components.StatusChip
import com.studyplanner.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimetableScreen(
    onSessionClick: (Long) -> Unit,
    viewModel: TimetableViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TimetableTopBar(
            state = state,
            onTabChange = { viewModel.setTab(it) },
            onPrev = { viewModel.previousPeriod() },
            onNext = { viewModel.nextPeriod() }
        )

        AnimatedContent(
            targetState = state.tab,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "timetableTab"
        ) { tab ->
            when (tab) {
                TimetableTab.TODAY -> TodayView(
                    sessions = state.todaySessions,
                    onSessionClick = onSessionClick
                )
                TimetableTab.WEEK -> WeekView(
                    weekSessions = state.weekSessions,
                    selectedDate = state.selectedDate,
                    onDayClick = { viewModel.setSelectedDate(it); viewModel.setTab(TimetableTab.TODAY) }
                )
                TimetableTab.MONTH -> MonthView(
                    monthDays = state.monthDays,
                    selectedDate = state.selectedDate,
                    onDayClick = { viewModel.setSelectedDate(it); viewModel.setTab(TimetableTab.TODAY) }
                )
            }
        }
    }
}

@Composable
private fun TimetableTopBar(
    state: TimetableUiState,
    onTabChange: (TimetableTab) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    val label = when (state.tab) {
        TimetableTab.TODAY -> dateFormat.format(Date(state.selectedDate))
        TimetableTab.WEEK -> {
            val weekEnd = state.selectedDate + 6 * 24 * 60 * 60 * 1000L
            "${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(state.selectedDate))} — ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(weekEnd))}"
        }
        TimetableTab.MONTH -> monthFormat.format(Date(state.selectedDate))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(label, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                    tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TimetableTab.entries.forEach { tab ->
                val selected = state.tab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onTabChange(tab) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Composable
private fun TodayView(
    sessions: List<SessionWithMeta>,
    onSessionClick: (Long) -> Unit
) {
    if (sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(emoji = "📅", title = "No sessions today",
                subtitle = "Enjoy your free day! 🎉")
        }
        return
    }

    val completed = sessions.count { it.session.status == "COMPLETED" }
    val total = sessions.size

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            DaySummaryBar(completed = completed, total = total)
            Spacer(Modifier.height(4.dp))
        }
        items(sessions) { meta ->
            TodaySessionCard(meta = meta, onClick = {
                if (meta.session.status == "UPCOMING") onSessionClick(meta.session.id)
            })
        }
    }
}

@Composable
private fun DaySummaryBar(completed: Int, total: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem("✅", "$completed", "Done")
            SummaryItem("📋", "$total", "Total")
            SummaryItem("⏳", "${total - completed}", "Remaining")
            SummaryItem("📊", "${if (total > 0) (completed * 100 / total) else 0}%", "Progress")
        }
    }
}

@Composable
private fun SummaryItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(emoji, fontSize = 18.sp)
        Text(value, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun TodaySessionCard(meta: SessionWithMeta, onClick: () -> Unit) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val session = meta.session

    val (statusColor, statusText) = when (session.status) {
        "COMPLETED" -> StatusDone to "Done ✅"
        "ONGOING" -> StatusOngoing to "Ongoing 🔵"
        "MISSED" -> StatusMissed to "Missed ❌"
        "MISSED_RESCHEDULED" -> StatusRescheduled to "Rescheduled 🔄"
        else -> StatusUpcoming to "Upcoming"
    }

    val subjectColor = runCatching {
        Color(android.graphics.Color.parseColor(meta.subjectColor))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(if (session.status == "UPCOMING") 2.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.status == "COMPLETED")
                MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 52.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(subjectColor)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(meta.topicName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1)
                Text(meta.subjectName, style = MaterialTheme.typography.bodySmall,
                    color = subjectColor)
                Text(
                    "${timeFormat.format(Date(session.scheduledStartTime))} → ${timeFormat.format(Date(session.scheduledEndTime))} • ${session.studyMinutes}min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusChip(text = statusText, color = statusColor)
                if (session.isRescheduled) {
                    StatusChip(text = "🔄", color = StatusRescheduled)
                }
            }
        }
    }
}

@Composable
private fun WeekView(
    weekSessions: Map<Long, List<SessionWithMeta>>,
    selectedDate: Long,
    onDayClick: (Long) -> Unit
) {
    val dayFormat = remember { SimpleDateFormat("EEE\ndd", Locale.getDefault()) }
    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    val weekStart = startOfDay(cal.timeInMillis)
    val weekDays = (0..6).map { weekStart + it * 24 * 60 * 60 * 1000L }
    val today = startOfDay(System.currentTimeMillis())

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                weekDays.forEach { day ->
                    val daySessions = weekSessions[day] ?: emptyList()
                    val completed = daySessions.count { it.session.status == "COMPLETED" }
                    val total = daySessions.size
                    val isToday = day == today
                    val hasData = total > 0

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isToday -> MaterialTheme.colorScheme.primary
                                    hasData && completed == total -> StatusDone.copy(alpha = 0.15f)
                                    hasData -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                            .clickable { onDayClick(day) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            dayFormat.format(Date(day)),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        if (hasData) {
                            Text(
                                "$completed/$total",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) MaterialTheme.colorScheme.onPrimary
                                else if (completed == total) StatusDone
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Box(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        weekDays.forEach { day ->
            val daySessions = weekSessions[day] ?: emptyList()
            if (daySessions.isNotEmpty()) {
                item {
                    Text(
                        SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(Date(day)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(daySessions) { meta ->
                    TodaySessionCard(meta = meta, onClick = { onDayClick(day) })
                }
            }
        }
    }
}

@Composable
private fun MonthView(
    monthDays: List<DaySummary>,
    selectedDate: Long,
    onDayClick: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate; set(Calendar.DAY_OF_MONTH, 1) }
    val firstDayOffset = (cal.get(Calendar.DAY_OF_WEEK) - cal.firstDayOfWeek + 7) % 7
    val today = startOfDay(System.currentTimeMillis())
    val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { header ->
                    Text(header, modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        val totalCells = firstDayOffset + monthDays.size
        val rows = (totalCells + 6) / 7

        items(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayIndex = cellIndex - firstDayOffset

                    if (dayIndex < 0 || dayIndex >= monthDays.size) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val day = monthDays[dayIndex]
                        val isToday = day.date == today
                        val bgColor = when {
                            isToday -> MaterialTheme.colorScheme.primary
                            day.total == 0 -> Color.Transparent
                            day.completionPercent == 1f -> StatusDone.copy(alpha = 0.2f)
                            day.missed > 0 -> StatusMissed.copy(alpha = 0.15f)
                            day.completionPercent > 0 -> StatusOngoing.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(bgColor)
                                .clickable { onDayClick(day.date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val dayNum = Calendar.getInstance()
                                    .apply { timeInMillis = day.date }
                                    .get(Calendar.DAY_OF_MONTH)
                                Text(
                                    "$dayNum",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (day.total > 0) {
                                    Text(
                                        "${(day.completionPercent * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = if (isToday) MaterialTheme.colorScheme.onPrimary.copy(0.8f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            MonthLegend()
        }
    }
}

@Composable
private fun MonthLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            StatusDone to "All done",
            StatusOngoing to "Partial",
            StatusMissed to "Missed",
            MaterialTheme.colorScheme.surfaceVariant to "Pending"
        ).forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
