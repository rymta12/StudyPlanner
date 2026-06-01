package com.studyplanner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.studyplanner.app.core.navigation.Route
import com.studyplanner.app.core.navigation.StudyPlannerNavGraph
import com.studyplanner.app.ui.theme.AppThemes
import com.studyplanner.app.ui.theme.BgAnimationStyle
import com.studyplanner.app.ui.theme.StudyPlannerTheme
import com.studyplanner.app.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var themeManager: ThemeManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        android.util.Log.d("Permission", "POST_NOTIFICATIONS granted=$granted")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Android 13+ pe notification permission maango
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val appTheme by themeManager.currentTheme.collectAsStateWithLifecycle(AppThemes.default)
            val bgStyle by themeManager.currentBgStyle.collectAsStateWithLifecycle(BgAnimationStyle.GRADIENT_FLOW)

            StudyPlannerTheme(appTheme = appTheme, bgStyle = bgStyle) {
                val startDestination = remember {
                    if (auth.currentUser != null) Route.Home.path else Route.Launch.path
                }
                StudyPlannerNavGraph(startDestination = startDestination)
            }
        }
    }
}
