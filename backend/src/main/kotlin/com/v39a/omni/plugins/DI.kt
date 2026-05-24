package com.v39a.omni.plugins

import com.v39a.omni.feature.video.domain.VideoEngineClient
import com.v39a.omni.feature.video.domain.VideoRepository
import com.v39a.omni.feature.video.domain.VideoStorage
import com.v39a.omni.feature.video.domain.usecase.GetVideoStreamUrlUseCase
import com.v39a.omni.feature.video.domain.usecase.GetVideoUseCase
import com.v39a.omni.feature.video.domain.usecase.UpdateVideoMetaUseCase
import com.v39a.omni.feature.video.domain.usecase.UploadVideoUseCase
import com.v39a.omni.feature.video.domain.usecase.VideoUseCases
import com.v39a.omni.feature.video.infrastructure.KtorHttpVideoClient
import com.v39a.omni.feature.video.infrastructure.MinioVideoStorage
import com.v39a.omni.feature.video.infrastructure.PostgresVideoRepository
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.dsl.onClose
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

        single {
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                        }
                    )
                }
                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = 3)
                    retryOnException(maxRetries = 3, retryOnTimeout = true)
                    exponentialDelay()
                }
            }
        } onClose { it?.close() }

        single<VideoEngineClient> {
            KtorHttpVideoClient(
                get()
            )
        }

        single<CoroutineScope> {
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }

        single {
            UploadVideoUseCase(
                videoStorage = get(),
                videoRepository = get(),
                backgroundScope = get(),
                videoEngineClient = get(),
            )
        }
        single {
            GetVideoUseCase(
                get()
            )
        }
        single {
            UpdateVideoMetaUseCase(
                videoRepository = get()
            )
        }

        single {
            GetVideoStreamUrlUseCase(
                videoStorage = get(),
                videoRepository = get(),
            )
        }

        single { VideoUseCases(get(), get(), get(), get()) }
    }

    install(Koin) {
        slf4jLogger()
        modules(videoModule)
    }
}