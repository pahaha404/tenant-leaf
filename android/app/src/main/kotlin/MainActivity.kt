package com.seipseip.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.meta.wearable.dat.core.Wearables
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val datPermissionsLauncher =
        registerForActivityResult(RequestMultiplePermissions()) {
            initializeDatWhenPermitted()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        com.seipseip.app.feature.inspection.VoiceGuideManager.warmUp(this)
        setContent {
            TenantLeafApp()
        }
        checkAndInitializeDat()
    }

    private fun checkAndInitializeDat() {
        if (hasDatPermissions()) {
            initializeDatWhenPermitted()
        } else {
            datPermissionsLauncher.launch(datPermissions)
        }
    }

    private fun initializeDatWhenPermitted() {
        if (!hasDatPermissions()) return
        Wearables.initialize(this)
            .onSuccess { Log.d(DAT_TAG, "DAT initialized successfully") }
            .onFailure { error, _ -> Log.e(DAT_TAG, "DAT initialization failed: ${error.description}") }
    }

    private fun hasDatPermissions(): Boolean =
        datPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private val datPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            emptyArray()
        }

    private companion object {
        const val DAT_TAG = "TenantLeafDAT"
    }
}
