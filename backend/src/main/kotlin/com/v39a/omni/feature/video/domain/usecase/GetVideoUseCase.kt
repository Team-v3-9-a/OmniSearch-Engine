package com.v39a.omni.feature.video.domain.usecase

import com.v39a.omni.core.exceptions.VideoNotFoundException
import com.v39a.omni.feature.video.domain.Video
import com.v39a.omni.feature.video.domain.VideoRepository
import java.util.UUID

class GetVideoUseCase(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(id: UUID): Video {
        return videoRepository.getById(id) ?: throw VideoNotFoundException(id)
    }
}