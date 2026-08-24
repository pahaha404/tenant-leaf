package com.seipseip.feature.media.presentation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seipseip.core.common.AppResult
import com.seipseip.feature.media.data.MediaWorkflowRepository
import com.seipseip.feature.media.domain.VideoCandidate
import com.seipseip.feature.media.domain.VideoSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface MediaUploadUiState {
    data object PermissionRequired : MediaUploadUiState
    data object FindingVideo : MediaUploadUiState
    data object NoVideo : MediaUploadUiState
    data class ConfirmNewest(val candidates: List<VideoCandidate>) : MediaUploadUiState
    data class Extracting(val completed: Int, val total: Int) : MediaUploadUiState
    data class Uploading(val completed: Int, val total: Int) : MediaUploadUiState
    data class Completed(val count: Int, val qualityReviewCount: Int) : MediaUploadUiState
    data class Error(val message: String, val completed: Int = 0, val total: Int = 0) : MediaUploadUiState
}

@HiltViewModel
class MediaUploadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MediaWorkflowRepository,
) : ViewModel() {
    private val inspectionId = savedStateHandle.get<String>(INSPECTION_ID_ARGUMENT)?.let {
        runCatching { UUID.fromString(it) }.getOrNull()
    }
    private val _state = MutableStateFlow<MediaUploadUiState>(MediaUploadUiState.PermissionRequired)
    val state: StateFlow<MediaUploadUiState> = _state.asStateFlow()

    fun start(hasPermission: Boolean) {
        if (!hasPermission) {
            _state.value = MediaUploadUiState.PermissionRequired
            return
        }
        findRecentVideo()
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) findRecentVideo() else _state.value = MediaUploadUiState.PermissionRequired
    }

    fun retry() = findRecentVideo()

    fun useNewest() {
        val candidate = (_state.value as? MediaUploadUiState.ConfirmNewest)?.candidates?.firstOrNull() ?: return
        process(candidate)
    }

    fun useSelected(uri: Uri) {
        viewModelScope.launch {
            when (val result = repository.describeVideo(uri)) {
                is AppResult.Success -> process(result.value)
                is AppResult.Failure -> _state.value = MediaUploadUiState.Error(result.error.userMessage)
            }
        }
    }

    private fun findRecentVideo() {
        val id = inspectionId ?: run {
            _state.value = MediaUploadUiState.Error("올바르지 않은 임장 번호입니다.")
            return
        }
        viewModelScope.launch {
            _state.value = MediaUploadUiState.FindingVideo
            when (val result = repository.findVideos(id)) {
                is AppResult.Success -> when (val selection = VideoSelection.from(result.value, VideoCandidate::createdAtMillis)) {
                    VideoSelection.None -> _state.value = MediaUploadUiState.NoVideo
                    is VideoSelection.Automatic -> process(selection.value)
                    is VideoSelection.ConfirmationRequired -> process(selection.newestFirst.first())
                }
                is AppResult.Failure -> _state.value = MediaUploadUiState.Error(result.error.userMessage)
            }
        }
    }

    private fun process(video: VideoCandidate) {
        val id = inspectionId ?: return
        viewModelScope.launch {
            _state.value = MediaUploadUiState.Extracting(0, 0)
            val photos = when (val result = repository.extract(video) { completed, total ->
                _state.value = MediaUploadUiState.Extracting(completed, total)
            }) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> {
                    _state.value = MediaUploadUiState.Error(result.error.userMessage)
                    return@launch
                }
            }
            _state.value = MediaUploadUiState.Uploading(0, photos.size)
            when (val result = repository.upload(id, photos) { progress ->
                _state.value = MediaUploadUiState.Uploading(progress.completed, progress.total)
            }) {
                is AppResult.Success -> _state.value = MediaUploadUiState.Completed(
                    count = photos.size,
                    qualityReviewCount = photos.count { it.needsQualityReview },
                )
                is AppResult.Failure -> {
                    val progress = _state.value as? MediaUploadUiState.Uploading
                    _state.value = MediaUploadUiState.Error(
                        message = result.error.userMessage,
                        completed = progress?.completed ?: 0,
                        total = progress?.total ?: photos.size,
                    )
                }
            }
        }
    }

    companion object {
        const val INSPECTION_ID_ARGUMENT = "inspectionId"
    }
}

