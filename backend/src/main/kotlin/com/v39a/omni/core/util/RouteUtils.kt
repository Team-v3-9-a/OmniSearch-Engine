package com.v39a.omni.core.util

import com.v39a.omni.core.exceptions.InternalApiForbiddenException
import com.v39a.omni.feature.video.api.dto.ParsedUploadRequest
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.formFieldLimit
import io.ktor.server.request.header
import io.ktor.server.request.receiveMultipart
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
    this.formFieldLimit = 500L * 1024 * 1024 * 1024

    var title = ""
    var durationSeconds = 0
    var thumbnailPath = ""
    var filePart: PartData.FileItem? = null

    receiveMultipart().forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                when (part.name) {
                    "title" -> title = part.value
                    "durationSeconds" -> durationSeconds = part.value.toIntOrNull() ?: 0
                    "thumbnailPath" -> thumbnailPath = part.value
                }
                part.dispose()
            }
            is PartData.FileItem -> filePart = part
            else -> part.dispose()
        }
    }

    return filePart?.let {
        ParsedUploadRequest(title, durationSeconds, thumbnailPath, it)
    } ?: throw IllegalArgumentException("File part is missing in the request")
}