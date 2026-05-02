package com.v39a.omni.feature.video.api

import com.v39a.omni.feature.video.domain.UpdateVideoMetaUseCase
import com.v39a.omni.feature.video.domain.UpdateVideoMetadataCommand
import com.v39a.omni.feature.video.domain.UploadVideoCommand
import com.v39a.omni.feature.video.domain.UploadVideoUseCase
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.slf4j.LoggerFactory
import java.util.*

fun Route.videoRoutes(uploadVideoUseCase: UploadVideoUseCase, updateVideoMetadataUseCase: UpdateVideoMetaUseCase) {
    val logger = LoggerFactory.getLogger(javaClass)

    route("/api/v1") {

        // MOCK: GET /search
        // Принимает ?query=something
        get("videos/search") {
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
            var title = ""
            var durationSeconds = 0
            var thumbnailPath = ""
            var filePart: PartData.FileItem? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "title" -> title = part.value
                            "durationSeconds" -> durationSeconds = part.value.toIntOrNull() ?: 0
                            "thumbnailPath" -> thumbnailPath = part.value
                        }
                    }

                    is PartData.FileItem -> {
                        filePart = part
                    }

                    else -> {}
                }
                if (part !is PartData.FileItem) part.dispose()
            }

            if (filePart != null) {
                val fileName = filePart.originalFileName ?: "unknown.mp4"
                val contentType = filePart.contentType?.toString() ?: "video/mp4"

                val video = withContext(Dispatchers.IO) {
                    filePart.provider().toInputStream().use { inputStream ->
                        val command = UploadVideoCommand(
                            fileName = fileName,
                            contentType = contentType,
                            title = title,
                            durationSeconds = durationSeconds,
                            thumbnailPath = thumbnailPath,
                            contentStream = inputStream,
                        )
                        uploadVideoUseCase.execute(command)
                    }
                }
                filePart.dispose()
                uploadResponse = UploadResponse(
                    id = video.id.toString(),
                    message = "Video uploaded successfully. Processing started."
                )

                call.respond(HttpStatusCode.Accepted, uploadResponse)
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file found in the request"))
            }

        }

        route("/internal/videos") {
            patch("/{id}") {
                val expectedSecret = call.application.environment.config.property("security.internalSecret").getString()
                val providedSecret = call.request.header("X-Internal-Secret")

                if (providedSecret != expectedSecret) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access Denied: Internal API only"))
                    return@patch
                }

                val idParam = call.parameters["id"]
                val videoId = try { UUID.fromString(idParam) } catch (_: Exception) { null }

                if (videoId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid video UUID"))
                    return@patch
                }

                val request = call.receive<UpdateVideoRequest>()

                if (request.status == null && request.durationSeconds == null && request.thumbnailPath == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No fields to update"))
                    return@patch
                }

                val command = UpdateVideoMetadataCommand(
                    status = request.status,
                    durationSeconds = request.durationSeconds,
                    thumbnailPath = request.thumbnailPath
                )

                updateVideoMetadataUseCase.execute(videoId, command)

                call.respond(HttpStatusCode.OK, mapOf("message" to "Video updated successfully"))
            }
        }
    }
}