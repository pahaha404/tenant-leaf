package com.seipseip.core.network

import javax.inject.Inject
import javax.inject.Singleton

interface AuthTokenProvider {
    fun token(): String?
}

@Singleton
class NoAuthTokenProvider @Inject constructor() : AuthTokenProvider {
    override fun token(): String? = null
}

