package com.v39a.omni.feature.video.port

import com.v39a.omni.feature.video.domain.Video
import com.v39a.omni.feature.video.domain.VideoStatus
import com.v39a.omni.feature.video.domain.command.UpdateVideoMetadataCommand
import java.util.UUID

interface VideoRepository {
    suspend fun create(video: Video)
    suspend fun updateStatus(id: UUID, newStatus: VideoStatus)
    suspend fun getById(id: UUID): Video?
    suspend fun patchVideo(videoId: UUID, command: UpdateVideoMetadataCommand)
    suspend fun getAll(): List<Video>
    suspend fun getByIds(ids: Collection<UUID>): List<Video>
    suspend fun deleteById(id: UUID)
}