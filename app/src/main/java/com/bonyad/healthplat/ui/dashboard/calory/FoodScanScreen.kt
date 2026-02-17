package com.bonyad.healthplat.ui.dashboard.calory

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

/**
 * Food Scan Camera Screen
 * Matches scan_food_9.png design with camera viewfinder and capture controls
 */
@Composable
fun FoodScanScreen(
    viewModel: CaloryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onFoodScanned: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imageCapture = remember { ImageCapture.Builder().build() }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onFoodScanned(it) }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Camera Preview
            CameraPreviewView(
                imageCapture = imageCapture,
                isFlashOn = isFlashOn,
                modifier = Modifier.fillMaxSize()
            )

            // Viewfinder Overlay
            ViewfinderOverlay(
                modifier = Modifier.fillMaxSize()
            )

            // Top Bar
            FoodScanTopBar(
                onBackClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            )

            // Bottom Controls
            CameraControls(
                isFlashOn = isFlashOn,
                isCapturing = isCapturing,
                onFlashToggle = { isFlashOn = !isFlashOn },
                onGalleryClick = { galleryLauncher.launch("image/*") },
                onCaptureClick = {
                    if (!isCapturing) {
                        isCapturing = true
                        captureImage(
                            context = context,
                            imageCapture = imageCapture,
                            executor = ContextCompat.getMainExecutor(context),
                            onImageCaptured = { uri ->
                                isCapturing = false
                                onFoodScanned(uri)
                            },
                            onError = {
                                isCapturing = false
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
        } else {
            // Permission Request UI
            PermissionRequestContent(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun CameraPreviewView(
    imageCapture: ImageCapture,
    isFlashOn: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(isFlashOn) {
        imageCapture.flashMode = if (isFlashOn) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    AndroidView(
        factory = { previewView.apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        } },
        modifier = modifier
    )
}

@Composable
private fun ViewfinderOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw semi-transparent overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.1f),
            size = size
        )

        // Viewfinder dimensions (square in center)
        val viewfinderSize = minOf(canvasWidth, canvasHeight) * 0.7f
        val left = (canvasWidth - viewfinderSize) / 2
        val top = (canvasHeight - viewfinderSize) / 2

        // Clear the viewfinder area
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(viewfinderSize, viewfinderSize),
            blendMode = BlendMode.Clear
        )

        // Draw corner brackets
        val cornerLength = viewfinderSize * 0.15f
        val cornerRadius = 16.dp.toPx()
        val strokeWidth = 4.dp.toPx()

        val bracketColor = Color.White

        // Top-left corner
        drawPath(
            path = Path().apply {
                moveTo(left, top + cornerLength)
                lineTo(left, top + cornerRadius)
                quadraticBezierTo(left, top, left + cornerRadius, top)
                lineTo(left + cornerLength, top)
            },
            color = bracketColor,
            style = Stroke(width = strokeWidth)
        )

        // Top-right corner
        drawPath(
            path = Path().apply {
                moveTo(left + viewfinderSize - cornerLength, top)
                lineTo(left + viewfinderSize - cornerRadius, top)
                quadraticBezierTo(left + viewfinderSize, top, left + viewfinderSize, top + cornerRadius)
                lineTo(left + viewfinderSize, top + cornerLength)
            },
            color = bracketColor,
            style = Stroke(width = strokeWidth)
        )

        // Bottom-left corner
        drawPath(
            path = Path().apply {
                moveTo(left, top + viewfinderSize - cornerLength)
                lineTo(left, top + viewfinderSize - cornerRadius)
                quadraticBezierTo(left, top + viewfinderSize, left + cornerRadius, top + viewfinderSize)
                lineTo(left + cornerLength, top + viewfinderSize)
            },
            color = bracketColor,
            style = Stroke(width = strokeWidth)
        )

        // Bottom-right corner
        drawPath(
            path = Path().apply {
                moveTo(left + viewfinderSize - cornerLength, top + viewfinderSize)
                lineTo(left + viewfinderSize - cornerRadius, top + viewfinderSize)
                quadraticBezierTo(left + viewfinderSize, top + viewfinderSize, left + viewfinderSize, top + viewfinderSize - cornerRadius)
                lineTo(left + viewfinderSize, top + viewfinderSize - cornerLength)
            },
            color = bracketColor,
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
private fun FoodScanTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(
                painter = painterResource(R.drawable.back_arrow),
                contentDescription = "بازگشت",
                tint = Color.Black
            )
        }

        // Title
        Text(
            text = "اسکن غذا",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = Color.White
        )

        // Placeholder for symmetry
        Spacer(modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun CameraControls(
    isFlashOn: Boolean,
    isCapturing: Boolean,
    onFlashToggle: () -> Unit,
    onGalleryClick: () -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gallery button
        IconButton(
            onClick = onGalleryClick,
            modifier = Modifier
                .size(56.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.image_placeholder),
                contentDescription = "گالری",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Capture button
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (isCapturing) Color.Gray else Color.White)
                .clickable(enabled = !isCapturing) { onCaptureClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }
        }

        // Flash button
        IconButton(
            onClick = onFlashToggle,
            modifier = Modifier
                .size(56.dp)
        ) {
            Icon(
                painter = if (isFlashOn) painterResource(R.drawable.flash) else painterResource(R.drawable.flash),
                contentDescription = if (isFlashOn) "فلش روشن" else "فلش خاموش",
                tint = if (isFlashOn) Color.Yellow else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun PermissionRequestContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "برای اسکن غذا نیاز به دسترسی دوربین است",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF5BA3A3))
                .clickable { onRequestPermission() }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "اجازه دسترسی",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}

private fun captureImage(
    context: android.content.Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onImageCaptured: (Uri) -> Unit,
    onError: (Exception) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        "food_scan_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}