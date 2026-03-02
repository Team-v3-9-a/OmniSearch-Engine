package com.v39a.omni.plugins

import com.v39a.omni.feature.video.infrastructure.VideoService
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()
        modules(module {
            single<VideoService> {
                VideoService {
                    println(environment.log.info("Hello, World!"))
                }
            }
        })
    }
}
