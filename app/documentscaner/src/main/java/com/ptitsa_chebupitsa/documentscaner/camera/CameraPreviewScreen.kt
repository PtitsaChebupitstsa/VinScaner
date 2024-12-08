package com.ptitsa_chebupitsa.documentscaner.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    onVinDetected: (String) -> Unit,
    onCloseCamera: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                textRecognizer.close()
                cameraExecutor.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        ) { view ->
            cameraProviderFuture.addListener({
                try {
                    cameraProvider?.unbindAll()

                    cameraProvider = cameraProviderFuture.get()

                    val preview =
                        Preview
                            .Builder()
                            .setTargetResolution(android.util.Size(1280, 720))
                            .build()
                            .also { it.setSurfaceProvider(view.surfaceProvider) }

                    val imageAnalyzer =
                        ImageAnalysis
                            .Builder()
                            .setTargetResolution(android.util.Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .apply {
                                setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image =
                                            InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees,
                                            )

                                        textRecognizer
                                            .process(image)
                                            .addOnSuccessListener { visionText ->
                                                val vinPattern = "[A-HJ-NPR-Z0-9]{17}".toRegex()
                                                val vinNumber = vinPattern.find(visionText.text.replace(" ", ""))?.value
                                                if (vinNumber != null) {
                                                    onVinDetected(vinNumber)
                                                }
                                            }.addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val rectWidth = size.width * 0.8f
            val rectHeight = size.height * 0.1f
            val left = (size.width - rectWidth) / 2
            val top = (size.height - rectHeight) / 2

            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                style = Stroke(width = 8f),
            )
        }

        IconButton(
            onClick = {
                try {
                    cameraProvider?.unbindAll()
                    onCloseCamera()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close camera",
                tint = Color.White,
            )
        }
    }
}
