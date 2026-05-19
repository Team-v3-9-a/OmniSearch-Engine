package com.v39a.omni.feature.video.domain.usecase

import com.v39a.omni.feature.video.domain.VideoStatus

data class UpdateVideoMetadataCommand(
    val status: VideoStatus? = null,
    val durationSeconds: Long? = null,
    val thumbnailPath: String? = null
)