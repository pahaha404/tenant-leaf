package com.seipseip.feature.property.data

import com.seipseip.feature.property.domain.model.FieldChange
import com.seipseip.feature.property.domain.model.PropertyPatch
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test

class PropertyPatchJsonEncoderTest {
    private val encoder = PropertyPatchJsonEncoder()

    @Test
    fun `unchanged fields are omitted from the JSON object`() {
        val json = encoder.encode(PropertyPatch()).readUtf8()

        assertEquals("{}", json)
    }

    @Test
    fun `cleared field is encoded as explicit null while unchanged fields stay omitted`() {
        val json = encoder.encode(
            PropertyPatch(addressSummary = FieldChange.Clear),
        ).readUtf8()

        assertEquals("{\"addressSummary\":null}", json)
    }

    @Test
    fun `changed value is encoded without adding unchanged fields`() {
        val json = encoder.encode(
            PropertyPatch(note = FieldChange.Value("확인 메모")),
        ).readUtf8()

        assertEquals("{\"note\":\"확인 메모\"}", json)
    }
}

private fun RequestBody.readUtf8(): String {
    val buffer = Buffer()
    writeTo(buffer)
    return buffer.readUtf8()
}

