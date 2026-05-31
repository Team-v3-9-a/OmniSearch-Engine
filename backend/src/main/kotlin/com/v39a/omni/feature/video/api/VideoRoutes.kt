package com.v39a.omni.feature.video.api

import com.v39a.omni.core.util.receiveVideoMultipart
import com.v39a.omni.core.util.requireInternalSecret
import com.v39a.omni.core.util.videoId
import com.v39a.omni.feature.video.api.dto.StreamUrlResponse
import com.v39a.omni.feature.video.api.dto.UpdateVideoRequest
import com.v39a.omni.feature.video.api.dto.UploadResponse
import com.v39a.omni.feature.video.api.dto.SearchResultItem
import com.v39a.omni.feature.video.api.dto.SearchResultSegment
import com.v39a.omni.feature.video.api.dto.toResponseDTO
import com.v39a.omni.feature.video.api.dto.toResponseDTOList
import com.v39a.omni.feature.video.domain.command.UpdateVideoMetadataCommand
import com.v39a.omni.feature.video.domain.usecase.UploadVideoCommand
import com.v39a.omni.feature.video.domain.usecase.VideoUseCases
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import org.koin.ktor.ext.inject

import org.slf4j.LoggerFactory

fun Route.videoRoutes() {
    val videoUseCases by inject<VideoUseCases>()
    val logger = LoggerFactory.getLogger(javaClass)

    route("/api/v1") {
        route("/videos") {

            get {
                call.respond(videoUseCases.getAll().toResponseDTOList())
            }
            get("/search") {
                val query = call.request.queryParameters["query"] ?: ""
                val searchResults = videoUseCases.search(query).map { item ->
                    SearchResultItem(
                        video_id = item.video.id.toString(),
                        title = item.video.title,
                        thumbnail_url = item.thumbnailUrl,
                        duration = item.video.durationSeconds,
                        created_date = item.video.createdAt.toString(),
                        upload_date = item.video.updatedAt.toString(),
                        score = item.score,
                        segments = item.segments.map { segment ->
                            SearchResultSegment(
                                text_snippet = segment.textSnippet,
                                start_time = segment.startTime,
                                end_time = segment.endTime
                            )
                        }
                    )
                }
                call.respond(HttpStatusCode.OK, searchResults)
            }

            // POST /upload
            post("/upload") {
                logger.info("Received file upload request")

                val parsedData = call.receiveVideoMultipart()

                val fileName = parsedData.filePart.originalFileName ?: "unknown.mp4"
                val contentType = parsedData.filePart.contentType?.toString() ?: "video/mp4"

                val video = parsedData.filePart.provider().toInputStream().use { inputStream ->
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

        route("/internal/videos") {
            patch("/{id}") {
                call.requireInternalSecret()

                val request = call.receive<UpdateVideoRequest>()

                if (request.status == null && request.durationSeconds == null &&
                    request.thumbnailPath == null && request.fps == null && request.resolution == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No fields to update"))
                    return@patch
                }

                val command = UpdateVideoMetadataCommand(
                    status = request.status,
                    durationSeconds = request.durationSeconds,
                    thumbnailPath = request.thumbnailPath,
                    fps = request.fps,
                    resolution = request.resolution
                )
                videoUseCases.patchMetadata.execute(call.videoId, command)

                call.respond(HttpStatusCode.OK, mapOf("message" to "Video updated successfully"))
            }
        }
    }
}
