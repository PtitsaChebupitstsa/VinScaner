package com.ptitsa_chebupitsa.documentscaner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScannerViewModel(
    private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow<DocumentScannerState>(DocumentScannerState.Initial)
    val state: StateFlow<DocumentScannerState> = _state.asStateFlow()

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var isVinRecognitionInProgress = false

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }

    fun handleIntent(intent: DocumentScannerIntent) {
        when (intent) {
            is DocumentScannerIntent.StartScanning -> {
                _state.value = DocumentScannerState.Scanning
                isVinRecognitionInProgress = false
            }
            is DocumentScannerIntent.ProcessScanResult -> {
                processResult(intent.result)
            }
            is DocumentScannerIntent.RetryScanning -> {
                _state.value = DocumentScannerState.Initial
                isVinRecognitionInProgress = false
            }
            is DocumentScannerIntent.UpdateVinNumber -> {
                updateVinNumber(intent.vinNumber)
            }
            is DocumentScannerIntent.ShowCamera -> {
                showCamera()
            }
            is DocumentScannerIntent.HideCamera -> {
                hideCamera()
            }
            is DocumentScannerIntent.AcceptResult -> {
                // Handle accepting the result
            }
        }
    }

    private fun processResult(result: GmsDocumentScanningResult?) {
        if (isVinRecognitionInProgress) return

        viewModelScope.launch {
            result?.pages?.firstOrNull()?.let { page ->
                try {
                    val bitmap =
                        withContext(Dispatchers.IO) {
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(context.contentResolver, page.imageUri),
                            )
                        }
                    isVinRecognitionInProgress = true
                    recognizeVinNumber(bitmap)
                    _state.value = DocumentScannerState.Preview(bitmap, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    isVinRecognitionInProgress = false
                }
            }
        }
    }

    private fun recognizeVinNumber(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer
            .process(image)
            .addOnSuccessListener { visionText ->
                val vinNumber = findVinNumber(visionText.text)
                if (vinNumber != null) {
                    updateVinNumber(vinNumber)
                }
                isVinRecognitionInProgress = false
            }.addOnFailureListener { e ->
                e.printStackTrace()
                isVinRecognitionInProgress = false
            }
    }

    private fun updateVinNumber(vinNumber: String) {
        val currentState = _state.value
        if (currentState is DocumentScannerState.Preview) {
            _state.value = currentState.copy(vinNumber = vinNumber)
        }
    }

    private fun showCamera() {
        val currentState = _state.value
        if (currentState is DocumentScannerState.Preview) {
            _state.value = currentState.copy(showCamera = true)
        }
    }

    private fun hideCamera() {
        val currentState = _state.value
        if (currentState is DocumentScannerState.Preview) {
            _state.value = currentState.copy(showCamera = false)
        }
    }

    private fun findVinNumber(text: String): String? {
        val vinPattern = "[A-HJ-NPR-Z0-9]{17}".toRegex()
        return vinPattern.find(text.replace(" ", ""))?.value
    }
}
