package com.tenantleaf.api.media

import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.StatObjectArgs
import io.minio.Http.Method
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Qualifier
import java.net.URI
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

data class PresignedUpload(val url: URI, val expiresAt: OffsetDateTime)
data class PresignedView(val url: URI, val expiresAt: OffsetDateTime)
data class StoredJpeg(val size: Long, val width: Int, val height: Int, val contentType: String?)

interface ObjectStorageGateway {
    fun createUploadUrl(key: String): PresignedUpload
    fun createViewUrl(key: String): PresignedView
    fun inspectJpeg(key: String, maximumBytes: Int): StoredJpeg
}

@ConfigurationProperties("app.object-storage")
data class ObjectStorageProperties(
    var endpoint: String = "http://localhost:9000",
    var publicEndpoint: String = "http://10.0.2.2:9000",
    var accessKey: String = "",
    var secretKey: String = "",
    var bucket: String = "tenant-leaf-media",
    var presignMinutes: Int = 15,
)

@Configuration
@EnableConfigurationProperties(ObjectStorageProperties::class)
class ObjectStorageConfiguration {
    @Bean
    fun minioInternalClient(properties: ObjectStorageProperties): MinioClient =
        MinioClient.builder()
            .endpoint(properties.endpoint)
            .credentials(properties.accessKey, properties.secretKey)
            .build()

    @Bean
    fun minioPresignClient(properties: ObjectStorageProperties): MinioClient =
        MinioClient.builder()
            .endpoint(properties.publicEndpoint)
            .credentials(properties.accessKey, properties.secretKey)
            .build()
}

@org.springframework.stereotype.Component
class MinioObjectStorageGateway(
    @Qualifier("minioInternalClient") private val client: MinioClient,
    @Qualifier("minioPresignClient") private val presignClient: MinioClient,
    private val properties: ObjectStorageProperties,
) : ObjectStorageGateway {
    override fun createUploadUrl(key: String): PresignedUpload = storageCall {
        ensureBucket()
        val expiresAt = OffsetDateTime.now().plusMinutes(properties.presignMinutes.toLong())
        val url = presignClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(properties.bucket)
                .`object`(key)
                .expiry(properties.presignMinutes, TimeUnit.MINUTES)
                .build(),
        )
        PresignedUpload(URI.create(url), expiresAt)
    }

    override fun createViewUrl(key: String): PresignedView = storageCall {
        val expiresAt = OffsetDateTime.now().plusMinutes(properties.presignMinutes.toLong())
        val url = presignClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(properties.bucket)
                .`object`(key)
                .expiry(properties.presignMinutes, TimeUnit.MINUTES)
                .build(),
        )
        PresignedView(URI.create(url), expiresAt)
    }

    override fun inspectJpeg(key: String, maximumBytes: Int): StoredJpeg = storageCall {
        val stat = client.statObject(
            StatObjectArgs.builder().bucket(properties.bucket).`object`(key).build(),
        )
        if (stat.size() > maximumBytes) {
            throw MediaFileTooLargeException()
        }
        val bytes = client.getObject(
            GetObjectArgs.builder().bucket(properties.bucket).`object`(key).build(),
        ).use { it.readNBytes(maximumBytes + 1) }
        if (bytes.size > maximumBytes || bytes.size < 3 || bytes[0] != 0xff.toByte() || bytes[1] != 0xd8.toByte() || bytes[2] != 0xff.toByte()) {
            throw UnsupportedMediaTypeException()
        }
        val imageInput = javax.imageio.ImageIO.createImageInputStream(bytes.inputStream())
            ?: throw UnsupportedMediaTypeException()
        imageInput.use { input ->
            val reader = javax.imageio.ImageIO.getImageReaders(input).asSequence().firstOrNull()
                ?: throw UnsupportedMediaTypeException()
            try {
                reader.input = input
                StoredJpeg(bytes.size.toLong(), reader.getWidth(0), reader.getHeight(0), stat.contentType())
            } finally {
                reader.dispose()
            }
        }
    }

    private fun ensureBucket() {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket).build())
        }
    }

    private fun <T> storageCall(block: () -> T): T = try {
        block()
    } catch (exception: MediaValidationException) {
        throw exception
    } catch (exception: MediaFileTooLargeException) {
        throw exception
    } catch (exception: UnsupportedMediaTypeException) {
        throw exception
    } catch (exception: Exception) {
        throw ObjectStorageUnavailableException(exception)
    }
}
