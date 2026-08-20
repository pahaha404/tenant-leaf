package com.seipseip.feature.property.domain.model

sealed interface FieldChange<out T> {
    data object Unchanged : FieldChange<Nothing>

    data class Value<T>(val value: T) : FieldChange<T>

    data object Clear : FieldChange<Nothing>
}

data class PropertyPatch(
    val name: FieldChange<String> = FieldChange.Unchanged,
    val addressSummary: FieldChange<String> = FieldChange.Unchanged,
    val depositAmount: FieldChange<Long> = FieldChange.Unchanged,
    val monthlyRentAmount: FieldChange<Long> = FieldChange.Unchanged,
    val maintenanceFeeAmount: FieldChange<Long> = FieldChange.Unchanged,
    val areaSquareMeters: FieldChange<Double> = FieldChange.Unchanged,
    val floor: FieldChange<String> = FieldChange.Unchanged,
    val options: FieldChange<Set<String>> = FieldChange.Unchanged,
    val brokerContact: FieldChange<String> = FieldChange.Unchanged,
    val note: FieldChange<String> = FieldChange.Unchanged,
) {
    val isEmpty: Boolean
        get() = listOf(
            name,
            addressSummary,
            depositAmount,
            monthlyRentAmount,
            maintenanceFeeAmount,
            areaSquareMeters,
            floor,
            options,
            brokerContact,
            note,
        ).all { it is FieldChange.Unchanged }

    companion object {
        fun between(original: Property, edited: PropertyDraft): PropertyPatch {
            val normalized = edited.normalized()
            return PropertyPatch(
                name = changedRequired(original.name, normalized.name),
                addressSummary = changedNullable(original.addressSummary, normalized.addressSummary),
                depositAmount = changedNullable(original.depositAmount, normalized.depositAmount),
                monthlyRentAmount = changedNullable(original.monthlyRentAmount, normalized.monthlyRentAmount),
                maintenanceFeeAmount = changedNullable(original.maintenanceFeeAmount, normalized.maintenanceFeeAmount),
                areaSquareMeters = changedNullableArea(original.areaSquareMeters, normalized.areaSquareMeters),
                floor = changedNullable(original.floor, normalized.floor),
                options = changedRequired(original.options, normalized.options),
                brokerContact = changedNullable(original.brokerContact, normalized.brokerContact),
                note = changedNullable(original.note, normalized.note),
            )
        }
    }
}

private fun <T> changedRequired(original: T, edited: T): FieldChange<T> =
    if (original == edited) FieldChange.Unchanged else FieldChange.Value(edited)

private fun <T> changedNullable(original: T?, edited: T?): FieldChange<T> = when {
    original == edited -> FieldChange.Unchanged
    edited == null -> FieldChange.Clear
    else -> FieldChange.Value(edited)
}

private fun changedNullableArea(original: Double?, edited: Double?): FieldChange<Double> = when {
    original == null && edited == null -> FieldChange.Unchanged
    original != null && edited != null && kotlin.math.abs(original - edited) < AREA_COMPARISON_EPSILON -> {
        FieldChange.Unchanged
    }
    edited == null -> FieldChange.Clear
    else -> FieldChange.Value(requireNotNull(edited))
}

private const val AREA_COMPARISON_EPSILON = 1e-9

