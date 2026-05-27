package com.example.workouttracker.feature.nutrition.presentation

import android.os.SystemClock
import androidx.camera.core.CameraInfoUnavailableException
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    onError: (String) -> Unit,
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
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_QR_CODE
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
                    val cameraProvider = runCatching { cameraProviderFuture.get() }
                        .getOrElse {
                            onError("Не удалось инициализировать камеру: ${it.message.orEmpty()}")
                            return@addListener
                        }

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastAnalyzedAt < THROTTLE_MS || hasDetected) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        lastAnalyzedAt = now

                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val candidate = barcodes
                                    .sortedByDescending { barcode ->
                                        val bbox = barcode.boundingBox
                                        if (bbox == null) 0 else bbox.width() * bbox.height()
                                    }
                                    .firstNotNullOfOrNull { barcode ->
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
                            .addOnFailureListener {
                                streakCount = 0
                                progress = 0f
                                onError("Ошибка распознавания штрихкода: ${it.message.orEmpty()}")
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }

                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    }.onFailure {
                        val message = if (it is CameraInfoUnavailableException) {
                            "Камера недоступна на этом устройстве."
                        } else {
                            "Не удалось запустить камеру: ${it.message.orEmpty()}"
                        }
                        onError(message)
                    }
                }, mainExecutor)

                previewView
            }
        )

        IconButton(
            onClick = {
                runCatching { cameraProviderFuture.get().unbindAll() }
                onClose()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Закрыть сканер")
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Наведите камеру на штрихкод",
                    style = MaterialTheme.typography.titleSmall
                )
                LinearProgressIndicator(progress = { progress })
                Text(
                    text = "Когда код уверенно считается несколько раз подряд, мы автоматически подтянем продукт.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
