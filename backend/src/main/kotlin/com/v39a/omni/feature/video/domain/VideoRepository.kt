package com.v39a.omni.feature.video.domain

interface VideoRepository {
    suspend fun create(video: Video)
}