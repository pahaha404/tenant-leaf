package com.seipseip.feature.media.data

import com.seipseip.core.common.AppError
import com.seipseip.core.common.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject

class DirectJpegUploader @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun put(uploadUrl: String, file: File): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(uploadUrl)
                .put(file.asRequestBody(JPEG_MEDIA_TYPE))
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) AppResult.Success(Unit)
                else AppResult.Failure(
                    AppError.Server(
                        response.code,
                        if (response.code == 403) "UPLOAD_URL_EXPIRED" else "OBJECT_UPLOAD_FAILED",
                        if (response.code == 403) "사진 전송 주소가 만료되어 새 주소가 필요합니다." else "사진 전송에 실패했습니다.",
                        null,
                    ),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            AppResult.Failure(AppError.Network)
        } catch (_: Exception) {
            AppResult.Failure(AppError.Unexpected)
        }
    }

    private companion object {
        val JPEG_MEDIA_TYPE = "image/jpeg".toMediaType()
    }
}
