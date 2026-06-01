package com.studyplanner.app.core.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studyplanner.app.feature.auth.AuthScreen
import com.studyplanner.app.feature.competitor.CompetitorScreen
import com.studyplanner.app.feature.home.HomeScreen
import com.studyplanner.app.feature.ocr.OcrScanScreen
import com.studyplanner.app.feature.onboarding.OnboardingScreen
import com.studyplanner.app.feature.onboarding.OnboardingViewModel
import com.studyplanner.app.feature.parent.ParentDashboardScreen
import com.studyplanner.app.feature.reflection.NightReflectionScreen
import com.studyplanner.app.feature.reflection.WeeklyReviewScreen
import com.studyplanner.app.feature.session.SessionScreen
import com.studyplanner.app.feature.settings.ProfileScreen

sealed class Route(val path: String) {
    data object Launch : Route("launch")
    data object Auth : Route("auth")
    data object Onboarding : Route("onboarding")
    data object Home : Route("home")
    data object ParentDashboard : Route("parent")
    data object OcrScan : Route("ocr_scan")
    data object WeeklyReview : Route("weekly_review")
    data object NightReflection : Route("night_reflection")
    data object Competitor : Route("competitor")
    data object ManualSession : Route("manual_session")

    data object Profile : Route("profile")
    data object SubjectDetail : Route("subject/{subjectId}") {
        fun go(id: Long) = "subject/$id"
    }

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
            OnboardingScreen(
                onComplete = { navController.navigate(Route.Home.path) { popUpTo(0) } },
                onBack = { navController.popBackStack() },
                navController = navController
            )
        }
        composable(Route.Home.path) {
            HomeScreen(navController = navController)
        }
        composable(Route.Session.path, arguments = listOf(navArgument("sessionId") { type = NavType.LongType })) {
            SessionScreen(
                onComplete = { navController.popBackStack() },
                onNightReflection = {
                    navController.navigate(Route.NightReflection.path)
                })
        }
        composable(
            route = Route.SubjectDetail.path,
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
        ) {
            com.studyplanner.app.feature.subject.SubjectDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.ParentDashboard.path) {
            ParentDashboardScreen()
        }
        composable(Route.Profile.path) {
            ProfileScreen(
                onSignOut = { navController.navigate(Route.Launch.path) { popUpTo(0) } }
            )
        }

        composable(Route.ManualSession.path) {
            com.studyplanner.app.feature.manualsession.ManualSessionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.OcrScan.path) {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(Route.Onboarding.path)
            }
            val onboardingViewModel: OnboardingViewModel = androidx.hilt.navigation.compose.hiltViewModel(parentEntry)
            OcrScanScreen(
                onConfirm = { lines ->
                    val chapters = lines.mapIndexed { i, line ->
                        com.studyplanner.app.feature.onboarding.ChapterDraft(
                            name = line,
                            pageStart = 1,
                            pageEnd = 1,
                            orderIndex = i
                        )
                    }
                    onboardingViewModel.addOcrChapters(chapters)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Route.WeeklyReview.path) {
            WeeklyReviewScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Route.NightReflection.path) {
            NightReflectionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Route.Competitor.path) {
            CompetitorScreen(
                onBack = { navController.popBackStack() }
            )
        }

    }
}
