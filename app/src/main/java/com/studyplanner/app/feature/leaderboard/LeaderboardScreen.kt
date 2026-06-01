package com.studyplanner.app.feature.leaderboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyplanner.app.core.util.Badge
import com.studyplanner.app.ui.components.LoadingScreen
import com.studyplanner.app.ui.theme.*

@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel = hiltViewModel(),
                      onCompetitor: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var activeTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LeaderboardHeader(state = state)

        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 },
                text = { Text("Leaderboard") },
                icon = { Icon(Icons.Default.EmojiEvents, null) })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 },
                text = { Text("My Badges") },
                icon = { Icon(Icons.Default.MilitaryTech, null) })
        }

        when (activeTab) {
            0 -> LeaderboardTab(state = state, onScopeChange = { viewModel.setScope(it) }, onCompetitor = onCompetitor, viewModel = viewModel)
            1 -> BadgesTab(badges = state.badges)
        }
    }
}

@Composable
private fun LeaderboardHeader(state: LeaderboardUiState) {
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
            Text("Leaderboard 🏆", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold, color = Color.White)
            if (state.myRank > 0) {
                Text("Your rank: #${state.myRank}",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
            }
        }
    }
}

@Composable
private fun LeaderboardTab(
    state: LeaderboardUiState,
    onCompetitor: () -> Unit,
    viewModel: LeaderboardViewModel,
    onScopeChange: (LeaderboardScope) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LeaderboardScope.entries.forEach { scope ->
                val selected = state.scope == scope
                FilterChip(
                    selected = selected,
                    onClick = { onScopeChange(scope) },
                    label = {
                        Text(
                            scope.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        if (state.isLoading) {
            LoadingScreen(); return
        }

        if (state.error != null) {
            val context = LocalContext.current
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text("😕", fontSize = 48.sp)
                    Text("Could not load leaderboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    if (state.indexUrl != null) {
                        Text("Firestore index banana padega (ek baar)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                        Button(
                            onClick = {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(state.indexUrl)
                                )
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Index Banao (Firebase Console)")
                        }
                        Text("Button dabao → browser mein 'Create Index' click karo → 2 min wait karo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("Retry")
                    }
                }
            }
            return
        }

        if (state.entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏆", fontSize = 56.sp)
                    Text(
                        "Be the first!", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Complete sessions to appear here",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.entries.size >= 3) {
                item { TopThreePodium(entries = state.entries.take(3)) }
            }

            itemsIndexed(state.entries.drop(if (state.entries.size >= 3) 3 else 0)) { index, entry ->
                val actualRank = if (state.entries.size >= 3) index + 4 else index + 1
                LeaderboardRow(entry = entry.copy(rank = actualRank))
            }

            state.myEntry?.let { myEntry ->
                if (myEntry.rank > state.entries.size) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your Position", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        LeaderboardRow(entry = myEntry)
                        LeaderboardRow(entry = myEntry)

                        Spacer(Modifier.height(8.dp))

                        // ADD THIS
                        OutlinedButton(
                            onClick = { onCompetitor() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PersonSearch, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("👀 Apne competitors dekho")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopThreePodium(entries: List<LeaderboardEntry>) {
    val medals = listOf(GoldMedal, SilverMedal, BronzeMedal)
    val order = if (entries.size >= 3) listOf(1, 0, 2) else entries.indices.toList()

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            order.forEach { i ->
                if (i >= entries.size) return@forEach
                val entry = entries[i]
                val isFirst = i == 0
                val podiumHeight = if (isFirst) 100.dp else 75.dp
                val scale by animateFloatAsState(
                    targetValue = if (isFirst) 1.1f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "podiumScale"
                )

                Column(
                    modifier = Modifier.scale(scale),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(medals[i].let {
                        when (i) { 0 -> "🥇"; 1 -> "🥈"; else -> "🥉" }
                    }, fontSize = if (isFirst) 28.sp else 22.sp)

                    Box(
                        modifier = Modifier
                            .size(if (isFirst) 52.dp else 44.dp)
                            .clip(CircleShape)
                            .background(medals[i].copy(0.2f))
                            .border(2.dp, medals[i], CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            entry.name.first().uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            color = medals[i],
                            style = if (isFirst) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        entry.name.split(" ").first(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        "⭐ ${entry.points}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(podiumHeight)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(medals[i].copy(if (isFirst) 0.3f else 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#${i + 1}",
                            fontWeight = FontWeight.ExtraBold,
                            color = medals[i],
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    val isTop7 = entry.rank <= 7
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                entry.isCurrentUser -> MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
                isTop7 -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (entry.isCurrentUser) BorderStroke(
            1.5.dp, MaterialTheme.colorScheme.primary.copy(0.5f)) else null,
        elevation = CardDefaults.cardElevation(if (entry.isCurrentUser) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "#${entry.rank}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isTop7) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 36.dp)
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.name.firstOrNull()?.uppercase() ?: "?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.name, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (entry.isCurrentUser) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("You", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("${entry.city} • 🔥${entry.streak} streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("⭐ ${entry.points}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = XpGold)
            }
        }
    }
}

@Composable
private fun BadgesTab(badges: List<Badge>) {
    if (badges.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingScreen()
        }
        return
    }

    val unlocked = badges.filter { it.isUnlocked }
    val locked = badges.filter { !it.isUnlocked }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Unlocked (${unlocked.size}/${badges.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
        }

        item {
            if (unlocked.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔒", fontSize = 36.sp)
                        Text("Complete sessions to unlock badges!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            } else {
                BadgesGrid(badges = unlocked, unlocked = true)
            }
        }

        if (locked.isNotEmpty()) {
            item {
                Text("Locked (${locked.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { BadgesGrid(badges = locked, unlocked = false) }
        }
    }
}

@Composable
private fun BadgesGrid(badges: List<Badge>, unlocked: Boolean) {
    badges.chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { badge ->
                BadgeCard(badge = badge, unlocked = unlocked, modifier = Modifier.weight(1f))
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BadgeCard(badge: Badge, unlocked: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(if (unlocked) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                if (unlocked) badge.emoji else "🔒",
                fontSize = 32.sp,
                modifier = Modifier.let {
                    if (!unlocked) it.then(Modifier) else it
                }
            )
            Text(badge.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (unlocked) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(badge.description,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
