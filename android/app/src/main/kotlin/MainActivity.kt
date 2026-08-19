package com.seipseip.app

import android.os.Bundle
import android.app.Application
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.meta.wearable.dat.core.Wearables

class TenantLeafApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Wearables.initialize(this).onFailure { error, _ -> Log.e("TenantLeafDAT", error.description) }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TenantLeafApp() }
    }
}
