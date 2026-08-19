package com.seipseip.feature.property.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.seipseip.core.ui.ContentState
import com.seipseip.feature.property.domain.AreaConverter
import com.seipseip.feature.property.domain.model.Property
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyListScreen(
    state: ContentState<List<Property>>,
    onRetry: () -> Unit,
    onCreate: () -> Unit,
    onSelect: (UUID) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("내 매물") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreate,
                modifier = Modifier.testTag("property-list-add"),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "매물 등록")
            }
        },
    ) { padding ->
        when (state) {
            ContentState.Idle,
            ContentState.Loading,
            -> LoadingContent(Modifier.padding(padding))
            ContentState.Empty -> EmptyProperties(
                onCreate = onCreate,
                modifier = Modifier.padding(padding),
            )
            is ContentState.Success -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(state.value, key = Property::id) { property ->
                    PropertyCard(property = property, onClick = { onSelect(property.id) })
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
            is ContentState.NetworkError -> ErrorContent(state.message, onRetry, Modifier.padding(padding))
            is ContentState.ServerError -> ErrorContent(state.message, onRetry, Modifier.padding(padding))
            is ContentState.ValidationError -> ErrorContent(state.message, onRetry, Modifier.padding(padding))
        }
    }
}

@Composable
private fun EmptyProperties(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("property-list-empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("등록된 매물이 없습니다.", style = MaterialTheme.typography.titleMedium)
        Text(
            "첫 매물을 등록하고 점검 준비를 시작해 보세요.",
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onCreate) { Text("매물 등록") }
    }
}

@Composable
private fun PropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("property-card-${property.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(property.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            property.addressSummary?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(property.priceSummary(), style = MaterialTheme.typography.bodyMedium)
            property.areaSquareMeters?.let {
                Text(
                    "${it.displayNumber()}㎡ · ${AreaConverter.squareMetersToPyeong(it).displayNumber()}평",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    state: PropertyDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onInspections: (UUID) -> Unit,
    onEdit: (UUID) -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("매물 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        when (val content = state.content) {
            ContentState.Idle,
            ContentState.Loading,
            -> LoadingContent(Modifier.padding(padding))
            is ContentState.Success -> PropertyDetailContent(
                property = content.value,
                deleting = state.deleting,
                onInspections = { onInspections(content.value.id) },
                onEdit = { onEdit(content.value.id) },
                onDelete = { confirmDelete = true },
                modifier = Modifier.padding(padding),
            )
            ContentState.Empty -> ErrorContent("매물 정보를 찾을 수 없습니다.", onRetry, Modifier.padding(padding))
            is ContentState.NetworkError -> ErrorContent(content.message, onRetry, Modifier.padding(padding))
            is ContentState.ServerError -> ErrorContent(content.message, onRetry, Modifier.padding(padding))
            is ContentState.ValidationError -> ErrorContent(content.message, onRetry, Modifier.padding(padding))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("매물을 삭제할까요?") },
            text = { Text("현재는 임장 기록이 없는 매물만 삭제할 수 있습니다. 임장 기록이 있는 매물의 처리 정책은 아직 정해지지 않았습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun PropertyDetailContent(
    property: Property,
    deleting: Boolean,
    onInspections: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(property.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        DetailRow("주소", property.addressSummary)
        DetailRow("보증금", property.depositAmount?.won())
        DetailRow("월세", property.monthlyRentAmount?.won())
        DetailRow("관리비", property.maintenanceFeeAmount?.won())
        DetailRow(
            "면적",
            property.areaSquareMeters?.let {
                "${it.displayNumber()}㎡ (${AreaConverter.squareMetersToPyeong(it).displayNumber()}평)"
            },
        )
        DetailRow("층수", property.floor)
        DetailRow("옵션", property.options.takeIf(Set<String>::isNotEmpty)?.joinToString())
        DetailRow("부동산 연락처", property.brokerContact)
        DetailRow("메모", property.note)
        DetailRow("최근 수정", property.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        Button(
            onClick = onInspections,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("property-inspections"),
        ) {
            Text("임장 기록 보기")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Text("수정", Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = onDelete, enabled = !deleting, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Text(if (deleting) "삭제 중" else "삭제", Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value ?: "입력하지 않음", modifier = Modifier.padding(top = 3.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyFormScreen(
    state: PropertyFormUiState,
    onBack: () -> Unit,
    onFieldsChange: ((PropertyFormFields) -> PropertyFormFields) -> Unit,
    onToggleAreaUnit: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editing) "매물 수정" else "매물 등록") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingContent(Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FormField(
                    value = state.fields.name,
                    onValueChange = { value -> onFieldsChange { it.copy(name = value) } },
                    label = "매물 이름 *",
                    error = state.validationErrors["name"],
                    testTag = "property-form-name",
                )
                FormField(state.fields.addressSummary, { value -> onFieldsChange { it.copy(addressSummary = value) } }, "주소 요약")
                FormField(state.fields.depositAmount, { value -> onFieldsChange { it.copy(depositAmount = value) } }, "보증금(원)", state.validationErrors["depositAmount"], keyboardType = KeyboardType.Number)
                FormField(state.fields.monthlyRentAmount, { value -> onFieldsChange { it.copy(monthlyRentAmount = value) } }, "월세(원)", state.validationErrors["monthlyRentAmount"], keyboardType = KeyboardType.Number)
                FormField(state.fields.maintenanceFeeAmount, { value -> onFieldsChange { it.copy(maintenanceFeeAmount = value) } }, "관리비(원)", state.validationErrors["maintenanceFeeAmount"], keyboardType = KeyboardType.Number)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FormField(
                        value = state.fields.area,
                        onValueChange = { value -> onFieldsChange { it.copy(area = value) } },
                        label = if (state.fields.areaUnit == AreaUnit.SQUARE_METERS) "면적(㎡)" else "면적(평)",
                        error = state.validationErrors["areaSquareMeters"],
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onToggleAreaUnit, modifier = Modifier.padding(start = 8.dp)) {
                        Text(if (state.fields.areaUnit == AreaUnit.SQUARE_METERS) "평으로" else "㎡로")
                    }
                }
                FormField(state.fields.floor, { value -> onFieldsChange { it.copy(floor = value) } }, "층수")
                FormField(state.fields.options, { value -> onFieldsChange { it.copy(options = value) } }, "옵션(쉼표로 구분)")
                FormField(state.fields.brokerContact, { value -> onFieldsChange { it.copy(brokerContact = value) } }, "부동산 연락처")
                FormField(state.fields.note, { value -> onFieldsChange { it.copy(note = value) } }, "메모", singleLine = false)
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("property-form-error"))
                }
                Button(
                    onClick = onSave,
                    enabled = !state.saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("property-form-save"),
                ) {
                    Text(if (state.saving) "저장 중" else "저장")
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    testTag: String? = null,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag)),
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text("불러오는 중", modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("property-error"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("다시 시도") }
    }
}

private fun Property.priceSummary(): String {
    val deposit = depositAmount?.won() ?: "보증금 미입력"
    val rent = monthlyRentAmount?.won()?.let { "월세 $it" } ?: "월세 미입력"
    return "$deposit · $rent"
}

private fun Long.won(): String = "${NumberFormat.getNumberInstance(Locale.KOREA).format(this)}원"

private fun Double.displayNumber(): String = AreaConverter.roundForDisplay(this).toString().trimEnd('0').trimEnd('.')
