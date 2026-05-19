package com.v39a.omni.feature.video.domain.usecase

import com.v39a.omni.feature.video.domain.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class UpdateVideoMetaUseCase(
    private val videoRepository: VideoRepository,
) {
    suspend fun execute(videoId: UUID, command: UpdateVideoMetadataCommand): Unit = withContext(Dispatchers.IO) {

        videoRepository.patchVideo(videoId, command)
    }
}