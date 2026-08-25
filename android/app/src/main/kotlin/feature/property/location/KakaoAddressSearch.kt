package com.seipseip.app.feature.property.location

import android.content.Context
import android.location.Geocoder
import com.seipseip.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

data class AddressCandidate(val address: String, val detail: String)

object KakaoAddressSearch {
    suspend fun search(query: String): List<AddressCandidate> = withContext(Dispatchers.IO) {
        require(BuildConfig.KAKAO_REST_API_KEY.isNotBlank()) { "카카오 주소 검색 키가 설정되지 않았습니다." }
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        searchEndpoint("address", encodedQuery, false).ifEmpty {
            searchEndpoint("keyword", encodedQuery, true)
        }
    }

    suspend fun resolveAddressLocation(context: Context, address: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (address.isBlank() || address == "주소 미입력") return@withContext null
        resolveCoordinates(address) ?: runCatching {
            if (Geocoder.isPresent()) {
                @Suppress("DEPRECATION")
                val list = Geocoder(context, Locale.KOREAN).getFromLocationName(address, 1)
                val first = list?.firstOrNull()
                if (first != null) first.latitude to first.longitude else null
            } else null
        }.getOrNull()
    }

    private fun resolveCoordinates(address: String): Pair<Double, Double>? {
        if (BuildConfig.KAKAO_REST_API_KEY.isBlank()) return null
        val encodedQuery = URLEncoder.encode(address, StandardCharsets.UTF_8.name())
        fun queryEndpoint(endpoint: String): Pair<Double, Double>? {
            val connection = (URL("https://dapi.kakao.com/v2/local/search/$endpoint.json?query=$encodedQuery&size=1").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3_000
                readTimeout = 3_000
                setRequestProperty("Authorization", "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}")
            }
            return try {
                if (connection.responseCode in 200..299) {
                    val docs = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getJSONArray("documents")
                    if (docs.length() > 0) {
                        val doc = docs.getJSONObject(0)
                        val lat = doc.optString("y").toDoubleOrNull()
                        val lng = doc.optString("x").toDoubleOrNull()
                        if (lat != null && lng != null) lat to lng else null
                    } else null
                } else null
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }
        return queryEndpoint("address") ?: queryEndpoint("keyword")
    }

    private fun searchEndpoint(endpoint: String, encodedQuery: String, keyword: Boolean): List<AddressCandidate> {
        val connection = (URL("https://dapi.kakao.com/v2/local/search/$endpoint.json?query=$encodedQuery&size=5").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 3_000
            readTimeout = 3_000
            setRequestProperty("Authorization", "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}")
        }
        return try {
            check(connection.responseCode in 200..299) { "주소 검색을 완료하지 못했습니다." }
            val documents = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getJSONArray("documents")
            buildList {
                for (index in 0 until documents.length()) {
                    val document = documents.getJSONObject(index)
                    val roadAddress = if (keyword) document.optString("road_address_name") else document.optJSONObject("road_address")?.optString("address_name").orEmpty()
                    val address = roadAddress.ifBlank { document.optString("address_name") }
                    val detail = if (keyword) document.optString("place_name") else document.optString("address_name")
                    normalizeAddress(address)?.let { add(AddressCandidate(it, detail)) }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
