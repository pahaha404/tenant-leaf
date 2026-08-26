package com.seipseip.app.feature.property

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnAttach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.seipseip.app.feature.property.location.KakaoAddressSearch
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.R
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppPageScaffold
import com.seipseip.app.feature.common.AppTab
import com.seipseip.app.feature.common.InfoCard
import com.seipseip.app.feature.common.PrimaryButton
import com.seipseip.app.feature.common.SectionTitle
import com.seipseip.app.feature.common.StateBadge
import com.seipseip.app.feature.property.location.addressWithDetail
import com.seipseip.app.feature.property.location.splitAddressForEditing
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

data class PropertyUiModel(
    val id: String,
    val name: String,
    val address: String = "주소 미입력",
    val depositAmount: Long? = null,
    val monthlyRentAmount: Long? = null,
    val maintenanceFeeAmount: Long? = null,
)

data class PropertyFormSubmission(
    val name: String,
    val address: String,
    val depositAmount: String,
    val monthlyRentAmount: String,
    val maintenanceFeeAmount: String,
)

internal fun propertyDeleteRevealOffset(cardWidthPx: Int): Float = -cardWidthPx / 4f

private enum class PropertySwipePosition { Closed, Delete }

@Composable
fun PropertyListScreen(
    properties: List<PropertyUiModel>,
    loading: Boolean,
    errorMessage: String?,
    deleteErrorMessage: String?,
    onAddProperty: () -> Unit,
    onOpenProperty: (String) -> Unit,
    onDeleteProperty: (String) -> Unit,
    onDeleteMultipleProperties: ((List<String>) -> Unit)? = null,
    onRetry: () -> Unit,
    onOpenMapOverview: (() -> Unit)? = null,
    onTabSelected: (String) -> Unit = {},
    showBottomBar: Boolean = true,
) {
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showBatchDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(properties) {
        if (properties.isEmpty()) {
            isSelectionMode = false
            selectedIds = emptySet()
        } else {
            selectedIds = selectedIds.filter { id -> properties.any { it.id == id } }.toSet()
        }
    }

    AppPageScaffold(
        title = if (isSelectionMode) "매물 삭제 (${selectedIds.size})" else "매물",
        selectedTab = AppTab.Property,
        showBottomBar = showBottomBar,
        isRefreshing = loading,
        onRefresh = onRetry,
        topTrailingAction = {
            if (properties.isNotEmpty() || onOpenMapOverview != null) {
                MapTrashDualPillButton(
                    isSelectionMode = isSelectionMode,
                    onMapClick = onOpenMapOverview,
                    onTrashClick = {
                        isSelectionMode = !isSelectionMode
                        selectedIds = emptySet()
                    },
                )
            }
        },
        floatingActionButton = if (!isSelectionMode) {
            {
                FloatingActionButton(
                    onClick = onAddProperty,
                    containerColor = Green,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "새 매물 등록",
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        } else null,
        bottomAction = if (isSelectionMode && properties.isNotEmpty()) {
            {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selectedIds.isNotEmpty()) Color(0xFFB42318) else Color(0xFFE0E0E0))
                            .clickable(enabled = selectedIds.isNotEmpty()) {
                                showBatchDeleteDialog = true
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = if (selectedIds.isNotEmpty()) Color.White else Secondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (selectedIds.isEmpty()) "삭제할 매물을 선택해 주세요" else "선택한 ${selectedIds.size}개 매물 삭제하기",
                                color = if (selectedIds.isNotEmpty()) Color.White else Secondary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }
        } else null,
        onTabSelected = { tab ->
            onTabSelected(
                when (tab) {
                    AppTab.Home -> "home"
                    AppTab.Property -> "property"
                    AppTab.Report -> "report"
                    AppTab.Profile -> "profile"
                },
            )
        },
    ) {
        if (isSelectionMode && properties.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${selectedIds.size}개 선택됨",
                    color = if (selectedIds.isNotEmpty()) Green else Secondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = {
                        selectedIds = if (selectedIds.size == properties.size) emptySet() else properties.map { it.id }.toSet()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (selectedIds.size == properties.size) "전체 해제" else "전체 선택",
                        color = Green,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        when {
            loading -> Text("매물 정보를 불러오고 있어요.", color = Secondary, fontSize = 13.sp)
            errorMessage != null -> InfoCard("서버 연결 확인 필요", errorMessage, onClick = onRetry)
            properties.isEmpty() -> InfoCard("등록된 매물이 없어요", "우측 하단 + 버튼으로 첫 매물을 등록해 주세요.", onClick = onAddProperty)
            isSelectionMode -> properties.forEach { property ->
                val isSelected = property.id in selectedIds
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(if (isSelected) 0.5.dp else 1.5.dp, RoundedCornerShape(20.dp))
                        .clickable {
                            selectedIds = if (isSelected) selectedIds - property.id else selectedIds + property.id
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) PaleGreen else Color.White,
                    border = if (isSelected) BorderStroke(1.5.dp, Green) else null,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Green else Color.Transparent)
                                .border(1.5.dp, if (isSelected) Green else Color(0xFFC0C0C0), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = property.name,
                                color = DeepGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = property.address,
                                color = Secondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            else -> properties.forEach { property ->
                SwipeToDeletePropertyCard(
                    property = property,
                    onClick = { onOpenProperty(property.id) },
                    onDelete = { onDeleteProperty(property.id) },
                )
            }
        }
        deleteErrorMessage?.let { Text(it, color = Color(0xFFC93B2B), fontSize = 12.sp) }

        if (showBatchDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteDialog = false },
                title = { Text("선택 매물 삭제", fontWeight = FontWeight.Bold) },
                text = { Text("선택한 ${selectedIds.size}개의 매물을 삭제하시겠습니까?\n임장 기록이 있는 매물은 상태가 정리됩니다.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val idsToDelete = selectedIds.toList()
                            if (onDeleteMultipleProperties != null) {
                                onDeleteMultipleProperties(idsToDelete)
                            } else {
                                idsToDelete.forEach(onDeleteProperty)
                            }
                            selectedIds = emptySet()
                            isSelectionMode = false
                            showBatchDeleteDialog = false
                        },
                    ) {
                        Text("삭제", color = Color(0xFFB42318), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteDialog = false }) {
                        Text("취소", color = Secondary)
                    }
                },
                shape = RoundedCornerShape(18.dp),
                containerColor = Color.White,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
private fun SwipeToDeletePropertyCard(
    property: PropertyUiModel,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val swipeState = remember(property.id) { AnchoredDraggableState(PropertySwipePosition.Closed) }
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(swipeState)
    var showDeleteDialog by rememberSaveable(property.id) { mutableStateOf(false) }
    val closeSwipe = { scope.launch { swipeState.animateTo(PropertySwipePosition.Closed) } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
    ) {
        val currentOffset = runCatching { swipeState.requireOffset() }.getOrDefault(0f)
        if (currentOffset < -1f) {
            Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.25f)
                        .background(Color(0xFFC93B2B), RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp))
                        .clickable { showDeleteDialog = true }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("삭제", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .onSizeChanged { size ->
                    swipeState.updateAnchors(
                        DraggableAnchors {
                            PropertySwipePosition.Closed at 0f
                            PropertySwipePosition.Delete at propertyDeleteRevealOffset(size.width)
                        },
                    )
                }
                .offset { IntOffset(currentOffset.roundToInt(), 0) }
                .anchoredDraggable(
                    state = swipeState,
                    orientation = Orientation.Horizontal,
                    flingBehavior = flingBehavior,
                ),
        ) {
            PropertyCard(property = property, onClick = onClick)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                closeSwipe()
            },
            title = { Text("매물을 삭제할까요?") },
            text = { Text("${property.name} 매물 정보가 삭제됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    closeSwipe()
                    onDelete()
                }) { Text("삭제", color = Color(0xFFC93B2B)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    closeSwipe()
                }) { Text("취소") }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PropertyFormScreen(
    onBack: () -> Unit,
    saving: Boolean,
    errorMessage: String?,
    onSaved: (PropertyFormSubmission) -> Unit,
    onOpenAddressPicker: () -> Unit,
    onOpenLocationPicker: () -> Unit,
    selectedAddress: String,
    initialProperty: PropertyUiModel? = null,
) {
    val initialAddress = remember(initialProperty?.address) {
        splitAddressForEditing(initialProperty?.address.orEmpty())
    }
    var propertyName by rememberSaveable { mutableStateOf(initialProperty?.name ?: "") }
    var address by rememberSaveable { mutableStateOf(initialAddress.address) }
    var addressDetail by rememberSaveable { mutableStateOf(initialAddress.detail) }
    var deposit by rememberSaveable { mutableStateOf(wonToManwonInput(initialProperty?.depositAmount)) }
    var monthlyRent by rememberSaveable { mutableStateOf(wonToManwonInput(initialProperty?.monthlyRentAmount)) }
    var maintenanceFee by rememberSaveable { mutableStateOf(wonToManwonInput(initialProperty?.maintenanceFeeAmount)) }
    var housingType by remember { mutableStateOf("원룸") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var visitDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var visitHour by rememberSaveable { mutableStateOf(14) }
    var visitMinute by rememberSaveable { mutableStateOf(0) }
    var visitTimeSelected by rememberSaveable { mutableStateOf(false) }
    val propertyNameFocus = remember { FocusRequester() }
    val addressDetailFocus = remember { FocusRequester() }
    val depositFocus = remember { FocusRequester() }
    val monthlyRentFocus = remember { FocusRequester() }
    val maintenanceFeeFocus = remember { FocusRequester() }

    LaunchedEffect(initialProperty) {
        if (initialProperty != null) {
            val editAddress = splitAddressForEditing(initialProperty.address)
            if (propertyName.isBlank()) propertyName = initialProperty.name
            if (address.isBlank()) address = editAddress.address
            if (addressDetail.isBlank()) addressDetail = editAddress.detail
            if (deposit.isBlank()) deposit = wonToManwonInput(initialProperty.depositAmount)
            if (monthlyRent.isBlank()) monthlyRent = wonToManwonInput(initialProperty.monthlyRentAmount)
            if (maintenanceFee.isBlank()) maintenanceFee = wonToManwonInput(initialProperty.maintenanceFeeAmount)
        }
    }

    LaunchedEffect(selectedAddress) {
        if (selectedAddress.isNotBlank() && selectedAddress != address) {
            address = selectedAddress
            addressDetail = ""
        }
    }
    val visitSchedule = visitDateMillis?.let { millis ->
        SimpleDateFormat("yyyy. MM. dd (EEE)", Locale.KOREAN).format(Date(millis)) + "  %02d:%02d".format(visitHour, visitMinute)
    } ?: "방문 날짜와 시간 선택"

    val isEditing = initialProperty != null
    AppPageScaffold(title = if (isEditing) "매물 수정" else "매물 등록", onBack = onBack) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isEditing) "매물 정보를 수정해요" else "점검할 방을 알려주세요",
                color = Green,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FormTextField(
                "매물 이름", "예: 연남동 햇살 원룸", propertyName, { propertyName = it },
                focusRequester = propertyNameFocus,
                nextFocusRequester = addressDetailFocus,
            )
            AddressFormField(
                value = address,
                onOpenAddressPicker = onOpenAddressPicker,
                onUseCurrentLocation = onOpenLocationPicker,
            )
            FormTextField(
                "상세 주소", "예: 101동 202호", addressDetail, { addressDetail = it },
                focusRequester = addressDetailFocus,
                nextFocusRequester = depositFocus,
            )
            FormTextField(
                "보증금(만원)", "예: 5000", deposit, { deposit = it },
                focusRequester = depositFocus,
                nextFocusRequester = monthlyRentFocus,
            )
            FormTextField(
                "월세(만원)", "예: 45", monthlyRent, { monthlyRent = it },
                focusRequester = monthlyRentFocus,
                nextFocusRequester = maintenanceFeeFocus,
            )
            FormTextField(
                "관리비(만원)", "예: 8", maintenanceFee, { maintenanceFee = it },
                focusRequester = maintenanceFeeFocus,
            )
            Text("주거 형태", color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HousingOption("원룸", housingType, { housingType = it }, Modifier.weight(1f))
                HousingOption("오피스텔", housingType, { housingType = it }, Modifier.weight(1f))
                HousingOption("투룸 이상", housingType, { housingType = it }, Modifier.weight(1f))
            }
            Text("방문 예정일", color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).clickable { showDatePicker = true }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(visitSchedule, color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("⌄", color = Secondary, fontSize = 18.sp)
            }
        }
        errorMessage?.let { Text(it, color = Color(0xFFC93B2B), fontSize = 12.sp) }
        PrimaryButton(
            if (saving) "저장 중..." else if (isEditing) "수정 완료" else "매물 등록하기",
            {
                onSaved(
                    PropertyFormSubmission(
                        name = propertyName,
                        address = addressWithDetail(address, addressDetail),
                        depositAmount = manwonInputToWon(deposit),
                        monthlyRentAmount = manwonInputToWon(monthlyRent),
                        maintenanceFeeAmount = manwonInputToWon(maintenanceFee),
                    ),
                )
            },
            enabled = propertyName.isNotBlank() && !saving,
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = visitDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    visitDateMillis = datePickerState.selectedDateMillis
                    visitTimeSelected = false
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("다음") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("취소") } },
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = visitHour,
            initialMinute = visitMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    visitHour = timePickerState.hour
                    visitMinute = timePickerState.minute
                    visitTimeSelected = true
                    showTimePicker = false
                }) { Text("완료") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("취소") } },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

@Composable
private fun AddressFormField(
    value: String,
    onOpenAddressPicker: () -> Unit,
    onUseCurrentLocation: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("주소", color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.fillMaxWidth().height(68.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxSize(),
                placeholder = { Text("도로명이나 건물명을 검색하세요", color = Secondary, fontSize = 14.sp) },
                trailingIcon = {
                    IconButton(onClick = onUseCurrentLocation) {
                        Icon(Icons.Outlined.MyLocation, contentDescription = "지도에서 현재 위치 선택", tint = Green)
                    }
                },
                singleLine = true,
                readOnly = true,
                shape = RoundedCornerShape(14.dp),
            )
            Box(modifier = Modifier.fillMaxSize().padding(end = 56.dp).clickable(onClick = onOpenAddressPicker))
        }
    }
}

@Composable
private fun FormTextField(
    label: String,
    placeholder: String,
    value: String,
    onChange: (String) -> Unit,
    onClick: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.fillMaxWidth().height(68.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier
                    .fillMaxSize()
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                    .onFocusChanged { state ->
                        if (state.isFocused) coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                    },
                placeholder = { Text(placeholder, color = Secondary, fontSize = 14.sp) },
                singleLine = true,
                readOnly = onClick != null,
                keyboardOptions = KeyboardOptions(imeAction = if (nextFocusRequester != null) ImeAction.Next else ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onNext = { nextFocusRequester?.requestFocus() },
                    onDone = { focusManager.clearFocus() },
                ),
                shape = RoundedCornerShape(14.dp),
            )
            onClick?.let { openAddressPicker ->
                Box(modifier = Modifier.fillMaxSize().clickable { openAddressPicker() })
            }
        }
    }
}

