package com.v39a.omni.feature.video.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchResultItem(
    val video_id: String,
    val title: String,
    val thumbnail_url: String,
    val duration: Int,
    val created_date: String,
    val upload_date: String,
    val score: Double,
    val segments: List<SearchResultSegment>
)

@Serializable
data class SearchResultSegment(
    val text_snippet: String,
    val start_time: Double,
    val end_time: Double
)
