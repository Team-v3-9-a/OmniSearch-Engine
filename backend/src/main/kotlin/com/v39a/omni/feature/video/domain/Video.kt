package com.v39a.omni.feature.video.domain

import kotlinx.serialization.Serializable
import java.util.UUID


//@Serializable/**/
data class Video(val id: UUID, val path: String
, val status: VideoStatus?
)

enum class VideoStatus {
    UPLOADING,
    FAILED,
    UPLOADED,

}