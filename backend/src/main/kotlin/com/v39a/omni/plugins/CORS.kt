package com.v39a.omni.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCORS() {

    val corsOriginsRaw = environment.config.propertyOrNull("http.allowedOrigins")?.getString() ?: ""

    install(CORS) {
        val origins = corsOriginsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (origins.contains("*")) {
            anyHost()
        } else {
            origins.forEach { origin ->
                val scheme = if (origin.startsWith("https://")) "https" else "http"
                val host = origin.removePrefix("$scheme://")

                allowHost(host, schemes = listOf(scheme))
            }
        }

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)

        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
}