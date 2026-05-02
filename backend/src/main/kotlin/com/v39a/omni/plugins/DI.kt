package com.v39a.omni.plugins

import com.v39a.omni.feature.video.domain.UpdateVideoMetaUseCase
import com.v39a.omni.feature.video.domain.UploadVideoUseCase
import com.v39a.omni.feature.video.domain.VideoRepository
import com.v39a.omni.feature.video.domain.VideoStorage
import com.v39a.omni.feature.video.infrastructure.MinioVideoStorage
import com.v39a.omni.feature.video.infrastructure.PostgresVideoRepository
import io.ktor.server.application.*
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    val videoModule = module {
        single {
            val endpoint = System.getProperty("S3_ENDPOINT") ?: System.getenv("S3_ENDPOINT") ?: "http://minio:9000"
            val accessKey = System.getProperty("S3_ACCESS_KEY") ?: System.getenv("S3_ACCESS_KEY") ?: "admin"
            val secretKey = System.getProperty("S3_SECRET_KEY") ?: System.getenv("S3_SECRET_KEY") ?: "password123"

            val client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build()

            try {
                val found = client.bucketExists(BucketExistsArgs.builder().bucket("videos").build())
                if (!found) {
                    client.makeBucket(MakeBucketArgs.builder().bucket("videos").build())
                    println("Bucket 'videos' created successfully!")
                } else {
                    println("Bucket 'videos' already exists.")
                }
            } catch (e: Exception) {
                println("Failed to initialize bucket: ${e.message}")
            }

            client
        }

        // Метод get() автоматически найдет MinioClient, зарегистрированный шагом выше
        single<VideoStorage> {
            MinioVideoStorage(
                minioClient = get(),
                bucket = System.getenv("minio.bucket") ?: "videos"
            )
        }

        single<VideoRepository> {
            PostgresVideoRepository(

            )
        }

        // Провайдим UseCase
        single {
            UploadVideoUseCase(
                videoStorage = get(),
                videoRepository = get()
            )
        }

        single {
            UpdateVideoMetaUseCase(
                videoRepository = get()
            )
        }
    }

    install(Koin) {
        slf4jLogger()
        modules(videoModule)
    }
}