package com.seipseip.feature.inspection.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID

@Composable
fun InspectionListRoute(
    onBack: () -> Unit,
    onSelect: (UUID) -> Unit,
    viewModel: InspectionListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is InspectionListEvent.Created) onSelect(event.inspectionId)
        }
    }
    InspectionListScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onStart = viewModel::start,
        onSelect = onSelect,
    )
}

@Composable
fun InspectionDetailRoute(
    onBack: () -> Unit,
    viewModel: InspectionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InspectionDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::load,
        onEnd = viewModel::end,
        onCancel = viewModel::cancel,
    )
}
