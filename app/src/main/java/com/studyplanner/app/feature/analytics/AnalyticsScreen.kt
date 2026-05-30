package com.studyplanner.app.feature.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.studyplanner.app.ui.components.LoadingScreen
import com.studyplanner.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.isLoading) { LoadingScreen(); return }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item { AnalyticsHeader(state = state) }
        item { InsightsRow(state = state) }
        item { WeeklyBarChart(bars = state.weeklyBars) }
        item { SubjectPieChart(data = state.subjectPieData) }
        item { HeatmapSection(cells = state.heatmapCells) }
        item { PredictionCard(state = state) }
    }
}

@Composable
private fun AnalyticsHeader(state: AnalyticsUiState) {
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Analytics 📈", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("${state.totalStudyHours.toInt()} hours studied total",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
        }
    }
}

@Composable
private fun InsightsRow(state: AnalyticsUiState) {
    val bestHourText = if (state.bestStudyHour >= 0) {
        val h = if (state.bestStudyHour >= 12) "${state.bestStudyHour - 12} PM" else "${state.bestStudyHour} AM"
        h
    } else "N/A"

    Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InsightCard("🎯", "Efficiency", "${state.efficiencyScore}%",
            "Sessions completed", Modifier.weight(1f))
        InsightCard("📅", "Consistency", "${state.monthlyConsistencyPercent}%",
            "Last 30 days", Modifier.weight(1f))
        InsightCard("⚡", "Best Time", bestHourText,
            "Most productive", Modifier.weight(1f))
        InsightCard("⏱️", "Avg Session", "${state.avgSessionMinutes}m",
            "Per session", Modifier.weight(1f))
    }
}

@Composable
private fun InsightCard(
    emoji: String, title: String, value: String, subtitle: String, modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(value, fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun WeeklyBarChart(bars: List<WeekBarData>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("This Week 📅", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (bars.isEmpty() || bars.all { it.minutes == 0 }) {
                Text("No data yet", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp))
                return@Column
            }
            val maxMins = bars.maxOf { it.minutes }.coerceAtLeast(1)
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { bar ->
                    val animHeight by animateFloatAsState(
                        targetValue = bar.minutes.toFloat() / maxMins,
                        animationSpec = tween(800, easing = EaseOutCubic),
                        label = "barHeight"
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (bar.minutes > 0) {
                            Text(
                                "${bar.minutes / 60}h",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((100 * animHeight).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (bar.minutes > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                bars.forEach { bar ->
                    Text(bar.day, modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SubjectPieChart(data: List<SubjectPieData>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Subject Distribution 📚", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (data.isEmpty()) {
                Text("No study sessions yet", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp))
                return@Column
            }
            data.forEach { item ->
                val color = runCatching {
                    Color(android.graphics.Color.parseColor(item.colorHex))
                }.getOrDefault(MaterialTheme.colorScheme.primary)
                val animPct by animateFloatAsState(
                    targetValue = item.percent,
                    animationSpec = tween(800, easing = EaseOutCubic),
                    label = "pct"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                    Text(item.name, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    Text("${item.minutes / 60}h ${item.minutes % 60}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(item.percent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, color = color)
                }
                LinearProgressIndicator(
                    progress = { animPct },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = color, trackColor = color.copy(0.15f)
                )
            }
        }
    }
}

@Composable
private fun HeatmapSection(cells: List<HeatmapCell>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Activity Heatmap 🔥", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (cells.isEmpty()) {
                Text("No activity yet", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp))
                return@Column
            }
            val last30 = cells.takeLast(30)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                last30.forEach { cell ->
                    val color = when {
                        cell.intensity >= 0.75f -> Color(0xFF1B5E20)
                        cell.intensity >= 0.5f -> Color(0xFF2E7D32)
                        cell.intensity >= 0.25f -> Color(0xFF43A047)
                        cell.intensity > 0f -> Color(0xFF81C784)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
                repeat(30 - last30.size) {
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)
                        .background(Color.Transparent))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    Color(0xFF81C784), Color(0xFF43A047), Color(0xFF2E7D32), Color(0xFF1B5E20)
                ).forEach { c ->
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(c))
                    Spacer(Modifier.width(2.dp))
                }
                Text("More", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PredictionCard(state: AnalyticsUiState) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🔮", fontSize = 32.sp)
            Column {
                Text("Predicted Completion", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                Text(
                    if (state.predictedCompletionDate > 0)
                        dateFormat.format(Date(state.predictedCompletionDate))
                    else "Not enough data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text("Based on last 7 days pace",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.6f))
            }
        }
    }
}
