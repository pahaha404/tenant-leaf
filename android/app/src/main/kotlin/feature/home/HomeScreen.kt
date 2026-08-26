package com.seipseip.app.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.shadow
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seipseip.app.Border
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.PaleOrange
import com.seipseip.app.R
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppTab
import com.seipseip.app.feature.common.AppBottomNavigation
import com.seipseip.app.feature.common.swipeToChangeTab

private val HomeBackground = Color.White
private val StartInspectionOrange = Color(0xFFF28A3A)
private val InspectionTips = listOf(
    "싱크대 아래 휴지로 누수를 확인해요.",
    "창문을 닫고 외풍을 확인해요.",
    "샤워기로 수압과 온수를 확인해요.",
    "콘센트에 충전기를 꽂아 확인해요.",
    "천장 모서리의 누수 흔적을 살펴봐요.",
    "관리비 포함 항목을 계약 전에 확인해요.",
)

@Composable
fun HomeScreen(
    onAddProperty: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenRecentReport: () -> Unit,
    onOpenMagazine: () -> Unit,
    onOpenMagazineArticle: (String) -> Unit,
    onStartInspection: () -> Unit,
    onOpenChecklist: () -> Unit,
    onTabSelected: (String) -> Unit,
    processing: Boolean = false,
    recentReportTitle: String? = null,
    recentReportDate: String? = null,
    showBottomBar: Boolean = true,
) {
    val activity = LocalContext.current as? ComponentActivity
    val glassViewModel: GlassConnectionViewModel = rememberGlassConnectionViewModel()
    val glassState by glassViewModel.uiState.collectAsState()
    TenantLeafHomeLayout(
        selectedTab = AppTab.Home,
        onAddProperty = onAddProperty,
        onOpenReports = onOpenReports,
        onOpenRecentReport = onOpenRecentReport,
        onOpenMagazine = onOpenMagazine,
        onOpenMagazineArticle = onOpenMagazineArticle,
        onStartInspection = onStartInspection,
        onOpenChecklist = onOpenChecklist,
        onTabSelected = onTabSelected,
        processing = processing,
        recentReportTitle = recentReportTitle,
        recentReportDate = recentReportDate,
        glassState = glassState,
        onGlassClick = { activity?.let(glassViewModel::connect) },
        showBottomBar = showBottomBar,
    )
}

@Composable
fun TenantLeafHomeLayout(
    selectedTab: AppTab,
    onAddProperty: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenRecentReport: () -> Unit = onOpenReports,
    onOpenMagazine: () -> Unit,
    onOpenMagazineArticle: (String) -> Unit = { onOpenMagazine() },
    onTabSelected: (String) -> Unit,
    onStartInspection: () -> Unit = onAddProperty,
    onOpenChecklist: () -> Unit = onAddProperty,
    processing: Boolean = false,
    recentReportTitle: String? = null,
    recentReportDate: String? = null,
    glassState: GlassConnectionUiState = GlassConnectionUiState(),
    onGlassClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    showBottomBar: Boolean = true,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        onTabSelected(
                            when (tab) {
                                AppTab.Home -> "home"
                                AppTab.Property -> "property"
                                AppTab.Report -> "report"
                                AppTab.Profile -> "profile"
                            },
                        )
                    },
                )
            }
        },
        containerColor = HomeBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(start = 20.dp, top = 36.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HomeHeader(processing, onNotificationClick)
            GlassStatusCard(glassState, onGlassClick)
            if (processing) ReportProcessingCard(onOpenReports) else StartInspectionCard(onStartInspection)
            HomeQuickActions(onAddProperty, onOpenChecklist)
            RecentReportCard(onOpenRecentReport, processing, recentReportTitle, recentReportDate)
            InspectionTipCard()
            HorizontalDivider(color = Border, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
            MagazineSection(onOpenAll = onOpenMagazine, onOpenArticle = onOpenMagazineArticle)
        }
    }
}

@Composable
private fun HomeHeader(processing: Boolean, onNotificationClick: () -> Unit = {}) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(if (processing) "리포트를 정리 중이에요" else "오늘도 안심되는 자취", color = Secondary, fontSize = 14.3.sp, fontWeight = FontWeight.Bold)
            Text(if (processing) "잠시만 기다려 주세요" else "세입세잎", color = DeepGreen, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(1.5.dp, CircleShape)
                .background(Color.White, CircleShape)
                .clip(CircleShape)
                .clickable {
                    onNotificationClick()
                    android.widget.Toast.makeText(context, "새로운 알림이 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.NotificationsNone, "알림", tint = DeepGreen, modifier = Modifier.size(22.dp))
        }
    }
}

