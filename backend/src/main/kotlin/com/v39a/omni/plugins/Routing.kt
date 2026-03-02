package com.v39a.omni.plugins

import com.v39a.omni.features.video.api.videoRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    routing {
        videoRoutes()
    }
}
