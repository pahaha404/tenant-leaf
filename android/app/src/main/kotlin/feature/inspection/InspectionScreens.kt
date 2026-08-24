package com.seipseip.app.feature.inspection

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seipseip.app.feature.home.GlassConnectionViewModel
import com.seipseip.app.feature.home.rememberGlassConnectionViewModel
import com.meta.wearable.dat.camera.types.VideoQuality
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlinx.coroutines.delay
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
    AppPageScaffold(title = "점검 준비", onBack = onBack) {
        Card(
            modifier = Modifier.fillMaxWidth().border(2.dp, Color(0xFF8B1E1E), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF241A1A)),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("⚠  촬영 허가를 반드시 받아주세요", color = Color(0xFFFFD6D2), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text("임대인과 기존 세입자에게 허가를 받고 촬영을 해야 합니다.", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                Text("허가 없이 촬영을 진행하지 마세요. 촬영·녹음에 동의하지 않은 사람이 포함되면 법적 책임이 발생할 수 있습니다.", color = Color(0xFFFFB4AB), fontSize = 11.sp, lineHeight = 17.sp)
            }
        }
        SectionTitle("점검을 시작하기 전이에요", "망원동 리버뷰 · 오늘 오후 4:00")
        InfoCard(title = "점검할 매물", description = "망원동 리버뷰 · 서울시 마포구 망원동", onClick = onSelectProperty)
        InfoCard(title = "촬영 전 확인", description = "휴대전화 카메라와 마이크 권한을 허용해 주세요.", accent = PaleGreen)
        InfoCard(title = "세입세잎 Glass 연결", description = "연결하지 않아도 휴대전화 카메라로 점검을 진행할 수 있어요.")
        errorMessage?.let { Text(it, color = Color(0xFFC93B2B), fontSize = 12.sp) }
        PrimaryButton(if (starting) "임장 생성 중..." else "점검 시작하기", onStartInspection, enabled = !starting)
    }
}
@Composable
fun InspectionPermissionWarningScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    AppPageScaffold(title = "촬영 전 경고", onBack = onBack) {
        Card(
            modifier = Modifier.fillMaxWidth().border(2.dp, Color(0xFF8B1E1E), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF241A1A)),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⚠  촬영 전 반드시 확인", color = Color(0xFFFFD6D2), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("허가를 받고 촬영을 해야 합니다.", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("임대인과 기존 세입자 모두에게 촬영·녹음 허가를 명확히 받으세요. 허가를 받지 못했다면 촬영을 시작하지 마세요.", color = Color(0xFFFFB4AB), fontSize = 12.sp, lineHeight = 19.sp)
                Text("공개되지 않은 타인 간 대화의 녹음·청취는 통신비밀보호법의 제한을 받을 수 있습니다. 위반 시 상황에 따라 민·형사상 책임이 발생할 수 있습니다.", color = Color(0xFFFFE4E1), fontSize = 11.sp, lineHeight = 17.sp)
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFB42318)).clickable(onClick = onContinue),
            contentAlignment = Alignment.Center,
        ) {
            Text("허가를 확인했고 촬영을 계속합니다", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
@Composable
fun InspectionCountdownScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var count by remember { mutableStateOf(3) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.KOREAN
                engine?.speak("3초 뒤 촬영이 시작됩니다.", TextToSpeech.QUEUE_FLUSH, null, "inspection_countdown")
            }
        }
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }
    LaunchedEffect(Unit) {
        count = 3
        delay(1_000)
        count = 2
        delay(1_000)
        count = 1
        delay(1_000)
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

@Composable
fun TutorialScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val tutorialVideoId = remember {
        context.resources.getIdentifier("tutorial_video", "raw", context.packageName)
    }
    var showVideo by remember { mutableStateOf(false) }

    AppPageScaffold(
        title = "튜토리얼",
        onBack = onBack,
        bottomAction = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFCFBF8))
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
        Text(
            "스마트 글래스로 한 번에!\n튜토리얼 영상 어쩌구",
            modifier = Modifier.fillMaxWidth().padding(top = 42.dp),
            color = Green,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            "스마트 글래스 촬영부터 직접 확인까지\n가장 중요한 흐름만 빠르게 알려드려요.",
            modifier = Modifier.fillMaxWidth(),
            color = Secondary,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .clickable { showVideo = true },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 218.dp, height = 140.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PaleGreen),
                contentAlignment = Alignment.Center,
            ) {
                Text("⌁", modifier = Modifier.align(Alignment.TopStart).padding(18.dp), color = Green, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.size(66.dp).clip(RoundedCornerShape(99.dp)).background(Green), contentAlignment = Alignment.Center) {
                    Text("▶", color = Color.White, fontSize = 25.sp)
                }
            }
        }
        StateBadge("◷  60초 영상", Orange)
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
                    .background(Color(0xFFFCFBF8))
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
        voiceGuide = "현관문과 공용 설비를 천천히 비춰주세요.",
        checkTitle = "현관·공용 확인 안내",
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

