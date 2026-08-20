package com.seipseip.app.feature.property.location

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class LocationPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LocationPickerScreen(
                    onBack = ::finish,
                    onConfirmed = { address ->
                        setResult(RESULT_OK, Intent().putExtra(EXTRA_ADDRESS, address))
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_ADDRESS = "address"
    }
}
