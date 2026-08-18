package com.tenantleaf.api.property

class PropertyNotFoundException : RuntimeException()

class PropertyValidationException(
    val field: String,
    val reason: String,
) : RuntimeException()
