package com.v39a.omni.feature.video.infrastructure

import com.v39a.omni.core.exceptions.MLEngineUnavailableException
import com.v39a.omni.feature.video.port.MLEngineClient
import com.v39a.omni.feature.video.port.MLEngineSearchResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.*

class KtorHttpMLEngineClient(
    private val client: HttpClient,
) : MLEngineClient, AutoCloseable {


    private val logger = LoggerFactory.getLogger(javaClass)

    private val baseUrl = System.getProperty("ML_ENGINE_URL")
        ?: System.getenv("ML_ENGINE_URL")
        ?: "http://ml-engine:8000"

    @Serializable
    private data class SearchRequest(
        val query: String,
        val top_k: Int
    )

    @Serializable
    private data class SearchResultItemDTO(
        val video_id: String,
        val score: Double,
        val start_time: Double? = null,
        val end_time: Double? = null,
        val text_snippet: String? = null
    )

    @Serializable
    private data class SearchResponse(
        val results: List<SearchResultItemDTO>
    )

    override suspend fun search(query: String, topK: Int): List<MLEngineSearchResult> {
        val response: HttpResponse = try {
            client.post("$baseUrl/api/v1/search") {
                contentType(ContentType.Application.Json)
                setBody(SearchRequest(query = query, top_k = topK))
            }
        } catch (e: Exception) {
            logger.error("ML Engine is unreachable at $baseUrl: ${e.message}", e)
            throw MLEngineUnavailableException("ML Engine is currently unavailable")
        }

        if (!response.status.isSuccess()) {
            logger.error("ML Engine search failed. Status: ${response.status}")
            throw MLEngineUnavailableException("ML Engine returned error status: ${response.status}")
        }

        val searchResponse = try {
            response.body<SearchResponse>()
        } catch (e: Exception) {
            logger.error("Failed to parse ML Engine search response: ${e.message}", e)
            throw MLEngineUnavailableException("ML Engine returned invalid response format")
        }

        return searchResponse.results.map {
            MLEngineSearchResult(
                videoId = it.video_id,
                score = it.score,
                startTime = it.start_time,
                endTime = it.end_time,
                textSnippet = it.text_snippet
            )
        }
    }

    override suspend fun deleteVectors(videoId: UUID) {
        val response = client.delete("$baseUrl/api/v1/videos/$videoId")

        when (response.status) {
            HttpStatusCode.NoContent -> return
            HttpStatusCode.NotFound -> {
                logger.warn("Vectors for video $videoId not found in ML Engine")
                return
            }
            else -> throw MLEngineUnavailableException("Status: ${response.status}")
        }
    }

    override fun close() {
        client.close()
    }
}
