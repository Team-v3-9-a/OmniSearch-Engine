package com.v39a.omni.feature.video.domain

import java.util.UUID


data class Video(val id: UUID, val path: String
, val status: VideoStatus?
)

enum class VideoStatus {
    UPLOADING,
    FAILED,
    UPLOADED,

}