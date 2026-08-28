package com.seipseip.app.feature.profile

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.Border
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.PaleOrange
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppPageScaffold
import com.seipseip.app.feature.common.AppTab
import java.io.File
import java.util.Locale

private val EmeraldCardBgStart = Color(0xFF0F3828)
private val EmeraldCardBgMid = Color(0xFF08261B)
private val EmeraldCardBgEnd = Color(0xFF134533)
private val NeonEmerald = Color(0xFF00FF87)
private val NeonGold = Color(0xFFFFD166)
private val SoftCardBorder = Color(0xFFE8ECE5)
private val DangerRed = Color(0xFFE11D48)
private val UtilityBlue = Color(0xFF2563EB)
private val UtilityPaleBlue = Color(0xFFEFF6FF)
private val UtilityPurple = Color(0xFF7C3AED)
private val UtilityPalePurple = Color(0xFFF5F3FF)

@Composable
fun ProfileScreen(
    nickname: String,
    onNicknameChanged: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenProperties: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    onOpenMagazine: () -> Unit = {},
    onOpenMap: () -> Unit = {},
    onTabSelected: (String) -> Unit = {},
    showBottomBar: Boolean = true,
) {
    val context = LocalContext.current

    // Dialog & Interaction states
    var currentNickname by rememberSaveable(nickname) { mutableStateOf(nickname) }
    var showEditNicknameDialog by rememberSaveable { mutableStateOf(false) }
    var showAreaConverterDialog by rememberSaveable { mutableStateOf(false) }
    var showBrokerageFeeDialog by rememberSaveable { mutableStateOf(false) }
    var showMovingGuideDialog by rememberSaveable { mutableStateOf(false) }
    var showFaqDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var showTermsDialog by rememberSaveable { mutableStateOf(false) }
    var showLicenseDialog by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }

    // App Preferences State (Persisted in UI session)
    var notifyAnalysisComplete by rememberSaveable { mutableStateOf(true) }
    var notifyInspectionReminder by rememberSaveable { mutableStateOf(true) }
    var hapticFeedbackEnabled by rememberSaveable { mutableStateOf(true) }
    var autoSaveOriginalVideo by rememberSaveable { mutableStateOf(true) }
    var highQualityExtraction by rememberSaveable { mutableStateOf(true) }

    // Real App Cache Calculation
    var cacheSizeFormatted by remember { mutableStateOf("계산 중...") }
    var hasCacheFiles by remember { mutableStateOf(true) }

    fun refreshCacheSize() {
        val totalBytes = calculateTotalAppCache(context)
        hasCacheFiles = totalBytes > 0L
        cacheSizeFormatted = formatBytesToReadable(totalBytes)
    }

    LaunchedEffect(Unit) {
        refreshCacheSize()
    }

    AppPageScaffold(
        title = "내 정보",
        selectedTab = AppTab.Profile,
        showBottomBar = showBottomBar,
        topTrailingAction = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(1.5.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .clip(CircleShape)
                    .clickable { showEditNicknameDialog = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "프로필 수정",
                    tint = DeepGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
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
    ) {
        // 1. Clean Hero Profile Card
        HeroProfileCard(
            nickname = currentNickname,
        )

        // 2. Activity & Inspection KPI Stat Dashboard
        ProfileStatsDashboard(
            propertyCount = 5,
            reportCount = 3,
            safetyScore = 98,
            onPropertyClick = onOpenProperties,
            onReportClick = onOpenReports,
            onSafetyClick = onOpenGuide,
        )

        // 3. 부동산 편의 도구 3종 (Area Converter, Brokerage Fee, Moving Guide)
        TenantUtilityToolsSection(
            onOpenAreaConverter = { showAreaConverterDialog = true },
            onOpenBrokerageFee = { showBrokerageFeeDialog = true },
            onOpenMovingGuide = { showMovingGuideDialog = true },
        )

        // 4. 앱 설정 및 환경 (App Preferences & Environment Settings)
        AppSettingsSection(
            notifyAnalysisComplete = notifyAnalysisComplete,
            onNotifyAnalysisChange = { notifyAnalysisComplete = it },
            notifyInspectionReminder = notifyInspectionReminder,
            onNotifyReminderChange = { notifyInspectionReminder = it },
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            onHapticFeedbackChange = { hapticFeedbackEnabled = it },
            highQualityExtraction = highQualityExtraction,
            onQualityToggle = { highQualityExtraction = !highQualityExtraction },
            autoSaveOriginalVideo = autoSaveOriginalVideo,
            onAutoSaveVideoChange = { autoSaveOriginalVideo = it },
            cacheSizeText = cacheSizeFormatted,
            hasCache = hasCacheFiles,
            onClearCache = {
                clearAppCache(context)
                refreshCacheSize()
                Toast.makeText(context, "임시 캐시와 미디어가 말끔히 정리되었습니다.", Toast.LENGTH_SHORT).show()
            },
            onOpenFaq = { showFaqDialog = true },
            onOpenPrivacy = { showPrivacyDialog = true },
            onOpenTerms = { showTermsDialog = true },
            onOpenLicenses = { showLicenseDialog = true },
        )

        // 5. 계정 관리 & 버전 정보
        AccountActionsSection(
            onLogoutClick = { showLogoutDialog = true },
            onDeleteAccountClick = { showDeleteAccountDialog = true },
        )

        Spacer(Modifier.height(16.dp))
    }

    // ==========================================
    // Interactive Dialogs & Sheets
    // ==========================================

    if (showEditNicknameDialog) {
        EditNicknameDialog(
            currentNickname = currentNickname,
            onDismiss = { showEditNicknameDialog = false },
            onConfirm = { newNick ->
                currentNickname = newNick
                onNicknameChanged(newNick)
                showEditNicknameDialog = false
                Toast.makeText(context, "닉네임이 '${newNick}'(으)로 변경되었습니다.", Toast.LENGTH_SHORT).show()
            },
        )
    }

    if (showAreaConverterDialog) {
        AreaConverterDialog(onDismiss = { showAreaConverterDialog = false })
    }

    if (showBrokerageFeeDialog) {
        BrokerageFeeCalculatorDialog(onDismiss = { showBrokerageFeeDialog = false })
    }

    if (showMovingGuideDialog) {
        MovingGuideDialog(onDismiss = { showMovingGuideDialog = false })
    }

    if (showFaqDialog) {
        FaqDialog(onDismiss = { showFaqDialog = false })
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showTermsDialog) {
        TermsOfServiceDialog(onDismiss = { showTermsDialog = false })
    }

    if (showLicenseDialog) {
        OpenSourceLicenseDialog(onDismiss = { showLicenseDialog = false })
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountConfirmDialog(
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = {
                showDeleteAccountDialog = false
                Toast.makeText(context, "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                onLogout()
            },
        )
    }
}

// =========================================================================
// 1. Clean Hero Profile Card
// =========================================================================

@Composable
private fun HeroProfileCard(
    nickname: String,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heroPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroAuraAlpha",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(EmeraldCardBgStart, EmeraldCardBgMid, EmeraldCardBgEnd),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    ),
                )
                .drawWithContent {
                    drawContent()
                    // Top glowing highlight border line
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.5f),
                                Color(0xFF00FF87).copy(alpha = 0.7f),
                                Color.Transparent,
                            ),
                        ),
                        start = Offset(24.dp.toPx(), 1.dp.toPx()),
                        end = Offset(size.width - 24.dp.toPx(), 1.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar with glowing ring and 3-leaf clover icon
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .shadow(4.dp, CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF1B4E38), Color(0xFF0C2E20)),
                            ),
                            CircleShape,
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    NeonEmerald.copy(alpha = pulseAlpha),
                                    NeonGold,
                                    NeonEmerald.copy(alpha = pulseAlpha),
                                ),
                            ),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "🍀",
                        fontSize = 28.sp,
                    )
                }

                Spacer(Modifier.width(16.dp))

                // User Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "${nickname}님",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = "세입세잎과 함께하는 안전한 자취 라이프",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