// Cyberpunk Futuristic Glassmorphic Color Palette
private val CyberVoidBg = Color(0xF2071118)
private val CyberGlassSurface = Color(0x38152B3C)
private val CyberNeonCyan = Color(0xFF00FFE0)
private val CyberNeonGreen = Color(0xFF00FF87)
private val CyberNeonAmber = Color(0xFFFFB800)
private val CyberNeonPink = Color(0xFFFF0055)
private val CyberTextDim = Color(0xFF7FA2B3)
private val CyberTextBright = Color(0xFFE2F8FF)

@Composable
private fun rememberRotatingBorderBrush(
    angle: Float,
    colors: IntArray,
    positions: FloatArray? = null,
): Brush {
    return remember(angle, colors, positions) {
        object : androidx.compose.ui.graphics.ShaderBrush() {
            override fun createShader(size: Size): android.graphics.Shader {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val shader = android.graphics.SweepGradient(cx, cy, colors, positions)
                val matrix = android.graphics.Matrix()
                matrix.postRotate(angle, cx, cy)
                shader.setLocalMatrix(matrix)
                return shader
            }
        }
    }
}

@Composable
private fun GlassStatusCard(state: GlassConnectionUiState, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cyberGlassScale",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "cyberHudAnimation")

    val isConnected = state.status == GlassConnectionStatus.CONNECTED
    val isConnecting = state.status == GlassConnectionStatus.CONNECTING
    val isError = state.status == GlassConnectionStatus.ERROR || state.status == GlassConnectionStatus.PAUSED

    // 1. Rotating Neon Border Beam Angle
    val borderRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnecting) 1400 else 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "borderRotationAngle",
    )

    // 2. Pulse alpha for live neon indicators
    val livePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cyberLivePulseAlpha",
    )

    // 3. Sonar radar wave radius when connecting
    val sonarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cyberSonarProgress",
    )

    // 4. Mini Data Stream Equalizer Bars
    val eq1 by infiniteTransition.animateFloat(0.25f, 0.95f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "eq1")
    val eq2 by infiniteTransition.animateFloat(0.85f, 0.35f, infiniteRepeatable(tween(580, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "eq2")
    val eq3 by infiniteTransition.animateFloat(0.4f, 1.0f, infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "eq3")
    val eq4 by infiniteTransition.animateFloat(0.9f, 0.45f, infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "eq4")

    val chromaticBorderColors = remember {
        intArrayOf(
            0xFF00FF87.toInt(),
            0xFF00F5C8.toInt(),
            0xFF00D4FF.toInt(),
            0xFF0091FF.toInt(),
            0xFF3B82F6.toInt(),
            0xFF93C5FD.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFEF08A.toInt(),
            0xFFFFE600.toInt(),
            0xFFF59E0B.toInt(),
            0xFF84CC16.toInt(),
            0xFF10B981.toInt(),
            0xFF00FF87.toInt(),
        )
    }
    val chromaticPositions = remember {
        floatArrayOf(
            0.00f, 0.08f, 0.17f, 0.26f, 0.35f, 0.44f,
            0.52f, 0.61f, 0.70f, 0.79f, 0.88f, 0.94f, 1.00f,
        )
    }

    val rotatingBorderBrush = when {
        isConnected || isConnecting -> rememberRotatingBorderBrush(borderRotationAngle, chromaticBorderColors, chromaticPositions)
        else -> null
    }

    val glassBaseBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F3828),
                Color(0xFF08261B),
                Color(0xFF134533),
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(15.dp))
            .background(glassBaseBrush)
            .drawWithContent {
                val cornerRadiusPx = 15.dp.toPx()
                val cr = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)

                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.5f, size.height * 1.1f),
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = cr,
                )

                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                    ),
                    start = Offset(cornerRadiusPx, 1.dp.toPx()),
                    end = Offset(size.width - cornerRadiusPx, 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx(),
                )

                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f),
                            Color(0x3300FF87),
                        ),
                    ),
                    topLeft = Offset(0.5.dp.toPx(), 0.5.dp.toPx()),
                    size = Size(size.width - 1.dp.toPx(), size.height - 1.dp.toPx()),
                    cornerRadius = cr,
                    style = Stroke(width = 1.dp.toPx()),
                )

                drawContent()

                val strokeWidth = 1.6.dp.toPx()
                val halfStroke = strokeWidth / 2f
                val borderSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(halfStroke, halfStroke)

                if (isConnected || isConnecting) {
                    val brush = rotatingBorderBrush
                    if (brush != null) {
                        drawRoundRect(
                            brush = brush,
                            topLeft = topLeft,
                            size = borderSize,
                            cornerRadius = cr,
                            style = Stroke(width = 4.8.dp.toPx(), cap = StrokeCap.Round),
                            alpha = 0.55f * livePulseAlpha,
                        )
                        drawRoundRect(
                            brush = brush,
                            topLeft = topLeft,
                            size = borderSize,
                            cornerRadius = cr,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                } else if (isError) {
                    drawRoundRect(
                        color = Color(0xFFE11D48).copy(alpha = 0.8f * livePulseAlpha),
                        topLeft = topLeft,
                        size = borderSize,
                        cornerRadius = cr,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                } else {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color(0xFF00FF87).copy(alpha = 0.35f),
                                Color(0xFF0091FF).copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.20f),
                            ),
                        ),
                        topLeft = topLeft,
                        size = borderSize,
                        cornerRadius = cr,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 13.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1. [LEFT] 3D Viewport
            CyberGlassHudViewport(
                status = state.status,
                livePulseAlpha = livePulseAlpha,
                sonarProgress = sonarProgress,
            )

            Spacer(Modifier.width(12.dp))

            // 2. [CENTER] Title + Status Dot & Text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Ray-Ban Meta AI",
                    color = when {
                        isConnected -> Color(0xFFF0FDF4)
                        isConnecting -> Color(0xFFFEF3C7)
                        isError -> Color(0xFFFFE4E6)
                        else -> Color(0xFFE6FAF0)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.2.sp,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dotColor = when {
                        isConnected -> Color(0xFF00FF87)
                        isConnecting -> Color(0xFFFFE600)
                        isError -> Color(0xFFFF4D6D)
                        else -> Color(0xFF7FA2B3)
                    }

                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )

                    Spacer(Modifier.width(5.dp))

                    val statusText = when {
                        isConnected -> "연결됨"
                        isConnecting -> "연결 중..."
                        isError -> "연결 오류"
                        else -> "미연결"
                    }

                    Text(
                        text = statusText,
                        color = when {
                            isConnected -> Color(0xFF00FF87)
                            isConnecting -> Color(0xFFFFE600)
                            isError -> Color(0xFFFF4D6D)
                            else -> Color(0xFF7FA2B3)
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // 3. [RIGHT] Equalizer Bars
            if (isConnected || isConnecting) {
                Canvas(
                    modifier = Modifier
                        .size(width = 16.dp, height = 11.dp)
                        .padding(end = 2.dp),
                ) {
                    val barW = 2.0.dp.toPx()
                    val spacing = 1.8.dp.toPx()
                    val color = if (isConnected) Color(0xFF00FF87) else Color(0xFFFFE600)
                    val h = size.height

                    val heights = listOf(eq1, eq2, eq3, eq4)
                    heights.forEachIndexed { i, factor ->
                        val barH = (h * factor).coerceAtLeast(1.8.dp.toPx())
                        val startX = i * (barW + spacing)
                        drawRect(
                            color = color.copy(alpha = 0.95f),
                            topLeft = Offset(startX, h - barH),
                            size = Size(barW, barH),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CyberGlassHudViewport(
    status: GlassConnectionStatus,
    livePulseAlpha: Float,
    sonarProgress: Float,
) {
    val isConnected = status == GlassConnectionStatus.CONNECTED
    val isConnecting = status == GlassConnectionStatus.CONNECTING
    val isError = status == GlassConnectionStatus.ERROR || status == GlassConnectionStatus.PAUSED

    val hudAccentColor = when {
        isConnected -> Color(0xFF00FF87)
        isConnecting -> Color(0xFFFFE600)
        isError -> Color(0xFFFF4D6D)
        else -> Color(0xFF00D4FF)
    }

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF072117),
                        Color(0xFF04150E),
                    ),
                ),
            )
            .border(
                width = 0.9.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        hudAccentColor.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.15f),
                    ),
                ),
                shape = RoundedCornerShape(10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bracketLen = 5.dp.toPx()
            val strokeW = 1.0.dp.toPx()
            val bracketColor = hudAccentColor.copy(alpha = 0.85f)

            // Top-Left ⌜
            drawLine(bracketColor, Offset(2.dp.toPx(), 2.dp.toPx()), Offset(2.dp.toPx() + bracketLen, 2.dp.toPx()), strokeW)
            drawLine(bracketColor, Offset(2.dp.toPx(), 2.dp.toPx()), Offset(2.dp.toPx(), 2.dp.toPx() + bracketLen), strokeW)

            // Top-Right ⌝
            drawLine(bracketColor, Offset(w - 2.dp.toPx(), 2.dp.toPx()), Offset(w - 2.dp.toPx() - bracketLen, 2.dp.toPx()), strokeW)
            drawLine(bracketColor, Offset(w - 2.dp.toPx(), 2.dp.toPx()), Offset(w - 2.dp.toPx(), 2.dp.toPx() + bracketLen), strokeW)

            // Bottom-Left ⌞
            drawLine(bracketColor, Offset(2.dp.toPx(), h - 2.dp.toPx()), Offset(2.dp.toPx() + bracketLen, h - 2.dp.toPx()), strokeW)
            drawLine(bracketColor, Offset(2.dp.toPx(), h - 2.dp.toPx()), Offset(2.dp.toPx(), h - 2.dp.toPx() + bracketLen), strokeW)

            // Bottom-Right ⌟
            drawLine(bracketColor, Offset(w - 2.dp.toPx(), h - 2.dp.toPx()), Offset(w - 2.dp.toPx() - bracketLen, h - 2.dp.toPx()), strokeW)
            drawLine(bracketColor, Offset(w - 2.dp.toPx(), h - 2.dp.toPx()), Offset(w - 2.dp.toPx(), h - 2.dp.toPx() - bracketLen), strokeW)

            if (isConnecting) {
                val maxRadius = size.minDimension / 1.4f
                val radius = maxRadius * sonarProgress
                val alpha = (1f - sonarProgress).coerceIn(0f, 1f) * 0.65f
                drawCircle(
                    color = Color(0xFFFFE600).copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.1.dp.toPx()),
                )
            }
        }

        CyberRayBanCanvas(
            status = status,
            livePulseAlpha = livePulseAlpha,
            modifier = Modifier.size(width = 36.dp, height = 22.dp),
        )
    }
}

