package com.seipseip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.seipseip.app.navigation.TenantLeafNavHost
import com.seipseip.app.ui.theme.TenantLeafTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TenantLeafTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TenantLeafNavHost()
                }
            }
        }
    }
}
