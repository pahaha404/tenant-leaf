package com.seipseip.app.feature.inspection

import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.view.Surface
import android.view.TextureView
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seipseip.app.R
import com.seipseip.app.feature.home.GlassConnectionViewModel
import com.seipseip.app.feature.home.rememberGlassConnectionViewModel
import com.seipseip.app.feature.inspection.preview.PhoneCameraPreviewHelper
import com.seipseip.app.feature.inspection.voice.VoiceRecordSection
import com.seipseip.app.feature.inspection.voice.VoiceRecordReviewCard
import com.seipseip.app.feature.inspection.voice.VoiceRecordSession
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.seipseip.app.feature.guide.guideImageResource
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.PaleOrange
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppPageScaffold
import com.seipseip.app.feature.common.InfoCard
import com.seipseip.app.feature.common.PrimaryButton
import com.seipseip.app.feature.common.SectionTitle
import com.seipseip.app.feature.common.StateBadge
import com.seipseip.app.feature.common.UiCatalog

@Composable
fun InspectionPrepScreen(
    onBack: () -> Unit,
    onStartInspection: () -> Unit,
    onSelectProperty: () -> Unit,
    starting: Boolean = false,
    errorMessage: String? = null,
) {
    AppPageScaffold(
        title = "점검 준비",
        onBack = onBack,
        bottomAction = {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                PrimaryButton(
                    label = if (starting) "임장 생성 중..." else "점검 시작하기",
                    onClick = onStartInspection,
                    enabled = !starting,
                )
            }
        },
    ) {
        SectionTitle("점검을 시작하기 전이에요", "망원동 리버뷰 · 오늘 오후 4:00")
        InfoCard(title = "점검할 매물", description = "망원동 리버뷰 · 서울시 마포구 망원동", onClick = onSelectProperty)
        InfoCard(title = "촬영 전 확인", description = "휴대전화 카메라와 마이크 권한을 허용해 주세요.", accent = PaleGreen)
        InfoCard(title = "세입세잎 Glass 연결", description = "연결하지 않아도 휴대전화 카메라로 점검을 진행할 수 있어요.")
        errorMessage?.let { Text(it, color = Color(0xFFC93B2B), fontSize = 12.sp) }
    }
}
@Composable
fun InspectionPermissionWarningScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    val context = LocalContext.current
    var landlordConsent by remember { mutableStateOf(false) }
    var occupantConsent by remember { mutableStateOf(false) }
    var recordingScopeConfirmed by remember { mutableStateOf(false) }
    val canContinue = landlordConsent && occupantConsent && recordingScopeConfirmed
    LaunchedEffect(Unit) {
        VoiceGuideManager.warmUp(context)
    }
    AppPageScaffold(
        title = "촬영 전 경고",
        onBack = onBack,
        bottomAction = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                PrimaryButton(
                    label = "허가를 확인했고 촬영을 계속합니다",
                    enabled = canContinue,
                    onClick = {
                        if (canContinue) {
                            onContinue()
                        }
                    },
                )
            }
        },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, Orange, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PaleOrange),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("촬영 전 꼭 확인해 주세요", color = DeepGreen, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("촬영과 녹음은 상대방이 알 수 있게 안내하고, 허가를 받은 뒤에만 시작합니다.", color = Secondary, fontSize = 13.sp, lineHeight = 20.sp)
                Text("집을 보여주는 임대인·중개인뿐 아니라, 기존 세입자나 동행자가 화면 또는 음성에 포함될 수 있다면 모두에게 먼저 알려야 합니다. 허가를 받지 못했거나 누가 포함되는지 확실하지 않다면 촬영하지 마세요.", color = Secondary, fontSize = 12.sp, lineHeight = 19.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("촬영을 시작하기 전에 아래 항목을 직접 확인해 주세요.", color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        ConsentCheckRow(landlordConsent, { landlordConsent = it }, "임대인 또는 중개인에게 촬영 사실을 알리고 허가를 받았습니다.")
        ConsentCheckRow(occupantConsent, { occupantConsent = it }, "현장에 있는 사람에게 영상과 음성 녹음 여부를 알렸습니다.")
        ConsentCheckRow(recordingScopeConfirmed, { recordingScopeConfirmed = it }, "촬영이 필요 없는 사람·사적인 대화·문서는 담지 않겠습니다.")
    }
}

@Composable
private fun ConsentCheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text, modifier = Modifier.padding(end = 10.dp), color = DeepGreen, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
@Composable
fun InspectionCountdownScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var count by remember { mutableStateOf(3) }

    LaunchedEffect(Unit) {
        VoiceGuideManager.speakWithPauses(context, INSPECTION_START_VOICE_GUIDE, 2_000)
        count = 3
        kotlinx.coroutines.delay(1_000)
        count = 2
        kotlinx.coroutines.delay(1_000)
        count = 1
        kotlinx.coroutines.delay(1_000)
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = .78f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("촬영을 시작합니다", color = Color.White.copy(alpha = .82f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(count.toString(), color = Color(0xFFFFCC45), fontSize = 120.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

internal val INSPECTION_START_VOICE_GUIDE = listOf(
    "3초 뒤 촬영이 시작됩니다.",
    "현관과 복도에서는 신발장 곰팡이와 벽·바닥 습기를, 거실에서는 천장, 벽지의 하자를 확인해 주세요.",
    "주방에서는 싱크대 아래 누수와 배수, 찬장 안쪽을 확인해 주세요.",
    "화장실에서는 누수·곰팡이와 배수 상태를 확인해 주세요.",
    "마지막으로 창틀에서는 창문 틈새와 결로, 곰팡이 흔적과 방충망을 확인해 주세요.",
)

@Composable
fun TutorialScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val tutorialVideoId = remember {
        context.resources.getIdentifier("tutorial_video", "raw", context.packageName)
    }
    val tutorialThumbnail by produceState<Bitmap?>(
        initialValue = null,
        key1 = tutorialVideoId,
    ) {
        value = if (tutorialVideoId == 0) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    MediaMetadataRetriever().run {
                        try {
                            setDataSource(
                                context,
                                Uri.parse("android.resource://${context.packageName}/$tutorialVideoId"),
                            )
                            getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        } finally {
                            release()
                        }
                    }
                }.getOrNull()
            }
        }
    }
    var showVideo by remember { mutableStateOf(false) }

    AppPageScaffold(
        title = "튜토리얼",
        onBack = onBack,
        scrollable = false,
        bottomAction = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PrimaryButton("▶  60초 영상 보기", onClick = { showVideo = true })
                Text(
                    "건너뛰기",
                    modifier = Modifier.clickable(onClick = onNext).padding(vertical = 12.dp),
                    color = Secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Text(
                "세입세잎, 이렇게 사용해요",
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 42.dp),
                color = Green,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PaleGreen)
                    .clickable { showVideo = true },
                contentAlignment = Alignment.Center,
            ) {
                tutorialThumbnail?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "세입세잎 튜토리얼 영상 썸네일",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Green.copy(alpha = 0.94f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("▶", color = Color.White, fontSize = 27.sp)
                }
            }
        }
    }

    if (showVideo) {
        if (tutorialVideoId != 0) {
            TutorialVideoPlayer(
                videoResId = tutorialVideoId,
                onDismiss = { showVideo = false },
                onFinished = {
                    showVideo = false
                    onNext()
                },
            )
        } else {
            TutorialVideoMissingDialog(onDismiss = { showVideo = false })
        }
    }
}