@Composable
private fun HousingOption(label: String, selected: String, onSelect: (String) -> Unit, modifier: Modifier) {
    val isSelected = label == selected
    Box(
        modifier = modifier.height(42.dp).clip(RoundedCornerShape(99.dp)).background(if (isSelected) Green else Color.White).clickable { onSelect(label) },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (isSelected) Color.White else Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecordMethodOption(icon: String, title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) PaleGreen else Color.White).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, color = if (selected) Green else Secondary, fontSize = 21.sp)
        Spacer(Modifier.width(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = if (selected) Green else DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Secondary, fontSize = 10.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(if (selected) "●" else "○", color = if (selected) Green else Secondary, fontSize = 18.sp)
    }
}
@Composable
fun PropertyDetailScreen(
    property: PropertyUiModel?,
    loading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onStartInspection: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenBasicInfo: () -> Unit = {},
    onEditProperty: (() -> Unit)? = null,
    onDeleteProperty: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onTabSelected: (String) -> Unit,
) {
    var showDeleteDialog by rememberSaveable(property?.id) { mutableStateOf(false) }

    AppPageScaffold(
        title = "매물 상세",
        onBack = onBack,
        selectedTab = AppTab.Property,
        isRefreshing = loading,
        onRefresh = onRefresh,
        topTrailingAction = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (property != null && onEditProperty != null) {
                    IconButton(onClick = onEditProperty) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "매물 수정",
                            tint = DeepGreen,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                if (property != null && onDeleteProperty != null) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "매물 삭제",
                            tint = Color(0xFFC93B2B),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        },
        onTabSelected = { tab ->
            onTabSelected(
                when (tab) {
                    AppTab.Home -> "home"
                    AppTab.Property -> "property"
                    AppTab.Report -> "report"
                    AppTab.Profile -> "profile"
                },
            )
        },
    ) {
        if (showDeleteDialog && property != null && onDeleteProperty != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("매물을 삭제할까요?") },
                text = { Text("${property.name} 매물 정보가 삭제됩니다.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        onDeleteProperty()
                    }) { Text("삭제", color = Color(0xFFC93B2B)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
                },
            )
        }

        if (loading) Text("매물 정보를 불러오고 있어요.", color = Secondary, fontSize = 13.sp)
        errorMessage?.let { Text(it, color = Color(0xFFC93B2B), fontSize = 12.sp) }

        // 1. Hero Title & Address
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = property?.name ?: "매물 정보",
                color = DeepGreen,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = property?.address ?: "주소 미입력",
                    color = Secondary,
                    fontSize = 12.sp,
                )
            }
        }

        // 2. Financial KPI Metric Stat Grid (보증금 · 월세 · 관리비 3단 타일)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FinancialMetricTile(
                modifier = Modifier.weight(1f),
                label = "보증금",
                value = property?.depositAmount?.let(::formatWon) ?: "미입력",
                isHighlight = true,
            )
            FinancialMetricTile(
                modifier = Modifier.weight(1f),
                label = "월세",
                value = property?.monthlyRentAmount?.let(::formatWon) ?: "미입력",
            )
            FinancialMetricTile(
                modifier = Modifier.weight(1f),
                label = "관리비",
                value = property?.maintenanceFeeAmount?.let(::formatWon) ?: "미입력",
            )
        }

        // 3. Report Action Card
        InfoCard(
            title = "점검 결과 리포트",
            description = "촬영한 현장 기록과 AI 분석 결과를 확인해요.",
            accent = PaleGreen,
            onClick = onOpenReport,
        )

        // 4. Kakao Map with Pinpoint at Address (해당 주소 핀포인트 카카오 지도)
        PropertyKakaoMapCard(
            propertyName = property?.name ?: "매물 위치",
            address = property?.address ?: "주소 미입력",
        )
    }
}

