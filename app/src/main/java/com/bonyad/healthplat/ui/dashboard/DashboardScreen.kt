package com.bonyad.healthplat.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.dashboard.care.CareScreen
import com.bonyad.healthplat.ui.dashboard.profile.ProfileScreenDashboard

// Bottom navigation items
sealed class DashboardScreen(
    val route: String,
    val title: String,
    val icon: Int
) {
    object Home : DashboardScreen("home", "خانه", R.drawable.home)
    object Care : DashboardScreen("health", "مراقبت", R.drawable.care)
    object Calory : DashboardScreen("calory", "کالری", R.drawable.fire_4)
    object Profile : DashboardScreen("profile", "پروفایل", R.drawable.profile)
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            DashboardBottomBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = DashboardScreen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(DashboardScreen.Home.route) {
                HomeScreen(viewModel = viewModel)
            }

            composable(DashboardScreen.Care.route) {
                CareScreenDashboard()
            }

            composable(DashboardScreen.Calory.route) {
                CaloryScreen()
            }

            composable(DashboardScreen.Profile.route) {
                ProfileScreen()
            }
        }
    }
}

@Composable
fun DashboardBottomBar(navController: NavHostController) {
    val items = listOf(
        DashboardScreen.Profile,
        DashboardScreen.Care,
        DashboardScreen.Calory,
        DashboardScreen.Home
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = androidx.compose.ui.graphics.Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Image(
                        painter = painterResource(screen.icon),
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = androidx.compose.ui.graphics.Color(0xFF5BA3A3),
                    selectedTextColor = androidx.compose.ui.graphics.Color(0xFF5BA3A3),
                    indicatorColor = androidx.compose.ui.graphics.Color(0xFF5BA3A3).copy(alpha = 0.1f),
                    unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF999999),
                    unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF999999)
                )
            )
        }
    }
}

// Placeholder screens - We'll implement these next
@Composable
fun CareScreenDashboard() {
    CareScreen()
}

@Composable
fun CaloryScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("Health Screen - Coming Soon")
    }
}

@Composable
fun ProfileScreen() {
    ProfileScreenDashboard()
}