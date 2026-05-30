package com.studyplanner.app.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            when (selectedTab) {
                0 -> HomeDashboardScreen(
                    onSessionClick = { navController.navigate(Route.Session.go(it)) }
                )
                1 -> TimetableScreen(
                    onSessionClick = { navController.navigate(Route.Session.go(it)) }
                )
                2 -> ProgressAnalyticsTab()
                3 -> LeaderboardScreen()
                4 -> SettingsScreen(
                    onNavigateToProfile = { navController.navigate(Route.Profile.path) }
                )
            }
        }
    }
}

@Composable
private fun ProgressAnalyticsTab() {
    var subTab by remember { mutableIntStateOf(0) }
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
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
            0 -> ProgressScreen()
            1 -> AnalyticsScreen()
        }
    }
}

@Composable
private fun PlaceholderTab(text: String) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
