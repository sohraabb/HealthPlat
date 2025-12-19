package com.bonyad.healthplat.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.dashboard.calory.CaloryScreen
import com.bonyad.healthplat.ui.dashboard.care.CareScreen
import com.bonyad.healthplat.ui.dashboard.profile.ProfileScreenDashboard
import com.bonyad.healthplat.ui.utils.PermissionUtils
import timber.log.Timber

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
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToRoot: (String) -> Unit
) {
    val bottomTabNavController = rememberNavController()
    val needsBluetoothPermissions by viewModel.needsBluetoothPermissions.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }

        if (allGranted) {
            Timber.i("✅ All Bluetooth permissions granted")
            viewModel.onPermissionsGranted()
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys
            Timber.w("⚠️ Denied permissions: $deniedPermissions")
            viewModel.onPermissionsDenied()
            showPermissionDialog = true
        }
    }

    // Show permission request when needed
    LaunchedEffect(needsBluetoothPermissions) {
        if (needsBluetoothPermissions) {
            Timber.d("🔐 Requesting Bluetooth permissions...")
            val requiredPermissions = PermissionUtils.getRequiredBluetoothPermissions()
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // Permission explanation dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("دسترسی بلوتوث") },
            text = {
                Text("برای اتصال به دستگاه سلامتی و دریافت داده‌های زنده، نیاز به دسترسی بلوتوث داریم.\n\nلطفا از تنظیمات، دسترسی‌های برنامه را فعال کنید.")
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    // You might want to open app settings here
                }) {
                    Text("متوجه شدم")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }


    Scaffold(
        bottomBar = {
            DashboardBottomBar(navController = bottomTabNavController)
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomTabNavController,
            startDestination = DashboardScreen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(DashboardScreen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { route ->
                        onNavigateToRoot(route)
                    },
                    onNavigateToAi = { route ->
                        onNavigateToRoot(route)
                    }
                )
            }

            composable(DashboardScreen.Care.route) {
                CareScreenDashboard()
            }

            composable(DashboardScreen.Calory.route) {
                CaloryScreenDashboard()
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
fun CaloryScreenDashboard() {
    CaloryScreen()
}

@Composable
fun ProfileScreen() {
    ProfileScreenDashboard()
}
