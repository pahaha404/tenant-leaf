package com.seipseip.feature.property.presentation

import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.seipseip.core.ui.ContentState

class ComposeTestActivity : ComponentActivity() {
    var createRequests: Int = 0
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PropertyListScreen(
                    state = ContentState.Empty,
                    onRetry = {},
                    onCreate = { createRequests += 1 },
                    onSelect = {},
                )
            }
        }
    }
}
