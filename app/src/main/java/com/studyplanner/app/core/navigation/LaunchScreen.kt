package com.studyplanner.app.core.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyplanner.app.ui.theme.*

@Composable
fun LaunchScreen(
    onStudent: () -> Unit,
    onParent: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animatedFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A1628), Color(0xFF1A237E), Color(0xFF0D47A1))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(Modifier.weight(1f))

            Text(
                text = "📚",
                fontSize = 72.sp
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "StudyPlanner",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = "UPSC | SSC | Banking | Railway",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "I am a...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ModeCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🎓",
                    title = "Student",
                    subtitle = "Plan & track\nyour studies",
                    gradient = Brush.verticalGradient(listOf(Blue700, Color(0xFF1A237E))),
                    onClick = onStudent
                )
                ModeCard(
                    modifier = Modifier.weight(1f),
                    emoji = "👨‍👩‍👧",
                    title = "Parent",
                    subtitle = "Monitor your\nchild's progress",
                    gradient = Brush.verticalGradient(listOf(Orange700, Color(0xFF7B1F00))),
                    onClick = onParent
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun ModeCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .height(160.dp),
        onClick = {
            pressed = true
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = emoji, fontSize = 40.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
