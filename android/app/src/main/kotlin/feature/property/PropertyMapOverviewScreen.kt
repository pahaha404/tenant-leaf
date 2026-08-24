package com.seipseip.app.feature.property

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.PaleGreen
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.PrimaryButton
import com.seipseip.app.feature.common.StateBadge
import com.seipseip.app.feature.property.location.KakaoAddressSearch
import com.seipseip.app.feature.property.location.fastCurrentCoordinates
import kotlinx.coroutines.launch

private val DEFAULT_CENTER_SEOUL = LatLng.from(37.5665, 126.9780)

@Composable
fun PropertyMapOverviewScreen(
    properties: List<PropertyUiModel>,
    loading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onOpenProperty: (String) -> Unit,
    onAddProperty: () -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedPropertyId by rememberSaveable { mutableStateOf<String?>(null) }
    val propertyCoordinates = remember { mutableStateMapOf<String, Pair<Double, Double>>() }
    var geocodingDone by remember { mutableStateOf(false) }
    var kakaoMapInstance by remember { mutableStateOf<KakaoMap?>(null) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

    // Fast user GPS coordinates state (lat to lng)
    var userCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val moveToMyLocation: () -> Unit = {
        // 1. If we already have coordinates cached, move immediately (0ms!)
        userCoordinates?.let { coords ->
            kakaoMapInstance?.moveCamera(
                CameraUpdateFactory.newCenterPosition(
                    LatLng.from(coords.first, coords.second)
                )
            )
        }
        // 2. Concurrently fetch fresh/last-known coordinates to ensure accuracy
        scope.launch {
            val freshCoords = fastCurrentCoordinates(context)
            if (freshCoords != null) {
                userCoordinates = freshCoords
                kakaoMapInstance?.moveCamera(
                    CameraUpdateFactory.newCenterPosition(
                        LatLng.from(freshCoords.first, freshCoords.second)
                    )
                )
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) moveToMyLocation()
    }

    // Request location permission & immediately fetch GPS coordinates on enter
    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        if (permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            moveToMyLocation()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    // Resolve coordinates for all properties
    LaunchedEffect(properties) {
        geocodingDone = false
        properties.forEach { prop ->
            if (!propertyCoordinates.containsKey(prop.id) && prop.address.isNotBlank()) {
                val coords = KakaoAddressSearch.resolveAddressLocation(context, prop.address)
                if (coords != null) {
                    propertyCoordinates[prop.id] = coords
                }
            }
        }
        geocodingDone = true
    }

    // Lifecycle handling for MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewInstance?.resume()
                Lifecycle.Event.ON_PAUSE -> mapViewInstance?.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Update labels whenever coordinates, selection, or user location changes
    LaunchedEffect(kakaoMapInstance, propertyCoordinates.toMap(), selectedPropertyId, userCoordinates) {
        val map = kakaoMapInstance ?: return@LaunchedEffect
        val labelManager = map.labelManager ?: return@LaunchedEffect
        val layer = labelManager.layer ?: return@LaunchedEffect

        layer.removeAll()

        // 1. My current location pulse marker
        val myCoords = userCoordinates
        if (myCoords != null) {
            val myLocBitmap = createMyLocationMarkerBitmap(context)
            val myLocStyle = labelManager.addLabelStyles(
                LabelStyles.from(LabelStyle.from(myLocBitmap).setAnchorPoint(0.5f, 0.5f))
            )
            layer.addLabel(
                LabelOptions.from(LatLng.from(myCoords.first, myCoords.second))
                    .setStyles(myLocStyle)
                    .setTag("MY_LOCATION")
            )
        }

        // 2. Property pins: Clean minimal red pin by default, speech bubble when selected
        properties.forEach { property ->
            val coords = propertyCoordinates[property.id] ?: return@forEach
            val isSelected = property.id == selectedPropertyId
            val markerBitmap = if (isSelected) {
                createSelectedPinBitmap(context, property.name)
            } else {
                createUnselectedPinBitmap(context)
            }
            val style = labelManager.addLabelStyles(
                LabelStyles.from(LabelStyle.from(markerBitmap).setAnchorPoint(0.5f, 1.0f))
            )
            layer.addLabel(
                LabelOptions.from(LatLng.from(coords.first, coords.second))
                    .setStyles(style)
                    .setTag(property.id)
            )
        }
    }

    val selectedProperty = properties.firstOrNull { it.id == selectedPropertyId }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full Interactive Map
        AndroidView(
            factory = { ctx ->
                MapView(ctx).also { view ->
                    mapViewInstance = view
                    val initialCenter = userCoordinates?.let {
                        LatLng.from(it.first, it.second)
                    } ?: propertyCoordinates.values.firstOrNull()?.let {
                        LatLng.from(it.first, it.second)
                    } ?: DEFAULT_CENTER_SEOUL

                    view.start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() = Unit
                            override fun onMapError(error: Exception) = Unit
                        },
                        object : KakaoMapReadyCallback() {
                            override fun getPosition(): LatLng = initialCenter
                            override fun getZoomLevel(): Int = 16

                            override fun onMapReady(kakaoMap: KakaoMap) {
                                kakaoMapInstance = kakaoMap
                                // Enable all interactive gestures: Pan, Zoom, Rotate, Tilt, Pinch
                                GestureType.entries.forEach { gesture ->
                                    kakaoMap.setGestureEnable(gesture, true)
                                }
                                // Move to my location if available
                                moveToMyLocation()

                                kakaoMap.setOnLabelClickListener { _, _, label ->
                                    val tag = label.tag as? String
                                    if (tag != null && tag != "MY_LOCATION") {
                                        selectedPropertyId = tag
                                        val targetCoords = propertyCoordinates[tag]
                                        if (targetCoords != null) {
                                            kakaoMap.moveCamera(
                                                CameraUpdateFactory.newCenterPosition(
                                                    LatLng.from(targetCoords.first, targetCoords.second)
                                                )
                                            )
                                        }
                                    }
                                    true
                                }
                            }
                        },
                    )
                    view.doOnAttach {
                        view.post {
                            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                view.resume()
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Top Header Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = DeepGreen,
                        )
                    }
                    Text(
                        text = "매물 지도",
                        color = DeepGreen,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    StateBadge("매물 ${properties.size}개")
                }
                IconButton(onClick = onAddProperty, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "새 매물 등록",
                        tint = Green,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // 3. Recenter to My Location FAB Button (Instant 0ms response!)
        FloatingActionButton(
            onClick = moveToMyLocation,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (selectedProperty != null) 210.dp else 24.dp)
                .size(46.dp),
            containerColor = Color.White,
            contentColor = DeepGreen,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Outlined.MyLocation,
                contentDescription = "내 위치로 이동",
                modifier = Modifier.size(22.dp),
            )
        }

        // 4. Selected Property Bottom Floating Card
        if (selectedProperty != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedProperty.name,
                                color = DeepGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = Secondary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = selectedProperty.address,
                                    color = Secondary,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                        IconButton(
                            onClick = { selectedPropertyId = null },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "닫기",
                                tint = Secondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    // Financial metrics row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PaleGreen.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)) {
                                Text("보증금", color = Secondary, fontSize = 9.5.sp)
                                Text(
                                    selectedProperty.depositAmount?.let(::formatWon) ?: "미입력",
                                    color = DeepGreen,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3F4F6),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)) {
                                Text("월세", color = Secondary, fontSize = 9.5.sp)
                                Text(
                                    selectedProperty.monthlyRentAmount?.let(::formatWon) ?: "미입력",
                                    color = DeepGreen,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3F4F6),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)) {
                                Text("관리비", color = Secondary, fontSize = 9.5.sp)
                                Text(
                                    selectedProperty.maintenanceFeeAmount?.let(::formatWon) ?: "미입력",
                                    color = DeepGreen,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    PrimaryButton(
                        label = "이 매물 상세보기",
                        onClick = { onOpenProperty(selectedProperty.id) },
                    )
                }
            }
        }

        // 5. Empty properties overlay
        if (!loading && properties.isEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("등록된 매물이 없어요", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("새 매물을 등록하면 지도에서 핀으로 한눈에 확인할 수 있어요.", color = Secondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    PrimaryButton(label = "새 매물 등록하기", onClick = onAddProperty)
                }
            }
        }

        // 6. Loading Indicator
        if (loading || (!geocodingDone && properties.isNotEmpty())) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 68.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Green,
                        strokeWidth = 2.dp,
                    )
                    Text("매물 위치를 불러오는 중...", color = DeepGreen, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Clean, compact red pinpoint marker for unselected properties (No text bubble = Zero collision!).
 */
private fun createUnselectedPinBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val width = (22 * density).toInt()
    val height = (26 * density).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val centerX = width / 2f
    val pinCenterY = 9 * density
    val radius = 7.5f * density

    // Drop shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#40000000")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, height - (2 * density), 4 * density, shadowPaint)

    // Red pin circle
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E11D48")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, pinCenterY, radius, pinPaint)

    // White center dot
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, pinCenterY, radius * 0.42f, whitePaint)

    // Pin pointer triangle
    val path = Path().apply {
        moveTo(centerX - radius * 0.72f, pinCenterY + radius * 0.32f)
        lineTo(centerX + radius * 0.72f, pinCenterY + radius * 0.32f)
        lineTo(centerX, height.toFloat() - (3 * density))
        close()
    }
    canvas.drawPath(path, pinPaint)

    return bitmap
}