// =========================================================================
// 2. Activity & Inspection KPI Stat Dashboard
// =========================================================================

@Composable
private fun ProfileStatsDashboard(
    propertyCount: Int,
    reportCount: Int,
    safetyScore: Int,
    onPropertyClick: () -> Unit,
    onReportClick: () -> Unit,
    onSafetyClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.RealEstateAgent,
            iconTint = Orange,
            iconBg = PaleOrange,
            title = "내 매물",
            value = "${propertyCount}개",
            onClick = onPropertyClick,
        )

        StatTile(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Outlined.Article,
            iconTint = Green,
            iconBg = PaleGreen,
            title = "점검 리포트",
            value = "${reportCount}건",
            onClick = onReportClick,
        )

        StatTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.CheckCircle,
            iconTint = Color(0xFF10B981),
            iconBg = Color(0xFFE6F8F0),
            title = "안전 지수",
            value = "${safetyScore}점",
            onClick = onSafetyClick,
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "statTileScale",
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(1.5.dp, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SoftCardBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconBg, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(17.dp),
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = Secondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = value,
                    color = DeepGreen,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = title,
                    color = Secondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// =========================================================================
// 3. 부동산 편의 도구 3종 (Tenant Utility Tools)
// =========================================================================

@Composable
private fun TenantUtilityToolsSection(
    onOpenAreaConverter: () -> Unit,
    onOpenBrokerageFee: () -> Unit,
    onOpenMovingGuide: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "부동산 편의 도구",
            color = DeepGreen,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.5.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SoftCardBorder),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingNavigationRow(
                    icon = Icons.Outlined.Calculate,
                    iconTint = UtilityBlue,
                    iconBg = UtilityPaleBlue,
                    title = "평수 ↔ ㎡ 스마트 면적 계산기",
                    subtitle = "방 크기 단위 즉시 환산 및 방 구조별 기준 면적 확인",
                    onClick = onOpenAreaConverter,
                )

                HorizontalDivider(color = Border, thickness = 0.6.dp, modifier = Modifier.padding(horizontal = 16.dp))

                SettingNavigationRow(
                    icon = Icons.Outlined.RealEstateAgent,
                    iconTint = Orange,
                    iconBg = PaleOrange,
                    title = "부동산 중개보수(복비) 안심 계산기",
                    subtitle = "보증금·월세 기준 법정 중개수수료 상한 요율 자동 계산",
                    onClick = onOpenBrokerageFee,
                )

                HorizontalDivider(color = Border, thickness = 0.6.dp, modifier = Modifier.padding(horizontal = 16.dp))

                SettingNavigationRow(
                    icon = Icons.Outlined.Description,
                    iconTint = UtilityPurple,
                    iconBg = UtilityPalePurple,
                    title = "전입신고 & 확정일자 안전 가이드",
                    subtitle = "보증금 보호를 위한 대항력 발생 시점 및 필수 체크리스트",
                    onClick = onOpenMovingGuide,
                )
            }
        }
    }
}

