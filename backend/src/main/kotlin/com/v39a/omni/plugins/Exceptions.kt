package com.v39a.omni.plugins

import com.v39a.omni.core.exceptions.InternalApiForbiddenException
import com.v39a.omni.core.exceptions.VideoEngineUnavailableException
import com.v39a.omni.core.exceptions.VideoNotFoundException
import com.v39a.omni.core.exceptions.VideoNotReadyException
import com.v39a.omni.core.exceptions.MLEngineUnavailableException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureExceptions() {
    val logger = environment.log

    install(StatusPages) {

        exception<RuntimeException> { call, cause ->
            logger.error("RuntimeException occurred: ${cause.message}", cause)
            if (cause.message?.contains("MinIO") == true) {
                call.respond(
                    HttpStatusCode.BadGateway,
                    mapOf("error" to "Storage integration failed")
                )
            } else {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error")
                )
            }
        }
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception: ${cause.message}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Something went wrong")
            )
        }
        exception<VideoNotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("error" to cause.message))
        }

        exception<VideoNotReadyException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, mapOf("error" to cause.message))
        }

        exception<VideoEngineUnavailableException> { call, cause ->
            call.application.environment.log.error(cause.message, cause)
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "Processing service temporarily unavailable"))
        }

        exception<MLEngineUnavailableException> { call, cause ->
            call.application.environment.log.error(cause.message, cause)
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to (cause.message ?: "Search service temporarily unavailable")))
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid UUID format"))
        }

        exception<InternalApiForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to cause.message))
        }
    }
}