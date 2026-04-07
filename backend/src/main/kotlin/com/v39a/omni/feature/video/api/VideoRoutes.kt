package com.v39a.omni.feature.video.api

import com.v39a.omni.feature.video.domain.UploadVideoCommand
import com.v39a.omni.feature.video.domain.UploadVideoUseCase
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.*

fun Route.videoRoutes(uploadVideoUseCase: UploadVideoUseCase) {
    val logger = LoggerFactory.getLogger(javaClass)

    route("/api/v1/videos") {

        // MOCK: GET /search
        // Принимает ?query=something
        get("/search") {
            val query = call.request.queryParameters["query"] ?: ""

            // Мок метаданных видео
            val mockVideos = listOf(
                VideoResponse(
                    id = UUID.randomUUID().toString(),
                    title = "Video about $query",
                    status = "READY",
                    durationSeconds = 120,
                    uploadDate = "2023-10-27T10:00:00Z"
                ),
                VideoResponse(
                    id = UUID.randomUUID().toString(),
                    title = "Another related video",
                    status = "PROCESSING",
                    durationSeconds = 45,
                    uploadDate = "2023-10-27T10:05:00Z"
                )
            )

            call.respond(mockVideos)
        }

        // POST /upload
        post("/upload") {
            //todo разобраться как его отключить или настроить иным образом, чтобы не было неявных лимитов
            call.formFieldLimit = 500L * 1024 * 1024 * 1024

            val multipart = call.receiveMultipart()
            var uploadResponse: UploadResponse? = null
            logger.info("received file upload")

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        // Фолбэки на случай кривого клиента
                        val fileName = part.originalFileName ?: "unknown_video.mp4"
                        val contentType = part.contentType?.toString() ?: "video/mp4"

                        val channel = part.provider()

                        // Command-объект для передачи в Domain-слой
                        val video = withContext(Dispatchers.IO) {

                            channel.toInputStream().use { inputStream ->

                                val command = UploadVideoCommand(
                                    fileName = fileName,
                                    contentType = contentType,
                                    contentStream = inputStream,
                                )

                                uploadVideoUseCase.execute(command)
                            }
                        }

                        uploadResponse = UploadResponse(
                            id = video.id.toString(),
                            message = "Video uploaded successfully. Processing started."
                        )
                    }
                    else -> {
                        // Пока ничего
                        // Можно обрабатывать PartData.FormItem
                        // если фронтенд передает текстовые параметры
                    }
                }
                part.dispose()
            }
            if (uploadResponse != null) {
                call.respond(HttpStatusCode.Accepted, uploadResponse)
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file found in the request"))
            }
        }
    }
}