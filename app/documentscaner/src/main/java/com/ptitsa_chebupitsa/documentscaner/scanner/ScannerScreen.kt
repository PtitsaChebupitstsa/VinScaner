package com.ptitsa_chebupitsa.documentscaner.scanner

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ptitsa_chebupitsa.documentscaner.camera.CameraPreview

@Composable
fun ScannerScreen(
    state: DocumentScannerState,
    onIntent: (DocumentScannerIntent) -> Unit,
) {
    when (state) {
        is DocumentScannerState.Initial -> {
        }
        is DocumentScannerState.Scanning -> {
        }
        is DocumentScannerState.Preview -> {
            if (state.showCamera) {
                CameraPreview(
                    onVinDetected = { vinNumber ->
                        onIntent(DocumentScannerIntent.UpdateVinNumber(vinNumber))
                        onIntent(DocumentScannerIntent.HideCamera)
                    },
                    onCloseCamera = {
                        onIntent(DocumentScannerIntent.HideCamera)
                    },
                )
            } else {
                ScannedDocumentPreview(
                    bitmap = state.bitmap,
                    vinNumber = state.vinNumber,
                    onAccept = { onIntent(DocumentScannerIntent.AcceptResult) },
                    onRetry = { onIntent(DocumentScannerIntent.RetryScanning) },
                    onVinNumberClick = { onIntent(DocumentScannerIntent.ShowCamera) },
                )
            }
        }
    }
}

@Composable
private fun ScannedDocumentPreview(
    bitmap: Bitmap,
    vinNumber: String?,
    onAccept: () -> Unit,
    onRetry: () -> Unit,
    onVinNumberClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageBitmap(bitmap)
                }
            },
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
        )

        Text(
            text = vinNumber?.let { "VIN номер: $it" } ?: "VIN номер не найден",
            style = MaterialTheme.typography.titleMedium,
            color = if (vinNumber != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier =
                Modifier
                    .padding(vertical = 16.dp)
                    .clickable(onClick = onVinNumberClick),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = onRetry,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("Повторить")
            }
        }
    }
}
