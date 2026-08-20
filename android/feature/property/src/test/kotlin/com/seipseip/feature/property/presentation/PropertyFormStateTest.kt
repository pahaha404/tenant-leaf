package com.seipseip.feature.property.presentation

import com.seipseip.feature.property.domain.model.Property
import com.seipseip.feature.property.domain.model.PropertyPatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.util.UUID

class PropertyFormStateTest {
    @Test
    fun `opening and saving an edit form keeps the server area precision`() {
        val original = property(areaSquareMeters = 19.83471)
        val draft = requireNotNull(original.toFormFields().parse().draft)

        assertEquals(19.83471, draft.areaSquareMeters ?: error("area missing"), 0.0)
        assertTrue(PropertyPatch.between(original, draft).isEmpty)
    }

    @Test
    fun `switching area units and back does not create a meaningless patch`() {
        val original = property(areaSquareMeters = 19.83471)
        val fields = original.toFormFields().convertAreaUnit().convertAreaUnit()
        val draft = requireNotNull(fields.parse().draft)

        assertTrue(PropertyPatch.between(original, draft).isEmpty)
    }

    private fun property(areaSquareMeters: Double) = Property(
        id = UUID.fromString("10000000-0000-0000-0000-000000000001"),
        name = "정밀도 테스트 매물",
        addressSummary = null,
        depositAmount = null,
        monthlyRentAmount = null,
        maintenanceFeeAmount = null,
        areaSquareMeters = areaSquareMeters,
        floor = null,
        options = emptySet(),
        brokerContact = null,
        note = null,
        createdAt = OffsetDateTime.parse("2026-08-18T10:00:00+09:00"),
        updatedAt = OffsetDateTime.parse("2026-08-18T10:00:00+09:00"),
    )
}

