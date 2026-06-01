package com.studyplanner.app.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.studyplanner.app.core.navigation.Route
import com.studyplanner.app.feature.analytics.AnalyticsScreen
import com.studyplanner.app.feature.leaderboard.LeaderboardScreen
import com.studyplanner.app.feature.progress.ProgressScreen
import com.studyplanner.app.feature.settings.SettingsScreen
import com.studyplanner.app.feature.timetable.TimetableScreen

private data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("Timetable", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem("Progress", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem("Leaderboard", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    BottomNavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun HomeScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Route.ManualSession.path) },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Extra Session") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            }
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding)
        ) {
            when (selectedTab) {
                0 -> HomeDashboardScreen(
                    onSessionClick = { navController.navigate(Route.Session.go(it)) }
                )
                1 -> TimetableScreen(
                    onSessionClick = { navController.navigate(Route.Session.go(it)) }
                )
                2 -> ProgressAnalyticsTab(navController)
                3 -> LeaderboardScreen(
                    onCompetitor = { navController.navigate(Route.Competitor.path) }
                )
                4 -> SettingsScreen(
                    onNavigateToProfile = { navController.navigate(Route.Profile.path) },
                    onNavigateToOnboarding = { navController.navigate(Route.Onboarding.path) { popUpTo(0) } },
                    onNavigateToWeeklyReview = { navController.navigate(Route.WeeklyReview.path) })
            }
        }
    }
}

@Composable
private fun ProgressAnalyticsTab(navController: NavController) {
    var subTab by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 },
                text = { Text("Progress") },
                icon = { Icon(Icons.Default.BarChart, null) })
            Tab(selected = subTab == 1, onClick = { subTab = 1 },
                text = { Text("Analytics") },
                icon = { Icon(Icons.Default.Analytics, null) })
        }
        when (subTab) {
            0 -> ProgressScreen(
                onSubjectClick = { navController.navigate(Route.SubjectDetail.go(it)) }
            )
            1 -> AnalyticsScreen()
        }
    }
}