@Composable
fun LiveInspectionScreen(
    zoneId: String,
    startedAt: Long,
    onBack: () -> Unit,
    onOpenGuide: (Int) -> Unit,
    onNextZone: (String) -> Unit,
    onFinish: (Long) -> Unit,
    glassViewModel: GlassConnectionViewModel = rememberGlassConnectionViewModel(),
) {
    val zone = UiCatalog.zone(zoneId)
    val nextZone = UiCatalog.nextZone(zoneId)
    val zoneRows = UiCatalog.guideZones
    val liveContent = liveInspectionContent(zoneId)
    val previewState by glassViewModel.previewUiState.collectAsState()
    val connectionState by glassViewModel.uiState.collectAsState()

    DisposableEffect(glassViewModel) {
        glassViewModel.startPreview()
        onDispose {
            glassViewModel.stopPreview()
            glassViewModel.setPreviewSurface(null)
        }
    }

    var showFinishDialog by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var nowElapsed by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    var accumulatedPausedTime by remember { mutableStateOf(0L) }
    var lastPauseTimestamp by remember { mutableStateOf(0L) }
    val context = LocalContext.current

    val togglePause = {
        if (isPaused) {
            if (lastPauseTimestamp > 0L) {
                accumulatedPausedTime += (SystemClock.elapsedRealtime() - lastPauseTimestamp)
                lastPauseTimestamp = 0L
            }
            isPaused = false
            glassViewModel.startPreview()
            VoiceGuideManager.speak(context, "촬영을 재개합니다.")
        } else {
            lastPauseTimestamp = SystemClock.elapsedRealtime()
            isPaused = true
            glassViewModel.stopPreview()
            VoiceGuideManager.speak(context, "촬영을 일시정지합니다.")
        }
    }

    LaunchedEffect(startedAt, isPaused) {
        while (!isPaused) {
            nowElapsed = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    val effectiveNow = if (isPaused && lastPauseTimestamp > 0L) lastPauseTimestamp else nowElapsed
    val durationSeconds = ((effectiveNow - startedAt - accumulatedPausedTime) / 1_000L).coerceAtLeast(0L)

    var showQualityMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF6F4EF))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Text("‹", color = DeepGreen, fontSize = 28.sp, fontWeight = FontWeight.Medium) }
                Spacer(Modifier.width(8.dp))
                Text("실시간 점검", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isPaused) PaleGreen else Color(0xFFFFE7E2))
                        .clickable(onClick = togglePause)
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).clip(RoundedCornerShape(9.dp)).background(if (isPaused) Green else Color(0xFFC9573D)))
                    Spacer(Modifier.width(5.dp))
                    Text(if (isPaused) "일시정지" else "녹화 중", color = if (isPaused) Green else Color(0xFFC9573D), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.weight(1f))
                Box {
                    val qualityLabel = when (previewState.selectedQuality) {
                        VideoQuality.HIGH -> "고화질"
                        VideoQuality.MEDIUM -> "일반"
                        VideoQuality.LOW -> "절전"
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .clickable { showQualityMenu = true }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⚙️ $qualityLabel", color = DeepGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(2.dp))
                        Text("▾", color = DeepGreen, fontSize = 9.sp)
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
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF2F4437))
                        .clickable(onClick = togglePause),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            TextureView(context).apply {
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                        glassViewModel.setPreviewSurface(Surface(surfaceTexture))
                                        applyCenterCropTransform(this@apply, previewState.videoWidth, previewState.videoHeight)
                                    }
                                    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                        applyCenterCropTransform(this@apply, previewState.videoWidth, previewState.videoHeight)
                                    }
                                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                                        glassViewModel.setPreviewSurface(null)
                                        return true
                                    }
                                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
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
                                Text("⏸️ 스트리밍 및 녹화가 일시 중지됨", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("탭하여 다시 시작하기", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            }
                        }
                    } else if (!previewState.hasFirstFrame) {
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

                    Row(
                        modifier = Modifier.align(Alignment.TopStart).padding(13.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .92f)).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⌖", color = Green, fontSize = 14.sp)
                        Spacer(Modifier.width(5.dp))
                        Text("현재 구역 · ${zone.title}", color = DeepGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(formatInspectionDuration(durationSeconds), modifier = Modifier.align(Alignment.BottomEnd).padding(13.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(PaleGreen), contentAlignment = Alignment.Center) {
                        Text("◖", color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("AI 음성 안내", color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text(liveContent.voiceGuide, color = Secondary, fontSize = 10.sp, lineHeight = 15.sp)
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(29.dp).clip(RoundedCornerShape(10.dp)).background(PaleGreen), contentAlignment = Alignment.Center) { Text("✓", color = Green, fontWeight = FontWeight.ExtraBold) }
                        Spacer(Modifier.width(8.dp))
                        Text(liveContent.checkTitle, color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(liveContent.checkDescription, color = Secondary, fontSize = 11.sp, lineHeight = 16.sp)
                    liveContent.items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (index == 0) PaleGreen else Color(0xFFF8F8F6)).clickable { onOpenGuide(index) }.padding(horizontal = 11.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (index == 0) "●" else "○", color = if (index == 0) Green else Secondary, fontSize = 11.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(item, color = DeepGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text("가이드", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("실시간 인식 구역", color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    val currentZoneIndex = zoneRows.indexOfFirst { it.id == zoneId }
                    zoneRows.forEachIndexed { index, itemZone ->
                        val isCurrent = itemZone.id == zoneId
                        val isCompleted = index < currentZoneIndex
                        val state = when {
                            isCurrent -> "촬영 중"
                            isCompleted -> "확인 완료"
                            else -> "대기"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp)).background(if (isCurrent) PaleGreen else Color.White).padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (isCurrent || isCompleted) "●" else "○", color = if (isCurrent || isCompleted) Green else Secondary, fontSize = 9.sp)
                            Spacer(Modifier.width(7.dp))
                            Text(itemZone.title, color = if (isCurrent) DeepGreen else Secondary, fontSize = 10.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                            Spacer(Modifier.weight(1f))
                            Text(state, color = if (isCurrent || isCompleted) Green else Secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).background(Green).clickable(onClick = togglePause),
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
                        Text(if (isPaused) "촬영 재개" else "일시정지", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Box(
                    modifier = Modifier.width(92.dp).height(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF0E4)).clickable { showFinishDialog = true },
                    contentAlignment = Alignment.Center,
                ) { Text("점검 종료", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold) }
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
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(13.dp)).background(Orange).clickable {
                            VoiceGuideManager.speak(context, "촬영을 종료합니다. 해당 영상을 업로드해주세요.")
                            onFinish(durationSeconds)
                        },
                        contentAlignment = Alignment.Center,
                    ) { Text("네, 종료할게요", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
    }
}

object VoiceGuideManager {
    private var tts: TextToSpeech? = null

    fun speak(context: Context, text: String) {
        val appContext = context.applicationContext
        if (tts == null) {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = java.util.Locale.KOREAN
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_guide_${System.currentTimeMillis()}")
                }
            }
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_guide_${System.currentTimeMillis()}")
        }
    }

    fun stop() {
        tts?.stop()
    }
}

private fun applyCenterCropTransform(textureView: TextureView, videoWidth: Int, videoHeight: Int) {
    val viewWidth = textureView.width.toFloat()
    val viewHeight = textureView.height.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f) return

    // 비디오 해상도가 아직 0인 경우 기본 3:4 세로 비율(예: 864x1152)을 가정하여 찌그러짐 방지
    val effectiveWidth = if (videoWidth > 0) videoWidth.toFloat() else 3f
    val effectiveHeight = if (videoHeight > 0) videoHeight.toFloat() else 4f

    val viewRatio = viewWidth / viewHeight
    val videoRatio = effectiveWidth / effectiveHeight

    val scaleX: Float
    val scaleY: Float

    if (videoRatio > viewRatio) {
        // 비디오가 뷰보다 가로로 긴 경우: 높이를 맞추고 좌우를 잘라냄
        scaleY = 1f
        scaleX = (effectiveWidth * (viewHeight / effectiveHeight)) / viewWidth
    } else {
        // 비디오가 뷰보다 세로로 긴 경우(Meta Glass 기본): 가로를 꽉 채우고 상하를 자연스럽게 잘라냄
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
                Text("현관·공용  ·  주방  ·  창틀·환기\n거실·방  ·  화장실", color = Color(0xFFDCE9D6), fontSize = 13.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
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

        Text("촬영 구역 상세", color = DeepGreen, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            zones.forEachIndexed { index, zone ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(9.dp)).background(if (index % 2 == 0) PaleGreen else Color(0xFFF8F8F6)).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✓", color = Green, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    Text(zone.title, color = DeepGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("촬영 완료", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PaleOrange).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ⓘ", color = Orange, fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            Text("분석 결과는 확인이 필요한 관찰 결과이며, 최종 상태는 사용자가 결정해요.", color = Color(0xFF8B542D), fontSize = 11.sp, lineHeight = 16.sp)
        }
        errorMessage?.let { Text(it, color = Color(0xFFC93B2B), fontSize = 12.sp) }
        PrimaryButton(if (updating) "촬영 종료 처리 중..." else "촬영 종료하고 사진 준비", onConfirm, enabled = !updating)
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
    onSelectVideo: () -> Unit,
) {
    AppPageScaffold(title = "분석 진행", onBack = onBackToHome) {
        SectionTitle("구역별 기록을 정리하고 있어요", "촬영한 사진과 메모를 점검 구역별로 묶는 중이에요.")
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = Green,
            trackColor = PaleGreen,
        )
        Text(statusMessage, color = Secondary, fontSize = 12.sp)
        errorMessage?.let { Text(it, color = Color(0xFFC93B2B), fontSize = 12.sp) }
        UiCatalog.guideZones.forEachIndexed { index, zone ->
            val completedZoneCount = (progress.coerceIn(0f, 1f) * UiCatalog.guideZones.size).toInt()
            val isComplete = index < completedZoneCount
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isComplete) PaleGreen else Color.White)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(zone.title, color = DeepGreen, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (isComplete) "기록 정리 완료" else "확인 중", color = Secondary, fontSize = 11.sp)
                }
                if (isComplete) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = "정리 완료",
                        tint = Green,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(21.dp),
                        color = Green,
                        strokeWidth = 2.5.dp,
                    )
                }
            }
        }
        PrimaryButton(primaryActionLabel, onPrimaryAction)
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
                .background(Color.White).border(1.dp, Green, RoundedCornerShape(14.dp))
                .clickable(onClick = onSelectVideo),
            contentAlignment = Alignment.Center,
        ) { Text("영상 직접 선택", color = Green, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
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
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF6F4EF))) {
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
    onNextZone: (String) -> Unit,
    onOpenReport: () -> Unit,
) {
    val zone = UiCatalog.zone(zoneId)
    val nextZone = UiCatalog.nextZone(zoneId)
    val observations = observationItems(zoneId)
    val nextLabel = nextZone?.let { "${it.title} 관찰 보기" } ?: "점검 리포트 보기"

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF6F4EF))) {
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
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(PaleGreen).padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ⓘ", color = Green, fontSize = 15.sp)
                Spacer(Modifier.width(7.dp))
                Text("AI 관찰은 하자 확정이 아닌 촬영 장면 정리예요.", color = Secondary, fontSize = 10.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
        }

        Box(modifier = Modifier.fillMaxWidth().height(72.dp).padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Orange).clickable {
                    if (nextZone != null) onNextZone(nextZone.id) else onOpenReport()
                },
                contentAlignment = Alignment.Center,
            ) { Text(nextLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
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
            Text(
                "확인 필요",
                modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(PaleOrange).padding(horizontal = 6.dp, vertical = 3.dp),
                color = Orange,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
            )
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
