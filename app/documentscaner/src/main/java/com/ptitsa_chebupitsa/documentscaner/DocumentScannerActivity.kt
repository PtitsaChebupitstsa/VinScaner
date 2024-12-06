package com.ptitsa_chebupitsa.documentscaner
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ptitsa_chebupitsa.documentscaner.ui.theme.VinScanerTheme
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class DocumentScannerActivity : ComponentActivity() {
    private val scanner = GmsDocumentScanning.getClient(GmsDocumentScannerOptions.Builder().build())
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var scannedVinNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startScanning()
    }

    private fun startScanning() {
        scanner
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                try {
                    startIntentSenderForResult(
                        intentSender,
                        SCANNER_REQUEST_CODE,
                        null,
                        0,
                        0,
                        0
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("Scanner", "Error starting scanner", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Scanner", "Error getting scanner intent", e)
            }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCANNER_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    val result = GmsDocumentScanningResult.fromActivityResultIntent(data)
                    result?.pages?.let { pages ->
                        if (pages.isNotEmpty()) {
                            val firstPage = pages[0]
                            val imageUri = firstPage.imageUri
                            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)

                            // Распознаем текст на изображении
                            recognizeVinNumber(bitmap)

                            setContent {
                                VinScanerTheme {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.background
                                    ) {
                                        ScannedDocumentPreview(
                                            bitmap = bitmap,
                                            vinNumber = scannedVinNumber,  // Передаем найденный VIN
                                            onAccept = { returnResult(bitmap) },
                                            onRetry = { startScanning() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                RESULT_CANCELED -> {
                    finish()
                }
            }
        }
    }


    private fun returnResult(bitmap: Bitmap) {
        val file = File(cacheDir, "scanned_document.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        setResult(RESULT_OK, Intent().apply {
            data = FileProvider.getUriForFile(
                this@DocumentScannerActivity,
                "${packageName}.provider",
                file
            )
        })
        finish()
    }

    private fun recognizeVinNumber(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                scannedVinNumber = findVinNumber(visionText.text)
            }
            .addOnFailureListener { e ->
                Log.e("TextRecognition", "Error recognizing text", e)
                scannedVinNumber = null
            }
    }

    private fun findVinNumber(text: String): String? {
        // Паттерн для поиска VIN номера
        // VIN состоит из 17 символов, исключая буквы I, O, Q
        val vinPattern = "[A-HJ-NPR-Z0-9]{17}".toRegex()
        return vinPattern.find(text.replace(" ", ""))?.value
    }


    private fun File.toBitmap(): Bitmap {
        return BitmapFactory.decodeFile(absolutePath)
    }

    companion object {
        private const val SCANNER_REQUEST_CODE = 1
    }
}

@Composable
private fun ScannedDocumentPreview(
    bitmap: Bitmap,
    vinNumber: String?,
    onAccept: () -> Unit,
    onRetry: () -> Unit
) {
    var showCamera by remember { mutableStateOf(false) }
    var currentVinNumber by remember { mutableStateOf(vinNumber) }

    if (showCamera) {
        CameraPreview(
            onVinDetected = { detectedVin ->
                showCamera = false
                currentVinNumber = detectedVin
            },
            onCloseCamera = {
                showCamera = false
            }
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageBitmap(bitmap)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Кликабельный VIN номер
            Text(
                text = currentVinNumber?.let { "VIN номер: $it" } ?: "VIN номер не найден",
                style = MaterialTheme.typography.titleMedium,
                color = if (currentVinNumber != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .clickable { showCamera = true }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Повторить")
                }

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Принять")
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    onVinDetected: (String) -> Unit,
    onCloseCamera: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Добавляем функцию поиска VIN номера
    fun findVinNumber(text: String): String? {
        // VIN состоит из 17 символов, исключая буквы I, O, Q
        val vinPattern = "[A-HJ-NPR-Z0-9]{17}".toRegex()
        return vinPattern.find(text.replace(" ", ""))?.value
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        ) { view ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .setTargetResolution(android.util.Size(1280, 720))
                    .build()
                    .also { it.setSurfaceProvider(view.surfaceProvider) }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(cameraExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                textRecognizer.process(image)
                                    .addOnSuccessListener { visionText ->
                                        val vinNumber = findVinNumber(visionText.text)
                                        if (vinNumber != null) {
                                            onVinDetected(vinNumber)
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer
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
                style = Stroke(width = 8f)
            )
        }

        IconButton(
            onClick = onCloseCamera,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close camera",
                tint = Color.White
            )
        }
    }

}
