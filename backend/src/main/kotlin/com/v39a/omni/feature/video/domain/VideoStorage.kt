package com.v39a.omni.feature.video.domain

import java.io.InputStream

interface VideoStorage {
    suspend fun upload(fileName: String, stream: InputStream, contentType: String): String
}