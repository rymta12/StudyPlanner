package com.studyplanner.app.core.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studyplanner.app.feature.auth.AuthScreen
import com.studyplanner.app.feature.home.HomeScreen
import com.studyplanner.app.feature.onboarding.OnboardingScreen
import com.studyplanner.app.feature.session.SessionScreen

sealed class Route(val path: String) {
    data object Launch : Route("launch")
    data object Auth : Route("auth")
    data object Onboarding : Route("onboarding")
    data object Home : Route("home")
    data object ParentDashboard : Route("parent")
    data object Profile : Route("profile")
    data object Session : Route("session/{sessionId}") {
        fun go(id: Long) = "session/$id"
    }
}

@Composable
fun StudyPlannerNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Route.Launch.path) {
            LaunchScreen(
                onStudent = { navController.navigate(Route.Auth.path) { popUpTo(0) } },
                onParent = { navController.navigate(Route.ParentDashboard.path) { popUpTo(0) } }
            )
        }
        composable(Route.Auth.path) {
            AuthScreen(
                onAuthSuccess = { isNew ->
                    if (isNew) navController.navigate(Route.Onboarding.path) { popUpTo(0) }
                    else navController.navigate(Route.Home.path) { popUpTo(0) }
                },
                onSkip = {
                    Log.d("AuthNav", "onSkip CLICKED! Attempting to navigate to Onboarding.")
                    try {
                        navController.navigate(Route.Onboarding.path) {
                            // popUpTo(0) kabhi kabhi backup stack complete clear karne mein dikkat karta hai
                            // Isko safe karne ke liye Route.Auth.path ko pop kar sakte hain
                            popUpTo(Route.Auth.path) { inclusive = true }
                        }
                        Log.d("AuthNav", "Navigation to Onboarding called successfully.")
                    } catch (e: Exception) {
                        Log.e("AuthNav", "Navigation failed with error: ${e.message}", e)
                    }
                }
            )
        }
        composable(Route.Onboarding.path) {
            OnboardingScreen(onComplete = { navController.navigate(Route.Home.path) { popUpTo(0) } },
                onBack = { navController.popBackStack() })
        }
        composable(Route.Home.path) {
            HomeScreen(navController = navController)
        }
        composable(Route.Session.path, arguments = listOf(navArgument("sessionId") { type = NavType.LongType })) {
            SessionScreen(onComplete = { navController.popBackStack() })
        }
        composable(Route.ParentDashboard.path) {
            com.studyplanner.app.feature.parent.ParentDashboardScreen()
        }
        composable(Route.Profile.path) {
            com.studyplanner.app.feature.settings.ProfileScreen(
                onSignOut = { navController.navigate(Route.Launch.path) { popUpTo(0) } }
            )
        }

    }
}
