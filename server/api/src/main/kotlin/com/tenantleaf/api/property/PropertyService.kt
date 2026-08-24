package com.tenantleaf.api.property

import com.tenantleaf.api.generated.model.CreatePropertyRequest
import com.tenantleaf.api.generated.model.Property
import com.tenantleaf.api.generated.model.PropertyPage
import com.tenantleaf.api.generated.model.UpdatePropertyRequest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class PropertyService(
    private val repository: PropertyRepository,
    private val userContext: DemoUserContext,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Transactional
    fun create(request: CreatePropertyRequest): Property {
        val now = OffsetDateTime.now(clock)
        val entity = PropertyEntity(
            id = UUID.randomUUID(),
            ownerId = userContext.requireUserId(),
            name = requiredText(request.name, "name"),
            addressSummary = optionalText(request.addressSummary),
            depositAmount = request.depositAmount,
            monthlyRentAmount = request.monthlyRentAmount,
            maintenanceFeeAmount = request.maintenanceFeeAmount,
            areaSquareMeters = request.areaSquareMeters?.let(BigDecimal::valueOf),
            floor = nullableRequiredText(request.floor, "floor"),
            options = normalizedOptions(request.options),
            brokerContact = nullableRequiredText(request.brokerContact, "brokerContact"),
            note = request.note,
            createdAt = now,
            updatedAt = now,
        )
        return repository.save(entity).toModel()
    }

    @Transactional(readOnly = true)
    fun list(page: Int, size: Int): PropertyPage {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        val result = repository.findAllByOwnerIdAndDeletedAtIsNull(userContext.requireUserId(), pageable)
        return PropertyPage(
            page = result.number,
            propertySize = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            items = result.content.map { it.toModel() },
        )
    }

    @Transactional(readOnly = true)
    fun get(propertyId: UUID): Property = ownedProperty(propertyId).toModel()

    @Transactional
    fun update(
        propertyId: UUID,
        request: UpdatePropertyRequest,
        fields: Set<String>,
    ): Property {
        if (fields.isEmpty()) {
            throw PropertyValidationException("body", "수정할 필드를 하나 이상 입력해 주세요.")
        }

        val entity = ownedProperty(propertyId)
        if ("name" in fields) {
            entity.name = requiredText(request.name, "name")
        }
        if ("addressSummary" in fields) entity.addressSummary = optionalText(request.addressSummary)
        if ("depositAmount" in fields) entity.depositAmount = request.depositAmount
        if ("monthlyRentAmount" in fields) entity.monthlyRentAmount = request.monthlyRentAmount
        if ("maintenanceFeeAmount" in fields) entity.maintenanceFeeAmount = request.maintenanceFeeAmount
        if ("areaSquareMeters" in fields) entity.areaSquareMeters = request.areaSquareMeters?.let(BigDecimal::valueOf)
        if ("floor" in fields) entity.floor = nullableRequiredText(request.floor, "floor")
        if ("options" in fields) entity.options = normalizedOptions(request.options)
        if ("brokerContact" in fields) {
            entity.brokerContact = nullableRequiredText(request.brokerContact, "brokerContact")
        }
        if ("note" in fields) entity.note = request.note
        entity.updatedAt = OffsetDateTime.now(clock)

        return repository.save(entity).toModel()
    }

    @Transactional
    fun delete(propertyId: UUID) {
        val property = ownedProperty(propertyId)
        val now = OffsetDateTime.now(clock)
        property.deletedAt = now
        property.updatedAt = now
        repository.save(property)
    }

    private fun ownedProperty(propertyId: UUID): PropertyEntity =
        repository.findByIdAndOwnerIdAndDeletedAtIsNull(propertyId, userContext.requireUserId())
            ?: throw PropertyNotFoundException()

    private fun requiredText(value: String?, field: String): String {
        val normalized = value?.trim()
        if (normalized.isNullOrEmpty()) {
            throw PropertyValidationException(field, "빈 값일 수 없습니다.")
        }
        return normalized
    }

    private fun optionalText(value: String?): String? = value?.trim()?.takeIf(String::isNotEmpty)

    private fun nullableRequiredText(value: String?, field: String): String? =
        value?.let { requiredText(it, field) }

    private fun normalizedOptions(values: Set<String>?): MutableSet<String> {
        if (values == null) return linkedSetOf()
        val normalized = values.map(String::trim)
        if (normalized.any(String::isEmpty)) {
            throw PropertyValidationException("options", "빈 옵션을 포함할 수 없습니다.")
        }
        return normalized.toCollection(linkedSetOf())
    }

    private fun PropertyEntity.toModel() = Property(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        addressSummary = addressSummary,
        depositAmount = depositAmount,
        monthlyRentAmount = monthlyRentAmount,
        maintenanceFeeAmount = maintenanceFeeAmount,
        areaSquareMeters = areaSquareMeters?.toDouble(),
        floor = floor,
        options = options.toSet(),
        brokerContact = brokerContact,
        note = note,
    )
}
