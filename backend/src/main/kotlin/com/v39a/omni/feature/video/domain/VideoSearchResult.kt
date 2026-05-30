package com.v39a.omni.feature.video.domain

data class VideoSearchResult(
    val video: Video,
    val score: Double,
    val thumbnailUrl: String,
    val segments: List<VideoSearchSegment>
)

data class VideoSearchSegment(
    val textSnippet: String,
    val startTime: Double,
    val endTime: Double
)
