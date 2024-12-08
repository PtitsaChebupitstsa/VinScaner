package com.ptitsa_chebupitsa.documentscaner.scanner

import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

sealed class DocumentScannerIntent {
    object StartScanning : DocumentScannerIntent()

    object RetryScanning : DocumentScannerIntent()

    object AcceptResult : DocumentScannerIntent()

    data class ProcessScanResult(
        val result: GmsDocumentScanningResult?,
    ) : DocumentScannerIntent()

    data class UpdateVinNumber(
        val vinNumber: String,
    ) : DocumentScannerIntent()

    object ShowCamera : DocumentScannerIntent()

    object HideCamera : DocumentScannerIntent()
}
