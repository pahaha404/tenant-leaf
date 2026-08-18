package com.seipseip.feature.property.data

import com.seipseip.core.network.generated.model.CreatePropertyRequest
import com.seipseip.feature.property.domain.model.Property
import com.seipseip.feature.property.domain.model.PropertyDraft
import com.seipseip.feature.property.domain.model.PropertyPage
import com.seipseip.core.network.generated.model.Property as ApiProperty
import com.seipseip.core.network.generated.model.PropertyPage as ApiPropertyPage

internal fun PropertyDraft.toApiRequest(): CreatePropertyRequest {
    val normalized = normalized()
    return CreatePropertyRequest(
        name = normalized.name,
        addressSummary = normalized.addressSummary,
        depositAmount = normalized.depositAmount,
        monthlyRentAmount = normalized.monthlyRentAmount,
        maintenanceFeeAmount = normalized.maintenanceFeeAmount,
        areaSquareMeters = normalized.areaSquareMeters,
        floor = normalized.floor,
        options = normalized.options.takeIf(Set<String>::isNotEmpty),
        brokerContact = normalized.brokerContact,
        note = normalized.note,
    )
}

internal fun ApiProperty.toDomain(): Property = Property(
    id = id,
    name = name,
    addressSummary = addressSummary,
    depositAmount = depositAmount,
    monthlyRentAmount = monthlyRentAmount,
    maintenanceFeeAmount = maintenanceFeeAmount,
    areaSquareMeters = areaSquareMeters,
    floor = floor,
    options = options.orEmpty(),
    brokerContact = brokerContact,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun ApiPropertyPage.toDomain(): PropertyPage = PropertyPage(
    page = page,
    size = propertySize,
    totalElements = totalElements,
    totalPages = totalPages,
    items = items.map(ApiProperty::toDomain),
)
