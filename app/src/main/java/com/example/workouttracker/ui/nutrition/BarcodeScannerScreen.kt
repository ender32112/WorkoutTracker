package com.example.workouttracker.ui.nutrition

import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val THROTTLE_MS = 220L
private const val STREAK_TARGET = 3
private const val STREAK_TIMEOUT_MS = 1500L

@Composable
fun BarcodeScannerScreen(
    onDetected: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E
                )
                .build()
        )
    }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var lastAnalyzedAt by remember { mutableStateOf(0L) }
    var streakValue by remember { mutableStateOf<String?>(null) }
    var streakCount by remember { mutableStateOf(0) }
    var streakAt by remember { mutableStateOf(0L) }
    var progress by remember { mutableFloatStateOf(0f) }
    var hasDetected by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
            scanner.close()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val mainExecutor = ContextCompat.getMainExecutor(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastAnalyzedAt < THROTTLE_MS || hasDetected) {
                            imageProxy.close(); return@setAnalyzer
                        }
                        lastAnalyzedAt = now

                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close(); return@setAnalyzer
                        }

                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        val frameWidth = image.width

                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val candidate = barcodes.firstNotNullOfOrNull { barcode ->
                                    val bbox = barcode.boundingBox ?: return@firstNotNullOfOrNull null
                                    if (bbox.width() < frameWidth * 0.30f) return@firstNotNullOfOrNull null
                                    BarcodeScannerParser.normalize(barcode.rawValue)
                                }

                                if (candidate == null) {
                                    streakCount = 0
                                    progress = 0f
                                    return@addOnSuccessListener
                                }

                                if (streakValue == candidate && now - streakAt <= STREAK_TIMEOUT_MS) {
                                    streakCount += 1
                                } else {
                                    streakValue = candidate
                                    streakCount = 1
                                }
                                streakAt = now
                                progress = (streakCount.toFloat() / STREAK_TARGET).coerceIn(0f, 1f)

                                if (streakCount >= STREAK_TARGET) {
                                    hasDetected = true
                                    cameraProvider.unbindAll()
                                    onDetected(candidate)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                }, mainExecutor)

                previewView
            }
        )

        IconButton(
            onClick = {
                runCatching { cameraProviderFuture.get().unbindAll() }
                onClose()
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Закрыть сканер")
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), MaterialTheme.shapes.medium)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(progress = { progress })
            Text("Наведите камеру на штрихкод")
        }
    }
}