@Composable
private fun CyberRayBanCanvas(
    status: GlassConnectionStatus,
    livePulseAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clayCanvasAnimation")

    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "clayScanLineProgress",
    )

    val reticleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "clayReticleRotation",
    )

    val isConnected = status == GlassConnectionStatus.CONNECTED
    val isConnecting = status == GlassConnectionStatus.CONNECTING
    val isError = status == GlassConnectionStatus.ERROR || status == GlassConnectionStatus.PAUSED

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val lensW = w * 0.38f
        val lensH = h * 0.70f
        val lensTop = h * 0.16f
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(lensH * 0.38f, lensH * 0.38f)

        val leftOffset = Offset(w * 0.05f, lensTop)
        val rightOffset = Offset(w - lensW - w * 0.05f, lensTop)
        val lensSize = Size(lensW, lensH)

        val leftCenter = Offset(leftOffset.x + lensW / 2, leftOffset.y + lensH / 2)
        val rightCenter = Offset(rightOffset.x + lensW / 2, rightOffset.y + lensH / 2)

        val frameThickness = 2.4.dp.toPx()
        val thinStroke = 0.8.dp.toPx()

        val shadowOffsetY = 1.8.dp.toPx()
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(leftOffset.x, leftOffset.y + shadowOffsetY),
            size = lensSize,
            cornerRadius = cornerRadius,
            style = Stroke(width = frameThickness * 1.3f, cap = StrokeCap.Round),
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(rightOffset.x, rightOffset.y + shadowOffsetY),
            size = lensSize,
            cornerRadius = cornerRadius,
            style = Stroke(width = frameThickness * 1.3f, cap = StrokeCap.Round),
        )

        val lensCavityColor = Color(0xFF03100B)
        drawRoundRect(
            color = lensCavityColor,
            topLeft = leftOffset,
            size = lensSize,
            cornerRadius = cornerRadius,
        )
        drawRoundRect(
            color = lensCavityColor,
            topLeft = rightOffset,
            size = lensSize,
            cornerRadius = cornerRadius,
        )

        val lensGlowColor = when {
            isConnected -> Color(0xFF00FF87)
            isConnecting -> Color(0xFFFFE600)
            isError -> Color(0xFFFF4D6D)
            else -> Color(0xFF00D4FF)
        }
        val fillAlpha = if (isConnected) 0.35f * livePulseAlpha else 0.15f
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    lensGlowColor.copy(alpha = fillAlpha),
                    Color.Transparent,
                ),
                center = leftCenter,
                radius = lensW * 0.8f,
            ),
            topLeft = leftOffset,
            size = lensSize,
            cornerRadius = cornerRadius,
        )
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    lensGlowColor.copy(alpha = fillAlpha),
                    Color.Transparent,
                ),
                center = rightCenter,
                radius = lensW * 0.8f,
            ),
            topLeft = rightOffset,
            size = lensSize,
            cornerRadius = cornerRadius,
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.50f),
            radius = 1.4.dp.toPx(),
            center = Offset(leftOffset.x + 4.dp.toPx(), leftOffset.y + 4.dp.toPx()),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.50f),
            radius = 1.4.dp.toPx(),
            center = Offset(rightOffset.x + 4.dp.toPx(), rightOffset.y + 4.dp.toPx()),
        )

        if (isConnected || isConnecting) {
            val scanY = lensTop + lensH * scanLineProgress
            val laserBeamColor = if (isConnected) Color(0xFF00FFD5) else Color(0xFFFFE600)
            drawLine(
                color = laserBeamColor.copy(alpha = 0.85f),
                start = Offset(leftOffset.x + 2.dp.toPx(), scanY),
                end = Offset(leftOffset.x + lensW - 2.dp.toPx(), scanY),
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = laserBeamColor.copy(alpha = 0.85f),
                start = Offset(rightOffset.x + 2.dp.toPx(), scanY),
                end = Offset(rightOffset.x + lensW - 2.dp.toPx(), scanY),
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        if (isConnected) {
            val hudColor = Color(0xFF00FF87).copy(alpha = 0.75f * livePulseAlpha)

            val reticleR = lensW * 0.28f
            drawArc(
                color = hudColor,
                startAngle = reticleRotation,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(leftCenter.x - reticleR, leftCenter.y - reticleR),
                size = Size(reticleR * 2, reticleR * 2),
                style = Stroke(width = thinStroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = hudColor,
                startAngle = reticleRotation + 180f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(leftCenter.x - reticleR, leftCenter.y - reticleR),
                size = Size(reticleR * 2, reticleR * 2),
                style = Stroke(width = thinStroke, cap = StrokeCap.Round),
            )

            drawCircle(
                color = Color.White,
                radius = 1.1.dp.toPx(),
                center = leftCenter,
            )

            val depthR1 = lensW * 0.22f
            val depthR2 = lensW * 0.35f
            drawCircle(
                color = Color(0xFF00D4FF).copy(alpha = 0.45f * livePulseAlpha),
                radius = depthR1,
                center = rightCenter,
                style = Stroke(width = thinStroke),
            )
            drawArc(
                color = Color(0xFF00D4FF).copy(alpha = 0.6f * livePulseAlpha),
                startAngle = -reticleRotation * 0.8f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(rightCenter.x - depthR2, rightCenter.y - depthR2),
                size = Size(depthR2 * 2, depthR2 * 2),
                style = Stroke(width = thinStroke, cap = StrokeCap.Round),
            )
            drawCircle(
                color = Color(0xFF00FF87),
                radius = 1.0.dp.toPx(),
                center = rightCenter,
            )
        }

        val clayFrameBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFE8FDF5),
                Color(0xFF34D399),
                Color(0xFF065F46),
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        )

        drawRoundRect(
            brush = clayFrameBrush,
            topLeft = leftOffset,
            size = lensSize,
            cornerRadius = cornerRadius,
            style = Stroke(width = frameThickness, cap = StrokeCap.Round),
        )
        drawRoundRect(
            brush = clayFrameBrush,
            topLeft = rightOffset,
            size = lensSize,
            cornerRadius = cornerRadius,
            style = Stroke(width = frameThickness, cap = StrokeCap.Round),
        )

        val bridgeY = lensTop + lensH * 0.30f
        drawLine(
            color = Color.Black.copy(alpha = 0.40f),
            start = Offset(leftOffset.x + lensW, bridgeY + shadowOffsetY * 0.8f),
            end = Offset(rightOffset.x, bridgeY + shadowOffsetY * 0.8f),
            strokeWidth = frameThickness * 1.2f,
            cap = StrokeCap.Round,
        )
        drawLine(
            brush = clayFrameBrush,
            start = Offset(leftOffset.x + lensW, bridgeY),
            end = Offset(rightOffset.x, bridgeY),
            strokeWidth = frameThickness * 1.15f,
            cap = StrokeCap.Round,
        )

        val cameraCenter = Offset(leftOffset.x + 3.2.dp.toPx(), lensTop + 3.2.dp.toPx())
        val ledCenter = Offset(rightOffset.x + lensW - 3.2.dp.toPx(), lensTop + 3.2.dp.toPx())

        drawCircle(
            color = Color(0xFF031A12),
            radius = 2.2.dp.toPx(),
            center = cameraCenter,
        )
        drawCircle(
            color = Color(0xFF00D4FF),
            radius = 1.3.dp.toPx(),
            center = cameraCenter,
        )

        if (isConnected) {
            drawCircle(
                color = Color(0xFF00FF87).copy(alpha = 0.50f * livePulseAlpha),
                radius = 3.8.dp.toPx(),
                center = ledCenter,
            )
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = ledCenter,
            )
            drawCircle(
                color = Color(0xFF00FF87).copy(alpha = livePulseAlpha),
                radius = 1.1.dp.toPx(),
                center = ledCenter,
            )
        } else if (isConnecting) {
            drawCircle(
                color = Color(0xFFFFE600).copy(alpha = livePulseAlpha),
                radius = 1.5.dp.toPx(),
                center = ledCenter,
            )
        } else {
            drawCircle(
                color = Color(0xFF042F24),
                radius = 1.3.dp.toPx(),
                center = ledCenter,
            )
        }
    }
}



