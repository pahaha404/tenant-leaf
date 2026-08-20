package com.seipseip.app.integration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seipseip.app.feature.property.PropertyDetailScreen
import com.seipseip.app.feature.property.PropertyFormScreen
import com.seipseip.app.feature.property.PropertyListScreen
import com.seipseip.app.feature.property.PropertyInfoScreen
import com.seipseip.app.feature.property.PropertySelectScreen
import com.seipseip.app.feature.property.PropertyUiModel
import com.seipseip.core.ui.ContentState
import com.seipseip.feature.property.domain.model.Property
import com.seipseip.feature.property.presentation.PropertyDetailViewModel
import com.seipseip.feature.property.presentation.PropertyFormEvent
import com.seipseip.feature.property.presentation.PropertyFormViewModel
import com.seipseip.feature.property.presentation.PropertyListViewModel

@Composable
fun PropertyListApiRoute(
    onAddProperty: () -> Unit,
    onOpenProperty: (String) -> Unit,
    onTabSelected: (String) -> Unit,
    viewModel: PropertyListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PropertyListScreen(
        properties = state.values().map(Property::toUi),
        loading = state is ContentState.Loading || state is ContentState.Idle,
        errorMessage = state.errorMessage(),
        onAddProperty = onAddProperty,
        onOpenProperty = onOpenProperty,
        onRetry = viewModel::refresh,
        onTabSelected = onTabSelected,
    )
}

@Composable
fun PropertyFormApiRoute(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: PropertyFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is PropertyFormEvent.Saved) onSaved(event.id.toString())
        }
    }
    PropertyFormScreen(
        onBack = onBack,
        saving = state.saving,
        errorMessage = state.errorMessage,
        onSaved = { input ->
            viewModel.updateFields { fields ->
                fields.copy(
                    name = input.name,
                    addressSummary = input.address,
                    depositAmount = input.depositAmount,
                    monthlyRentAmount = input.monthlyRentAmount,
                )
            }
            viewModel.save()
        },
    )
}

@Composable
fun PropertyDetailApiRoute(
    onBack: () -> Unit,
    onStartInspection: (String) -> Unit,
    onOpenReport: () -> Unit,
    onOpenBasicInfo: (PropertyUiModel?) -> Unit,
    onTabSelected: (String) -> Unit,
    viewModel: PropertyDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val property = (state.content as? ContentState.Success<Property>)?.value?.toUi()
    PropertyDetailScreen(
        property = property,
        loading = state.content is ContentState.Loading || state.content is ContentState.Idle,
        errorMessage = state.content.errorMessage(),
        onBack = onBack,
        onStartInspection = { property?.id?.let(onStartInspection) },
        onOpenReport = onOpenReport,
        onOpenBasicInfo = { onOpenBasicInfo(property) },
        onTabSelected = onTabSelected,
    )
}

@Composable
fun PropertyInfoApiRoute(
    onBack: () -> Unit,
    viewModel: PropertyDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val property = (state.content as? ContentState.Success<Property>)?.value?.toUi()
    PropertyInfoScreen(property = property, onBack = onBack)
}

@Composable
fun PropertySelectApiRoute(
    onBack: () -> Unit,
    onSelected: (String) -> Unit,
    onAddProperty: () -> Unit,
    viewModel: PropertyListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val properties = state.values().map(Property::toUi)
    var selectedId by remember(properties) { mutableStateOf(properties.firstOrNull()?.id) }
    PropertySelectScreen(
        properties = properties,
        selectedId = selectedId,
        loading = state is ContentState.Loading || state is ContentState.Idle,
        onBack = onBack,
        onPropertySelected = { selectedId = it },
        onSelected = onSelected,
        onAddProperty = onAddProperty,
    )
}

fun Property.toUi() = PropertyUiModel(
    id = id.toString(), name = name, address = addressSummary ?: "주소 미입력",
    depositAmount = depositAmount, monthlyRentAmount = monthlyRentAmount,
    maintenanceFeeAmount = maintenanceFeeAmount,
)

private fun ContentState<List<Property>>.values(): List<Property> =
    (this as? ContentState.Success<List<Property>>)?.value.orEmpty()

private fun ContentState<*>.errorMessage(): String? = when (this) {
    is ContentState.NetworkError -> message
    is ContentState.ServerError -> message
    is ContentState.ValidationError -> message
    else -> null
}
