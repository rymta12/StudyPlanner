package com.studyplanner.app.feature.competitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.ui.components.SurfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitorScreen(
    onBack: () -> Unit,
    viewModel: CompetitorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Competition 🔥") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.error?.let {
                Text("Leaderboard load nahi hua: $it", color = MaterialTheme.colorScheme.error)
            }

            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Aapki rank", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("#${state.myRank}", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("${state.totalAspirants} aspirants me" +
                            if (state.examType.isNotEmpty()) " (${state.examType})" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (state.pointsToNext > 0 && state.nextName.isNotEmpty()) {
                SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "${state.nextName} sirf ${state.pointsToNext} points aage hai. " +
                                    "Ek focused session aur — overtake kar lo!",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Text("🏆 Tum apne group me TOP par ho! Lead banaye rakho.",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Medium)
                }
            }

            Text("Aas-paas ke aspirants", fontWeight = FontWeight.Bold)
            state.nearby.forEach { r ->
                SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("#${r.rank}", fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (r.isYou) "${r.name} (You)" else r.name,
                                fontWeight = if (r.isYou) FontWeight.Bold else FontWeight.Normal,
                                color = if (r.isYou) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface)
                        }
                        Text("${r.points} pts", fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
