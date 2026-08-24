package com.ducks.common.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI

/**
 * Тонкая обёртка над S3-совместимым хранилищем. Синхронный клиент AWS SDK, поэтому
 * каждый вызов уходит на [Dispatchers.IO] — блокировать поток Netty нельзя.
 */
class S3Storage(private val config: S3Config) {

    private val client: S3Client = S3Client.builder()
        .endpointOverride(URI.create(config.endpoint))
        // У не-amazon провайдеров регион почти всегда фиктивный, но без него SDK не соберётся.
        .region(Region.of(config.region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(config.accessKey, config.secretKey)
            )
        )
        // Path-style (endpoint/bucket/key) вместо virtual-host (bucket.endpoint/key):
        // wildcard-поддомены под бакеты у сторонних провайдеров чаще всего не настроены.
        .forcePathStyle(true)
        .build()

    suspend fun put(key: String, bytes: ByteArray, contentType: String): String =
        withContext(Dispatchers.IO) {
            val request = PutObjectRequest.builder()
                .bucket(config.bucket)
                .key(key)
                // Без явного типа хранилище отдаст application/octet-stream,
                // и картинка начнёт скачиваться файлом вместо показа.
                .contentType(contentType)
                // Имя объекта — UUID, содержимое по нему уже не изменится никогда, поэтому
                // клиент может держать картинку в кэше сколько угодно. Исходящий трафик —
                // основная статья расходов на хранилище, этот заголовок режет её кратно.
                .cacheControl(CACHE_CONTROL)
                .apply { if (config.publicReadAcl) acl(ObjectCannedACL.PUBLIC_READ) }
                .build()

            client.putObject(request, RequestBody.fromBytes(bytes))

            publicUrl(key)
        }

    suspend fun delete(key: String) {
        withContext(Dispatchers.IO) {
            client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(config.bucket)
                    .key(key)
                    .build()
            )
        }
    }

    suspend fun exists(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client.headObject(
                HeadObjectRequest.builder()
                    .bucket(config.bucket)
                    .key(key)
                    .build()
            )
            true
        } catch (_: NoSuchKeyException) {
            false
        }
    }

    suspend fun listKeys(prefix: String): List<String> = withContext(Dispatchers.IO) {
        val request = ListObjectsV2Request.builder()
            .bucket(config.bucket)
            .prefix(prefix)
            .build()

        // Пагинатор, а не listObjectsV2: один ответ отдаёт максимум 1000 ключей, и без
        // обхода страниц чистильщик со временем перестал бы видеть часть файлов.
        client.listObjectsV2Paginator(request)
            .contents()
            .map { it.key() }
    }

    fun publicUrl(key: String): String = "${config.publicBaseUrl.trimEnd('/')}/$key"

    private companion object {
        const val CACHE_CONTROL = "public, max-age=31536000, immutable"
    }
}
