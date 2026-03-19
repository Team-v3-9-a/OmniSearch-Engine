package com.v39a.omni
import com.v39a.omni.feature.video.infrastructure.MinioVideoStorage
import com.v39a.omni.plugins.*
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.minio.MinioClient


fun main(args: Array<String>) {

    // Загрузка переменных окружения из /backend/.env
    try {
        val dotenv = dotenv {
            directory = "./backend"
            ignoreIfMissing = true
        }
        dotenv.entries().forEach { entry ->
            System.setProperty(entry.key, entry.value)
        }
    } catch (e: Exception) {
        println("Error loading .env file: ${e.message}")
    }

    EngineMain.main(args)

//    val minioClient = MinioClient.builder()
//        .endpoint(System.getenv("minio.endpoint"))
//        .credentials(
//            System.getenv("minio.accessKey"),
//            System.getenv("minio.secretKey"),
//        )
//        .build()
//
//    val videoStorage = MinioVideoStorage(minioClient, System.getenv("minio.bucket"))
}

fun Application.module() {
    configureFrameworks()
    configureSerialization()
    configureHTTP()
    configureRouting()
    configureDatabase()
}
