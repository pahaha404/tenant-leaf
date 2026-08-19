package com.seipseip.feature.property.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seipseip.core.common.AppResult
import com.seipseip.feature.property.domain.model.Property
import com.seipseip.feature.property.domain.model.PropertyPatch
import com.seipseip.feature.property.domain.usecase.CreatePropertyUseCase
import com.seipseip.feature.property.domain.usecase.GetPropertyUseCase
import com.seipseip.feature.property.domain.usecase.UpdatePropertyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface PropertyFormEvent {
    data class Saved(val id: UUID) : PropertyFormEvent
}

@HiltViewModel
class PropertyFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProperty: GetPropertyUseCase,
    private val createProperty: CreatePropertyUseCase,
    private val updateProperty: UpdatePropertyUseCase,
) : ViewModel() {
    private val propertyId = savedStateHandle.get<String>(PROPERTY_ID_ARGUMENT)?.let {
        runCatching { UUID.fromString(it) }.getOrNull()
    }
    private var original: Property? = null

    private val _state = MutableStateFlow(PropertyFormUiState(editing = propertyId != null))
    val state: StateFlow<PropertyFormUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PropertyFormEvent>()
    val events: SharedFlow<PropertyFormEvent> = _events.asSharedFlow()

    init {
        if (propertyId != null) load(propertyId)
    }

    fun updateFields(transform: (PropertyFormFields) -> PropertyFormFields) {
        _state.update {
            it.copy(
                fields = transform(it.fields),
                validationErrors = emptyMap(),
                errorMessage = null,
            )
        }
    }

    fun toggleAreaUnit() = updateFields(PropertyFormFields::convertAreaUnit)

    fun save() {
        if (_state.value.saving || _state.value.loading) return
        val parsed = _state.value.fields.parse()
        val draft = parsed.draft
        if (draft == null) {
            _state.update { it.copy(validationErrors = parsed.errors) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null) }
            val result = original?.let { current ->
                val patch = PropertyPatch.between(current, draft)
                if (patch.isEmpty) AppResult.Success(current) else updateProperty(current.id, patch)
            } ?: createProperty(draft)

            when (result) {
                is AppResult.Success -> _events.emit(PropertyFormEvent.Saved(result.value.id))
                is AppResult.Failure -> _state.update { it.copy(errorMessage = result.error.userMessage) }
            }
            _state.update { it.copy(saving = false) }
        }
    }

    private fun load(id: UUID) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            when (val result = getProperty(id)) {
                is AppResult.Success -> {
                    original = result.value
                    _state.update { it.copy(fields = result.value.toFormFields(), loading = false) }
                }
                is AppResult.Failure -> _state.update {
                    it.copy(loading = false, errorMessage = result.error.userMessage)
                }
            }
        }
    }

    companion object {
        const val PROPERTY_ID_ARGUMENT = "propertyId"
    }
}
