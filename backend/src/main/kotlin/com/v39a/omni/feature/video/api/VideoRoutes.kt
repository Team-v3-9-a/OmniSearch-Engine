package com.v39a.omni.feature.video.api

import com.v39a.omni.feature.video.domain.usecase.UpdateVideoMetadataCommand
import com.v39a.omni.feature.video.domain.usecase.UploadVideoCommand
import com.v39a.omni.feature.video.domain.usecase.VideoUseCases
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.ktor.ext.inject

import org.slf4j.LoggerFactory
import java.util.*

fun Route.videoRoutes() {
    val videoUseCases by inject<VideoUseCases>()
    val logger = LoggerFactory.getLogger(javaClass)

    route("/api/v1") {
        route("/videos") {

            get{
                call.respond(videoUseCases.getAll().toResponseDTOList())
            }
            // MOCK: GET /search
            // Принимает ?query=something
            get("/search") {
                val query = call.request.queryParameters["query"] ?: ""

                call.respond("Coming Soon... Stay tuned!)")
            }

            // POST /upload
            post("/upload") {
                logger.info("Received file upload request")

                val parsedData = call.receiveVideoMultipart()

                val fileName = parsedData.filePart.originalFileName ?: "unknown.mp4"
                val contentType = parsedData.filePart.contentType?.toString() ?: "video/mp4"

                val video = withContext(Dispatchers.IO) {
                    parsedData.filePart.provider().toInputStream().use { inputStream ->
                        val command = UploadVideoCommand(
                            fileName = fileName,
                            contentType = contentType,
                            title = parsedData.title,
                            durationSeconds = parsedData.durationSeconds,
                            thumbnailPath = parsedData.thumbnailPath,
                            contentStream = inputStream
                        )
                        videoUseCases.upload.execute(command)
                    }
                }

                parsedData.filePart.dispose()

                call.respond(
                    HttpStatusCode.Accepted,
                    UploadResponse(video.id.toString(), "Video uploaded successfully. Processing started.")
                )
            }

            route("/{id}") {
                get {
                    val video = videoUseCases.getById(call.videoId)
                    call.respond(HttpStatusCode.OK, video.toResponseDTO())
                }

                get("/stream") {
                    val presignedUrl = videoUseCases.getStreamUrl(call.videoId)
                    call.respond(HttpStatusCode.OK, StreamUrlResponse(url = presignedUrl))
                }
            }

        }

        route("/api/v1/internal/videos") {
            patch("/{id}") {
                call.requireInternalSecret()

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
                videoUseCases.patchMetadata.execute(call.videoId, command)

                call.respond(HttpStatusCode.OK, mapOf("message" to "Video updated successfully"))
            }
        }
    }
}
