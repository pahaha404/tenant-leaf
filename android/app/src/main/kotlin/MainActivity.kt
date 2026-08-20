package com.seipseip.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat
import com.meta.wearable.dat.core.Wearables
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var contentSet = false
    private var datInitialized = false

    private val datPermissionsLauncher =
        registerForActivityResult(RequestMultiplePermissions()) {
            initializeDatWhenPermitted()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        if (hasDatPermissions()) {
            initializeDatWhenPermitted()
        } else {
            datPermissionsLauncher.launch(datPermissions)
        }
    }

    private fun initializeDatWhenPermitted() {
        if (!hasDatPermissions()) {
            showApp()
            return
        }
        if (!datInitialized) {
            Wearables.initialize(this)
                .onSuccess { datInitialized = true }
                .onFailure { error, _ -> Log.e(DAT_TAG, "DAT initialization failed: ${error.description}") }
        }
        showApp()
    }

    private fun showApp() {
        if (contentSet) return
        contentSet = true
        setContent { TenantLeafApp() }
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
