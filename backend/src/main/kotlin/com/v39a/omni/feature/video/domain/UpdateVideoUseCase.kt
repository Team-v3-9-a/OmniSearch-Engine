package com.v39a.omni.feature.video.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class UpdateVideoStatusUseCase(
    private val videoRepository: VideoRepository,
) {
    suspend fun execute(videoId: UUID, status: VideoStatus): Unit = withContext(Dispatchers.IO) {
        videoRepository.updateStatus(videoId, status)
    }
}