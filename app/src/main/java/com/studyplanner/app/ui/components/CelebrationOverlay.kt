package com.studyplanner.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun CelebrationOverlay(
    show: Boolean,
    emoji: String,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = show,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Box {
                ConfettiCanvas()
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val scale by rememberInfiniteTransition(label = "scale")
                            .animateFloat(
                                initialValue = 1f, targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    tween(600), RepeatMode.Reverse),
                                label = "emojiScale"
                            )
                        Text(emoji,
                            fontSize = (64 * scale).sp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale; scaleY = scale
                            })
                        Text(title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Awesome! 🙌", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfettiCanvas() {
    val particles = remember {
        List(60) { ConfettiParticle(
            x = Random.nextFloat(),
            y = Random.nextFloat() * -0.5f,
            color = listOf(
                Color(0xFFFFD700), Color(0xFFFF6B6B), Color(0xFF4ECDC4),
                Color(0xFF45B7D1), Color(0xFF96CEB4), Color(0xFFFF9FF3),
                Color(0xFFFECA57), Color(0xFF48DBFB)
            ).random(),
            speed = 0.003f + Random.nextFloat() * 0.005f,
            angle = Random.nextFloat() * 360f,
            size = 6f + Random.nextFloat() * 10f,
            rotation = Random.nextFloat() * 360f,
        )}
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "confettiTime"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val currentY = (p.y + time * p.speed * 3f) % 1.2f
            val currentX = p.x + sin(time * 3f + p.angle) * 0.03f
            drawRect(
                color = p.color.copy(alpha = if (currentY > 1f) 0f else 0.9f),
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = currentX * size.width,
                    y = currentY * size.height
                ),
                size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f)
            )
        }
    }
}

private data class ConfettiParticle(
    val x: Float, val y: Float, val color: Color,
    val speed: Float, val angle: Float, val size: Float, val rotation: Float
)

@Composable
fun MilestoneToast(
    show: Boolean,
    emoji: String,
    message: String,
    onDismiss: () -> Unit
) {
    LaunchedEffect(show) {
        if (show) {
            kotlinx.coroutines.delay(3000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A237E)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(emoji, fontSize = 28.sp)
                    Text(message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
