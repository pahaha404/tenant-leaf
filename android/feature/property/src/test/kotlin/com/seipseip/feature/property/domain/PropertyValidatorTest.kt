package com.seipseip.feature.property.domain

import com.seipseip.feature.property.domain.model.PropertyDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PropertyValidatorTest {
    @Test
    fun `blank name negative amounts and non-positive area are rejected`() {
        val errors = PropertyValidator.validate(
            PropertyDraft(
                name = "   ",
                depositAmount = -1,
                monthlyRentAmount = -2,
                maintenanceFeeAmount = -3,
                areaSquareMeters = 0.0,
            ),
        )

        assertEquals(
            setOf(
                "name",
                "depositAmount",
                "monthlyRentAmount",
                "maintenanceFeeAmount",
                "areaSquareMeters",
            ),
            errors.keys,
        )
    }

    @Test
    fun `zero amounts positive area and trimmed non-blank name are accepted`() {
        val errors = PropertyValidator.validate(
            PropertyDraft(
                name = "  테스트 매물  ",
                depositAmount = 0,
                monthlyRentAmount = 0,
                maintenanceFeeAmount = 0,
                areaSquareMeters = 0.01,
            ),
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `area below contract minimum and non-finite area are rejected`() {
        val tooSmall = PropertyValidator.validate(
            PropertyDraft(name = "테스트", areaSquareMeters = 0.009),
        )
        val infinite = PropertyValidator.validate(
            PropertyDraft(name = "테스트", areaSquareMeters = Double.POSITIVE_INFINITY),
        )

        assertTrue("areaSquareMeters" in tooSmall)
        assertTrue("areaSquareMeters" in infinite)
    }
}

