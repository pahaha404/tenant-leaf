package com.seipseip.app.feature.property.location

fun normalizeAddress(input: String): String? =
    input.trim().takeIf { address ->
        address.length in 1..500 && address.none { it.code in 0..31 || it.code == 127 }
    }

fun normalizeAddressQuery(input: String): String? = input.trim().takeIf { it.length >= 2 }

fun addressAfterLocationLookup(current: String, resolved: String?): String =
    resolved?.let(::normalizeAddress) ?: current
