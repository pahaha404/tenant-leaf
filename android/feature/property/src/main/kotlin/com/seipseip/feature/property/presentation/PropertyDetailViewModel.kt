package com.seipseip.feature.property.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seipseip.core.common.AppResult
import com.seipseip.core.ui.ContentState
import com.seipseip.feature.property.domain.model.Property
import com.seipseip.feature.property.domain.usecase.DeletePropertyUseCase
import com.seipseip.feature.property.domain.usecase.GetPropertyUseCase
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

sealed interface PropertyDetailEvent {
    data object Deleted : PropertyDetailEvent
}

data class PropertyDetailUiState(
    val content: ContentState<Property> = ContentState.Idle,
    val deleting: Boolean = false,
)

@HiltViewModel
class PropertyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProperty: GetPropertyUseCase,
    private val deleteProperty: DeletePropertyUseCase,
) : ViewModel() {
    private val propertyId = savedStateHandle.get<String>(PROPERTY_ID_ARGUMENT)?.let {
        runCatching { UUID.fromString(it) }.getOrNull()
    }

    private val _state = MutableStateFlow(PropertyDetailUiState())
    val state: StateFlow<PropertyDetailUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PropertyDetailEvent>()
    val events: SharedFlow<PropertyDetailEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        val id = propertyId ?: run {
            _state.value = PropertyDetailUiState(
                content = ContentState.ValidationError("올바르지 않은 매물 번호입니다."),
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(content = ContentState.Loading)
            _state.value = _state.value.copy(
                content = when (val result = getProperty(id)) {
                    is AppResult.Success -> ContentState.Success(result.value)
                    is AppResult.Failure -> result.error.toContentState()
                },
            )
        }
    }

    fun delete() {
        val id = propertyId ?: return
        if (_state.value.deleting) return
        viewModelScope.launch {
            _state.value = _state.value.copy(deleting = true)
            when (val result = deleteProperty(id)) {
                is AppResult.Success -> _events.emit(PropertyDetailEvent.Deleted)
                is AppResult.Failure -> {
                    _state.value = _state.value.copy(content = result.error.toContentState())
                }
            }
            _state.value = _state.value.copy(deleting = false)
        }
    }

    companion object {
        const val PROPERTY_ID_ARGUMENT = "propertyId"
    }
}