@Composable
private fun TutorialVideoPlayer(
    videoResId: Int,
    onDismiss: () -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var videoAspectRatio by remember { mutableStateOf(16f / 9f) }

    DisposableEffect(activity) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(onBack = onDismiss)
    if (isLandscape) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                val containerAspectRatio = maxWidth.value / maxHeight.value
                val videoModifier = if (videoAspectRatio > containerAspectRatio) {
                    Modifier.fillMaxWidth().aspectRatio(videoAspectRatio)
                } else {
                    Modifier.fillMaxHeight().aspectRatio(videoAspectRatio)
                }
                AndroidView(
                    modifier = videoModifier,
                    factory = { viewContext ->
                        VideoView(viewContext).apply {
                            val controller = MediaController(viewContext)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setVideoURI(Uri.parse("android.resource://${context.packageName}/$videoResId"))
                            setOnPreparedListener { player ->
                                if (player.videoWidth > 0 && player.videoHeight > 0) {
                                    videoAspectRatio = player.videoWidth.toFloat() / player.videoHeight
                                }
                                player.start()
                            }
                            setOnCompletionListener { onFinished() }
                        }
                    },
                )
                Text(
                    "닫기",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.Black.copy(alpha = .55f))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TutorialVideoMissingDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("튜토리얼 영상을 준비 중이에요", color = DeepGreen, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("영상 파일을 추가하면 이 자리에서 자동으로 재생돼요.", color = Secondary, fontSize = 12.sp)
                Text(
                    "확인",
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss).padding(vertical = 8.dp),
                    color = Green,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}
@Composable
fun TutorialChecklistScreen(
    onBack: () -> Unit,
    onOpenGuide: () -> Unit,
    onStart: () -> Unit,
) {
    AppPageScaffold(
        title = "점검 시작 전",
        onBack = onBack,
        bottomAction = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PrimaryButton("기본 체크리스트 훑어보기", onOpenGuide)
                Text(
                    "건너뛰기",
                    modifier = Modifier.clickable(onClick = onStart).padding(vertical = 12.dp),
                    color = Secondary,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        },
    ) {
        Text(
            "집 처음 구하는 사람을 위한\n기본 체크리스트",
            color = Green,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            "체크리스트를 훑어보며 집을 볼 때 무엇을 직접 확인해야 하는지 먼저 익혀봐요.",
            color = Secondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        Card(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(PaleGreen), contentAlignment = Alignment.Center) {
                        Text("✓", color = Green, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("방문 전 기본 체크리스트", color = DeepGreen, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Text("5개 구역 · 20개 기본 항목", color = Secondary, fontSize = 12.sp)
                    }
                }
                ChecklistGuideLine("◉", "문과 창문은 직접 열고 닫아봐요")
                ChecklistGuideLine("✋", "물은 틀어보고 배수까지 확인해요")
                ChecklistGuideLine("▣", "곰팡이와 누수 흔적은 가까이서 봐요")
                ChecklistGuideLine("◌", "관리비와 수리 약속은 꼭 기록해요")
            }
        }
    }
}

@Composable
private fun ChecklistGuideLine(symbol: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(symbol, color = Green, fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Secondary, fontSize = 13.sp)
    }
}

