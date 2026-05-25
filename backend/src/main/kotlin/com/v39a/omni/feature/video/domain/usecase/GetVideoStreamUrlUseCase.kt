package com.v39a.omni.feature.video.domain.usecase

import com.v39a.omni.core.exceptions.VideoNotFoundException
import com.v39a.omni.core.exceptions.VideoNotReadyException
import com.v39a.omni.feature.video.domain.VideoRepository
import com.v39a.omni.feature.video.domain.VideoStatus
import com.v39a.omni.feature.video.domain.VideoStorage
import java.util.UUID

class GetVideoStreamUrlUseCase(
    private val videoRepository: VideoRepository,
    private val videoStorage: VideoStorage
) {
    suspend operator fun invoke(id: UUID): String {
        val video = videoRepository.getById(id) ?: throw VideoNotFoundException(id)

        if (video.status != VideoStatus.READY) {
            throw VideoNotReadyException(id, video.status?: VideoStatus.UNKNOWN)
        }

        val url = videoStorage.getPresignedUrl(video.path)

        // Вроде для mvp норм
        return url.replace("http://minio:9000", "http://localhost:9000")
    }
}