@Composable
private fun PropertyKakaoMapCard(
    propertyName: String,
    address: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var coordinates by remember(address) { mutableStateOf<Pair<Double, Double>?>(null) }
    var loading by remember(address) { mutableStateOf(true) }

    LaunchedEffect(address) {
        loading = true
        coordinates = KakaoAddressSearch.resolveAddressLocation(context, address)
        loading = false
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEBE8E1)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center,
        ) {
            val coords = coordinates
            if (coords != null) {
                PropertyKakaoMapView(
                    latitude = coords.first,
                    longitude = coords.second,
                    modifier = Modifier.fillMaxSize(),
                )
                // Pinpoint Marker with Property Name Callout
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-18).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DeepGreen,
                        shadowElevation = 3.dp,
                    ) {
                        Text(
                            text = propertyName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "매물 위치 핀",
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(32.dp),
                    )
                }
            } else if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Green,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Text(
                    text = if (address.isBlank() || address == "주소 미입력") "등록된 주소가 없습니다." else "지도를 불러올 수 없습니다.",
                    color = Secondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun PropertyKakaoMapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.resume()
                Lifecycle.Event.ON_PAUSE -> mapView?.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = {
            MapView(context).also { view ->
                mapView = view
                view.start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() = Unit
                        override fun onMapError(error: Exception) = Unit
                    },
                    object : KakaoMapReadyCallback() {
                        override fun getPosition(): LatLng = LatLng.from(latitude, longitude)
                        override fun getZoomLevel(): Int = 18
                        override fun onMapReady(kakaoMap: KakaoMap) {
                            GestureType.entries.forEach { gesture ->
                                kakaoMap.setGestureEnable(gesture, false)
                            }
                        }
                    },
                )
                view.doOnAttach {
                    view.post {
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            view.resume()
                        }
                    }
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun FinancialMetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    isHighlight: Boolean = false,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) PaleGreen.copy(alpha = 0.55f) else Color.White,
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isHighlight) Green.copy(alpha = 0.3f) else Color(0xFFEBE8E1)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                color = Secondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = if (isHighlight) DeepGreen else Color(0xFF234B38),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}
@Composable
fun PropertyInfoScreen(property: PropertyUiModel?, onBack: () -> Unit) {
    AppPageScaffold(title = "매물 정보", onBack = onBack) {
        SectionTitle(property?.name ?: "매물 정보", "등록한 매물 정보를 확인해요.")
        InfoCard("주소", property?.address ?: "미입력")
        InfoCard("보증금", property?.depositAmount?.let(::formatWon) ?: "미입력")
        InfoCard("월세", property?.monthlyRentAmount?.let(::formatWon) ?: "미입력")
        InfoCard("관리비", property?.maintenanceFeeAmount?.let(::formatWon) ?: "미입력")
    }
}

