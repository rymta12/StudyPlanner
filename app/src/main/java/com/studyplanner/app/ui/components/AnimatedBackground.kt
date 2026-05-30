package com.studyplanner.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.studyplanner.app.ui.theme.BgAnimationStyle
import com.studyplanner.app.ui.theme.LocalAppTheme
import com.studyplanner.app.ui.theme.LocalBgStyle
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedBackground(
    modifier: Modifier = Modifier,
    style: BgAnimationStyle = LocalBgStyle.current,
    content: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current
    val g = theme.gradients

    Box(modifier = modifier) {
        when (style) {
            BgAnimationStyle.GRADIENT_FLOW -> GradientFlowBg(g.backgroundStart, g.backgroundMid, g.backgroundEnd)
            BgAnimationStyle.PULSE -> PulseBg(g.backgroundStart, g.backgroundEnd, g.primaryGradientStart)
            BgAnimationStyle.PARTICLES -> ParticlesBg(g.backgroundStart, g.backgroundEnd, g.accentGradientStart)
            BgAnimationStyle.STATIC -> StaticBg(g.backgroundStart, g.backgroundEnd)
        }
        content()
    }
}

@Composable
private fun GradientFlowBg(start: Color, mid: Color, end: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "grad")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "gradFlow"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerp(start, mid, animValue),
                        lerp(mid, end, animValue),
                        end
                    )
                )
            )
    )
}

@Composable
private fun PulseBg(start: Color, end: Color, accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "pulseScale"
    )
    Box(modifier = Modifier.fillMaxSize().background(start)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.3f * scale), Color.Transparent),
                    center = Offset(size.width * 0.3f, size.height * 0.3f),
                    radius = size.minDimension * scale
                ),
                radius = size.minDimension * scale,
                center = Offset(size.width * 0.3f, size.height * 0.3f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(end.copy(alpha = 0.2f * (1f - scale)), Color.Transparent),
                    center = Offset(size.width * 0.7f, size.height * 0.7f),
                    radius = size.minDimension * (1f - scale)
                ),
                radius = size.minDimension * (1f - scale),
                center = Offset(size.width * 0.7f, size.height * 0.7f)
            )
        }
    }
}

@Composable
private fun ParticlesBg(start: Color, end: Color, accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "time"
    )
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(start, end)))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawParticles(time, accent)
        }
    }
}

private fun DrawScope.drawParticles(time: Float, accent: Color) {
    val particleCount = 12
    repeat(particleCount) { i ->
        val angle = (time + i * (360f / particleCount)) * (Math.PI / 180f).toFloat()
        val radius = size.minDimension * 0.35f
        val cx = size.width / 2f + cos(angle) * radius * (0.5f + (i % 3) * 0.2f)
        val cy = size.height / 2f + sin(angle * 0.7f) * radius * (0.4f + (i % 4) * 0.15f)
        drawCircle(
            color = accent.copy(alpha = 0.15f + (i % 4) * 0.05f),
            radius = 6f + (i % 5) * 4f,
            center = Offset(cx, cy)
        )
    }
}

@Composable
private fun StaticBg(start: Color, end: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(start, end)))
    )
}

private fun lerp(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f
)
