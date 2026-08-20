package com.seipseip.feature.property.presentation

import com.seipseip.feature.property.domain.AreaConverter
import com.seipseip.feature.property.domain.PropertyValidator
import com.seipseip.feature.property.domain.model.Property
import com.seipseip.feature.property.domain.model.PropertyDraft
import java.math.BigDecimal

enum class AreaUnit {
    SQUARE_METERS,
    PYEONG,
}

data class PropertyFormFields(
    val name: String = "",
    val addressSummary: String = "",
    val depositAmount: String = "",
    val monthlyRentAmount: String = "",
    val maintenanceFeeAmount: String = "",
    val area: String = "",
    val areaUnit: AreaUnit = AreaUnit.SQUARE_METERS,
    val floor: String = "",
    val options: String = "",
    val brokerContact: String = "",
    val note: String = "",
)

data class PropertyFormUiState(
    val fields: PropertyFormFields = PropertyFormFields(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val editing: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
)

internal data class ParsedPropertyDraft(
    val draft: PropertyDraft?,
    val errors: Map<String, String>,
)

internal fun PropertyFormFields.parse(): ParsedPropertyDraft {
    val errors = linkedMapOf<String, String>()
    val deposit = depositAmount.parseOptionalLong("depositAmount", "보증금", errors)
    val rent = monthlyRentAmount.parseOptionalLong("monthlyRentAmount", "월세", errors)
    val fee = maintenanceFeeAmount.parseOptionalLong("maintenanceFeeAmount", "관리비", errors)
    val typedArea = area.parseOptionalDouble("areaSquareMeters", "면적", errors)
    val squareMeters = typedArea?.let {
        if (areaUnit == AreaUnit.PYEONG) AreaConverter.pyeongToSquareMeters(it) else it
    }
    val draft = PropertyDraft(
        name = name,
        addressSummary = addressSummary,
        depositAmount = deposit,
        monthlyRentAmount = rent,
        maintenanceFeeAmount = fee,
        areaSquareMeters = squareMeters,
        floor = floor,
        options = options.split(',').map(String::trim).filter(String::isNotEmpty).toSet(),
        brokerContact = brokerContact,
        note = note,
    ).normalized()
    errors.putAll(PropertyValidator.validate(draft))
    return ParsedPropertyDraft(draft.takeIf { errors.isEmpty() }, errors)
}

internal fun Property.toFormFields(): PropertyFormFields = PropertyFormFields(
    name = name,
    addressSummary = addressSummary.orEmpty(),
    depositAmount = depositAmount?.toString().orEmpty(),
    monthlyRentAmount = monthlyRentAmount?.toString().orEmpty(),
    maintenanceFeeAmount = maintenanceFeeAmount?.toString().orEmpty(),
    area = areaSquareMeters?.formatEditable().orEmpty(),
    floor = floor.orEmpty(),
    options = options.joinToString(", "),
    brokerContact = brokerContact.orEmpty(),
    note = note.orEmpty(),
)

internal fun PropertyFormFields.convertAreaUnit(): PropertyFormFields {
    val numeric = area.toDoubleOrNull()
    val nextUnit = if (areaUnit == AreaUnit.SQUARE_METERS) AreaUnit.PYEONG else AreaUnit.SQUARE_METERS
    val converted = numeric?.let {
        if (nextUnit == AreaUnit.PYEONG) {
            AreaConverter.squareMetersToPyeong(it)
        } else {
            AreaConverter.pyeongToSquareMeters(it)
        }
    }
    return copy(area = converted?.let(Double::formatEditable) ?: area, areaUnit = nextUnit)
}

private fun String.parseOptionalLong(
    field: String,
    label: String,
    errors: MutableMap<String, String>,
): Long? {
    if (isBlank()) return null
    return toLongOrNull()?.takeIf { it >= 0 } ?: run {
        errors[field] = "${label}은 0 이상의 정수로 입력해 주세요."
        null
    }
}

private fun String.parseOptionalDouble(
    field: String,
    label: String,
    errors: MutableMap<String, String>,
): Double? {
    if (isBlank()) return null
    return toDoubleOrNull()?.takeIf { it > 0.0 } ?: run {
        errors[field] = "${label}은 0보다 큰 숫자로 입력해 주세요."
        null
    }
}

private fun Double.formatEditable(): String = BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()

