package com.v39a.omni.feature.video.api.dto

import io.ktor.http.content.PartData

data class ParsedUploadRequest(
    val title: String,
    val durationSeconds: Int,
    val thumbnailPath: String,
    val filePart: PartData.FileItem
)