private data class LiveInspectionContent(
    val voiceGuide: String,
    val checkTitle: String,
    val checkDescription: String,
    val items: List<String>,
)

private fun liveInspectionContent(zoneId: String): LiveInspectionContent = when (zoneId) {
    "entry" -> LiveInspectionContent(
        voiceGuide = "현관문과 설비를 천천히 비춰주세요.",
        checkTitle = "현관 확인 안내",
        checkDescription = "문과 문틀부터 도어락, 신발장, 인터폰 순서로 확인해요.",
        items = listOf("문과 문틀", "도어락 비밀번호", "신발장 내부", "인터폰과 공용 복도"),
    )
    "kitchen" -> LiveInspectionContent(
        voiceGuide = "싱크대와 가스레인지 주변을 넓게 비춰주세요.",
        checkTitle = "주방 확인 안내",
        checkDescription = "물과 가스가 닿는 설비를 직접 작동하며 확인해요.",
        items = listOf("싱크대 상판 · 문짝", "하부장 · 배수관", "수도꼭지 · 싱크대 배수", "가스레인지 · 가스 밸브"),
    )
    "window" -> LiveInspectionContent(
        voiceGuide = "창문 전체와 창틀 모서리를 천천히 비춰주세요.",
        checkTitle = "창틀·환기 확인 안내",
        checkDescription = "개폐와 잠금, 틈새, 습기 흔적, 환기 상태를 확인해요.",
        items = listOf("창문 개폐 · 잠금", "창틀 틈새 · 파손", "곰팡이 · 결로 · 물자국", "방충망 · 환기 · 채광"),
    )
    "room" -> LiveInspectionContent(
        voiceGuide = "벽과 천장, 바닥, 전기 설비를 순서대로 비춰주세요.",
        checkTitle = "거실·방 확인 안내",
        checkDescription = "생활 공간의 누수 흔적과 마감, 냉난방 설비를 확인해요.",
        items = listOf("벽지 · 천장 누수 흔적", "바닥 상태", "콘센트 · 조명 · 스위치", "에어컨 · 난방 조절기"),
    )
    else -> LiveInspectionContent(
        voiceGuide = "벽·천장부터 바닥 배수와 수압까지 순서대로 살펴보세요.",
        checkTitle = "화장실 확인 안내",
        checkDescription = "습기 흔적과 배수, 환기, 수압·온수를 직접 확인해요.",
        items = listOf("천장 · 벽 곰팡이", "바닥 배수 · 역류", "환풍기 · 문틀 · 조명", "샤워기 수압 · 온수"),
    )
}
private fun formatInspectionDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

enum class CameraSource {
    GLASS, PHONE
}