// =========================================================================
// 4. 앱 설정 및 환경 (AppSettingsSection)
// =========================================================================

@Composable
private fun AppSettingsSection(
    notifyAnalysisComplete: Boolean,
    onNotifyAnalysisChange: (Boolean) -> Unit,
    notifyInspectionReminder: Boolean,
    onNotifyReminderChange: (Boolean) -> Unit,
    hapticFeedbackEnabled: Boolean,
    onHapticFeedbackChange: (Boolean) -> Unit,
    highQualityExtraction: Boolean,
    onQualityToggle: () -> Unit,
    autoSaveOriginalVideo: Boolean,
    onAutoSaveVideoChange: (Boolean) -> Unit,
    cacheSizeText: String,
    hasCache: Boolean,
    onClearCache: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "앱 설정 및 환경",
            color = DeepGreen,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        // 1. 알림 및 피드백
        SettingsGroupCard(title = "알림 및 피드백") {
            SettingToggleRow(
                icon = Icons.Outlined.Notifications,
                title = "AI 관찰 분석 완료 알림",
                subtitle = "촬영 종료 후 리포트 생성이 끝나면 알려드려요",
                checked = notifyAnalysisComplete,
                onCheckedChange = onNotifyAnalysisChange,
            )

            HorizontalDivider(color = Border, thickness = 0.6.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingToggleRow(
                icon = Icons.Outlined.CheckCircle,
                title = "임장 체크포인트 리마인더",
                subtitle = "놓치기 쉬운 필수 확인 구역을 미리 알려드려요",
                checked = notifyInspectionReminder,
                onCheckedChange = onNotifyReminderChange,
            )

            HorizontalDivider(color = Border, thickness = 0.6.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingToggleRow(
                icon = Icons.Outlined.Vibration,
                title = "점검 중 햅틱 & 진동 피드백",
                subtitle = "클립 분할 및 상태 변화 시 진동으로 안내해요",
                checked = hapticFeedbackEnabled,
                onCheckedChange = onHapticFeedbackChange,
            )
        }

        // 2. 촬영 및 미디어 환경
        SettingsGroupCard(title = "촬영 및 미디어 환경") {
            SettingActionRow(
                icon = Icons.Outlined.Tune,
                title = "AI 사진 추출 품질",
                subtitle = if (highQualityExtraction) "고화질 FHD (1080p · 권장)" else "표준 (720p)",
                actionLabel = "변경",
                onClick = onQualityToggle,
            )

            HorizontalDivider(color = Border, thickness = 0.6.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingToggleRow(
                icon = Icons.Outlined.CameraAlt,
                title = "휴대전화 갤러리 원본 자동 보관",
                subtitle = "원본 비디오는 갤러리에만 안전하게 보관돼요",
                checked = autoSaveOriginalVideo,
                onCheckedChange = onAutoSaveVideoChange,
            )
        }

        // 3. 저장공간 및 고객지원 안내
        SettingsGroupCard(title = "저장공간 및 안내") {
            SettingActionRow(
                icon = Icons.Outlined.CleaningServices,
                title = "임시 캐시 및 분석 미디어 정리",
                subtitle = if (hasCache) "$cacheSizeText 사용 중" else "0.0 KB (정리 완료)",
                actionLabel = if (hasCache) "지금 정리" else "완료",
                actionEnabled = hasCache,
                onClick = onClearCache,
            )

            HorizontalDivider(color = Border, thickness = 0.6.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingNavigationRow(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                iconTint = Green,
                iconBg = PaleGreen,
                title = "자주 묻는 질문 (FAQ)",
                subtitle = "임장 촬영, 영상 보관 원칙, AI 분석 안내",
                onClick = onOpenFaq,
            )

            HorizontalDivider(color = Border, thickness = 0.6.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingNavigationRow(
                icon = Icons.Outlined.Security,
                title = "개인정보 처리방침",
                subtitle = "촬영 미디어 보호 및 익명화 처리 정책",
                onClick = onOpenPrivacy,
            )

            HorizontalDivider(color = Border, thickness = 0.6.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingNavigationRow(
                icon = Icons.Outlined.Description,
                title = "서비스 이용약관",
                subtitle = "세입세잎 임장 보조 서비스 이용 안내",
                onClick = onOpenTerms,
            )

            HorizontalDivider(color = Border, thickness = 0.6.dp, modifier = Modifier.padding(horizontal = 16.dp))

            SettingNavigationRow(
                icon = Icons.Outlined.Policy,
                title = "오픈소스 라이선스",
                subtitle = "사용된 오픈소스 소프트웨어 라이선스 고지",
                onClick = onOpenLicenses,
            )
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = Secondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp),
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.5.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SoftCardBorder),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFFF3F5F3), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeepGreen,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = DeepGreen,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    color = Secondary,
                    fontSize = 11.sp,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Green,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD1D5DB),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun SettingActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    actionEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFFF3F5F3), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeepGreen,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = DeepGreen,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    color = Secondary,
                    fontSize = 11.sp,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (actionEnabled) PaleGreen else Color(0xFFF3F4F6))
                .clickable(enabled = actionEnabled, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = actionLabel,
                color = if (actionEnabled) Green else Secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SettingNavigationRow(
    icon: ImageVector,
    iconTint: Color = DeepGreen,
    iconBg: Color = Color(0xFFF3F5F3),
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconBg, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = DeepGreen,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    color = Secondary,
                    fontSize = 11.sp,
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = Secondary.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp),
        )
    }
}

