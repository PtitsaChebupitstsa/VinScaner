package com.ptitsa_chebupitsa.documentscaner
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.ptitsa_chebupitsa.documentscaner.ui.theme.VinScanerTheme
import kotlinx.coroutines.launch

class DocumentScannerActivity : ComponentActivity() {
    private val scanner =
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions
                .Builder()
                .setGalleryImportAllowed(true)
                .setScannerMode(SCANNER_MODE_FULL)
                .build(),
        )

    private val viewModel by viewModels<ScannerViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ScannerViewModel(this@DocumentScannerActivity) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state is DocumentScannerState.Initial) {
                    startScanning()
                }
            }
        }

        viewModel.handleIntent(DocumentScannerIntent.StartScanning)

        setContent {
            VinScanerTheme {
                ScannerScreen(
                    state = viewModel.state.collectAsState().value,
                    onIntent = viewModel::handleIntent,
                )
            }
        }
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
                        0,
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("Scanner", "Error starting scanner", e)
                }
            }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCANNER_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    val result = GmsDocumentScanningResult.fromActivityResultIntent(data)
                    viewModel.handleIntent(DocumentScannerIntent.ProcessScanResult(result))
                }
                RESULT_CANCELED -> {
                    finish()
                }
            }
        }
    }

    companion object {
        private const val SCANNER_REQUEST_CODE = 1
    }
}
