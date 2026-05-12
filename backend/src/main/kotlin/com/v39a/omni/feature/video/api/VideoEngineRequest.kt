package com.v39a.omni.feature.video.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoEngineRequest(
    @SerialName("video_id")
    val videoId: String,
    @SerialName("s3_path")
    val s3Path: String
)