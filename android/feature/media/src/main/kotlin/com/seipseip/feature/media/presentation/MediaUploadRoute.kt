package com.seipseip.feature.media.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.max

@Composable
fun MediaUploadRoute(
    onBack: () -> Unit,
    viewModel: MediaUploadViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        viewModel::onPermissionResult,
    )
    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.useSelected(it)
        }
    }
    val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) { viewModel.start(hasPermission) }

    MediaUploadScreen(
        state = state,
        onBack = onBack,
        onRequestPermission = { permissionLauncher.launch(permission) },
        onRetry = viewModel::retry,
        onUseNewest = viewModel::useNewest,
        onPickVideo = {
            pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaUploadScreen(
    state: MediaUploadUiState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
    onUseNewest: () -> Unit,
    onPickVideo: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("사진 준비 및 전송") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (state) {
                MediaUploadUiState.PermissionRequired -> {
                    Text("최근 임장 영상을 찾으려면 동영상 접근 권한이 필요합니다.")
                    PrimaryAction("권한 허용", onRequestPermission)
                    SecondaryAction("영상 직접 선택", onPickVideo)
                }
                MediaUploadUiState.FindingVideo -> ProgressContent("최근 임장 영상을 찾는 중입니다.")
                MediaUploadUiState.NoVideo -> {
                    Text("임장 시작 이후 생성된 영상을 찾지 못했습니다.")
                    PrimaryAction("다시 찾기", onRetry)
                    SecondaryAction("영상 직접 선택", onPickVideo)
                }
                is MediaUploadUiState.ConfirmNewest -> {
                    Text("후보 영상이 ${state.candidates.size}개 있습니다.")
                    Text(
                        "가장 최근 영상: ${state.candidates.first().displayName}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrimaryAction("최근 영상으로 계속", onUseNewest)
                    SecondaryAction("다른 영상 선택", onPickVideo)
                }
                is MediaUploadUiState.Extracting -> StepProgress(
                    title = "영상에서 분석용 JPEG를 만드는 중입니다.",
                    completed = state.completed,
                    total = state.total,
                )
                is MediaUploadUiState.Uploading -> StepProgress(
                    title = "사진을 안전하게 전송하는 중입니다.",
                    completed = state.completed,
                    total = state.total,
                )
                is MediaUploadUiState.Completed -> {
                    Text("업로드 완료 · 분석 대기", style = MaterialTheme.typography.titleMedium)
                    Text("JPEG ${state.count}장 전송이 완료됐습니다.")
                    if (state.qualityReviewCount > 0) {
                        Text(
                            "선명도가 낮을 수 있는 사진 ${state.qualityReviewCount}장은 결과에서 다시 확인해 주세요.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        "원본 영상은 휴대전화 갤러리에만 남아 있으며 서버로 전송되지 않았습니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrimaryAction("임장으로 돌아가기", onBack)
                }
                is MediaUploadUiState.Error -> {
                    Text(
                        if (state.completed > 0) "업로드 일부 실패" else "업로드 또는 처리 실패",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    if (state.total > 0) Text("완료 ${state.completed} / 전체 ${state.total}")
                    PrimaryAction("다시 시도", onRetry)
                    SecondaryAction("영상 직접 선택", onPickVideo)
                }
            }
        }
    }
}

@Composable
private fun ProgressContent(message: String) {
    CircularProgressIndicator()
    Text(message, modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun StepProgress(title: String, completed: Int, total: Int) {
    Text(title)
    Spacer(Modifier.height(16.dp))
    if (total > 0) {
        LinearProgressIndicator(
            progress = { completed.toFloat() / max(1, total) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("$completed / $total", modifier = Modifier.padding(top = 8.dp))
    } else {
        CircularProgressIndicator()
    }
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
}

@Composable
private fun SecondaryAction(label: String, onClick: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
}
