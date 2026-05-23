package com.v39a.omni.feature.video.infrastructure

import com.v39a.omni.feature.video.api.VideoEngineRequest
import com.v39a.omni.feature.video.domain.VideoEngineClient
import io.ktor.client.HttpClient
import io.ktor.client.request.post

import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.util.UUID


class KtorHttpVideoClient(
    private val client: HttpClient
) : VideoEngineClient {

    private val baseUrl = System.getProperty("VIDEO_ENGINE_URL")
        ?: System.getenv("VIDEO_ENGINE_URL")
        ?: "http://video-engine:8081"

    override suspend fun startProcessing(videoId: UUID, s3Path: String) {

        val response: HttpResponse = client.post("$baseUrl/process") {
            contentType(ContentType.Application.Json)
            setBody(
                VideoEngineRequest(
                    videoId = videoId.toString(),
                    s3Path = s3Path
                )
            )
        }

        if (!response.status.isSuccess()) {

            throw Exception("Failed to start processing. Video Engine returned: ${response.status}")
        }
    }
}