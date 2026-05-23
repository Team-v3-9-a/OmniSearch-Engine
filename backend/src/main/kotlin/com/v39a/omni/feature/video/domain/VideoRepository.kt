package com.v39a.omni.feature.video.domain

import com.v39a.omni.feature.video.domain.usecase.UpdateVideoMetadataCommand
import java.util.UUID

interface VideoRepository {
    suspend fun create(video: Video)
    suspend fun updateStatus(id: UUID, newStatus: VideoStatus)
    suspend fun getById(id: UUID): Video?
    suspend fun patchVideo(videoId: UUID, command: UpdateVideoMetadataCommand)

}