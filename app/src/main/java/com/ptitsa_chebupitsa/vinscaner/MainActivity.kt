package com.ptitsa_chebupitsa.vinscaner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.ptitsa_chebupitsa.documentscaner.DocumentScannerActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, DocumentScannerActivity::class.java)
        startActivity(intent)
    }
}
