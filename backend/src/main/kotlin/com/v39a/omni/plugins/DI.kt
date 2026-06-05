package com.v39a.omni.plugins

import com.v39a.omni.feature.video.domain.VideoEngineClient
import com.v39a.omni.feature.video.domain.VideoRepository
import com.v39a.omni.feature.video.domain.VideoStorage
import com.v39a.omni.feature.video.domain.MLEngineClient
import com.v39a.omni.feature.video.domain.usecase.GetVideoStreamUrlUseCase
import com.v39a.omni.feature.video.domain.usecase.GetVideoUseCase
import com.v39a.omni.feature.video.domain.usecase.GetVideosUseCase
import com.v39a.omni.feature.video.domain.usecase.UpdateVideoMetaUseCase
import com.v39a.omni.feature.video.domain.usecase.UploadVideoUseCase
import com.v39a.omni.feature.video.domain.usecase.VideoUseCases
import com.v39a.omni.feature.video.domain.usecase.SearchVideosUseCase
import com.v39a.omni.feature.video.infrastructure.KtorHttpVideoClient
import com.v39a.omni.feature.video.infrastructure.MinioVideoStorage
import com.v39a.omni.feature.video.infrastructure.PostgresVideoRepository
import com.v39a.omni.feature.video.infrastructure.KtorHttpMLEngineClient
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
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    val config = environment.config

    val videoModule = module {
        single {
            val endpoint = config.propertyOrNull("minio.endpoint")?.getString() ?: "http://minio:9000"
            val accessKey = config.propertyOrNull("minio.accessKey")?.getString() ?: "admin"
            val secretKey = config.propertyOrNull("minio.secretKey")?.getString() ?: "password123"

            val client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build()

            val bucket = config.propertyOrNull("minio.bucket")?.getString() ?: "videos"
            try {
                val found = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
                if (!found) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
                    println("Bucket '$bucket' created successfully!")
                } else {
                    println("Bucket '$bucket' already exists.")
                }
            } catch (e: Exception) {
                println("Failed to initialize bucket '$bucket': ${e.message}")
            }

            client
        }

        single<VideoStorage> {
            MinioVideoStorage(
                minioClient = get(),
                bucket = config.propertyOrNull("minio.bucket")?.getString() ?: "videos"
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
        } onClose {
            (it as? AutoCloseable)?.close()
        }

        single<VideoEngineClient> {
            KtorHttpVideoClient(
                get()
            )
        }

        single<MLEngineClient> {
            KtorHttpMLEngineClient(
                get()
            )
        }

        single<CoroutineScope> {
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
        } onClose { scope ->
            scope?.cancel()
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
            GetVideosUseCase(
                videoRepository = get()
            )
        }

        single {
            GetVideoStreamUrlUseCase(
                videoStorage = get(),
                videoRepository = get(),
                externalUrl = config.propertyOrNull("minio.externalUrl")?.getString() ?: "http://localhost:9000"
            )
        }

        single {
            SearchVideosUseCase(
                mlEngineClient = get(),
                videoRepository = get(),
                videoStorage = get(),
                externalUrl = config.propertyOrNull("minio.externalUrl")?.getString() ?: "http://localhost:9000"
            )
        }

        single { VideoUseCases(get(), get(), get(), get(), get(), get()) }
    }

    install(Koin) {
        slf4jLogger()
        modules(videoModule)
    }
}