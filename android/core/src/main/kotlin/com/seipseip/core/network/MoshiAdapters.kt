package com.seipseip.core.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.OffsetDateTime
import java.net.URI
import java.util.UUID

class UuidJsonAdapter {
    @ToJson
    fun toJson(value: UUID): String = value.toString()

    @FromJson
    fun fromJson(value: String): UUID = UUID.fromString(value)
}

class OffsetDateTimeJsonAdapter {
    @ToJson
    fun toJson(value: OffsetDateTime): String = value.toString()

    @FromJson
    fun fromJson(value: String): OffsetDateTime = OffsetDateTime.parse(value)
}

class UriJsonAdapter {
    @ToJson
    fun toJson(value: URI): String = value.toString()

    @FromJson
    fun fromJson(value: String): URI = URI.create(value)
}

