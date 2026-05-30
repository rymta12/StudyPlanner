package com.studyplanner.app.feature.parent

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
import com.studyplanner.app.ui.components.LoadingScreen
import com.studyplanner.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ParentDashboardScreen(viewModel: ParentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkCode by remember { mutableStateOf("") }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Add Child") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the monitor code from your child's app",
                        style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = linkCode,
                        onValueChange = { linkCode = it.uppercase() },
                        label = { Text("Monitor Code (e.g. ABC1-1234)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.linkChild(linkCode)
                    showLinkDialog = false
                    linkCode = ""
                }, enabled = linkCode.isNotBlank()) { Text("Link") }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ParentHeader(onAddChild = { showLinkDialog = true })

        if (state.isLoading) { LoadingScreen(); return@Column }

        if (state.children.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("👨‍👩‍👧", fontSize = 64.sp)
                    Text("No children linked yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("Ask your child to share their monitor code from Settings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Button(onClick = { showLinkDialog = true },
                        shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.PersonAdd, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Child")
                    }
                }
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.children) { child ->
                ChildCard(
                    child = child,
                    onClick = { viewModel.selectChild(child) }
                )
            }
        }
    }
}

@Composable
private fun ParentHeader(onAddChild: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0A1628), Color(0xFF0D47A1)))
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Parent Dashboard 👨‍👩‍👧",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("Monitor your children's progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.7f))
            }
            IconButton(onClick = onAddChild) {
                Icon(Icons.Default.PersonAdd, null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun ChildCard(child: ChildSummary, onClick: () -> Unit) {
    val statusColor = when (child.sessionStatus) {
        "ONGOING" -> StatusOngoing
        "COMPLETED" -> StatusDone
        "MISSED" -> StatusMissed
        else -> StatusUpcoming
    }
    val statusLabel = when (child.sessionStatus) {
        "ONGOING" -> "🟢 Session Active"
        "COMPLETED" -> "✅ Done for today"
        "MISSED" -> "🔴 Missed session"
        else -> "⬜ No session"
    }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(child.name.first().uppercase(), fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(child.name, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(statusLabel, style = MaterialTheme.typography.bodySmall,
                        color = statusColor)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("🔥 ${child.streak}", fontWeight = FontWeight.Bold,
                        color = StreakFire)
                    Text("⭐ ${child.points}", style = MaterialTheme.typography.bodySmall,
                        color = XpGold)
                }
            }

            if (child.currentTopic.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text("Studying: ${child.currentTopic}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ParentStatItem("✅", "${child.todayCompleted}/${child.todayTotal}", "Today")
                ParentStatItem("❌", "${child.missedCount}", "Missed")
                ParentStatItem("🕐", if (child.lastActive > 0)
                    timeFormat.format(Date(child.lastActive)) else "—", "Last Active")
            }

            if (child.todayTotal > 0) {
                LinearProgressIndicator(
                    progress = { if (child.todayTotal == 0) 0f
                    else child.todayCompleted.toFloat() / child.todayTotal },
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape),
                    color = statusColor,
                    trackColor = statusColor.copy(0.15f)
                )
            }
        }
    }
}

@Composable
private fun ParentStatItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 16.sp)
        Text(value, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