@Composable
fun LiveInspectionScreen(
    inspectionId: String,
    zoneId: String,
    startedAt: Long,
    onBack: () -> Unit,
    onOpenGuide: (Int) -> Unit,
    onNextZone: (String) -> Unit,
    onFinish: (Long) -> Unit,
    canceling: Boolean = false,
    cancelErrorMessage: String? = null,
    glassViewModel: GlassConnectionViewModel = rememberGlassConnectionViewModel(),
) {
    var currentZoneId by remember(zoneId) { mutableStateOf(zoneId) }
    val nextZone = UiCatalog.nextZone(currentZoneId)
    val liveContent = liveInspectionContent(currentZoneId)
    val previewState by glassViewModel.previewUiState.collectAsState()
    val connectionState by glassViewModel.uiState.collectAsState()

    val context = LocalContext.current
    var microphonePermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var microphonePermissionResolved by remember { mutableStateOf(microphonePermissionGranted) }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        microphonePermissionGranted = granted
        microphonePermissionResolved = true
    }
    var cameraPermissionRequested by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = Wearables.RequestPermissionContract(),
    ) { result ->
        result.fold(
            onSuccess = { status ->
                if (status == PermissionStatus.Granted) glassViewModel.startPreview()
            },
            onFailure = { _, _ -> Unit },
        )
    }
    LaunchedEffect(previewState.needsCameraPermission) {
        if (previewState.needsCameraPermission && !cameraPermissionRequested) {
            cameraPermissionRequested = true
            cameraPermissionLauncher.launch(Permission.CAMERA)
        } else if (!previewState.needsCameraPermission) {
            cameraPermissionRequested = false
        }
    }
    LaunchedEffect(inspectionId, microphonePermissionGranted, microphonePermissionResolved) {
        if (!microphonePermissionGranted && !microphonePermissionResolved) {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val coroutineScope = rememberCoroutineScope()
    var isFinishing by remember { mutableStateOf(false) }
    val recorder = remember(context) {
        com.seipseip.app.feature.inspection.preview.InspectionVideoRecorder(context) { bytes, size ->
            VoiceRecordSession.appendPcm(bytes, size)
        }
    }
    val isGlassConnected = connectionState.isConnected
    var cameraSource by remember(isGlassConnected) {
        mutableStateOf(if (isGlassConnected) CameraSource.GLASS else CameraSource.PHONE)
    }

    LaunchedEffect(isGlassConnected) {
        if (!isGlassConnected && cameraSource == CameraSource.GLASS) {
            cameraSource = CameraSource.PHONE
        }
    }
    var activeSurface by remember { mutableStateOf<Surface?>(null) }
    val phoneCameraHelper = remember(context) { PhoneCameraPreviewHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            if (recorder.isRecording) {
                recorder.stopRecordingSync()
            }
        }
    }

    DisposableEffect(glassViewModel, cameraSource, inspectionId, microphonePermissionGranted, microphonePermissionResolved) {
        // PCM 콜백보다 먼저 파일을 열어 두어 첫 음성 프레임부터 저장한다.
        if (microphonePermissionGranted) {
            VoiceRecordSession.start(context, inspectionId)
        }
        if (microphonePermissionResolved && !recorder.isRecording) {
            recorder.startRecording()
        }
        val surface = activeSurface
        if (cameraSource == CameraSource.GLASS) {
            phoneCameraHelper.stopPreview()
            glassViewModel.startPreview()
            if (surface != null) {
                glassViewModel.setPreviewSurface(surface)
            }
        } else {
            glassViewModel.stopPreview()
            glassViewModel.setPreviewSurface(null)
            if (surface != null) {
                phoneCameraHelper.startPreview(surface)
            }
        }
        onDispose {
            glassViewModel.stopPreview()
            glassViewModel.setPreviewSurface(null)
            phoneCameraHelper.stopPreview()
        }
    }

    var showFinishDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var activeGuideIndex by remember { mutableStateOf<Int?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var nowElapsed by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    var accumulatedPausedTime by remember { mutableStateOf(0L) }
    var lastPauseTimestamp by remember { mutableStateOf(0L) }

    BackHandler(enabled = !isFinishing && !canceling) {
        showExitDialog = true
    }

    val togglePause = {
        if (isPaused) {
            if (lastPauseTimestamp > 0L) {
                accumulatedPausedTime += (SystemClock.elapsedRealtime() - lastPauseTimestamp)
                lastPauseTimestamp = 0L
            }
            isPaused = false
            recorder.resumeRecording()
            if (cameraSource == CameraSource.GLASS) {
                glassViewModel.startPreview()
            } else {
                activeSurface?.let { phoneCameraHelper.startPreview(it) }
            }
            VoiceGuideManager.speak(context, "촬영을 재개합니다.")
        } else {
            lastPauseTimestamp = SystemClock.elapsedRealtime()
            isPaused = true
            recorder.pauseRecording()
            if (cameraSource == CameraSource.GLASS) {
                glassViewModel.stopPreview()
            } else {
                phoneCameraHelper.stopPreview()
            }
            VoiceGuideManager.speak(context, "촬영을 일시정지합니다.")
        }
    }

    var durationSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(isPaused) {
        while (true) {
            durationSeconds = recorder.getRecordedDurationSeconds()
            kotlinx.coroutines.delay(500)
        }
    }

    var showQualityMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rec_alpha",
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable { showExitDialog = true },
                    contentAlignment = Alignment.Center,
                ) { Text("‹", color = DeepGreen, fontSize = 28.sp, fontWeight = FontWeight.Medium) }

                Spacer(Modifier.width(10.dp))
                Column {
                    Text("실시간 점검", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("안경·스마트폰 동시 녹화 중", color = Secondary, fontSize = 10.5.sp)
                }
                Spacer(Modifier.weight(1f))

                if (isGlassConnected) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (cameraSource == CameraSource.GLASS) Green else Color.Transparent)
                                .clickable {
                                    if (cameraSource != CameraSource.GLASS) {
                                        cameraSource = CameraSource.GLASS
                                        val surface = activeSurface
                                        phoneCameraHelper.stopPreview()
                                        glassViewModel.startPreview()
                                        if (surface != null) {
                                            glassViewModel.setPreviewSurface(surface)
                                        }
                                        VoiceGuideManager.speak(context, "안경 카메라로 전환합니다.")
                                    }
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                        ) {
                            Text("안경", color = if (cameraSource == CameraSource.GLASS) Color.White else Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (cameraSource == CameraSource.PHONE) Green else Color.Transparent)
                                .clickable {
                                    if (cameraSource != CameraSource.PHONE) {
                                        cameraSource = CameraSource.PHONE
                                        val surface = activeSurface
                                        glassViewModel.stopPreview()
                                        glassViewModel.setPreviewSurface(null)
                                        if (surface != null) {
                                            phoneCameraHelper.startPreview(surface)
                                        }
                                        VoiceGuideManager.speak(context, "스마트폰 카메라로 전환합니다.")
                                    }
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                        ) {
                            Text("핸드폰", color = if (cameraSource == CameraSource.PHONE) Color.White else Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.width(6.dp))
                    Box {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(Color.White)
                                .clickable { showQualityMenu = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("⚙️", fontSize = 14.sp)
                        }
                        DropdownMenu(
                            expanded = showQualityMenu,
                            onDismissRequest = { showQualityMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("고화질 (HIGH - 30fps/최대화질)") },
                                onClick = {
                                    glassViewModel.setVideoQuality(VideoQuality.HIGH)
                                    showQualityMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("일반화질 (MEDIUM - 표준해상도)") },
                                onClick = {
                                    glassViewModel.setVideoQuality(VideoQuality.MEDIUM)
                                    showQualityMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("절전화질 (LOW - 배터리/데이터 절약)") },
                                onClick = {
                                    glassViewModel.setVideoQuality(VideoQuality.LOW)
                                    showQualityMenu = false
                                },
                            )
                        }
                    }
                } else {
                    StateBadge("핸드폰 촬영", Green)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E2E25))
                        .clickable(onClick = togglePause),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                        val surface = Surface(surfaceTexture)
                                        activeSurface = surface
                                        if (cameraSource == CameraSource.GLASS) {
                                            glassViewModel.setPreviewSurface(surface)
                                        } else {
                                            phoneCameraHelper.startPreview(surface)
                                        }
                                        applyCenterCropTransform(this@apply, previewState.videoWidth, previewState.videoHeight)
                                    }
                                    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                        applyCenterCropTransform(this@apply, previewState.videoWidth, previewState.videoHeight)
                                    }
                                    private var lastFrameTime = 0L
                                    private var reusableBitmap: Bitmap? = null

                                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                                        activeSurface = null
                                        glassViewModel.setPreviewSurface(null)
                                        phoneCameraHelper.stopPreview()
                                        reusableBitmap?.recycle()
                                        reusableBitmap = null
                                        return true
                                    }

                                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                                        val now = SystemClock.elapsedRealtime()
                                        if (now - lastFrameTime >= 30L) {
                                            lastFrameTime = now
                                            val viewW = this@apply.width
                                            val viewH = this@apply.height
                                            if (viewW > 0 && viewH > 0) {
                                                val bmp = reusableBitmap?.takeIf { it.width == viewW && it.height == viewH } ?: run {
                                                    reusableBitmap?.recycle()
                                                    Bitmap.createBitmap(viewW, viewH, Bitmap.Config.ARGB_8888).also { reusableBitmap = it }
                                                }
                                                this@apply.getBitmap(bmp)
                                                recorder.drawBitmapFrame(bmp)
                                            }
                                        }
                                    }
                                }
                                addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                                    applyCenterCropTransform(this, previewState.videoWidth, previewState.videoHeight)
                                }
                            }
                        },
                        update = { textureView ->
                            applyCenterCropTransform(textureView, previewState.videoWidth, previewState.videoHeight)
                        },
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable(onClick = togglePause)
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isPaused) Orange else Color(0xFFE53935).copy(alpha = if (isPaused) 1f else pulseAlpha))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPaused) "일시정지" else "REC", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(6.dp))
                        Text(formatInspectionDuration(durationSeconds), color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (cameraSource == CameraSource.GLASS) "👓 AI 안경" else "📱 스마트폰",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (isPaused) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Green),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = "촬영 재개",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Text("촬영이 일시 중지되었습니다", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("화면이나 아래 버튼을 탭하여 다시 시작", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            }
                        }
                    } else if (cameraSource == CameraSource.GLASS && !previewState.hasFirstFrame) {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                if (connectionState.isConnected) "AI 글래스 카메라 스트리밍 연결 중..." else "AI 글래스 연결 대기 중",
                                color = Color.White.copy(alpha = .85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            previewState.message?.let {
                                Text(it, color = Color(0xFFFFCC45), fontSize = 11.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    UiCatalog.guideZones.forEach { gz ->
                        val isSelected = gz.id == currentZoneId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Green else Color.White)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Green else Color(0xFFE2E8E0),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable {
                                    if (currentZoneId != gz.id) {
                                        currentZoneId = gz.id
                                    }
                                }
                                .padding(horizontal = 13.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                gz.title,
                                color = if (isSelected) Color.White else DeepGreen,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(PaleGreen),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("💡", fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(liveContent.checkTitle, color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            Text(liveContent.checkDescription, color = Secondary, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    liveContent.items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFFF8F9F8))
                                .clickable { activeGuideIndex = index }
                                .padding(horizontal = 11.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(PaleGreen),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${index + 1}", color = Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.width(9.dp))
                            Text(
                                item,
                                color = DeepGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(0.8.dp, Color(0xFFD6E4DB), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("가이드", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(2.dp))
                                Text("›", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                VoiceRecordSection()

                Spacer(Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isPaused) Orange else Green)
                        .clickable(onClick = togglePause),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                            contentDescription = if (isPaused) "촬영 재개" else "일시정지",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (isPaused) "촬영 재개" else "일시정지",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF0E4))
                        .clickable { showFinishDialog = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("점검 종료", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        activeGuideIndex?.let { guideIdx ->
            val zone = UiCatalog.zone(currentZoneId)
            val item = zone.items.getOrElse(guideIdx) { zone.items.first() }
            val steps = item.steps
            val imageRes = guideImageResource(currentZoneId, guideIdx)
            val progress = "${zone.title} ${guideIdx + 1} / ${zone.items.size}"

            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x8A173426)).padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            progress,
                            modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(PaleGreen).padding(horizontal = 9.dp, vertical = 4.dp),
                            color = Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier.size(30.dp).clip(CircleShape).background(Color(0xFFF0F0F0)).clickable { activeGuideIndex = null },
                            contentAlignment = Alignment.Center,
                        ) { Text("✕", color = Secondary, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    }

                    Text(item.title, color = DeepGreen, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(item.description, color = Secondary, fontSize = 11.sp, lineHeight = 16.sp)

                    Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp)).background(PaleGreen)) {
                        Image(
                            painter = painterResource(imageRes),
                            contentDescription = "${item.title} 사진 예시",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PaleGreen).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("이렇게 살펴보세요", color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        steps.forEachIndexed { idx, step ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("${idx + 1}. ", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(step, color = DeepGreen, fontSize = 10.sp, lineHeight = 14.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (guideIdx < zone.items.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PaleGreen)
                                    .clickable { activeGuideIndex = guideIdx + 1 },
                                contentAlignment = Alignment.Center,
                            ) { Text("다음 가이드", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Green)
                                .clickable { activeGuideIndex = null },
                            contentAlignment = Alignment.Center,
                        ) { Text("확인 완료", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        if (showExitDialog) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x8A173426)).padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(PaleOrange),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "경고",
                            tint = Orange,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(
                        "점검을 중단하시겠습니까?",
                        color = DeepGreen,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "점검을 종료하지 않고 나가면 현재까지의 실시간 촬영 및 음성 녹음 내용이 저장되지 않고 취소됩니다.",
                        color = Secondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                    cancelErrorMessage?.let {
                        Text(it, color = Color(0xFFB3261E), fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Green)
                            .clickable { showExitDialog = false },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("계속 점검하기", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF2F4F2))
                            .clickable(enabled = !canceling, onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (canceling) "취소 처리 중..." else "점검 나가기 (취소)",
                            color = Secondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        if (showFinishDialog) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x8A173426)).padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(PaleOrange),
                        contentAlignment = Alignment.Center,
                    ) { Text("■", color = Orange, fontSize = 18.sp) }
                    Text("정말 촬영을\n종료하시겠습니까?", color = DeepGreen, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("촬영을 종료하면 AI가 수집된 장면을 분석해 구역별 관찰 결과를 정리해요.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PaleGreen).padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("♧", color = Green, fontSize = 16.sp)
                        Spacer(Modifier.width(7.dp))
                        Text("분석이 끝나면 알림으로 알려드릴게요.", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(13.dp)).background(Color.White).border(1.dp, Color(0xFFD9E1DA), RoundedCornerShape(13.dp)).clickable { showFinishDialog = false },
                        contentAlignment = Alignment.Center,
                    ) { Text("아니요, 계속 촬영할게요", color = Green, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(13.dp)).background(if (isFinishing) Secondary else Orange).clickable(enabled = !isFinishing) {
                            if (!isFinishing) {
                                isFinishing = true
                                coroutineScope.launch {
                                    val finalDuration = recorder.getRecordedDurationSeconds().coerceAtLeast(durationSeconds)
                                    recorder.stopRecording()
                                    VoiceRecordSession.finish(context)
                                    VoiceGuideManager.speak(context, "촬영을 종료합니다. 해당 영상을 업로드해주세요.")
                                    onFinish(finalDuration)
                                }
                            }
                        },
                        contentAlignment = Alignment.Center,
                    ) { Text(if (isFinishing) "영상 저장 및 정리 중..." else "네, 종료할게요", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
    }
}

object VoiceGuideManager {
    private val lock = Any()
    @Volatile
    private var tts: TextToSpeech? = null
    @Volatile
    private var isInitialized = false
    private var pendingTexts: List<String>? = null
    private var pendingPauseMillis = 0L

    fun warmUp(context: Context) {
        if (tts != null) return
        val appContext = context.applicationContext
        synchronized(lock) {
            if (tts != null) return
            tts = TextToSpeech(appContext) { status ->
                synchronized(lock) {
                    if (status == TextToSpeech.SUCCESS) {
                        try {
                            val result = tts?.setLanguage(java.util.Locale.KOREAN)
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                tts?.language = java.util.Locale.getDefault()
                            }
                            tts?.setSpeechRate(1.05f)
                        } catch (_: Exception) {}
                        isInitialized = true
                        pendingTexts?.let { texts ->
                            speakInternal(texts, pendingPauseMillis)
                            pendingTexts = null
                            pendingPauseMillis = 0L
                        }
                    }
                }
            }
        }
    }

    private fun speakInternal(texts: List<String>, pauseMillis: Long) {
        val params = android.os.Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
        }
        texts.forEachIndexed { index, text ->
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val utteranceId = "voice_guide_${System.currentTimeMillis()}_$index"
            try {
                tts?.speak(text, queueMode, params, utteranceId)
            } catch (_: Exception) {
                tts?.speak(text, queueMode, null, utteranceId)
            }
            if (index < texts.lastIndex && pauseMillis > 0) {
                tts?.playSilentUtterance(pauseMillis, TextToSpeech.QUEUE_ADD, "${utteranceId}_pause")
            }
        }
    }

    fun speak(context: Context, text: String) {
        speakWithPauses(context, listOf(text), 0)
    }

    fun speakWithPauses(context: Context, texts: List<String>, pauseMillis: Long) {
        val appContext = context.applicationContext
        synchronized(lock) {
            if (tts == null) {
                pendingTexts = texts
                pendingPauseMillis = pauseMillis
                warmUp(appContext)
            } else if (isInitialized) {
                speakInternal(texts, pauseMillis)
            } else {
                pendingTexts = texts
                pendingPauseMillis = pauseMillis
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            tts?.stop()
        }
    }

    fun shutdown() {
        synchronized(lock) {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            pendingTexts = null
            pendingPauseMillis = 0L
        }
    }
}

private fun applyCenterCropTransform(textureView: TextureView, videoWidth: Int, videoHeight: Int) {
    val viewWidth = textureView.width.toFloat()
    val viewHeight = textureView.height.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f) return

    val effectiveWidth = if (videoWidth > 0) videoWidth.toFloat() else 720f
    val effectiveHeight = if (videoHeight > 0) videoHeight.toFloat() else 1280f

    val viewRatio = viewWidth / viewHeight
    val videoRatio = effectiveWidth / effectiveHeight

    val scaleX: Float
    val scaleY: Float

    if (videoRatio > viewRatio) {
        scaleY = 1f
        scaleX = (effectiveWidth * (viewHeight / effectiveHeight)) / viewWidth
    } else {
        scaleX = 1f
        scaleY = (effectiveHeight * (viewWidth / effectiveWidth)) / viewHeight
    }

    val matrix = Matrix().apply {
        setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    }
    textureView.setTransform(matrix)
}

@Composable
fun FinishConfirmScreen(
    onBack: () -> Unit,
    durationSeconds: Long,
    onConfirm: () -> Unit,
    updating: Boolean = false,
    errorMessage: String? = null,
) {
    val zones = UiCatalog.guideZones
    AppPageScaffold(title = "촬영 종료 확인", onBack = onBack) {
        SectionTitle("점검 촬영을 마칠까요?", "촬영 기록을 정리한 뒤 구역별 분석을 시작해요.")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepGreen),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("촬영 구역", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${zones.size}개 구역 완료",
                        modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = .16f)).padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Text("현관  ·  주방  ·  창틀·환기\n거실·방  ·  화장실", color = Color(0xFFDCE9D6), fontSize = 13.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .11f)).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("총 촬영 시간", color = Color(0xFFDCE9D6), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(formatInspectionDuration(durationSeconds), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        VoiceRecordReviewCard()

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PaleOrange).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ⓘ", color = Orange, fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            Text("분석 결과는 확인이 필요한 내용이에요. 사진을 확인하고 직접 결정하세요!", color = Color(0xFF8B542D), fontSize = 11.sp, lineHeight = 16.sp)
        }
        errorMessage?.let { Text(it, color = Color(0xFFC93B2B), fontSize = 12.sp) }
        PrimaryButton(if (updating) "촬영 종료 처리 중..." else "다음", onConfirm, enabled = !updating)
    }
}
@Composable
fun AnalysisProgressScreen(
    onBackToHome: () -> Unit,
    progress: Float,
    statusMessage: String,
    errorMessage: String?,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
) {
    val uploadCompleted = progress >= 1f && errorMessage == null
    AppPageScaffold(
        title = "분석 진행",
        onBack = onBackToHome,
        scrollable = false,
        bottomAction = {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                PrimaryButton(primaryActionLabel, onPrimaryAction)
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(
                    if (uploadCompleted) "사진 전송을 완료했어요" else "촬영 내용을 분석하고 있어요",
                    if (uploadCompleted) "리포트에서 AI 분석 진행 상태를 확인할 수 있어요."
                    else "촬영한 영상에서 분석할 사진을 준비하는 중이에요.",
                )
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = Green,
                    trackColor = PaleGreen,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).background(Color.White).padding(vertical = 30.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.analysis_loading),
                    contentDescription = "촬영 내용 분석 준비",
                    modifier = Modifier.size(240.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!uploadCompleted) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Green, strokeWidth = 3.dp)
                    }
                    Text(
                        if (uploadCompleted) "사진 전송을 완료했어요" else "촬영 내용을 준비하고 있어요",
                        color = DeepGreen,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(statusMessage, color = Secondary, fontSize = 14.sp, lineHeight = 20.sp)
            }
            errorMessage?.let { Text(it, color = Color(0xFFC93B2B), fontSize = 12.sp) }
        }
    }
}

private data class CaptureResult(
    val zoneId: String,
    val media: String,
    val observation: String,
    val hasObservation: Boolean,
)

private val captureResults = listOf(
    CaptureResult("entry", "사진 4장", "관찰 없음", false),
    CaptureResult("kitchen", "사진 5장", "확인 필요 2건", true),
    CaptureResult("window", "사진 3장", "관찰 없음", false),
    CaptureResult("room", "사진 7장 · 영상 2개", "확인 필요 1건", true),
    CaptureResult("bathroom", "사진 6장 · 영상 1개", "확인 필요 2건", true),
)

@Composable
fun CaptureResultsScreen(
    onBack: () -> Unit,
    onOpenObservation: (String) -> Unit,
    onHome: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(99.dp)).background(Color.White).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("‹", color = DeepGreen, fontSize = 28.sp, fontWeight = FontWeight.Medium) }
            Text("촬영 결과", modifier = Modifier.weight(1f), color = DeepGreen, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.size(40.dp))
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("촬영이 종료되었습니다!", color = Green, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("촬영한 구역별 결과를 확인해 보세요.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(17.dp)).background(DeepGreen).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                    Text("⌕", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(11.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("구역별 촬영 기록 정리 완료", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("구역을 선택하면 촬영 장면과 관찰 결과를 볼 수 있어요.", color = Color(0xFFDCE9D6), fontSize = 10.sp)
                }
            }
            Text("구역별 촬영 결과", color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            captureResults.forEach { result ->
                val zone = UiCatalog.zone(result.zoneId)
                CaptureResultRow(zone.title, result.media, result.observation, result.hasObservation) {
                    onOpenObservation(result.zoneId)
                }
            }
            Text("AI 관찰은 하자 확정이 아닌 촬영 장면 정리예요.", modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), color = Secondary, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Box(modifier = Modifier.fillMaxWidth().height(72.dp).padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Orange).clickable(onClick = onHome),
                contentAlignment = Alignment.Center,
            ) { Text("홈으로 이동", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun CaptureResultRow(title: String, media: String, observation: String, hasObservation: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).border(1.dp, Color(0xFFD9E1DA), RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(33.dp).clip(RoundedCornerShape(10.dp)).background(if (hasObservation) PaleOrange else PaleGreen),
            contentAlignment = Alignment.Center,
        ) { Text(if (hasObservation) "!" else "✓", color = if (hasObservation) Orange else Green, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Text(media, color = Secondary, fontSize = 10.sp)
        }
        Text(
            observation,
            modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(if (hasObservation) PaleOrange else Color(0xFFDCE9D6)).padding(horizontal = 7.dp, vertical = 5.dp),
            color = if (hasObservation) Orange else Green,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.width(7.dp))
        Text("›", color = Green, fontSize = 21.sp, fontWeight = FontWeight.Medium)
    }
}
private data class ObservationItem(val title: String, val description: String)

private fun observationItems(zoneId: String): List<ObservationItem> = when (zoneId) {
    "entry" -> listOf(
        ObservationItem("문틀 주변 손상 흔적 확인 필요", "문틀과 문 손잡이 주변을 가까이에서 한 번 더 살펴보세요."),
        ObservationItem("공용 설비 상태 확인 필요", "인터폰과 공동 현관 장치가 정상 작동하는지 직접 확인해 주세요."),
    )
    "kitchen" -> listOf(
        ObservationItem("변색 또는 오염 흔적 확인 필요", "촬영된 장면에서 발견된 흔적이에요. 실제 상태를 직접 확인해 주세요."),
        ObservationItem("벽면 손상 흔적 확인 필요", "촬영 장면을 크게 보고 현장에서 한 번 더 살펴보세요."),
    )
    "window" -> listOf(
        ObservationItem("창틀 주변 습기 흔적 확인 필요", "창틀 모서리와 실리콘 주변을 만져보고 확인해 주세요."),
        ObservationItem("환기 설비 작동 확인 필요", "환풍기와 창문 잠금장치가 부드럽게 작동하는지 확인해 주세요."),
    )
    "room" -> listOf(
        ObservationItem("벽지·천장 마감 상태 확인 필요", "빛을 비춰 들뜸이나 변색된 부분이 있는지 살펴보세요."),
        ObservationItem("콘센트 주변 상태 확인 필요", "콘센트와 스위치가 흔들리거나 파손되지 않았는지 확인해 주세요."),
    )
    else -> listOf(
        ObservationItem("천장·벽 습기 흔적 확인 필요", "곰팡이와 변색 흔적이 있는지 가까이에서 살펴보세요."),
        ObservationItem("배수·수압 상태 확인 필요", "물을 틀어 배수 속도와 수압·온수가 정상인지 확인해 주세요."),
    )
}

@Composable
fun ObservationScreen(
    zoneId: String,
    onBack: () -> Unit,
    onReturnToProperty: () -> Unit,
) {
    val zone = UiCatalog.zone(zoneId)
    val observations = observationItems(zoneId)

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(99.dp)).background(Color.White).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("‹", color = DeepGreen, fontSize = 28.sp, fontWeight = FontWeight.Medium) }
            Text("구역 관찰", modifier = Modifier.weight(1f), color = Green, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(45.dp).clip(RoundedCornerShape(14.dp)).background(PaleOrange),
                    contentAlignment = Alignment.Center,
                ) { Text("⌖", color = Orange, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold) }
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(zone.title, color = Green, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("촬영 장면 5장 · 확인 필요 관찰 ${observations.size}건", color = Secondary, fontSize = 11.sp)
                }
            }
            Text("AI가 이 구역에서 정리한 확인 항목이에요.", color = Secondary, fontSize = 11.sp)

            observations.forEach { observation ->
                ObservationResultCard(observation)
            }

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(PaleOrange).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Info, contentDescription = "안내", tint = Orange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "AI 관찰 결과는 하자 또는 안전 상태를 확정하지 않습니다. 촬영 사진에서 확인이 필요한 흔적을 정리한 참고 정보이므로, 계약 전 현장에서 실제 상태를 직접 확인해 주세요.",
                    color = Color(0xFF8B542D),
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            color = Color.White,
            shadowElevation = 8.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp)
                    .height(54.dp).clip(RoundedCornerShape(16.dp)).background(Orange).clickable(onClick = onReturnToProperty),
                contentAlignment = Alignment.Center,
            ) { Text("매물 상세로 돌아가기", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ObservationResultCard(observation: ObservationItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFD9E1DA), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.width(84.dp).height(94.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFF7DECE)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("▧", color = Green, fontSize = 26.sp)
            Text("촬영 장면", color = Green, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(observation.title, color = DeepGreen, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(observation.description, color = Secondary, fontSize = 10.sp, lineHeight = 14.sp)
            Row(
                modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(PaleOrange).padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = "확인 필요", tint = Orange, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(3.dp))
                Text("확인 필요", color = Orange, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
@Composable
private fun CheckLine(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StateBadge("확인", Green)
        Text(text, modifier = Modifier.padding(start = 10.dp), color = DeepGreen, fontSize = 13.sp)
    }
}
