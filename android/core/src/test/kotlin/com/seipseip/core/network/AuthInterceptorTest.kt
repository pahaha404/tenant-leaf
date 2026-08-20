package com.seipseip.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `authorization header is omitted while demo server has no token`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(NoAuthTokenProvider()))
            .build()

        client.newCall(Request.Builder().url(server.url("properties")).build()).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `bearer token is added when a real provider supplies one`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val provider = object : AuthTokenProvider {
            override fun token(): String = "demo-token"
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(provider))
            .build()

        client.newCall(Request.Builder().url(server.url("properties")).build()).execute().close()

        assertEquals("Bearer demo-token", server.takeRequest().getHeader("Authorization"))
    }
}

