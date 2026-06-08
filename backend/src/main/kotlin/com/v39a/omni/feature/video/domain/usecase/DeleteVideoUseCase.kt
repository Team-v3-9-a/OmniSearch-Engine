package com.v39a.omni.feature.video.domain.usecase

import com.v39a.omni.core.exceptions.VideoNotFoundException
import com.v39a.omni.feature.video.port.MLEngineClient
import com.v39a.omni.feature.video.port.VideoRepository
import com.v39a.omni.feature.video.port.VideoStorage
import java.util.UUID
import org.slf4j.LoggerFactory
class DeleteVideoUseCase(
    private val videoRepository: VideoRepository,
    private val videoStorage: VideoStorage,
    private val mlClient: MLEngineClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend operator fun invoke(id: UUID) {

        val video = videoRepository.getById(id) ?: throw VideoNotFoundException(id)

        videoRepository.deleteById(id)

        try {
            mlClient.deleteVectors(id)
            logger.info("Successfully deleted vectors from ML Engine for video: $id")
        } catch (e: Exception) {
            // ML недоступен
            logger.error("Orphaned vectors in ML Engine! Failed to delete for video: $id", e)
        }

        try {
            videoStorage.delete(video.path)
            logger.info("Successfully deleted video file from S3: ${video.path}")
        } catch (e: Exception) {
            // S3 недоступен
            logger.error("Orphaned file in S3! Failed to delete file: ${video.path}", e)
        }
    }
}