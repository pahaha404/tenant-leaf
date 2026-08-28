package com.seipseip.app.feature.inspection.voice

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppPageScaffold
import kotlinx.coroutines.delay

/** 점검 시작부터 종료까지 자동으로 동작하는 로컬 음성 기록 상태 카드. */
@Composable
fun VoiceRecordSection() {
    val state = VoiceRecordSession.result

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (state.recording) Color(0xFFFFF0E4) else Color.White)
            .border(1.dp, if (state.recording) Orange else Color(0xFFD9E1DA), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (state.recording) Orange else PaleGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Mic,
                contentDescription = null,
                tint = if (state.recording) Color.White else Green,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (state.recording) "음성 기록 중" else "음성 기록 완료",
                color = DeepGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                if (state.recording) "촬영과 함께 현장 이야기도 남기고 있어요."
                else "종료 화면에서 저장된 녹음을 확인할 수 있어요.",
                color = Secondary,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
        if (state.recording) {
            Text("● REC", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** 점검 종료 뒤 기기에 저장된 녹음을 재생하고 원하는 위치로 탐색한다. */
@Composable
fun VoiceRecordReviewCard() {
    val state = VoiceRecordSession.result

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFD9E1DA), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        VoiceRecordHeader(
            title = "점검 중 음성 기록",
            description = if (state.audioPath != null) "녹음 파일은 이 휴대전화에 저장됐어요."
            else "이번 점검에는 저장된 음성 파일이 없어요.",
        )
        state.audioPath?.let { VoicePlaybackControls(audioPath = it) }
    }
}

/** 매물 상세에서 해당 매물의 최근 점검 음성 기록을 확인하는 카드. */
@Composable
fun PropertyVoiceRecordCard(
    propertyId: String,
    onOpenSummary: (String) -> Unit,
) {
    val context = LocalContext.current
    val archiveVersion = VoiceRecordArchive.version
    val record = remember(propertyId, archiveVersion) {
        VoiceRecordArchive.latestForProperty(context, propertyId)
    }
    var importingLegacyRecord by remember(propertyId) { mutableStateOf(true) }

    LaunchedEffect(propertyId, archiveVersion) {
        if (record == null) VoiceRecordArchive.adoptLatestUnlinkedRecording(context, propertyId)
        importingLegacyRecord = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFD9E1DA), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VoiceRecordHeader(
            title = "점검 음성 기록",
            description = when {
                record != null -> "최근 점검에서 저장한 음성 기록이에요."
                importingLegacyRecord -> "이전 점검의 녹음 파일을 확인하고 있어요."
                else -> "이 매물에서 저장된 음성 기록이 없어요."
            },
        )

        if (record != null) {
            VoicePlaybackControls(audioPath = record.audioPath)
            VoiceArchiveButton(
                label = "음성 기록 크게 보기",
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenSummary(propertyId) },
            )
            Text("동의를 받은 현장 대화만 이 휴대전화에서 확인하세요.", color = Secondary, fontSize = 10.sp)
        }
    }
}

/** 매물별 최근 녹음을 큰 재생 컨트롤로 확인하는 화면. */
@Composable
fun VoiceSummaryScreen(propertyId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val archiveVersion = VoiceRecordArchive.version
    val record = remember(propertyId, archiveVersion) {
        VoiceRecordArchive.latestForProperty(context, propertyId)
    }

    LaunchedEffect(propertyId, archiveVersion) {
        if (record == null) VoiceRecordArchive.adoptLatestUnlinkedRecording(context, propertyId)
    }

    AppPageScaffold(title = "점검 음성 기록", onBack = onBack) {
        Text("최근 점검에서 저장한 음성을 확인해요.", color = DeepGreen, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        Text("녹음 파일은 서버로 전송하지 않고 이 휴대전화에만 저장됩니다.", color = Secondary, fontSize = 11.sp)

        if (record == null) {
            VoiceMessageBlock("저장된 음성 기록이 없어요", "이 매물에서 점검을 마친 뒤 다시 확인해 주세요.")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFD9E1DA), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VoiceRecordHeader("저장된 점검 음성", "재생하거나 탐색바를 움직여 원하는 부분을 들어보세요.")
                VoicePlaybackControls(audioPath = record.audioPath)
            }
        }
    }
}

@Composable
private fun VoiceRecordHeader(title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(PaleGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Mic, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(description, color = Secondary, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun VoicePlaybackControls(audioPath: String) {
    var player by remember(audioPath) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(audioPath) { mutableStateOf(false) }
    var positionMillis by remember(audioPath) { mutableIntStateOf(0) }
    var durationMillis by remember(audioPath) { mutableIntStateOf(0) }
    var playbackError by remember(audioPath) { mutableStateOf<String?>(null) }

    LaunchedEffect(audioPath) {
        runCatching {
            MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                durationMillis = duration.coerceAtLeast(0)
                setOnCompletionListener {
                    playing = false
                    positionMillis = durationMillis
                }
            }
        }.onSuccess {
            player = it
            playbackError = null
        }.onFailure {
            playbackError = "음성 파일을 재생하지 못했어요."
        }
    }

    LaunchedEffect(playing, player) {
        while (playing) {
            positionMillis = runCatching { player?.currentPosition ?: 0 }.getOrDefault(positionMillis)
            delay(250)
        }
    }

    DisposableEffect(audioPath) {
        onDispose { player?.release() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PaleGreen)
                    .clickable(enabled = player != null) {
                        val activePlayer = player ?: return@clickable
                        if (playing) {
                            activePlayer.pause()
                            playing = false
                        } else {
                            if (durationMillis > 0 && positionMillis >= durationMillis) {
                                activePlayer.seekTo(0)
                                positionMillis = 0
                            }
                            activePlayer.start()
                            playing = true
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (playing) "음성 기록 일시정지" else "음성 기록 재생",
                    tint = if (player == null) Secondary else Green,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Slider(
                value = positionMillis.coerceIn(0, durationMillis.coerceAtLeast(0)).toFloat(),
                onValueChange = { value ->
                    val target = value.toInt()
                    positionMillis = target
                    runCatching { player?.seekTo(target) }
                },
                valueRange = 0f..durationMillis.coerceAtLeast(1).toFloat(),
                enabled = player != null && durationMillis > 0,
                colors = SliderDefaults.colors(
                    thumbColor = Green,
                    activeTrackColor = Green,
                    inactiveTrackColor = Color(0xFFD9E1DA),
                ),
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(start = 52.dp)) {
            Text(formatPlaybackTime(positionMillis), color = Secondary, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text(formatPlaybackTime(durationMillis), color = Secondary, fontSize = 10.sp)
        }
        playbackError?.let { Text(it, color = Orange, fontSize = 10.sp) }
    }
}

internal fun formatPlaybackTime(milliseconds: Int): String {
    val totalSeconds = milliseconds.coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun VoiceMessageBlock(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(PaleGreen).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Text(body, color = DeepGreen, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun VoiceArchiveButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(PaleGreen)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}
