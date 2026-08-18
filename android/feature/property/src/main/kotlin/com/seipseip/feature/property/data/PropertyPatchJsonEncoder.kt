package com.seipseip.feature.property.data

import com.seipseip.feature.property.domain.model.FieldChange
import com.seipseip.feature.property.domain.model.PropertyPatch
import com.squareup.moshi.JsonWriter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import javax.inject.Inject

internal class PropertyPatchJsonEncoder @Inject constructor() {
    fun encode(patch: PropertyPatch): RequestBody {
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer ->
            writer.serializeNulls = true
            writer.beginObject()
            writer.writeChange("name", patch.name) { text -> value(text) }
            writer.writeChange("addressSummary", patch.addressSummary) { text -> value(text) }
            writer.writeChange("depositAmount", patch.depositAmount) { amount -> value(amount) }
            writer.writeChange("monthlyRentAmount", patch.monthlyRentAmount) { amount -> value(amount) }
            writer.writeChange("maintenanceFeeAmount", patch.maintenanceFeeAmount) { amount -> value(amount) }
            writer.writeChange("areaSquareMeters", patch.areaSquareMeters) { area -> value(area) }
            writer.writeChange("floor", patch.floor) { text -> value(text) }
            writer.writeChange("options", patch.options) { options ->
                beginArray()
                options.forEach(::value)
                endArray()
            }
            writer.writeChange("brokerContact", patch.brokerContact) { text -> value(text) }
            writer.writeChange("note", patch.note) { text -> value(text) }
            writer.endObject()
        }
        return buffer.readByteString().toRequestBody(JSON_MEDIA_TYPE)
    }

    private inline fun <T> JsonWriter.writeChange(
        name: String,
        change: FieldChange<T>,
        writeValue: JsonWriter.(T) -> Unit,
    ) {
        when (change) {
            FieldChange.Unchanged -> Unit
            FieldChange.Clear -> name(name).nullValue()
            is FieldChange.Value -> {
                name(name)
                writeValue(change.value)
            }
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
