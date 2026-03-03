package com.v39a.omni.feature.video.api

import kotlinx.serialization.Serializable

@Serializable
data class VideoResponse(
    val id: String,
    val title: String,
    val status: String, // "PROCESSING", "READY", "ERROR"
    val durationSeconds: Int,
    val uploadDate: String
)

@Serializable
data class UploadResponse(
    val id: String,
    val message: String
)