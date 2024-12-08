package com.ptitsa_chebupitsa.documentscaner.camera

sealed class CameraPreviewState {
    object Loading : CameraPreviewState()

    data class Ready(
        val isVinDetected: Boolean = false,
        val lastDetectedVin: String? = null,
    ) : CameraPreviewState()

    object Error : CameraPreviewState()
}
