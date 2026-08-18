package com.seipseip.feature.property.domain.model

data class PropertyDraft(
    val name: String,
    val addressSummary: String? = null,
    val depositAmount: Long? = null,
    val monthlyRentAmount: Long? = null,
    val maintenanceFeeAmount: Long? = null,
    val areaSquareMeters: Double? = null,
    val floor: String? = null,
    val options: Set<String> = emptySet(),
    val brokerContact: String? = null,
    val note: String? = null,
) {
    fun normalized(): PropertyDraft = copy(
        name = name.trim(),
        addressSummary = addressSummary.normalizedOptional(),
        floor = floor.normalizedOptional(),
        options = options.map(String::trim).filter(String::isNotEmpty).toSet(),
        brokerContact = brokerContact.normalizedOptional(),
        note = note.normalizedOptional(),
    )
}

private fun String?.normalizedOptional(): String? = this?.trim()?.takeIf(String::isNotEmpty)
