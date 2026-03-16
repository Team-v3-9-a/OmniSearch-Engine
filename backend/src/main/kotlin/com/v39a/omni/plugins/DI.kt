package com.v39a.omni.plugins

import com.v39a.omni.feature.video.domain.UploadVideoUseCase
import com.v39a.omni.feature.video.domain.VideoStorage
import com.v39a.omni.feature.video.infrastructure.MinioVideoStorage
import io.ktor.server.application.*
import io.minio.MinioClient
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    val videoModule = module {
        single {
            MinioClient.builder()
                .endpoint(System.getenv("minio.endpoint"))
                .credentials(
                    System.getenv("minio.accessKey"),
                    System.getenv("minio.secretKey"),
                )
                .build()
        }

        // Метод get() автоматически найдет MinioClient, зарегистрированный шагом выше
        single<VideoStorage> {
            MinioVideoStorage(
                minioClient = get(),
                bucket = System.getenv("MINIO_BUCKET") ?: "videos"
            )
        }

        // Провайдим UseCase
        // get() сам подставит VideoStorage и VideoRepository.
        single {
            UploadVideoUseCase(
                videoStorage = get(),
                videoRepository = get() // todo биндинг репозитория
            )
        }
    }

    install(Koin) {
        slf4jLogger()
        modules(videoModule)
    }
}