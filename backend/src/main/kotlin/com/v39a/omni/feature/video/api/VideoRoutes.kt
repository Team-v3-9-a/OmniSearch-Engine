package com.v39a.omni.feature.video.api

import com.v39a.omni.feature.video.domain.UploadVideoCommand
import com.v39a.omni.feature.video.domain.UploadVideoUseCase

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.PartData.FileItem
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.toInputStream

import java.util.UUID

fun Route.videoRoutes(uploadVideoUseCase: UploadVideoUseCase) {
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
            val multipart = call.receiveMultipart()
            var uploadResponse: UploadResponse? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        // Фолбэки на случай кривого клиента
                        val fileName = part.originalFileName ?: "unknown_video.mp4"
                        val contentType = part.contentType?.toString() ?: "video/mp4"

                        val inputStream = part.provider().toInputStream()

                        // Command-объект для передачи в Domain-слой
                        val command = UploadVideoCommand(
                            fileName = fileName,
                            contentType = contentType,
                            contentStream = inputStream
                        )

                        val video = uploadVideoUseCase.execute(command)

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