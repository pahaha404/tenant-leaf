package com.seipseip.feature.inspection.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seipseip.core.common.AppResult
import com.seipseip.core.ui.ContentState
import com.seipseip.feature.inspection.domain.model.Inspection
import com.seipseip.feature.inspection.domain.model.InspectionStatus
import com.seipseip.feature.inspection.domain.usecase.GetInspectionUseCase
import com.seipseip.feature.inspection.domain.usecase.UpdateInspectionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class InspectionDetailUiState(
    val content: ContentState<Inspection> = ContentState.Idle,
    val updating: Boolean = false,
)

@HiltViewModel
class InspectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getInspection: GetInspectionUseCase,
    private val updateInspectionStatus: UpdateInspectionStatusUseCase,
) : ViewModel() {
    private val inspectionId = savedStateHandle.get<String>(INSPECTION_ID_ARGUMENT)?.let {
        runCatching { UUID.fromString(it) }.getOrNull()
    }

    private val _state = MutableStateFlow(InspectionDetailUiState())
    val state: StateFlow<InspectionDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val id = inspectionId ?: run {
            _state.value = InspectionDetailUiState(
                content = ContentState.ValidationError("올바르지 않은 임장 번호입니다."),
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(content = ContentState.Loading)
            _state.value = _state.value.copy(
                content = when (val result = getInspection(id)) {
                    is AppResult.Success -> ContentState.Success(result.value)
                    is AppResult.Failure -> result.error.toInspectionContentState()
                },
            )
        }
    }

    fun end() = changeStatus(InspectionStatus.ENDED)

    fun cancel() = changeStatus(InspectionStatus.CANCELLED)

    private fun changeStatus(status: InspectionStatus) {
        val id = inspectionId ?: return
        val current = (_state.value.content as? ContentState.Success<Inspection>)?.value ?: return
        if (current.status != InspectionStatus.IN_PROGRESS || _state.value.updating) return
        viewModelScope.launch {
            _state.value = _state.value.copy(updating = true)
            _state.value = when (val result = updateInspectionStatus(id, status)) {
                is AppResult.Success -> InspectionDetailUiState(content = ContentState.Success(result.value))
                is AppResult.Failure -> _state.value.copy(
                    content = result.error.toInspectionContentState(),
                    updating = false,
                )
            }
        }
    }

    companion object {
        const val INSPECTION_ID_ARGUMENT = "inspectionId"
    }
}
