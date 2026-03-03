package com.v39a.omni.features.video.api

import com.v39a.omni.feature.video.api.UploadResponse
import com.v39a.omni.feature.video.api.VideoResponse
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.videoRoutes() {
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

        // MOCK: POST /upload
        post("/upload") {
            // todo call.receiveMultipart()
            val newId = UUID.randomUUID().toString()

            call.respond(
                UploadResponse(
                    id = newId,
                    message = "Video uploaded successfully. Processing started."
                )
            )
        }
    }
}