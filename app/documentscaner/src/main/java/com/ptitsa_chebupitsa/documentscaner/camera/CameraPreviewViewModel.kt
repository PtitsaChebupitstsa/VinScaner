package com.ptitsa_chebupitsa.documentscaner.camera

import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraPreviewViewModel : ViewModel() {
    private val _state = MutableStateFlow<CameraPreviewState>(CameraPreviewState.Loading)
    val state: StateFlow<CameraPreviewState> = _state.asStateFlow()

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun handleIntent(intent: CameraPreviewIntent) {
        when (intent) {
            is CameraPreviewIntent.Initialize -> {
                _state.value = CameraPreviewState.Ready()
            }
            is CameraPreviewIntent.OnVinDetected -> {
                val currentState = _state.value
                if (currentState is CameraPreviewState.Ready) {
                    _state.value =
                        currentState.copy(
                            isVinDetected = true,
                            lastDetectedVin = intent.vin,
                        )
                }
            }
            is CameraPreviewIntent.OnError -> {
                _state.value = CameraPreviewState.Error
            }
            is CameraPreviewIntent.CloseCamera -> {
                // Handle closing camera
            }
        }
    }

    private fun findVinNumber(text: String): String? {
        val vinPattern = "[A-HJ-NPR-Z0-9]{17}".toRegex()
        return vinPattern.find(text.replace(" ", ""))?.value
    }
}
