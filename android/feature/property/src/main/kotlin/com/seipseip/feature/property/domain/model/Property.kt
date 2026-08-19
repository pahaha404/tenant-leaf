package com.seipseip.feature.property.domain.model

import java.time.OffsetDateTime
import java.util.UUID

data class Property(
    val id: UUID,
    val name: String,
    val addressSummary: String?,
    val depositAmount: Long?,
    val monthlyRentAmount: Long?,
    val maintenanceFeeAmount: Long?,
    val areaSquareMeters: Double?,
    val floor: String?,
    val options: Set<String>,
    val brokerContact: String?,
    val note: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class PropertyPage(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<Property>,
)
