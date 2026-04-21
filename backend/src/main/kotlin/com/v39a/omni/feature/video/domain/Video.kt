package com.v39a.omni.feature.video.domain

import kotlinx.datetime.LocalDateTime
import java.math.BigInteger
import java.util.UUID


data class Video(val id: UUID, val path: String, val status: VideoStatus?,
                 val title: String,
                 val durationSeconds: BigInteger,
                 val thumbnailPath: String,
                 val createdAt: LocalDateTime,
)

enum class VideoStatus {
    UPLOADED,
    PROCESSING_MEDIA,
    PROCESSING_ML,
    READY,
    ERROR,
}