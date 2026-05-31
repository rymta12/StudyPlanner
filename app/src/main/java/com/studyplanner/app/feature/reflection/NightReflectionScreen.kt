package com.studyplanner.app.feature.reflection


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyplanner.app.ui.components.PrimaryButton
import com.studyplanner.app.ui.components.SurfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NightReflectionScreen(
    onBack: () -> Unit,
    viewModel: NightReflectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val moods = listOf("😣", "😕", "😐", "🙂", "🤩")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Night Reflection 🌙") },
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
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Aaj ka din", fontWeight = FontWeight.Bold)
                    Text("${state.completed}/${state.total} sessions • ${state.studyMinutes} min padhe",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aaj kaisa feel hua?", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    moods.forEachIndexed { i, emoji ->
                        val selected = state.mood == i + 1
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setMood(i + 1) },
                            contentAlignment = Alignment.Center
                        ) { Text(emoji, fontSize = 24.sp) }
                    }
                }
            }

            ReflectField("✅ Aaj kya accha hua?", state.wentWell, viewModel::setWentWell)
            ReflectField("⚠️ Kahan dhyaan bhatka?", state.toImprove, viewModel::setToImprove)
            ReflectField("🌅 Kal ka pehla kaam", state.tomorrowIntention, viewModel::setTomorrow)

            PrimaryButton(
                text = if (state.saved) "Saved ✓ Good night!" else "Save & Sleep",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saved
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
