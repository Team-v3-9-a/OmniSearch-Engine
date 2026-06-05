package com.v39a.omni.core.util

import com.v39a.omni.core.exceptions.InternalApiForbiddenException
import com.v39a.omni.feature.video.api.dto.ParsedUploadRequest
import io.ktor.http.content.PartData
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.formFieldLimit
import io.ktor.server.request.header
import io.ktor.server.request.receiveMultipart
import java.io.File
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

val ApplicationCall.videoId: UUID
    get() = UUID.fromString(parameters["id"] ?: throw IllegalArgumentException("Missing ID parameter"))

fun ApplicationCall.requireInternalSecret() {
    val expectedSecret = application.environment.config.property("security.internalSecret").getString()
    val providedSecret = request.header("X-Internal-Secret")

    if (providedSecret != expectedSecret) {
        throw InternalApiForbiddenException()
    }
}

suspend fun ApplicationCall.receiveVideoMultipart(): ParsedUploadRequest {
    this.formFieldLimit = 10L * 1024 * 1024 // 10 MB limit for form fields to prevent DoS

    var title = ""
    var durationSeconds = 0
    var thumbnailPath = ""
    var tempFile: File? = null
    var fileName = "unknown.mp4"
    var contentType = "video/mp4"

    val multipart = receiveMultipart()
    while (true) {
        val part = multipart.readPart() ?: break
        when (part) {
            is PartData.FormItem -> {
                when (part.name) {
                    "title" -> title = part.value
                    "durationSeconds" -> durationSeconds = part.value.toIntOrNull() ?: 0
                    "thumbnailPath" -> thumbnailPath = part.value
                }
                part.dispose()
            }
            is PartData.FileItem -> {
                fileName = part.originalFileName ?: "unknown.mp4"
                contentType = part.contentType?.toString() ?: "video/mp4"
                
                val file = withContext(Dispatchers.IO) {
                    File.createTempFile("upload-", ".tmp")
                }
                part.provider().toInputStream().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile = file
                part.dispose()
            }
            else -> part.dispose()
        }
    }

    return tempFile?.let {
        ParsedUploadRequest(title, durationSeconds, thumbnailPath, fileName, contentType, it)
    } ?: throw IllegalArgumentException("File part is missing in the request")
}