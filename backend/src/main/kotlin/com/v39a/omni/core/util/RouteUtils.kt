package com.v39a.omni.core.util

import io.ktor.server.application.ApplicationCall
import java.util.UUID

val ApplicationCall.videoId: UUID
    get() = UUID.fromString(parameters["id"] ?: throw IllegalArgumentException("Missing ID parameter"))