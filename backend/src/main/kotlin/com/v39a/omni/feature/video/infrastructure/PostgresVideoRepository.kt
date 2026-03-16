package com.v39a.omni.feature.video.infrastructure

import com.v39a.omni.feature.video.domain.Video
import com.v39a.omni.feature.video.domain.VideoRepository

class PostgresVideoRepository : VideoRepository {
    override suspend fun create(video: Video) {
        TODO("Not yet implemented")
    }
}