@Composable
private fun StartInspectionCard(onClick: () -> Unit) = HomeHeroCard(
    icon = Icons.Outlined.PlayArrow,
    title = "점검 시작하기",
    description = "",
    onClick = onClick,
    background = StartInspectionOrange,
    contentColor = Color.White,
    iconBackground = Color.White.copy(alpha = .18f),
    verticalPadding = 24.dp,
    iconBoxSize = 44.dp,
    iconSize = 24.dp,
    titleFontSize = 17.sp,
)

@Composable
private fun ReportProcessingCard(onClick: () -> Unit) = HomeHeroCard(
    icon = Icons.AutoMirrored.Outlined.Article,
    title = "리포트 정리 중",
    description = "촬영한 구역을 차례대로 분석하고 있어요",
    onClick = onClick,
)

@Composable
private fun HomeHeroCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    background: Color = PaleGreen,
    contentColor: Color = DeepGreen,
    iconBackground: Color = Color.White,
    verticalPadding: Dp = 9.dp,
    iconBoxSize: Dp = 38.dp,
    iconSize: Dp = 20.dp,
    titleFontSize: TextUnit = 14.5.sp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.5.dp, RoundedCornerShape(18.dp))
            .background(background, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(iconBoxSize)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (contentColor == Color.White) Color.White else Orange,
                modifier = Modifier.size(iconSize),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = contentColor,
                fontSize = titleFontSize,
                fontWeight = FontWeight.ExtraBold,
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = if (contentColor == Color.White) Color.White.copy(alpha = .85f) else Secondary,
                    fontSize = 14.3.sp,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = if (contentColor == Color.White) Color.White else Green,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun RecentReportCard(
    onClick: () -> Unit,
    processing: Boolean,
    recentReportTitle: String?,
    recentReportDate: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.5.dp, RoundedCornerShape(18.dp))
            .background(Color.White, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(41.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.Article, null, tint = Green, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(if (processing) "하자 점검 및 리포트 작성 중" else "최근 점검 리포트", color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                if (processing) "촬영한 사진을 분석하고 리포트를 작성 중이에요"
                else if (recentReportTitle != null && recentReportDate != null) "$recentReportTitle · $recentReportDate 점검"
                else "아직 점검 리포트가 없어요",
                color = Secondary,
                fontSize = 13.sp,
            )
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Green, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HomeQuickActions(onAddProperty: () -> Unit, onOpenChecklist: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickAction(Modifier.weight(1f), Icons.Outlined.AddHome, "매물 등록하기", "", Color.White, onAddProperty)
        QuickAction(Modifier.weight(1f), Icons.Outlined.Checklist, "체크리스트 확인", "", Color.White, onOpenChecklist)
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    background: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(142.dp)
            .shadow(1.5.dp, RoundedCornerShape(24.dp))
            .background(background, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Green, modifier = Modifier.size(31.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, color = DeepGreen, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        if (description.isNotBlank()) {
            Text(description, color = Secondary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun InspectionTipCard() {
    val pagerState = rememberPagerState(pageCount = { InspectionTips.size })

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8F9FA))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = Orange, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "오늘의 점검 팁",
                        color = Orange,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        InspectionTips[page],
                        color = DeepGreen,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(InspectionTips.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.5.dp)
                        .size(if (pagerState.currentPage == index) 6.dp else 4.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == index) Orange else Color(0xFFFFD8B7)),
                )
            }
        }
    }
}

@Composable
private fun MagazineSection(onOpenAll: () -> Unit, onOpenArticle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("자취 매거진", color = DeepGreen, fontSize = 19.2.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Text("전체보기  ›", modifier = Modifier.clickable(onClick = onOpenAll), color = Green, fontSize = 13.2.sp, fontWeight = FontWeight.Bold)
        }
        MagazineArticle(R.drawable.magazine_1, "생활 준비", "첫 자취생 필수템 체크리스트", onClick = { onOpenArticle("first_essentials") })
        MagazineArticle(R.drawable.magazine_2, "집 구하기", "집 볼 때 흔히 하는 5가지 실수", onClick = { onOpenArticle("home_viewing_mistakes") })
        MagazineArticle(R.drawable.magazine_3, "계약 전", "계약서 쓰기 전 반드시 확인할 것", onClick = { onOpenArticle("contract_checklist") })
    }
}

