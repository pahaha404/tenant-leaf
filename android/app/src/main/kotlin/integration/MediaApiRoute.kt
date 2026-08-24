package com.seipseip.app.integration

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seipseip.app.feature.inspection.AnalysisProgressScreen
import com.seipseip.feature.media.presentation.MediaUploadUiState
import com.seipseip.feature.media.presentation.MediaUploadViewModel

@Composable
fun MediaUploadApiRoute(
    onBackToHome: () -> Unit,
    viewModel: MediaUploadViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO
    else Manifest.permission.READ_EXTERNAL_STORAGE
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onPermissionResult(it)
    }
    val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    LaunchedEffect(Unit) { viewModel.start(hasPermission) }

    val presentation = state.toPresentation(
        requestPermission = { permissionLauncher.launch(permission) },
        useNewest = viewModel::useNewest,
        retry = viewModel::retry,
        finish = onBackToHome,
    )
    AnalysisProgressScreen(
        onBackToHome = onBackToHome,
        progress = presentation.progress,
        statusMessage = presentation.message,
        errorMessage = presentation.error,
        primaryActionLabel = presentation.primaryLabel,
        onPrimaryAction = presentation.primaryAction,
    )
}

private data class MediaPresentation(
    val progress: Float,
    val message: String,
    val error: String? = null,
    val primaryLabel: String,
    val primaryAction: () -> Unit,
)

private fun MediaUploadUiState.toPresentation(
    requestPermission: () -> Unit,
    useNewest: () -> Unit,
    retry: () -> Unit,
    finish: () -> Unit,
): MediaPresentation = when (this) {
    MediaUploadUiState.PermissionRequired -> MediaPresentation(0f, "동영상 접근 권한이 필요해요.", primaryLabel = "권한 허용", primaryAction = requestPermission)
    MediaUploadUiState.FindingVideo -> MediaPresentation(.05f, "갤러리에서 촬영된 영상을 자동으로 찾고 있어요.", primaryLabel = "영상 검색 중", primaryAction = {})
    MediaUploadUiState.NoVideo -> MediaPresentation(0f, "갤러리에서 촬영된 영상을 찾는 중이에요.", primaryLabel = "다시 확인", primaryAction = retry)
    is MediaUploadUiState.ConfirmNewest -> MediaPresentation(.1f, "가장 최근 촬영 영상을 처리 중이에요.", primaryLabel = "최근 영상 분석", primaryAction = useNewest)
    is MediaUploadUiState.Extracting -> MediaPresentation(
        progress = if (total == 0) .15f else .15f + .35f * completed / total,
        message = "3초 구간별 JPEG를 준비 중이에요. ${completed} / ${total}",
        primaryLabel = "처리 중",
        primaryAction = {},
    )
    is MediaUploadUiState.Uploading -> MediaPresentation(
        progress = if (total == 0) .55f else .55f + .4f * completed / total,
        message = "서버 저장소로 사진을 전송 중이에요. ${completed} / ${total}",
        primaryLabel = "업로드 중",
        primaryAction = {},
    )
    is MediaUploadUiState.Completed -> MediaPresentation(1f, "JPEG ${count}장 전송이 완료됐어요. 화질 확인 필요 ${qualityReviewCount}장", primaryLabel = "홈으로 이동", primaryAction = finish)
    is MediaUploadUiState.Error -> MediaPresentation(
        progress = if (total == 0) 0f else completed.toFloat() / total,
        message = "사진 준비 또는 전송을 완료하지 못했어요.",
        error = message,
        primaryLabel = "다시 시도",
        primaryAction = retry,
    )
}
