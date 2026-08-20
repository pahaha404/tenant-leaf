package com.seipseip.app.feature.property.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

internal suspend fun currentLocationAddress(context: Context): String {
    val locationManager = context.getSystemService(LocationManager::class.java)
    val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        .firstOrNull(locationManager::isProviderEnabled)
        ?: error("위치 서비스가 꺼져 있습니다.")
    val location = currentLocation(context, locationManager, provider)
        ?: error("현재 위치를 찾지 못했습니다.")

    return withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) error("주소 변환 서비스를 사용할 수 없습니다.")
        @Suppress("DEPRECATION")
        val resolved = Geocoder(context, Locale.KOREAN)
            .getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()
            ?.getAddressLine(0)
        normalizeAddress(resolved.orEmpty()) ?: error("현재 위치의 주소를 찾지 못했습니다.")
    }
}

@SuppressLint("MissingPermission")
private suspend fun currentLocation(
    context: Context,
    locationManager: LocationManager,
    provider: String,
): Location? = suspendCancellableCoroutine { continuation ->
    val cancellationSignal = CancellationSignal()
    continuation.invokeOnCancellation { cancellationSignal.cancel() }
    LocationManagerCompat.getCurrentLocation(
        locationManager,
        provider,
        cancellationSignal,
        ContextCompat.getMainExecutor(context),
        continuation::resume,
    )
}
