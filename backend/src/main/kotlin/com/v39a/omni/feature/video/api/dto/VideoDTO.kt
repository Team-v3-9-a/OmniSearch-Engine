package com.v39a.omni.feature.video.api.dto

import com.v39a.omni.feature.video.domain.Video
import com.v39a.omni.feature.video.domain.VideoStatus
import kotlinx.serialization.Serializable

@Serializable
data class VideoResponseDTO(
    val id: String,
    val title: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val fps: Double? = null,
    val resolution: String? = null
)

fun Video.toResponseDTO(): VideoResponseDTO {
    return VideoResponseDTO(
        id = this.id.toString(),
        title = this.title,
        status = this.status?.name ?: VideoStatus.ERROR.name,
        createdAt = this.createdAt.toString(),
        updatedAt = this.updatedAt.toString(),
        fps = this.fps,
        resolution = this.resolution
    )
}



fun List<Video>.toResponseDTOList() = this.map { it.toResponseDTO() }