package com.seipseip.app.feature.property.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraPosition
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppPageScaffold
import com.seipseip.app.feature.common.PrimaryButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun LocationPickerScreen(onBack: () -> Unit, onConfirmed: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selection by remember { mutableStateOf<CurrentLocationSelection?>(null) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lookupJob by remember { mutableStateOf<Job?>(null) }

    val loadCurrentLocation: () -> Unit = {
        loading = true
        error = null
        scope.launch {
            runCatching { currentLocationSelection(context) }
                .onSuccess {
                    selection = it
                    selectedAddress = it.address
                }
                .onFailure { error = "현재 위치를 찾지 못했어요. 위치 서비스를 확인해 주세요." }
            loading = false
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) loadCurrentLocation()
        else error = "지도에서 내 위치를 사용하려면 위치 권한이 필요해요."
    }

    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        if (permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            loadCurrentLocation()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    DisposableEffect(Unit) {
        onDispose { lookupJob?.cancel() }
    }

    AppPageScaffold(title = "위치 선택", onBack = onBack, scrollable = false) {
        Text("지도를 움직여 핀을 정확한 위치에 맞춰 주세요.", color = DeepGreen, fontSize = 18.sp)
        Box(
            modifier = Modifier.fillMaxWidth().height(440.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFE9EEE8)),
            contentAlignment = Alignment.Center,
        ) {
            selection?.let { initial ->
                KakaoLocationMap(
                    initial = initial,
                    onMapError = { error = "지도를 불러오지 못했어요." },
                    onCenterChanged = { latitude, longitude ->
                        loading = true
                        error = null
                        selectedAddress = null
                        lookupJob?.cancel()
                        lookupJob = scope.launch {
                            runCatching { reverseGeocode(context, latitude, longitude) }
                                .onSuccess { selectedAddress = it }
                                .onFailure { error = "선택한 위치의 주소를 찾지 못했어요." }
                            loading = false
                        }
                    },
                )
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "선택 위치",
                    tint = Orange,
                    modifier = Modifier.align(Alignment.Center).offset(y = (-22).dp).size(48.dp),
                )
            }
            if (loading && selection == null) CircularProgressIndicator(color = Green)
            if (!loading && selection == null) Text(error ?: "현재 위치를 준비하고 있어요.", color = Secondary, fontSize = 13.sp)
        }
        Text(
            when {
                loading -> "주소를 확인하고 있어요."
                selectedAddress != null -> selectedAddress.orEmpty()
                else -> error ?: "핀 위치의 주소를 확인해 주세요."
            },
            color = if (error != null && selectedAddress == null) Orange else DeepGreen,
            fontSize = 14.sp,
        )
        PrimaryButton(
            "이 위치 선택",
            { selectedAddress?.let(onConfirmed) },
            enabled = selectedAddress != null && !loading,
        )
    }
}

@Composable
private fun KakaoLocationMap(
    initial: CurrentLocationSelection,
    onMapError: () -> Unit,
    onCenterChanged: (Double, Double) -> Unit,
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        onDispose { mapView?.finish() }
    }

    AndroidView(
        factory = {
            MapView(context).also { view ->
                mapView = view
                view.start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() = Unit
                        override fun onMapError(error: Exception) = onMapError()
                    },
                    object : KakaoMapReadyCallback() {
                        override fun getPosition(): LatLng = LatLng.from(initial.latitude, initial.longitude)
                        override fun getZoomLevel(): Int = 18

                        override fun onMapReady(kakaoMap: KakaoMap) {
                            kakaoMap.setOnCameraMoveEndListener(
                                object : KakaoMap.OnCameraMoveEndListener {
                                    override fun onCameraMoveEnd(
                                        map: KakaoMap,
                                        position: CameraPosition,
                                        gestureType: GestureType,
                                    ) {
                                        onCenterChanged(position.position.latitude, position.position.longitude)
                                    }
                                },
                            )
                        }
                    },
                )
            }
        },
        modifier = Modifier.fillMaxWidth().height(440.dp),
    )
}
