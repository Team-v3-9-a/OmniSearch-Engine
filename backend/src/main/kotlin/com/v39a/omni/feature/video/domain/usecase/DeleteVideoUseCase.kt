package com.v39a.omni.feature.video.domain.usecase

import com.v39a.omni.core.exceptions.VideoNotFoundException
import com.v39a.omni.feature.video.port.VideoRepository
import com.v39a.omni.feature.video.port.VideoStorage
import java.util.UUID
import org.slf4j.LoggerFactory

class DeleteVideoUseCase(
    private val videoRepository: VideoRepository,
    private val videoStorage: VideoStorage,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend operator fun invoke(id: UUID) {
        val video = videoRepository.getById(id) ?: throw VideoNotFoundException(id)

        videoRepository.deleteById(id)

        try {
            videoStorage.delete(video.path)
            logger.info("Successfully deleted video file from S3: ${video.path}")
        } catch (e: Exception) {
            // MinIO упал
            // Для клиента видео удалено (из БД исчезло)
            // На данном этапе просто оставлю алерт в логах, чтобы админ потом почистил этот "осиротевший" файл.
            logger.error("Failed to delete video file from S3 (Orphaned file): ${video.path}", e)
        }
    }
}