// =========================================================================
// 5. Account Actions & Version Info
// =========================================================================

@Composable
private fun AccountActionsSection(
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Logout Card Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(16.dp))
                .clickable(onClick = onLogoutClick),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "로그아웃",
                    tint = DangerRed,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "로그아웃",
                    color = DangerRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Delete Account / Withdrawal Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "회원 탈퇴",
                color = Secondary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable(onClick = onDeleteAccountClick)
                    .padding(vertical = 6.dp, horizontal = 10.dp),
            )
        }

        // App Version & Brand Copyright Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "세입세잎 v1.2.0 (Build 2026.08) · 최신 버전",
                color = Secondary.copy(alpha = 0.7f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "세입자의 든든한 세 잎 클로버 🍀",
                color = Secondary.copy(alpha = 0.5f),
                fontSize = 10.5.sp,
            )
        }
    }
}

// =========================================================================
// Real Cache Calculation Utilities
// =========================================================================

private fun calculateTotalAppCache(context: Context): Long {
    var size = 0L
    context.cacheDir?.let { size += getFolderSize(it) }
    context.externalCacheDir?.let { size += getFolderSize(it) }
    return size
}

private fun getFolderSize(file: File): Long {
    var size = 0L
    if (file.isDirectory) {
        file.listFiles()?.forEach { size += getFolderSize(it) }
    } else {
        size = file.length()
    }
    return size
}

