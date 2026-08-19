package com.seipseip.feature.inspection.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seipseip.core.common.AppResult
import com.seipseip.core.ui.ContentState
import com.seipseip.feature.inspection.domain.model.Inspection
import com.seipseip.feature.inspection.domain.usecase.CreateInspectionUseCase
import com.seipseip.feature.inspection.domain.usecase.ListInspectionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class InspectionListUiState(
    val content: ContentState<List<Inspection>> = ContentState.Idle,
    val starting: Boolean = false,
)

sealed interface InspectionListEvent {
    data class Created(val inspectionId: UUID) : InspectionListEvent
}

@HiltViewModel
class InspectionListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listInspections: ListInspectionsUseCase,
    private val createInspection: CreateInspectionUseCase,
) : ViewModel() {
    private val propertyId = savedStateHandle.get<String>(PROPERTY_ID_ARGUMENT)?.let {
        runCatching { UUID.fromString(it) }.getOrNull()
    }

    private val _state = MutableStateFlow(InspectionListUiState())
    val state: StateFlow<InspectionListUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<InspectionListEvent>()
    val events: SharedFlow<InspectionListEvent> = _events.asSharedFlow()

    init { refresh() }

    fun refresh() {
        val id = propertyId ?: run {
            _state.value = InspectionListUiState(
                content = ContentState.ValidationError("올바르지 않은 매물 번호입니다."),
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(content = ContentState.Loading)
            _state.value = _state.value.copy(
                content = when (val result = listInspections(id)) {
                    is AppResult.Success -> if (result.value.items.isEmpty()) {
                        ContentState.Empty
                    } else {
                        ContentState.Success(result.value.items)
                    }
                    is AppResult.Failure -> result.error.toInspectionContentState()
                },
            )
        }
    }

    fun start() {
        val id = propertyId ?: return
        if (_state.value.starting) return
        viewModelScope.launch {
            _state.value = _state.value.copy(starting = true)
            when (val result = createInspection(id)) {
                is AppResult.Success -> _events.emit(InspectionListEvent.Created(result.value.id))
                is AppResult.Failure -> _state.value = _state.value.copy(
                    content = result.error.toInspectionContentState(),
                )
            }
            _state.value = _state.value.copy(starting = false)
        }
    }

    companion object {
        const val PROPERTY_ID_ARGUMENT = "propertyId"
    }
}
