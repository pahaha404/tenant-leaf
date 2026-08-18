package com.tenantleaf.api.property

import com.tenantleaf.api.generated.api.PropertiesApi
import com.tenantleaf.api.generated.model.CreatePropertyRequest
import com.tenantleaf.api.generated.model.Property
import com.tenantleaf.api.generated.model.PropertyPage
import com.tenantleaf.api.generated.model.UpdatePropertyRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class PropertyController(
    private val service: PropertyService,
    private val patchFields: PropertyPatchFields,
) : PropertiesApi {
    override fun createProperty(createPropertyRequest: CreatePropertyRequest): ResponseEntity<Property> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(createPropertyRequest))

    override fun listProperties(page: Int, size: Int): ResponseEntity<PropertyPage> =
        ResponseEntity.ok(service.list(page, size))

    override fun getProperty(propertyId: UUID): ResponseEntity<Property> =
        ResponseEntity.ok(service.get(propertyId))

    override fun updateProperty(
        propertyId: UUID,
        updatePropertyRequest: UpdatePropertyRequest,
    ): ResponseEntity<Property> =
        ResponseEntity.ok(service.update(propertyId, updatePropertyRequest, patchFields.current()))

    override fun deleteProperty(propertyId: UUID): ResponseEntity<Unit> {
        service.delete(propertyId)
        return ResponseEntity.noContent().build()
    }
}
