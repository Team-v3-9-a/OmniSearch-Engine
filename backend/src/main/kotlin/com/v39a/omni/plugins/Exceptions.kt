package com.v39a.omni.plugins

import com.v39a.omni.core.exceptions.VideoNotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureExceptions() {
    install(StatusPages) {

        exception<RuntimeException> { call, cause ->
            if (cause.message?.contains("MinIO") == true) {
                call.respond(
                    HttpStatusCode.BadGateway,
                    mapOf("error" to "Storage integration failed", "details" to cause.message)
                )
            } else {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error", "details" to cause.localizedMessage)
                )
            }
        }
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Something went wrong", "details" to cause.message)
            )
        }
        exception<VideoNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to cause.message)
            )
        }
    }
}