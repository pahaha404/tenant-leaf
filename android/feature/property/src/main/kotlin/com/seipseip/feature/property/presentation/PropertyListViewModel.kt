package com.seipseip.feature.property.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seipseip.core.common.AppResult
import com.seipseip.core.ui.ContentState
import com.seipseip.feature.property.domain.model.Property
import com.seipseip.feature.property.domain.usecase.DeletePropertyUseCase
import com.seipseip.feature.property.domain.usecase.ListPropertiesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PropertyListViewModel @Inject constructor(
    private val listProperties: ListPropertiesUseCase,
    private val deleteProperty: DeletePropertyUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<ContentState<List<Property>>>(ContentState.Idle)
    val state: StateFlow<ContentState<List<Property>>> = _state.asStateFlow()
    private val _deleteErrorMessage = MutableStateFlow<String?>(null)
    val deleteErrorMessage: StateFlow<String?> = _deleteErrorMessage.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value is ContentState.Loading) return
        viewModelScope.launch {
            _state.value = ContentState.Loading
            _state.value = when (val result = listProperties()) {
                is AppResult.Success -> if (result.value.items.isEmpty()) {
                    ContentState.Empty
                } else {
                    ContentState.Success(result.value.items)
                }
                is AppResult.Failure -> result.error.toContentState()
            }
        }
    }

    fun delete(id: java.util.UUID) {
        viewModelScope.launch {
            _deleteErrorMessage.value = null
            when (val result = deleteProperty(id)) {
                is AppResult.Success -> {
                    val remaining = (_state.value as? ContentState.Success)?.value.orEmpty()
                        .filterNot { it.id == id }
                    _state.value = if (remaining.isEmpty()) ContentState.Empty else ContentState.Success(remaining)
                }
                is AppResult.Failure -> _deleteErrorMessage.value = result.error.userMessage
            }
        }
    }

    fun deleteMultiple(ids: List<java.util.UUID>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _deleteErrorMessage.value = null
            var hasFailure = false
            var lastError: String? = null
            val deletedIds = mutableSetOf<java.util.UUID>()
            for (id in ids) {
                when (val result = deleteProperty(id)) {
                    is AppResult.Success -> deletedIds.add(id)
                    is AppResult.Failure -> {
                        hasFailure = true
                        lastError = result.error.userMessage
                    }
                }
            }
            if (deletedIds.isNotEmpty()) {
                val remaining = (_state.value as? ContentState.Success)?.value.orEmpty()
                    .filterNot { it.id in deletedIds }
                _state.value = if (remaining.isEmpty()) ContentState.Empty else ContentState.Success(remaining)
            }
            if (hasFailure) {
                _deleteErrorMessage.value = lastError
            }
        }
    }
}
