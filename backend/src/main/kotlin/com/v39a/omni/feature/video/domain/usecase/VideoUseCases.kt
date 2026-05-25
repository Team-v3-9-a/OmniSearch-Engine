package com.v39a.omni.feature.video.domain.usecase

data class VideoUseCases(
    val upload: UploadVideoUseCase,
    val getById: GetVideoUseCase,
    val patchMetadata: UpdateVideoMetaUseCase,
    val getAll: GetVideosUseCase
)