package com.v39a.omni.feature.video.api.dto

import java.io.File

data class ParsedUploadRequest(
    val title: String,
    val durationSeconds: Int,
    val thumbnailPath: String,
    val fileName: String,
    val contentType: String,
    val tempFile: File
)