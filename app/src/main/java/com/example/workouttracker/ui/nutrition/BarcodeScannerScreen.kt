package com.example.workouttracker.ui.nutrition

import android.graphics.RectF
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import java.util.concurrent.atomic.AtomicBoolean

private const val CONSENSUS_N = 3
private const val BBOX_WIDTH_THRESHOLD = 0.30f
private const val FRAME_THROTTLE_MS = 220L
private const val TIMEOUT_STREAK_MS = 1_500L

@Composable
fun BarcodeScannerScreen(
    viewModel: NutritionViewModel,
    onClose: () -> Unit,
    onDetected: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val scannerOptions = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE
            )
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(scannerOptions) }

    var hasDetected by remember { mutableStateOf(false) }
    var streak by remember { mutableIntStateOf(0) }
    var lastDetected by remember { mutableStateOf<String?>(null) }
    var lastProcessedAt by remember { mutableLongStateOf(0L) }
    var lastStreakAt by remember { mutableLongStateOf(0L) }
    var lastBoundingBox by remember { mutableStateOf<RectF?>(null) }
    val detectionLock = remember { AtomicBoolean(false) }

    fun resetStreak() {
        streak = 0
        lastDetected = null
        lastBoundingBox = null
    }

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
                        .setTargetResolution(android.util.Size(1280, 720))
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        val now = SystemClock.elapsedRealtime()

                        if (mediaImage == null || detectionLock.get()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        if (now - lastProcessedAt < FRAME_THROTTLE_MS) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        if (now - lastStreakAt > TIMEOUT_STREAK_MS) {
                            mainExecutor.execute { resetStreak() }
                        }
                        lastProcessedAt = now

                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        val frameWidth = imageProxy.width

                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val candidate = barcodes
                                    .sortedByDescending { it.boundingBox?.width() ?: 0 }
                                    .firstOrNull { barcode ->
                                        val boxWidth = barcode.boundingBox?.width()?.toFloat() ?: 0f
                                        val widthRatio = if (frameWidth > 0) boxWidth / frameWidth else 0f
                                        val rawCode = BarcodeScannerParser.extractCode(listOf(barcode))
                                        val normalized = rawCode?.let(::normalizeBarcode)
                                        val hasCorrectLength = normalized != null && isLengthAllowed(normalized, barcode.format)
                                        widthRatio >= BBOX_WIDTH_THRESHOLD && hasCorrectLength
                                    }

                                val detectedCode = candidate
                                    ?.let { BarcodeScannerParser.extractCode(listOf(it)) }
                                    ?.let(::normalizeBarcode)
                                    ?.takeIf(::isValidEan)

                                if (detectedCode != null) {
                                    lastStreakAt = SystemClock.elapsedRealtime()
                                    mainExecutor.execute {
                                        lastBoundingBox = candidate?.boundingBox?.let { box ->
                                            RectF(
                                                box.left.toFloat(),
                                                box.top.toFloat(),
                                                box.right.toFloat(),
                                                box.bottom.toFloat()
                                            )
                                        }
                                        if (detectedCode == lastDetected) {
                                            streak += 1
                                        } else {
                                            lastDetected = detectedCode
                                            streak = 1
                                        }

                                        if (streak >= CONSENSUS_N && detectionLock.compareAndSet(false, true)) {
                                            hasDetected = true
                                            cameraProvider.unbindAll()
                                            viewModel.lookupBarcode(detectedCode)
                                            onDetected?.invoke(detectedCode)
                                            onClose()
                                        }
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
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

        lastBoundingBox?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.65f)
                    .height(140.dp)
                    .border(
                        width = 3.dp,
                        color = if (streak > 0) Color(0xFF4CAF50) else Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }

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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    MaterialTheme.shapes.medium
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Поднесите штрихкод, дождитесь зелёной рамки")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(CONSENSUS_N) { index ->
                    val segmentColor = when {
                        hasDetected || index < streak -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(8.dp)
                            .background(segmentColor, RoundedCornerShape(10.dp))
                    )
                }
            }
        }
    }
}

private fun isLengthAllowed(code: String, format: Int): Boolean {
    return when (format) {
        Barcode.FORMAT_EAN_13 -> code.length == 13
        Barcode.FORMAT_UPC_A -> code.length == 12
        Barcode.FORMAT_EAN_8,
        Barcode.FORMAT_UPC_E -> code.length == 8
        else -> code.length in setOf(8, 12, 13)
    }
}

fun normalizeBarcode(raw: String): String? {
    val normalized = raw.trim().replace(Regex("\\s+"), "").filter { it.isDigit() }
    if (normalized.isBlank()) return null
    return normalized
}

fun isValidEan(code: String): Boolean {
    return when (code.length) {
        8 -> isValidChecksum(code, payloadLength = 7)
        12 -> isValidChecksum(code, payloadLength = 11)
        13 -> isValidChecksum(code, payloadLength = 12)
        else -> false
    }
}

private fun isValidChecksum(code: String, payloadLength: Int): Boolean {
    val digits = code.filter { it.isDigit() }
    if (digits.length != payloadLength + 1) return false

    val weightedSum = digits.take(payloadLength).mapIndexed { index, char ->
        val value = char - '0'
        val multiplier = if ((payloadLength - index) % 2 == 0) 3 else 1
        value * multiplier
    }.sum()

    val expectedCheckDigit = (10 - (weightedSum % 10)) % 10
    return expectedCheckDigit == (digits.last() - '0')
}
