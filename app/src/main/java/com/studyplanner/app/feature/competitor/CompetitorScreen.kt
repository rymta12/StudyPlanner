package com.studyplanner.app.feature.competitor

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitorScreen(
    onBack: () -> Unit,
    viewModel: CompetitorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) { viewModel.searchUser(searchQuery) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF283593)))
                )
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Column {
                        Text("⚔️ Competitors",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("${state.competitors.size} rivals tracked",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.7f))
                    }
                }
                Row {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                    IconButton(onClick = { showSearch = !showSearch; searchQuery = "" }) {
                        Icon(if (showSearch) Icons.Default.Close else Icons.Default.PersonAdd,
                            null, tint = Color.White)
                    }
                }
            }
        }

        // Search bar
        AnimatedVisibility(visible = showSearch) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by name (min 3 chars)") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (state.isSearching)
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else if (searchQuery.isNotEmpty())
                            IconButton(onClick = { searchQuery = ""; viewModel.clearSearch() }) {
                                Icon(Icons.Default.Clear, null)
                            }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (state.searchResults.isNotEmpty()) {
                    Text("Results:", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.searchResults.forEach { result ->
                        val alreadyAdded = state.competitors.any { it.uid == result.uid }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AvatarCircle(result.name, size = 36)
                                    Column {
                                        Text(result.name, fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium)
                                        if (result.examTarget.isNotEmpty())
                                            Text(result.examTarget,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (alreadyAdded) {
                                    Text("Added ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                } else {
                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.addCompetitor(result)
                                            showSearch = false
                                            searchQuery = ""
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Add ⚔️", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                } else if (searchQuery.length >= 3 && !state.isSearching) {
                    Text("No users found", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // My stats card
            item {
                MyStatsCard(state = state)
            }

            if (state.competitors.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚔️", fontSize = 48.sp)
                            Text("No competitors yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("Add people to compete with and stay motivated!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
                return@LazyColumn
            }

            item {
                Text("YOUR RIVALS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp))
            }

            itemsIndexed(state.competitors) { index, competitor ->
                CompetitorCard(
                    competitor = competitor,
                    rank = index + 1,
                    myPoints = state.myPoints,
                    onRemove = { viewModel.removeCompetitor(competitor.uid) }
                )
            }
        }
    }
}

@Composable
private fun MyStatsCard(state: CompetitorUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👤 You", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold, color = Color.White)
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700))
                ) {
                    Text(" #${state.myRank} ",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MyStatItem("⭐", "${state.myPoints}", "Points", Color.White)
                MyStatItem("🔥", "${state.myStreak}", "Streak", Color(0xFFFF9800))
                MyStatItem("⏱️", "${state.myTodayMinutes}m", "Today", Color(0xFF4FC3F7))
            }
        }
    }
}

@Composable
private fun MyStatItem(emoji: String, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(value, fontWeight = FontWeight.ExtraBold, color = color,
            style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(0.7f))
    }
}

@Composable
private fun CompetitorCard(
    competitor: Competitor,
    rank: Int,
    myPoints: Int,
    onRemove: () -> Unit,
) {
    val diff = myPoints - competitor.points
    val isAhead = diff > 0
    val timeAgo = remember(competitor.lastActive) {
        if (competitor.lastActive == 0L) "Never"
        else {
            val mins = (System.currentTimeMillis() - competitor.lastActive) / 60000
            when {
                mins < 5 -> "Just now"
                mins < 60 -> "${mins}m ago"
                mins < 1440 -> "${mins / 60}h ago"
                else -> "${mins / 1440}d ago"
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box {
                    AvatarCircle(competitor.name, size = 44)
                    if (competitor.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                                .align(Alignment.BottomEnd)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(competitor.name, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge)
                        Text("#$rank",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (competitor.examTarget.isNotEmpty())
                        Text(competitor.examTarget,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Active $timeAgo",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (competitor.isOnline) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompStatChip("⭐ ${competitor.points}", "pts")
                CompStatChip("🔥 ${competitor.streak}", "streak")
                CompStatChip("⏱️ ${competitor.todayMinutes}m", "today")
                CompStatChip("📅 ${competitor.weekMinutes}m", "week")
            }

            // Gap card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAhead) Color(0xFF1B5E20).copy(0.15f)
                    else Color(0xFFB71C1C).copy(0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (isAhead) "🟢" else "🔴", fontSize = 14.sp)
                    Text(
                        if (isAhead) "You're ahead by $diff pts! Keep it up 💪"
                        else "You're behind by ${-diff} pts! Catch up 🏃",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isAhead) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompStatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AvatarCircle(name: String, size: Int) {
    val colors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC6), Color(0xFF018786),
        Color(0xFFB00020), Color(0xFF3700B3), Color(0xFF0A74DA)
    )
    val color = colors[name.hashCode().mod(colors.size).let { if (it < 0) -it else it }]
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            style = if (size >= 44) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyMedium
        )
    }
}