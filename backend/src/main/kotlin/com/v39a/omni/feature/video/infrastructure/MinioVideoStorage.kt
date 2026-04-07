package com.v39a.omni.feature.video.infrastructure

import com.v39a.omni.feature.video.domain.VideoStorage
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class MinioVideoStorage(
    private val minioClient: MinioClient,
    private val bucket: String
) : VideoStorage {

    init {
        val found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
        }
    }

    override suspend fun upload(fileName: String, stream: InputStream, contentType: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // todo проверить работу с objectSize -1 и partSize 10 МБ
                // (размер заранее неизвестен)
                val partSize = 10 * 1024 * 1024L

                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucket)
                        .`object`(fileName)
                        .stream(stream, -1, partSize)
                        .contentType(contentType)
                        .build()
                )

                // Путь, по которому файл можно будет идентифицировать.
                // В зависимости от логики ML/C++ движков, можно возвращать полный URL
                "$bucket/$fileName"
                return@withContext "$bucket/$fileName"
            } catch (e: Exception) {
                throw RuntimeException("MinIO upload failed: ${e.message}", e)
            } finally {
                stream.close()
            }
        }
    }
}