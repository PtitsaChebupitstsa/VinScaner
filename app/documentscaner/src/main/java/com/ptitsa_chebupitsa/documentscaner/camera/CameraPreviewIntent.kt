package com.ptitsa_chebupitsa.documentscaner.camera

sealed class CameraPreviewIntent {
    object Initialize : CameraPreviewIntent()

    data class OnVinDetected(
        val vin: String,
    ) : CameraPreviewIntent()

    object OnError : CameraPreviewIntent()

    object CloseCamera : CameraPreviewIntent()
}
