package com.studyplanner.app.feature.progress

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.ui.components.LoadingScreen
import com.studyplanner.app.ui.theme.*

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) { LoadingScreen(); return }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { ProgressHeader(state = state, onPeriodChange = { viewModel.setPeriod(it) }) }
        item { OverallCircle(percent = state.overallPercent) }
        item { StatsGrid(state = state) }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Subjects", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
        }
        if (state.subjectProgress.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center) {
                    Text("No subjects added yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.subjectProgress) { sp ->
                SubjectProgressCard(sp = sp)
            }
        }
    }
}

@Composable
private fun ProgressHeader(state: ProgressUiState, onPeriodChange: (ProgressPeriod) -> Unit) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(theme.gradients.backgroundStart, theme.gradients.backgroundMid)
                )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .systemBarsPadding()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Progress 📊", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold, color = Color.White)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(0.12f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProgressPeriod.entries.forEach { period ->
                    val selected = state.period == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color.White else Color.Transparent)
                            .clickable { onPeriodChange(period) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            period.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) theme.colors.primary else Color.White.copy(0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallCircle(percent: Float) {
    val animPercent by animateFloatAsState(
        targetValue = percent,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "progress"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                drawArc(color = Color.Gray.copy(0.15f),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color(0xFF1565C0), Color(0xFF42A5F5))),
                    startAngle = -90f, sweepAngle = animPercent * 360f,
                    useCenter = false, style = stroke
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(animPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary)
                Text("Syllabus\nComplete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun StatsGrid(state: ProgressUiState) {
    val cards = listOf(
        Triple("✅", "${state.completedSessions}", "Done"),
        Triple("❌", "${state.missedSessions}", "Missed"),
        Triple("⏱️", "${state.totalStudyMinutes / 60}h ${state.totalStudyMinutes % 60}m", "Study Time"),
        Triple("🔥", "${state.currentStreak}", "Streak"),
        Triple("🏆", "${state.longestStreak}", "Best Streak"),
        Triple("⭐", "${state.totalPoints}", "Points"),
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        cards.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (emoji, value, label) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(emoji, fontSize = 22.sp)
                            Text(value, fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(label, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SubjectProgressCard(sp: SubjectProgress) {
    var expanded by remember { mutableStateOf(false) }
    val subjectColor = runCatching {
        Color(android.graphics.Color.parseColor(sp.subject.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val animProgress by animateFloatAsState(
        targetValue = sp.completionPercent,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "subjectProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        .background(subjectColor.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        sp.subject.name.first().toString(),
                        fontWeight = FontWeight.ExtraBold,
                        color = subjectColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sp.subject.name, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("${(sp.completionPercent * 100).toInt()}%",
                            fontWeight = FontWeight.Bold, color = subjectColor,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = subjectColor,
                        trackColor = subjectColor.copy(0.15f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${sp.completedMinutes / 60}h / ${sp.totalMinutes / 60}h • ${sp.chapters.size} chapters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f))
                    sp.chapters.forEach { cp ->
                        ChapterProgressRow(cp = cp, subjectColor = subjectColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterProgressRow(cp: ChapterProgress, subjectColor: Color) {
    val completedTopics = cp.topics.count { it.status == "COMPLETED" }
    val statusIcon = when {
        cp.completionPercent == 1f -> "✅"
        cp.completionPercent > 0f -> "🔵"
        else -> "⬜"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(statusIcon, fontSize = 14.sp)
            Text(cp.chapter.name, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (cp.topics.isEmpty()) "${cp.chapter.totalPages}pg"
                else "$completedTopics/${cp.topics.size} topics",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (cp.topics.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { cp.completionPercent },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape)
                    .padding(start = 22.dp),
                color = subjectColor.copy(0.7f),
                trackColor = subjectColor.copy(0.1f)
            )
        }
    }
}
