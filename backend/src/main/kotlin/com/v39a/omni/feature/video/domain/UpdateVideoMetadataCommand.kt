package com.v39a.omni.feature.video.domain

data class UpdateVideoMetadataCommand(
    val status: VideoStatus? = null,
    val durationSeconds: Long? = null,
    val thumbnailPath: String? = null
)