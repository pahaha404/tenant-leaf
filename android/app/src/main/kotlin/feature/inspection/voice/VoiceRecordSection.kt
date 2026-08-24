package com.seipseip.app.feature.inspection.voice

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.Secondary

/** 점검 시작부터 종료까지 자동으로 동작하는 로컬 음성 기록 상태 카드. */
@Composable
fun VoiceRecordSection(inspectionId: String) {
    val context = LocalContext.current
    val state = VoiceRecordSession.result
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) VoiceRecordSession.start(context, inspectionId)
    }

    LaunchedEffect(inspectionId) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            VoiceRecordSession.start(context, inspectionId)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

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
            Text("녹음 파일은 이 휴대전화에 저장됐어요.", color = Secondary, fontSize = 11.sp)
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
