package com.v39a.omni.feature.video.domain.usecase

import com.v39a.omni.feature.video.domain.Video
import com.v39a.omni.feature.video.domain.VideoRepository

class GetVideosUseCase(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(): List<Video> {
        return videoRepository.getAll()
    }
}