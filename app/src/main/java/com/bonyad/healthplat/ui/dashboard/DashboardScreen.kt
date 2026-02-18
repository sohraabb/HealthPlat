package com.bonyad.healthplat.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.bonyad.healthplat.ui.navigation.NavRoutes
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
    onNavigateToRoot: (String) -> Unit,
    onLogout: () -> Unit = {}
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
        containerColor = Color(0xFFF5F5F5),
        bottomBar = {
            DashboardBottomBar(navController = bottomTabNavController)
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomTabNavController,
            startDestination = DashboardScreen.Home.route,
            modifier = Modifier.padding(
                top = paddingValues.calculateTopPadding()
            )
        ) {
            composable(DashboardScreen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { route ->
                        onNavigateToRoot(route)
                    },
                    onNavigateToAi = { route ->
                        onNavigateToRoot(route)
                    },
                    onNavigateToNotifications = {
                        onNavigateToRoot(NavRoutes.Notifications.route)
                    }
                )
            }

            composable(DashboardScreen.Care.route) {
                CareScreenDashboard()
            }

            composable(DashboardScreen.Calory.route) {
                // ✅ FIX: Pass onNavigateToRoot to CaloryScreenDashboard
                CaloryScreenDashboard(
                    onNavigateToRoute = onNavigateToRoot
                )
            }

            composable(DashboardScreen.Profile.route) {
                ProfileScreen(
                    onNavigateToRoute = { route -> onNavigateToRoot(route) },
                    onLogout = onLogout
                )
            }
        }
    }
}


@Composable
fun DashboardBottomBar(navController: NavHostController) {
    val items = listOf(
        DashboardScreen.Profile,
        DashboardScreen.Calory,
        DashboardScreen.Care,
        DashboardScreen.Home
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedColor = Color(0xFF5BA3A3)
    val unselectedColor = Color(0xFF6B6B6B)

    // Transparent container that sits above system navigation
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent) // Transparent background
            .navigationBarsPadding() // Respect system navigation bar
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val isSelected = currentRoute == screen.route

                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) selectedColor else Color.Transparent,
                        label = "bgColor"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else unselectedColor,
                        label = "contentColor"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(backgroundColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(screen.icon),
                            contentDescription = screen.title,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = screen.title,
                            color = contentColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// Placeholder screens - We'll implement these next
@Composable
fun CareScreenDashboard() {
    CareScreen()
}

@Composable
fun CaloryScreenDashboard(
    onNavigateToRoute: (String) -> Unit = {}
) {
    CaloryScreen(
        onNavigateToRoute = onNavigateToRoute
    )
}

@Composable
fun ProfileScreen(
    onNavigateToRoute: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    ProfileScreenDashboard(
        onNavigateToProfileRoutes = onNavigateToRoute,
        onNavigateToLogin = onLogout  // ← ADD THIS
    )
}
