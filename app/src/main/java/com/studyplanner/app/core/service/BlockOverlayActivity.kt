package com.studyplanner.app.core.service

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyplanner.app.ui.theme.StudyPlannerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BLOCKED_PKG = "blocked_pkg"
        const val EXTRA_IS_WARNING = "is_warning"
        const val EXTRA_SESSION_ID = "session_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val isWarning = intent.getBooleanExtra(EXTRA_IS_WARNING, false)
        val blockedPkg = intent.getStringExtra(EXTRA_BLOCKED_PKG) ?: ""

        setContent {
            StudyPlannerTheme {
                if (isWarning) {
                    WarningOverlay(
                        onDismiss = { finish() },
                        onGoBack = {
                            startActivity(packageManager.getLaunchIntentForPackage(packageName))
                            finish()
                        }
                    )
                } else {
                    BlockOverlay(
                        appName = getAppName(blockedPkg),
                        onGoBack = {
                            startActivity(packageManager.getLaunchIntentForPackage(packageName))
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun getAppName(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}

@Composable
private fun BlockOverlay(appName: String, onGoBack: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "blockScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF7B0000), Color(0xFF1A0000)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("🚫", fontSize = 72.sp)

            Text(
                "$appName\nBlocked!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val motivationalLines = listOf(
                        "\"Sapne woh nahi jo neend mein aate hain,\nsapne woh hain jo neend nahi aane dete.\"",
                        "\"IAS banna hai toh focus rakh!\"",
                        "\"Har minute important hai. Wapas aa jao!\"",
                        "\"Distraction temporary hai, success permanent.\"",
                    )
                    Text(
                        motivationalLines.random(),
                        color = Color.White.copy(0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = onGoBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF7B0000))
                Spacer(Modifier.width(8.dp))
                Text("Back to Study", color = Color(0xFF7B0000), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun WarningOverlay(onDismiss: () -> Unit, onGoBack: () -> Unit) {
    var countdown by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        val timer = object : CountDownTimer(5000, 1000) {
            override fun onTick(ms: Long) { countdown = (ms / 1000).toInt() + 1 }
            override fun onFinish() { onDismiss() }
        }
        timer.start()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF4A2800), Color(0xFF1A0E00)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("⚠️", fontSize = 64.sp)

            Text(
                "YouTube ke sirf\nstudy channels allowed hain!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                "Sirf whitelisted channels kholo.\nDistracting content block hai.",
                color = Color.White.copy(0.7f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White.copy(0.15f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$countdown",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFB74D)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onGoBack,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Text("Back to Study") }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00))
                ) { Text("Got it", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
