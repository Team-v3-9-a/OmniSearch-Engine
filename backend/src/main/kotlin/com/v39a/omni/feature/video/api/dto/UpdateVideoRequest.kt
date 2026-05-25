package com.v39a.omni.feature.video.api.dto

import com.v39a.omni.feature.video.domain.VideoStatus
import kotlinx.serialization.Serializable

@Serializable
data class UpdateVideoRequest(
    val status: VideoStatus? = null,
    val durationSeconds: Long? = null,
    val thumbnailPath: String? = null
)