@Composable
private fun MagazineArticle(imageRes: Int, tag: String, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.size(width = 76.dp, height = 58.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                tag,
                color = Orange,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                title,
                color = DeepGreen,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Normal,
            )
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Green, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HomeBottomNavigation(selectedTab: AppTab, onTabSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(72.dp).background(Color.White).padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        HomeTab(Icons.Outlined.Home, "홈", selectedTab == AppTab.Home) { onTabSelected("home") }
        HomeTab(Icons.Outlined.RealEstateAgent, "매물", selectedTab == AppTab.Property) { onTabSelected("property") }
        HomeTab(Icons.AutoMirrored.Outlined.Article, "리포트", selectedTab == AppTab.Report) { onTabSelected("report") }
        HomeTab(Icons.Outlined.Person, "내 정보", selectedTab == AppTab.Profile) { onTabSelected("profile") }
    }
}

@Composable
private fun HomeTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.width(58.dp).clip(RoundedCornerShape(14.dp)).clickable(enabled = !selected, onClick = onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, null, tint = if (selected) Orange else Secondary, modifier = Modifier.size(20.dp))
        Text(label, color = if (selected) Orange else Secondary, fontSize = 9.sp, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun CyberGlassStatusCardPreview() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassStatusCard(
            state = GlassConnectionUiState(status = GlassConnectionStatus.CONNECTED),
            onClick = {},
        )
        GlassStatusCard(
            state = GlassConnectionUiState(status = GlassConnectionStatus.CONNECTING),
            onClick = {},
        )
        GlassStatusCard(
            state = GlassConnectionUiState(status = GlassConnectionStatus.DISCONNECTED),
            onClick = {},
        )
        GlassStatusCard(
            state = GlassConnectionUiState(status = GlassConnectionStatus.ERROR),
            onClick = {},
        )
    }
}
