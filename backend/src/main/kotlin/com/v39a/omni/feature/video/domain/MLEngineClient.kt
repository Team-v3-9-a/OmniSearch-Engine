package com.v39a.omni.feature.video.domain

data class MLEngineSearchResult(
    val videoId: String,
    val score: Double,
    val startTime: Double?,
    val endTime: Double?,
    val textSnippet: String?
)

interface MLEngineClient {
    suspend fun search(query: String, topK: Int = 10): List<MLEngineSearchResult>
}
