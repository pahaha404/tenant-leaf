package com.seipseip.feature.property.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seipseip.core.common.AppResult
import com.seipseip.core.ui.ContentState
import com.seipseip.feature.property.domain.model.Property
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
) : ViewModel() {
    private val _state = MutableStateFlow<ContentState<List<Property>>>(ContentState.Idle)
    val state: StateFlow<ContentState<List<Property>>> = _state.asStateFlow()

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
}

