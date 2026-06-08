package com.v39a.omni.feature.video.port

import java.io.InputStream

interface VideoStorage {
    suspend fun upload(fileName: String, stream: InputStream, contentType: String): String
    suspend fun getPresignedUrl(s3Path: String): String
    suspend fun delete(s3Path: String)

}