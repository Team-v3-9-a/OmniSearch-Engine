package com.v39a.omni.feature.video.domain

import java.util.UUID

interface VideoEngineClient {
    suspend fun startProcessing(videoId: UUID, s3Path: String)
}