/**
 * Expanded speech bubble badge + highlighted red pin when selected.
 */
private fun createSelectedPinBitmap(context: Context, name: String): Bitmap {
    val density = context.resources.displayMetrics.density
    val paddingHorizontal = (7 * density).toInt()
    val paddingVertical = (3.5f * density).toInt()
    val cornerRadius = 6 * density

    val displayName = if (name.length > 8) name.take(7) + "…" else name

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textWidth = paint.measureText(displayName).toInt()
    val textHeight = (paint.fontMetrics.descent - paint.fontMetrics.ascent).toInt()

    val badgeWidth = textWidth + paddingHorizontal * 2
    val badgeHeight = textHeight + (paddingVertical * 2).toInt()
    val totalHeight = badgeHeight + (18 * density).toInt()
    val totalWidth = maxOf(badgeWidth, (26 * density).toInt())

    val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Badge bubble
    val badgeRect = RectF(
        (totalWidth - badgeWidth) / 2f,
        0f,
        (totalWidth + badgeWidth) / 2f,
        badgeHeight.toFloat(),
    )
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1B4D3E")
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, bgPaint)

    // Badge text
    paint.color = android.graphics.Color.WHITE
    val textX = (totalWidth - textWidth) / 2f
    val textY = paddingVertical - paint.fontMetrics.ascent
    canvas.drawText(displayName, textX, textY, paint)

    // Pin circle
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E11D48")
        style = Paint.Style.FILL
    }
    val centerX = totalWidth / 2f
    val pinCenterY = badgeHeight + (7 * density)
    val radius = 5.5f * density
    canvas.drawCircle(centerX, pinCenterY, radius, pinPaint)

    // White dot inside pin
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, pinCenterY, radius * 0.4f, whitePaint)

    // Pointer
    val path = Path().apply {
        moveTo(centerX - radius * 0.7f, pinCenterY + radius * 0.3f)
        lineTo(centerX + radius * 0.7f, pinCenterY + radius * 0.3f)
        lineTo(centerX, totalHeight.toFloat())
        close()
    }
    canvas.drawPath(path, pinPaint)

    return bitmap
}

/**
 * Blue pulse GPS dot for the user's current live location.
 */
private fun createMyLocationMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (28 * density).toInt()

    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    // Outer aura pulse
    val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#332563EB")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, 13 * density, auraPaint)

    // White border ring
    val whiteRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, 7.5f * density, whiteRing)

    // Inner bright blue dot
    val blueDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#2563EB")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, 5.5f * density, blueDot)

    return bitmap
}

private fun formatWon(value: Long): String = "%,d원".format(value)
