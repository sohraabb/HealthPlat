package com.bonyad.healthplat.ui.device

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R


@Composable
fun DeviceConnectionStartScreen(
    viewModel: DeviceConnectionViewModel = hiltViewModel(),
    onStartScan: () -> Unit,
    onSkip: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var permissionsGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.all { it.value }
        if (permissionsGranted) {
            onStartScan()
        }
    }

    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            // 1. Back Button (Absolute positioning at Top Left)
            if (onBack != null) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        // IMPORTANT: Adds padding for status bar (clock/battery)
                        .statusBarsPadding()
                        // Fixed padding to match Figma (not too close to edge)
                        .padding(start = 24.dp, top = 16.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        // FIX: Using your custom drawable
                        painter = painterResource(id = R.drawable.back_arrow),
                        contentDescription = "بازگشت",
                        tint = Color(0xFF2C2C2C)
                    )
                }
            }

            // 2. Main Content Column

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState)
                    // Ensures content doesn't overlap with system bars at bottom
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Push text down slightly so it's not behind the back button
                Spacer(modifier = Modifier.height(80.dp))

                // --- TEXT SECTION ---
                Text(
                    text = "حلقه خود را متصل کنید",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = Color(0xFF2C2C2C),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "لطفاً مطمئن شوید که بلوتوث گوشی شما فعال است",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    ),
                    color = Color(0xFF666666)
                )

                // --- IMAGE SECTION (CENTERED) ---
                // Using weights to center the image between text and buttons
                Spacer(modifier = Modifier.weight(1f))

                Image(
                    painter = painterResource(id = R.drawable.ring_img),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(1f) // Adjust size (60% of screen width)
                        .aspectRatio(1f),   // Keep it square
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.weight(1f))

                // --- BUTTONS SECTION ---
                Button(
                    onClick = {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        } else {
                            arrayOf(
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        }
                        permissionLauncher.launch(permissions)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5BA3A3)
                    )
                ) {
                    Text(
                        text = "شروع",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { onSkip?.invoke() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF5BA3A3)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color(0xFF5BA3A3), Color(0xFF5BA3A3)))
                    )
                ) {
                    Text(
                        text = "دستی وارد کنید",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF5BA3A3)
                    )
                }

                // Bottom padding
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}