package com.v39a.omni.feature.video.domain.usecase

import com.v39a.omni.feature.video.port.VideoRepository
import com.v39a.omni.feature.video.domain.VideoStatus
import com.v39a.omni.feature.video.domain.command.UpdateVideoMetadataCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.UUID

class UpdateVideoMetaUseCase(
    private val videoRepository: VideoRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun execute(videoId: UUID, command: UpdateVideoMetadataCommand): Unit = withContext(Dispatchers.IO) {
        if (command.error != null) {
            logger.error("External processing failed for video $videoId. Reason: ${command.error}")

            val forceErrorCommand = command.copy(status = VideoStatus.ERROR)

            videoRepository.patchVideo(videoId, forceErrorCommand)
            return@withContext
        }
        videoRepository.patchVideo(videoId, command)
    }
}