package com.seipseip.feature.media.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.seipseip.feature.media.domain.VideoCandidate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AndroidVideoLocator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun findCreatedAfter(startedAtMillis: Long): List<VideoCandidate> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DURATION,
        )
        val selection = "${MediaStore.Video.Media.DATE_ADDED} >= ?"
        val arguments = arrayOf((startedAtMillis / 1_000L).toString())
        val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arguments,
            sort,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            buildList {
                while (cursor.moveToNext()) {
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idColumn),
                    )
                    val taken = cursor.getLong(takenColumn)
                    val createdAt = taken.takeIf { it > 0 } ?: cursor.getLong(addedColumn) * 1_000L
                    if (createdAt >= startedAtMillis) {
                        add(
                            VideoCandidate(
                                uri = uri,
                                displayName = cursor.getString(nameColumn) ?: "최근 임장 영상",
                                createdAtMillis = createdAt,
                                durationMillis = cursor.getLong(durationColumn),
                            ),
                        )
                    }
                }
            }
        }.orEmpty()
    }

    suspend fun describe(uri: Uri): VideoCandidate = withContext(Dispatchers.IO) {
        var displayName = "선택한 임장 영상"
        var createdAt = System.currentTimeMillis()
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.Video.Media.DATE_TAKEN, MediaStore.MediaColumns.DATE_ADDED),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let {
                    displayName = cursor.getString(it) ?: displayName
                }
                val taken = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
                    .takeIf { it >= 0 }?.let(cursor::getLong).orZero()
                val added = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                    .takeIf { it >= 0 }?.let(cursor::getLong).orZero() * 1_000L
                createdAt = taken.takeIf { it > 0 } ?: added.takeIf { it > 0 } ?: createdAt
            }
        }
        val retriever = MediaMetadataRetriever()
        val duration = try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
        require(duration > 0) { "재생 시간을 확인할 수 없는 영상입니다." }
        VideoCandidate(uri, displayName, createdAt, duration)
    }

    private fun Long?.orZero() = this ?: 0L
}
