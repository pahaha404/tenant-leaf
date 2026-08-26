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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
            if (state.transcribing) {
                CircularProgressIndicator(modifier = Modifier.size(19.dp), color = Green, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.Mic, contentDescription = null, tint = if (state.recording) Color.White else Green, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                when {
                    state.recording -> "음성 기록 중"
                    state.transcribing -> "음성 기록 정리 중"
                    else -> "음성 기록 완료"
                },
                color = DeepGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                when {
                    state.recording -> "촬영과 함께 현장 이야기도 남기고 있어요."
                    state.transcribing -> "끝난 녹음을 텍스트로 바꾸는 중이에요."
                    else -> "종료 화면에서 녹음과 정리된 내용을 확인해요."
                },
                color = Secondary,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
        if (state.recording) Text("● REC", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** 점검 종료 뒤 녹음 재생, STT 원문, 짧은 요약을 한 장에서 확인한다. */
@Composable
fun VoiceRecordReviewCard() {
    val state = VoiceRecordSession.result
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose { player?.release() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFD9E1DA), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(PaleGreen), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Mic, contentDescription = null, tint = Green, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("점검 중 음성 기록", color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (state.transcribing) "말한 내용을 정리하고 있어요." else "녹음과 메모를 같이 확인해 보세요.", color = Secondary, fontSize = 10.sp)
            }
            state.audioPath?.let { path ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PaleGreen)
                        .clickable {
                            if (playing) {
                                player?.pause()
                                playing = false
                            } else {
                                player?.release()
                                player = MediaPlayer().apply {
                                    setDataSource(path)
                                    setOnCompletionListener { playing = false }
                                    prepare()
                                    start()
                                }
                                playing = true
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = if (playing) "음성 기록 일시정지" else "음성 기록 재생", tint = Green, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (state.transcribing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(15.dp), color = Green, strokeWidth = 2.dp)
                Spacer(Modifier.width(7.dp))
                Text("음성을 텍스트로 바꾸는 중이에요.", color = Secondary, fontSize = 11.sp)
            }
        } else if (state.transcript.isNotBlank()) {
            VoiceTextBlock("음성 메모", state.transcript)
            if (state.summary.isNotBlank()) VoiceTextBlock("짧게 정리하면", state.summary, highlighted = true)
        } else {
            Text(
                if (state.audioPath != null) "녹음 파일은 이 휴대전화에 저장됐어요."
                else "이번 점검에는 저장된 음성 파일이 없어요.",
                color = Secondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun VoiceTextBlock(title: String, body: String, highlighted: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(if (highlighted) PaleGreen else Color(0xFFF8F8F6)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = if (highlighted) Green else DeepGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        Text(body, color = DeepGreen, fontSize = 11.sp, lineHeight = 16.sp)
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
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var importingLegacyRecord by remember(propertyId) { mutableStateOf(true) }

    LaunchedEffect(propertyId, archiveVersion) {
        if (record == null) {
            VoiceRecordArchive.adoptLatestUnlinkedRecording(context, propertyId)?.let { adopted ->
                VoiceRecordSession.retryTranscription(
                    context = context,
                    inspectionId = adopted.inspectionId,
                    audioPath = adopted.audioPath,
                )
            }
        }
        importingLegacyRecord = false
    }
    DisposableEffect(Unit) {
        onDispose { player?.release() }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(PaleGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Mic, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("점검 음성 기록", color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    when {
                        record != null -> "최근 점검에서 저장한 음성 기록이에요."
                        importingLegacyRecord -> "이전 점검의 녹음 파일을 확인하고 있어요."
                        else -> "이 매물에서 저장된 음성 기록이 없어요."
                    },
                    color = Secondary,
                    fontSize = 10.sp,
                )
            }
        }

        if (record != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceArchiveButton(
                    label = if (playing) "일시정지" else "음성 녹음 재생",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (playing) {
                            player?.pause()
                            playing = false
                        } else {
                            runCatching {
                                player?.release()
                                player = MediaPlayer().apply {
                                    setDataSource(record!!.audioPath)
                                    setOnCompletionListener { playing = false }
                                    prepare()
                                    start()
                                }
                                playing = true
                            }
                        }
                    },
                )
                VoiceArchiveButton(
                    label = "음성 요약 보기",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSummary(propertyId) },
                    emphasized = true,
                )
            }
            Text("동의를 받은 현장 대화와 메모만 이 휴대전화에서 확인하세요.", color = Secondary, fontSize = 10.sp)
        }
    }

}

/** 매물별 최근 녹음의 핵심 요약을 먼저 보여주고, 요청할 때만 STT 원문을 펼치는 화면. */
@Composable
fun VoiceSummaryScreen(propertyId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val archiveVersion = VoiceRecordArchive.version
    val record = remember(propertyId, archiveVersion) {
        VoiceRecordArchive.latestForProperty(context, propertyId)
    }
    var showTranscript by remember { mutableStateOf(false) }

    LaunchedEffect(propertyId, archiveVersion) {
        if (record == null) {
            VoiceRecordArchive.adoptLatestUnlinkedRecording(context, propertyId)?.let { adopted ->
                VoiceRecordSession.retryTranscription(
                    context = context,
                    inspectionId = adopted.inspectionId,
                    audioPath = adopted.audioPath,
                )
            }
        }
    }

    AppPageScaffold(title = "점검 음성 요약", onBack = onBack) {
        Text("중개사와 나눈 이야기, 이렇게 정리했어요.", color = DeepGreen, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        Text("녹음과 텍스트는 이 휴대전화에만 저장됩니다.", color = Secondary, fontSize = 11.sp)

        when {
            record == null -> {
                VoiceTextBlock("저장된 음성 기록이 없어요", "이 매물에서 점검을 마친 뒤 다시 확인해 주세요.")
            }
            record.transcribing -> {
                VoiceTextBlock("음성을 텍스트로 바꾸는 중이에요", "정리가 끝나면 이 화면에 핵심 내용이 표시됩니다.", highlighted = true)
            }
            record.summary.isNotBlank() -> {
                VoiceTextBlock("핵심 내용", record.summary, highlighted = true)
            }
            else -> {
                VoiceTextBlock("아직 요약할 텍스트가 없어요", "녹음은 저장됐지만 이번 음성 인식 결과를 받지 못했습니다. 녹음 파일은 매물 상세에서 재생할 수 있어요.")
            }
        }

        if (record != null && !record.transcribing) {
            VoiceArchiveButton(
                label = if (showTranscript) "전체 STT 접기" else "전체 STT 보기",
                modifier = Modifier.fillMaxWidth(),
                onClick = { showTranscript = !showTranscript },
            )
            if (showTranscript) {
                if (record.transcript.isNotBlank()) {
                    VoiceTextBlock("전체 STT", record.transcript)
                } else {
                    VoiceTextBlock("전체 STT", "이번 음성 인식 결과를 아직 받지 못했어요. 아래 버튼으로 다시 변환할 수 있어요.")
                }
            }
        }

        if (record != null && !record.transcribing && record.transcript.isBlank()) {
            VoiceArchiveButton(
                label = "텍스트 변환 다시 시도",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    VoiceRecordSession.retryTranscription(
                        context = context,
                        inspectionId = record.inspectionId,
                        audioPath = record.audioPath,
                    )
                },
                emphasized = true,
            )
        }
    }
}

@Composable
private fun VoiceArchiveButton(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (emphasized) Green else PaleGreen)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (emphasized) Color.White else Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}
