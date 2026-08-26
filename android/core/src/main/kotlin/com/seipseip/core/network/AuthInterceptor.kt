package com.seipseip.core.network

import com.seipseip.core.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.token()
        val request = chain.request()
            .newBuilder()
            // Temporary presentation-only partition. This is intentionally not authentication.
            .header("X-Demo-User", BuildConfig.DEMO_USER)
            .apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()
        return chain.proceed(request)
    }
}