@Composable
fun PropertySelectScreen(
    properties: List<PropertyUiModel>,
    selectedId: String?,
    loading: Boolean,
    onBack: () -> Unit,
    onPropertySelected: (String) -> Unit,
    onSelected: (String) -> Unit,
    onAddProperty: () -> Unit,
) {
    AppPageScaffold(
        title = "점검할 매물 선택",
        onBack = onBack,
        bottomAction = if (!loading) {
            {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    PrimaryButton(
                        label = "선택한 매물로 계속하기",
                        onClick = { selectedId?.let(onSelected) },
                        enabled = selectedId != null,
                    )
                }
            }
        } else {
            null
        },
    ) {
        if (loading) {
            PropertySelectLoadingContent()
        } else {
            Column(
                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("어느 매물을 점검할까요?", color = DeepGreen, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
        if (properties.isEmpty() && !loading) {
            InfoCard(
                title = "다른 매물이 없나요?",
                description = "새 매물을 등록하면 방문 준비와 점검을 바로 시작할 수 있어요.",
                onClick = onAddProperty,
            )
        } else {
            properties.forEach { property ->
                PropertyCard(property, selected = property.id == selectedId) { onPropertySelected(property.id) }
            }
        }
    }
}

@Composable
private fun PropertySelectLoadingContent() {
    val transition = rememberInfiniteTransition(label = "property_select_loading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "property_select_loading_rotation",
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 56.dp, bottom = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.loading),
                contentDescription = "매물 정보를 불러오는 중",
                modifier = Modifier.size(190.dp),
            )
            CircularProgressIndicator(
                color = Green,
                strokeWidth = 4.dp,
                modifier = Modifier.size(208.dp).rotate(rotation),
            )
        }
        Text("매물 정보를 불러오고 있어요", color = DeepGreen, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun PropertyCard(property: PropertyUiModel, selected: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (selected) 0.5.dp else 1.5.dp, RoundedCornerShape(18.dp))
            .background(if (selected) PaleGreen else Color.White, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Green.copy(alpha = 0.15f) else Color(0xFFF4F6F5)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.HomeWork,
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = property.name,
                color = DeepGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = property.address,
                color = Secondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun formatWon(value: Long): String {
    if (value <= 0) return "0원"
    val eok = value / 100_000_000L
    val remainderEok = value % 100_000_000L
    val man = remainderEok / 10_000L
    val remainderMan = remainderEok % 10_000L

    return when {
        eok > 0 -> {
            if (remainderMan == 0L && man % 1000L == 0L && man > 0) {
                val decimal = eok.toDouble() + man / 10000.0
                "%.1f억".format(Locale.KOREAN, decimal).replace(".0", "")
            } else if (man > 0) {
                if (remainderMan == 0L) {
                    "${eok}억 %,d만".format(Locale.KOREAN, man)
                } else {
                    "${eok}억 %,d만 %,d원".format(Locale.KOREAN, man, remainderMan)
                }
            } else {
                "${eok}억"
            }
        }
        man > 0 -> {
            if (remainderMan == 0L) {
                "%,d만".format(Locale.KOREAN, man)
            } else {
                "%,d만 %,d원".format(Locale.KOREAN, man, remainderMan)
            }
        }
        else -> "%,d원".format(Locale.KOREAN, value)
    }
}

internal fun wonToManwonInput(value: Long?): String = value?.let { amount ->
    if (amount % 10_000L == 0L) (amount / 10_000L).toString()
    else java.math.BigDecimal.valueOf(amount).movePointLeft(4).stripTrailingZeros().toPlainString()
}.orEmpty()

internal fun manwonInputToWon(value: String): String {
    if (value.isBlank()) return ""
    return runCatching {
        java.math.BigDecimal(value.trim()).movePointRight(4).toBigIntegerExact().toString()
    }.getOrElse { value }
}

@Composable
private fun MapTrashDualPillButton(
    isSelectionMode: Boolean,
    onMapClick: (() -> Unit)?,
    onTrashClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 1.5.dp,
        modifier = Modifier.height(44.dp),
    ) {
        Row(
            modifier = Modifier.height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onMapClick != null && !isSelectionMode) {
                val mapInteraction = remember { MutableInteractionSource() }
                val isMapPressed by mapInteraction.collectIsPressedAsState()

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp))
                        .background(if (isMapPressed) Color(0xFFF2F4F7) else Color.White)
                        .clickable(
                            interactionSource = mapInteraction,
                            indication = null,
                            onClick = onMapClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = "매물 지도 보기",
                        tint = DeepGreen,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(Color(0xFFE5E7EB)),
                )
            }

            val trashInteraction = remember { MutableInteractionSource() }
            val isTrashPressed by trashInteraction.collectIsPressedAsState()

            val trashShape = if (onMapClick != null && !isSelectionMode) {
                RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp)
            } else {
                CircleShape
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(trashShape)
                    .background(if (isTrashPressed) Color(0xFFF2F4F7) else Color.White)
                    .clickable(
                        interactionSource = trashInteraction,
                        indication = null,
                        onClick = onTrashClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isSelectionMode) Icons.Outlined.Close else Icons.Outlined.DeleteOutline,
                    contentDescription = if (isSelectionMode) "선택 모드 종료" else "매물 다중 삭제",
                    tint = if (isSelectionMode) Color(0xFFC93B2B) else DeepGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
