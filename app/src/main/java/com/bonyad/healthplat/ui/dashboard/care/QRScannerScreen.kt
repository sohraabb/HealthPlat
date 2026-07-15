package com.bonyad.healthplat.ui.dashboard.care

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onQrCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isScanning by remember { mutableStateOf(true) }
    var scannedCode by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "اسکن QR کد",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasCameraPermission) {
                // Camera Preview
                CameraPreviewWithScanner(
                    onQrCodeDetected = { code ->
                        if (isScanning && scannedCode == null) {
                            isScanning = false
                            scannedCode = code
                            Timber.i("✅ QR Code scanned: $code")
                            onQrCodeScanned(code)
                        }
                    }
                )

                // Scanning overlay
                ScannerOverlay()

                // Instructions
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "QR کد تن‌بار را اسکن کنید",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "کد QR باید در داخل کادر قرار گیرد",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // No camera permission
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "دسترسی به دوربین لازم است",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "برای اسکن QR کد، لطفاً دسترسی دوربین را فعال کنید",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5BA3A3)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("فعال‌سازی دوربین")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewWithScanner(
    onQrCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            @androidx.camera.core.ExperimentalGetImage
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            if (barcode.format == Barcode.FORMAT_QR_CODE) {
                                                barcode.rawValue?.let { value ->
                                                    onQrCodeDetected(value)
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Timber.e(e, "Barcode scanning failed")
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Camera binding failed")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ScannerOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Dark overlay with transparent center
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // Scanner frame
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Transparent)
                .border(
                    width = 3.dp,
                    color = Color(0xFF5BA3A3),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // Corner decorations
            // Top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(40.dp, 4.dp)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .background(Color(0xFF5BA3A3), RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(4.dp, 40.dp)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .background(Color(0xFF5BA3A3), RoundedCornerShape(2.dp))
            )

            // Top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp, 4.dp)
                    .offset(x = 2.dp, y = (-2).dp)
                    .background(Color(0xFF5BA3A3), RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(4.dp, 40.dp)
                    .offset(x = 2.dp, y = (-2).dp)
                    .background(Color(0xFF5BA3A3), RoundedCornerShape(2.dp))
            )

            // Bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(40.dp, 4.dp)
                    .offset(x = (-2).dp, y = 2.dp)
                    .background(Color(0xFF5BA3A3), RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(4.dp, 40.dp)
                    .offset(x = (-2).dp, y = 2.dp)
                    .background(Color(0xFF5BA3A3), RoundedCornerShape(2.dp))
            )

            // Bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(40.dp, 4.dp)
                    .offset(x = 2.dp, y = 2.dp)
                    .background(Color(0xFF5BA3A3), RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(4.dp, 40.dp)
                    .offset(x = 2.dp, y = 2.dp)
                    .background(Color(0xFF5BA3A3), RoundedCornerShape(2.dp))
            )
        }
    }
}