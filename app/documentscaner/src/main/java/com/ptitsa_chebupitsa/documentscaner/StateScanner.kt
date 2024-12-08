package com.ptitsa_chebupitsa.documentscaner

import android.graphics.Bitmap

sealed class DocumentScannerState {
    object Initial : DocumentScannerState()

    object Scanning : DocumentScannerState()

    data class Preview(
        val bitmap: Bitmap,
        val vinNumber: String?,
        val showCamera: Boolean = false,
    ) : DocumentScannerState()
}
