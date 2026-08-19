package com.seipseip.feature.media.data

import com.seipseip.core.common.AppError
import com.seipseip.core.common.AppResult
import com.seipseip.core.network.executeApiCall
import com.seipseip.core.network.generated.api.InspectionsApi
import com.seipseip.core.network.generated.model.InspectionStatus
import com.seipseip.feature.media.domain.ExtractedJpeg
import com.seipseip.feature.media.domain.MediaUploadProgress
import com.seipseip.feature.media.domain.VideoCandidate
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class MediaWorkflowRepository @Inject constructor(
    private val inspectionsApi: InspectionsApi,
    private val moshi: Moshi,
    private val videoLocator: AndroidVideoLocator,
    private val jpegExtractor: AndroidJpegExtractor,
    private val mediaBatchUploader: MediaBatchUploader,
) {
    suspend fun findVideos(inspectionId: UUID): AppResult<List<VideoCandidate>> {
        val inspection = when (val result = executeApiCall(moshi) { inspectionsApi.getInspection(inspectionId) }) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        if (inspection.status != InspectionStatus.ENDED) {
            return AppResult.Failure(
                AppError.Server(409, "INSPECTION_NOT_ENDED", "촬영을 종료한 임장에서만 사진을 준비할 수 있습니다.", null),
            )
        }
        return runLocal { videoLocator.findCreatedAfter(inspection.startedAt.toInstant().toEpochMilli()) }
    }

    suspend fun describeVideo(uri: android.net.Uri): AppResult<VideoCandidate> =
        runLocal { videoLocator.describe(uri) }

    suspend fun extract(
        video: VideoCandidate,
        onProgress: (Int, Int) -> Unit,
    ): AppResult<List<ExtractedJpeg>> = runLocal { jpegExtractor.extract(video, onProgress) }

    suspend fun upload(
        inspectionId: UUID,
        photos: List<ExtractedJpeg>,
        onProgress: (MediaUploadProgress) -> Unit,
    ): AppResult<Unit> = mediaBatchUploader.upload(inspectionId, photos, onProgress)

    private suspend fun <T> runLocal(block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: SecurityException) {
        AppResult.Failure(AppError.Server(403, "MEDIA_PERMISSION_REQUIRED", "동영상을 찾으려면 사진 및 동영상 권한이 필요합니다.", null))
    } catch (_: IllegalArgumentException) {
        AppResult.Failure(AppError.Server(400, "INVALID_VIDEO", "선택한 영상을 처리하지 못했습니다.", null))
    } catch (_: IOException) {
        AppResult.Failure(AppError.Server(400, "LOCAL_MEDIA_READ_FAILED", "휴대전화 영상을 읽지 못했습니다.", null))
    } catch (_: Exception) {
        AppResult.Failure(AppError.Unexpected)
    }

}
