package com.v39a.omni.feature.video.domain

import java.util.UUID

interface VideoRepository {
    suspend fun create(video: Video)
    suspend fun updateStatus(id: UUID, newStatus: VideoStatus): Unit
    suspend fun getById(id: UUID): Video?
}