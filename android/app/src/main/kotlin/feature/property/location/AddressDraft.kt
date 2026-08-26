package com.seipseip.app.feature.property.location

fun normalizeAddress(input: String): String? =
    input.trim().takeIf { address ->
        address.length in 1..500 && address.none { it.code in 0..31 || it.code == 127 }
    }

fun normalizeAddressQuery(input: String): String? = input.trim().takeIf { it.length >= 2 }

fun addressAfterLocationLookup(current: String, resolved: String?): String =
    resolved?.let(::normalizeAddress) ?: current

fun addressWithDetail(address: String, detail: String): String =
    listOfNotNull(normalizeAddress(address), normalizeAddress(detail)).joinToString(" ")

data class EditableAddress(val address: String, val detail: String)

private val trailingBuildingUnit = Regex("(?:^|\\s)(\\d+동(?:\\s+\\d+호)?|\\d+호)$")

/** 서버에 한 줄로 저장된 주소에서 숫자 동·호만 수정 화면의 상세 주소로 복원한다. */
fun splitAddressForEditing(fullAddress: String): EditableAddress {
    val normalized = fullAddress.trim()
    val match = trailingBuildingUnit.find(normalized) ?: return EditableAddress(normalized, "")
    return EditableAddress(
        address = normalized.removeRange(match.range).trim(),
        detail = match.groupValues[1],
    )
}
