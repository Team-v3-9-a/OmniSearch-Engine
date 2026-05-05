package com.v39a.omni.plugins

import com.v39a.omni.feature.video.api.videoRoutes
import com.v39a.omni.feature.video.domain.UpdateVideoMetaUseCase
import com.v39a.omni.feature.video.domain.UploadVideoUseCase
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val uploadVideoUseCase by inject<UploadVideoUseCase>()
    val updateVideoStatusUseCase by inject<UpdateVideoMetaUseCase>()

    routing {
        videoRoutes(uploadVideoUseCase, updateVideoStatusUseCase)
    }
}