private fun clearAppCache(context: Context) {
    runCatching {
        context.cacheDir.deleteRecursively()
        context.externalCacheDir?.deleteRecursively()
    }
}

private fun formatBytesToReadable(bytes: Long): String {
    if (bytes <= 0L) return "0.0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.1f KB", kb)
    }
}

// =========================================================================
// Dialog Components (Utility & Policy)
// =========================================================================

@Composable
private fun AreaConverterDialog(onDismiss: () -> Unit) {
    var isSquareMeterMode by remember { mutableStateOf(true) } // true: m2 -> 평, false: 평 -> m2
    var inputValue by remember { mutableStateOf("33.0") }

    val inputNum = inputValue.toDoubleOrNull() ?: 0.0
    val convertedResult = if (isSquareMeterMode) {
        inputNum / 3.305785
    } else {
        inputNum * 3.305785
    }

    val unitFrom = if (isSquareMeterMode) "㎡" else "평"
    val unitTo = if (isSquareMeterMode) "평" else "㎡"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Calculate,
                    contentDescription = null,
                    tint = UtilityBlue,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "평수 ↔ ㎡ 면적 계산기",
                    color = DeepGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Mode Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSquareMeterMode) Color.White else Color.Transparent)
                            .clickable {
                                isSquareMeterMode = true
                                inputValue = "33.0"
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "㎡ ➔ 평",
                            color = if (isSquareMeterMode) DeepGreen else Secondary,
                            fontWeight = if (isSquareMeterMode) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isSquareMeterMode) Color.White else Color.Transparent)
                            .clickable {
                                isSquareMeterMode = false
                                inputValue = "10.0"
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "평 ➔ ㎡",
                            color = if (!isSquareMeterMode) DeepGreen else Secondary,
                            fontWeight = if (!isSquareMeterMode) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                        )
                    }
                }

                // Input Field
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("입력 면적 ($unitFrom)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UtilityBlue,
                        unfocusedBorderColor = Border,
                    ),
                )

                // Result Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = UtilityPaleBlue),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "환산 결과",
                            color = UtilityBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", convertedResult)} $unitTo",
                            color = DeepGreen,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = if (isSquareMeterMode) {
                                when {
                                    convertedResult < 7 -> "원룸·초소형 오피스텔 크기"
                                    convertedResult < 14 -> "1.5룸~투룸 주거 공간 크기"
                                    convertedResult < 22 -> "소형 아파트·쓰리룸 크기"
                                    else -> "중대형 아파트 크기"
                                }
                            } else {
                                "법정 기준: 1평 = 3.305785㎡"
                            },
                            color = Secondary,
                            fontSize = 11.5.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = DeepGreen, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
    )
}

