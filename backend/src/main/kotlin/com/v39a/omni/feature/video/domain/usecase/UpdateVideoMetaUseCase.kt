package com.v39a.omni.feature.video.domain.usecase

import com.v39a.omni.feature.video.port.VideoRepository
import com.v39a.omni.feature.video.domain.VideoStatus
import com.v39a.omni.feature.video.domain.command.UpdateVideoMetadataCommand
import com.v39a.omni.plugins.MetricsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.util.concurrent.TimeUnit

class UpdateVideoMetaUseCase(
    private val videoRepository: VideoRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun execute(videoId: UUID, command: UpdateVideoMetadataCommand): Unit = withContext(Dispatchers.IO) {
        val existingVideo = videoRepository.getById(videoId)
        
        val isError = command.error != null || command.status == VideoStatus.ERROR
        val isFinalStatus = isError || command.status == VideoStatus.READY
        val wasAlreadyFinal = existingVideo?.status == VideoStatus.READY || existingVideo?.status == VideoStatus.ERROR
        
        if (isFinalStatus && !wasAlreadyFinal) {
            val resultTag = if (isError) "error" else "ready"
            MetricsManager.registry.counter("omnisearch_videos_processed_total", "result", resultTag).increment()
            
            if (existingVideo != null) {
                val createdInstant = existingVideo.createdAt.toInstant(TimeZone.UTC)
                val durationSeconds = (Clock.System.now() - createdInstant).inWholeSeconds
                MetricsManager.pipelineDuration.record(durationSeconds, TimeUnit.SECONDS)
            }
        }

        if (command.error != null) {
            logger.error("External processing failed for video $videoId. Reason: ${command.error}")

            val forceErrorCommand = command.copy(status = VideoStatus.ERROR)

            videoRepository.patchVideo(videoId, forceErrorCommand)
            return@withContext
        }
        videoRepository.patchVideo(videoId, command)
    }
}