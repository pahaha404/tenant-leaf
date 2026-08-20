package com.seipseip.app.integration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seipseip.app.feature.inspection.FinishConfirmScreen
import com.seipseip.app.feature.inspection.InspectionPrepScreen
import com.seipseip.app.feature.inspection.LiveInspectionScreen
import com.seipseip.core.ui.ContentState
import com.seipseip.feature.inspection.domain.model.InspectionStatus
import com.seipseip.feature.inspection.presentation.InspectionDetailEvent
import com.seipseip.feature.inspection.presentation.InspectionDetailViewModel
import com.seipseip.feature.inspection.presentation.InspectionListEvent
import com.seipseip.feature.inspection.presentation.InspectionListViewModel

@Composable
fun InspectionPrepApiRoute(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    onSelectProperty: () -> Unit,
    viewModel: InspectionListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is InspectionListEvent.Created) onCreated(event.inspectionId.toString())
        }
    }
    InspectionPrepScreen(
        onBack = onBack,
        onStartInspection = viewModel::start,
        onSelectProperty = onSelectProperty,
        starting = state.starting,
        errorMessage = state.content.errorMessage(),
    )
}

@Composable
fun LiveInspectionApiRoute(
    zoneId: String,
    startedAt: Long,
    onCancelled: () -> Unit,
    onOpenGuide: (Int) -> Unit,
    onNextZone: (String) -> Unit,
    onFinish: (Long) -> Unit,
    viewModel: InspectionDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is InspectionDetailEvent.StatusChanged && event.status == InspectionStatus.CANCELLED) {
                onCancelled()
            }
        }
    }
    LiveInspectionScreen(
        zoneId = zoneId,
        startedAt = startedAt,
        onBack = viewModel::cancel,
        onOpenGuide = onOpenGuide,
        onNextZone = onNextZone,
        onFinish = onFinish,
    )
}

@Composable
fun InspectionFinishApiRoute(
    durationSeconds: Long,
    onBack: () -> Unit,
    onEnded: () -> Unit,
    viewModel: InspectionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is InspectionDetailEvent.StatusChanged && event.status == InspectionStatus.ENDED) {
                onEnded()
            }
        }
    }
    FinishConfirmScreen(
        onBack = onBack,
        durationSeconds = durationSeconds,
        onConfirm = viewModel::end,
        updating = state.updating,
        errorMessage = state.content.errorMessage(),
    )
}

private fun ContentState<*>.errorMessage(): String? = when (this) {
    is ContentState.NetworkError -> message
    is ContentState.ServerError -> message
    is ContentState.ValidationError -> message
    else -> null
}