@Composable
private fun BrokerageFeeCalculatorDialog(onDismiss: () -> Unit) {
    var isMonthlyRent by remember { mutableStateOf(true) }
    var depositText by remember { mutableStateOf("1000") } // 만원 단위
    var rentText by remember { mutableStateOf("60") } // 만원 단위

    val deposit = depositText.toLongOrNull() ?: 0L
    val rent = rentText.toLongOrNull() ?: 0L

    // 환산보증금 계산 (월세인 경우: 보증금 + 월세 * 100, 5000만원 미만 시 보증금 + 월세 * 70)
    val convertedAmount = if (isMonthlyRent) {
        val standard = deposit + (rent * 100)
        if (standard < 5000) deposit + (rent * 70) else standard
    } else {
        deposit
    }

    // 주택 임대차 법정 상한 요율 (5천만 미만: 0.5% 한도 20만, 1억 미만: 0.4% 한도 30만, 6억 미만: 0.3%, 6억 이상: 0.4%)
    val (feeRate, maxLimit) = when {
        convertedAmount < 5000 -> Pair(0.005, 20L)
        convertedAmount < 10000 -> Pair(0.004, 30L)
        convertedAmount < 60000 -> Pair(0.003, null)
        else -> Pair(0.004, null)
    }

    val calculatedFee = (convertedAmount * feeRate).toLong()
    val maxBrokerageFee = if (maxLimit != null && calculatedFee > maxLimit) maxLimit else calculatedFee
    val vatAmount = (maxBrokerageFee * 0.1).toLong()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.RealEstateAgent,
                    contentDescription = null,
                    tint = Orange,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "중개보수(복비) 안심 계산기",
                    color = DeepGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Rent Type Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMonthlyRent) Color.White else Color.Transparent)
                            .clickable { isMonthlyRent = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "월세",
                            color = if (isMonthlyRent) DeepGreen else Secondary,
                            fontWeight = if (isMonthlyRent) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isMonthlyRent) Color.White else Color.Transparent)
                            .clickable { isMonthlyRent = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "전세",
                            color = if (!isMonthlyRent) DeepGreen else Secondary,
                            fontWeight = if (!isMonthlyRent) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                        )
                    }
                }

                // Deposit Input
                OutlinedTextField(
                    value = depositText,
                    onValueChange = { depositText = it.filter(Char::isDigit) },
                    label = { Text("보증금 (만원)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                // Monthly Rent Input (only for monthly rent)
                if (isMonthlyRent) {
                    OutlinedTextField(
                        value = rentText,
                        onValueChange = { rentText = it.filter(Char::isDigit) },
                        label = { Text("월세 (만원)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                // Calculation Result Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PaleOrange),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "법정 최대 중개수수료",
                            color = Orange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${maxBrokerageFee}만원",
                            color = DeepGreen,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "부가세 10% 포함 시: 약 ${maxBrokerageFee + vatAmount}만원\n(주택 임대차 상한요율 ${(feeRate * 100)}% 적용)",
                            color = Secondary,
                            fontSize = 11.5.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인", color = DeepGreen, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
    )
}

@Composable
private fun MovingGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("📝", fontSize = 20.sp)
                Text(
                    text = "전입신고 & 확정일자 안전 가이드",
                    color = DeepGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GuideStepItem(
                    step = "1",
                    title = "이사 당일 즉시 전입신고 + 확정일자",
                    detail = "이사한 당일 주민센터 방문 또는 '정부24'에서 전입신고와 확정일자를 동시에 받아야 대항력과 우선변제권이 생겨요.",
                )
                GuideStepItem(
                    step = "2",
                    title = "대항력 발생 효력 시점 주의",
                    detail = "전입신고 효력은 '다음 날 0시'부터 발생합니다. 잔금 지급 당일 집주인의 추가 대출(근저당) 설정을 막는 특약을 반드시 넣으세요.",
                )
                GuideStepItem(
                    step = "3",
                    title = "전세보증금 반환보증보험 가입",
                    detail = "HUG, SGI, HF 보증보험에 가입하여 만기 시 보증금을 안전하게 반환받을 수 있도록 준비하세요.",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인", color = Green, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
    )
}

@Composable
private fun GuideStepItem(
    step: String,
    title: String,
    detail: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(PaleGreen, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = step,
                color = Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = DeepGreen,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = detail,
                color = Secondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun FaqDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("❓", fontSize = 20.sp)
                Text(
                    text = "자주 묻는 질문 (FAQ)",
                    color = DeepGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FaqItem(
                    question = "Q. 스마트 글라스가 없어도 쓸 수 있나요?",
                    answer = "네! Meta 스마트 글라스가 없어도 스마트폰 카메라로 방의 각 구역을 동일하게 점검하고 AI 리포트를 생성할 수 있습니다.",
                )
                FaqItem(
                    question = "Q. 촬영한 영상이 서버에 저장되나요?",
                    answer = "아닙니다. 원본 영상은 휴대전화 갤러리에만 안전하게 남고 서버로는 분석용 정지 사진(JPEG)만 전송되어 개인정보를 철저히 보호합니다.",
                )
                FaqItem(
                    question = "Q. AI가 하자를 확정하나요?",
                    answer = "AI는 하자를 법적으로 확정하지 않으며, 근거 사진과 함께 '확인 필요 관찰'을 제안합니다. 최종 리포트 채택 여부는 사용자가 직접 검토합니다.",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인", color = Green, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
    )
}

@Composable
private fun FaqItem(
    question: String,
    answer: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = question,
            color = DeepGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = answer,
            color = Secondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

// =========================================================================
// Dialog Components (Account & Policy)
// =========================================================================

@Composable
private fun EditNicknameDialog(
    currentNickname: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(currentNickname) }
    val isValid = input.trim().length in 2..12

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("닉네임 변경", color = DeepGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "새로운 닉네임을 2~12자 사이로 입력해 주세요.",
                    color = Secondary,
                    fontSize = 12.5.sp,
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 12) input = it },
                    singleLine = true,
                    placeholder = { Text("닉네임 입력") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Green,
                        unfocusedBorderColor = Border,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (isValid) onConfirm(input.trim()) },
                    ),
                )

                Text(
                    text = "${input.length}/12자",
                    color = if (isValid) Secondary else DangerRed,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { onConfirm(input.trim()) },
            ) {
                Text(
                    text = "저장",
                    color = if (isValid) Green else Secondary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Secondary)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
    )
}

@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("개인정보 처리방침", color = DeepGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "세입세잎은 사용자의 프라이버시를 최우선으로 보호합니다.\n\n" +
                        "1. 수집 항목: 점검 분석용 JPEG 이미지, 매물 기본 주소 및 메모.\n" +
                        "2. 보관 원칙: 원본 영상 및 음성은 기기 내에만 보관하며 서버에 영구 업로드하지 않습니다.\n" +
                        "3. 인물 식별 정보 보호: 얼굴 및 민감 정보는 AI 분석 전 블러/가명 처리됩니다.\n" +
                        "4. 데이터 삭제: 사용자가 매물 삭제 시 관련 분석 데이터는 즉시 파기됩니다.",
                    color = Secondary,
                    fontSize = 12.5.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = Green, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
    )
}

@Composable
private fun TermsOfServiceDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("서비스 이용약관", color = DeepGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "세입세잎 서비스 이용약관 요약:\n\n" +
                        "1. 목적: 본 앱은 세입자의 현장 점검(임장)을 보조하는 AI 분석 도구입니다.\n" +
                        "2. AI 결과의 성격: AI는 하자를 법적으로 확정하지 않으며, 사용자의 최종 확인이 필요합니다.\n" +
                        "3. 촬영 권한: 현장 촬영 시 집주인 또는 중개인과의 상호 동의 하에 진행해야 합니다.",
                    color = Secondary,
                    fontSize = 12.5.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = Green, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
    )
}

@Composable
private fun OpenSourceLicenseDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("오픈소스 라이선스", color = DeepGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("• Android Jetpack Compose (Apache 2.0)", color = Secondary, fontSize = 12.sp)
                Text("• Google Dagger Hilt (Apache 2.0)", color = Secondary, fontSize = 12.sp)
                Text("• Square Retrofit & OkHttp (Apache 2.0)", color = Secondary, fontSize = 12.sp)
                Text("• Meta Wearables DAT SDK (Meta License)", color = Secondary, fontSize = 12.sp)
                Text("• Kakao Maps Open API SDK (Kakao License)", color = Secondary, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = Green, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
    )
}

@Composable
private fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("로그아웃할까요?", color = DeepGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = { Text("로그아웃하면 로그인 화면으로 이동합니다.", color = Secondary, fontSize = 13.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("로그아웃", color = DangerRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Secondary)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
    )
}

@Composable
private fun DeleteAccountConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("회원 탈퇴", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Text(
                text = "정말 탈퇴하시겠습니까?\n탈퇴 시 등록된 모든 매물 정보와 점검 리포트가 완전히 삭제되며 복구할 수 없습니다.",
                color = Secondary,
                fontSize = 13.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("탈퇴하기", color = DangerRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Secondary)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
    )
}