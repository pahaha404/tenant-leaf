package com.seipseip.feature.property.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID

@Composable
fun PropertyListRoute(
    onCreate: () -> Unit,
    onSelect: (UUID) -> Unit,
    viewModel: PropertyListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PropertyListScreen(
        state = state,
        onRetry = viewModel::refresh,
        onCreate = onCreate,
        onSelect = onSelect,
    )
}

@Composable
fun PropertyDetailRoute(
    onBack: () -> Unit,
    onInspections: (UUID) -> Unit,
    onEdit: (UUID) -> Unit,
    onDeleted: () -> Unit,
    viewModel: PropertyDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is PropertyDetailEvent.Deleted) onDeleted()
        }
    }
    PropertyDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::load,
        onInspections = onInspections,
        onEdit = onEdit,
        onDelete = viewModel::delete,
    )
}

@Composable
fun PropertyFormRoute(
    onBack: () -> Unit,
    onSaved: (UUID) -> Unit,
    viewModel: PropertyFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is PropertyFormEvent.Saved) onSaved(event.id)
        }
    }
    PropertyFormScreen(
        state = state,
        onBack = onBack,
        onFieldsChange = viewModel::updateFields,
        onToggleAreaUnit = viewModel::toggleAreaUnit,
        onSave = viewModel::save,
    )
}
