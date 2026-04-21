package com.v39a.omni.feature.video.api

import com.v39a.omni.feature.video.domain.VideoStatus
import kotlinx.serialization.Serializable

@Serializable
data class UpdateStatusRequest(
    val status: VideoStatus
)