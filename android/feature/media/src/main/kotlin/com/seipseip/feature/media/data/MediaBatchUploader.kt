package com.seipseip.feature.media.data

import com.seipseip.core.common.AppError
import com.seipseip.core.common.AppResult
import com.seipseip.core.network.executeApiCall
import com.seipseip.core.network.generated.api.MediaApi
import com.seipseip.core.network.generated.model.CaptureSource
import com.seipseip.core.network.generated.model.CreateMediaUploadBatchRequest
import com.seipseip.core.network.generated.model.CreateMediaUploadRequest
import com.seipseip.core.network.generated.model.MediaUploadInstruction
import com.seipseip.core.network.generated.model.Zone
import com.seipseip.feature.media.domain.ExtractedJpeg
import com.seipseip.feature.media.domain.MediaPlanning
import com.seipseip.feature.media.domain.MediaUploadProgress
import com.squareup.moshi.Moshi
import java.util.UUID
import javax.inject.Inject

class MediaBatchUploader @Inject constructor(
    private val mediaApi: MediaApi,
    private val directJpegUploader: DirectJpegUploader,
    private val moshi: Moshi,
) {
    suspend fun upload(
        inspectionId: UUID,
        photos: List<ExtractedJpeg>,
        onProgress: (MediaUploadProgress) -> Unit,
    ): AppResult<Unit> {
        var completed = 0
        for (batch in MediaPlanning.batches(photos)) {
            val batchKey = MediaPlanning.idempotencyKey(
                operation = "register-media",
                resourceId = inspectionId,
                discriminator = batch.joinToString(",") { it.clientMediaId.toString() },
            )
            val registration = executeApiCall(moshi) {
                mediaApi.createMediaUploadRequests(
                    inspectionId,
                    batchKey,
                    CreateMediaUploadBatchRequest(batch.map { it.toRequest() }),
                )
            }
            val instructions = when (registration) {
                is AppResult.Success -> registration.value.items.associateBy { it.clientMediaId }
                is AppResult.Failure -> return registration
            }

            for (photo in batch) {
                val initial = instructions[photo.clientMediaId]
                    ?: return AppResult.Failure(AppError.InvalidResponse)
                when (val result = uploadOne(photo, initial)) {
                    is AppResult.Success -> {
                        completed += 1
                        onProgress(MediaUploadProgress(completed, photos.size))
                        photo.file.delete()
                    }
                    is AppResult.Failure -> return result
                }
            }
        }
        return AppResult.Success(Unit)
    }

    private suspend fun uploadOne(
        photo: ExtractedJpeg,
        initialInstruction: MediaUploadInstruction,
    ): AppResult<Unit> {
        var instruction = initialInstruction
        repeat(MediaPlanning.MAX_UPLOAD_ATTEMPTS) { attempt ->
            when (val put = directJpegUploader.put(instruction.uploadUrl.toString(), photo.file)) {
                is AppResult.Success -> {
                    val completeKey = MediaPlanning.idempotencyKey(
                        "complete-media",
                        instruction.mediaId,
                        photo.clientMediaId.toString(),
                    )
                    return when (val complete = executeApiCall(moshi) {
                        mediaApi.completeMediaUpload(instruction.mediaId, completeKey)
                    }) {
                        is AppResult.Success -> AppResult.Success(Unit)
                        is AppResult.Failure -> complete
                    }
                }
                is AppResult.Failure -> {
                    if (attempt == MediaPlanning.MAX_UPLOAD_ATTEMPTS - 1) return put
                    val retryKey = MediaPlanning.idempotencyKey(
                        "retry-media",
                        instruction.mediaId,
                        photo.clientMediaId.toString(),
                    )
                    instruction = when (val retry = executeApiCall(moshi) {
                        mediaApi.retryMediaUpload(instruction.mediaId, retryKey)
                    }) {
                        is AppResult.Success -> retry.value
                        is AppResult.Failure -> return retry
                    }
                }
            }
        }
        return AppResult.Failure(AppError.Unexpected)
    }

    private fun ExtractedJpeg.toRequest() = CreateMediaUploadRequest(
        clientMediaId = clientMediaId,
        zone = Zone.UNKNOWN,
        contentType = CreateMediaUploadRequest.ContentType.imageSlashJpeg,
        fileSize = file.length(),
        width = width,
        height = height,
        sourceVideoId = sourceVideoId,
        sourceVideoOffsetMs = sourceVideoOffsetMs,
        frameOrigin = CreateMediaUploadRequest.FrameOrigin.POST_RECORDING_EXTRACTION,
        captureSource = CaptureSource.META_GLASS,
        capturedAt = capturedAt,
    )
}

