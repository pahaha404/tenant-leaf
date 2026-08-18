package com.tenantleaf.api.property

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "properties")
class PropertyEntity(
    @Id
    val id: UUID,

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(name = "address_summary", length = 500)
    var addressSummary: String? = null,

    @Column(name = "deposit_amount")
    var depositAmount: Long? = null,

    @Column(name = "monthly_rent_amount")
    var monthlyRentAmount: Long? = null,

    @Column(name = "maintenance_fee_amount")
    var maintenanceFeeAmount: Long? = null,

    @Column(name = "area_square_meters", precision = 12, scale = 6)
    var areaSquareMeters: BigDecimal? = null,

    @Column(length = 100)
    var floor: String? = null,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "property_options", joinColumns = [JoinColumn(name = "property_id")])
    @Column(name = "option_name", nullable = false, length = 200)
    var options: MutableSet<String> = linkedSetOf(),

    @Column(name = "broker_contact", length = 300)
    var brokerContact: String? = null,

    @Column(columnDefinition = "TEXT")
    var note: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime,
)
