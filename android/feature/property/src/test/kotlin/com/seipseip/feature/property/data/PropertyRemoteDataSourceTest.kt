package com.seipseip.feature.property.data

import com.seipseip.core.common.AppResult
import com.seipseip.core.network.OffsetDateTimeJsonAdapter
import com.seipseip.core.network.UuidJsonAdapter
import com.seipseip.core.network.generated.api.PropertiesApi
import com.seipseip.feature.property.domain.model.FieldChange
import com.seipseip.feature.property.domain.model.PropertyDraft
import com.seipseip.feature.property.domain.model.PropertyPatch
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.UUID

class PropertyRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var source: PropertyRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        val moshi = Moshi.Builder()
            .add(UuidJsonAdapter())
            .add(OffsetDateTimeJsonAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        source = PropertyRemoteDataSource(
            propertiesApi = retrofit.create(PropertiesApi::class.java),
            propertyPatchApi = retrofit.create(PropertyPatchApi::class.java),
            patchEncoder = PropertyPatchJsonEncoder(),
            moshi = moshi,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `list uses the generated path and decodes UUID and offset date time`() = runTest {
        server.enqueue(jsonResponse(propertyPageJson))

        val result = source.list(page = 0, size = 20)

        assertTrue(result is AppResult.Success)
        val page = (result as AppResult.Success).value
        assertEquals(PROPERTY_ID, page.items.single().id)
        assertEquals(9, page.items.single().createdAt.offset.totalSeconds / 3600)
        assertEquals("/api/v1/properties?page=0&size=20", server.takeRequest().path)
    }

    @Test
    fun `create sends POST JSON to the contract path`() = runTest {
        server.enqueue(jsonResponse(propertyJson, 201))

        val result = source.create(PropertyDraft(name = "테스트 매물"))

        assertTrue(result is AppResult.Success)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/properties", request.path)
        assertTrue(request.body.readUtf8().contains("\"name\":\"테스트 매물\""))
    }

    @Test
    fun `detail uses the generated UUID path`() = runTest {
        server.enqueue(jsonResponse(propertyJson))

        val result = source.get(PROPERTY_ID)

        assertTrue(result is AppResult.Success)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/properties/$PROPERTY_ID", request.path)
    }

    @Test
    fun `patch sends explicit null without unrelated fields`() = runTest {
        server.enqueue(jsonResponse(propertyJson))

        val result = source.update(
            PROPERTY_ID,
            PropertyPatch(note = FieldChange.Clear),
        )

        assertTrue(result is AppResult.Success)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/properties/$PROPERTY_ID", request.path)
        assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"))
        assertEquals("{\"note\":null}", request.body.readUtf8())
    }

    @Test
    fun `delete accepts a 204 response with no body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = source.delete(PROPERTY_ID)

        assertEquals(AppResult.Success(Unit), result)
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/properties/$PROPERTY_ID", request.path)
    }

    private fun jsonResponse(body: String, status: Int = 200) = MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private companion object {
        val PROPERTY_ID: UUID = UUID.fromString("10000000-0000-0000-0000-000000000001")
        val propertyJson = """
            {
              "id": "$PROPERTY_ID",
              "name": "테스트 매물",
              "createdAt": "2026-08-18T10:00:00+09:00",
              "updatedAt": "2026-08-18T10:30:00+09:00"
            }
        """.trimIndent()
        val propertyPageJson = """
            {
              "page": 0,
              "size": 20,
              "totalElements": 1,
              "totalPages": 1,
              "items": [$propertyJson]
            }
        """.trimIndent()
    }
}

