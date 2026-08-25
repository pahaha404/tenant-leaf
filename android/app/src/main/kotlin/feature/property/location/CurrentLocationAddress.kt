package com.seipseip.app.feature.property.location

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

internal data class CurrentLocationSelection(
    val latitude: Double,
    val longitude: Double,
    val address: String,
)

internal suspend fun currentLocationSelection(context: Context): CurrentLocationSelection {
    val locationManager = context.getSystemService(LocationManager::class.java)
    val provider = preferredLocationProvider(
        precise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED,
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER),
        networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER),
    )
        ?: error("위치 서비스가 꺼져 있습니다.")
    val location = withTimeoutOrNull(6_000) { currentLocation(context, locationManager, provider) }
        ?: lastKnownLocation(locationManager, provider)
        ?: error("현재 위치를 찾지 못했습니다.")

    return CurrentLocationSelection(
        latitude = location.latitude,
        longitude = location.longitude,
        address = reverseGeocode(context, location.latitude, location.longitude),
    )
}

internal suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String =
    withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) error("주소 변환 서비스를 사용할 수 없습니다.")
        @Suppress("DEPRECATION")
        val resolved = Geocoder(context, Locale.KOREAN)
            .getFromLocation(latitude, longitude, 1)
            ?.firstOrNull()
            ?.getAddressLine(0)
        normalizeAddress(resolved.orEmpty()) ?: error("현재 위치의 주소를 찾지 못했습니다.")
    }

internal fun preferredLocationProvider(
    precise: Boolean,
    gpsEnabled: Boolean,
    networkEnabled: Boolean,
): String? = when {
    precise && gpsEnabled -> LocationManager.GPS_PROVIDER
    networkEnabled -> LocationManager.NETWORK_PROVIDER
    else -> null
}

internal fun locationFallbackProviders(preferred: String): List<String> =
    listOf(preferred, LocationManager.NETWORK_PROVIDER).distinct()

@SuppressLint("MissingPermission")
private fun lastKnownLocation(locationManager: LocationManager, preferred: String): Location? =
    locationFallbackProviders(preferred)
        .filter(locationManager::isProviderEnabled)
        .mapNotNull(locationManager::getLastKnownLocation)
        .maxByOrNull(Location::getTime)

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
