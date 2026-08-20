package com.seipseip.feature.property.domain

import com.seipseip.feature.property.domain.model.PropertyDraft

object PropertyValidator {
    fun validate(draft: PropertyDraft): Map<String, String> = buildMap {
        if (draft.name.trim().isEmpty()) put("name", "매물 이름을 입력해 주세요.")
        validateNonNegative("depositAmount", draft.depositAmount, "보증금")
        validateNonNegative("monthlyRentAmount", draft.monthlyRentAmount, "월세")
        validateNonNegative("maintenanceFeeAmount", draft.maintenanceFeeAmount, "관리비")
        if (draft.areaSquareMeters != null &&
            (!draft.areaSquareMeters.isFinite() || draft.areaSquareMeters < MIN_AREA_SQUARE_METERS)
        ) {
            put("areaSquareMeters", "면적은 0.01㎡ 이상이어야 합니다.")
        }
    }

    private fun MutableMap<String, String>.validateNonNegative(
        field: String,
        value: Long?,
        label: String,
    ) {
        if (value != null && value < 0) put(field, "$label 금액은 0원 이상이어야 합니다.")
    }

    private const val MIN_AREA_SQUARE_METERS = 0.01
}

