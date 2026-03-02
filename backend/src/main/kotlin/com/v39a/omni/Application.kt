package com.v39a.omni
import com.v39a.omni.plugins.configureDatabase
import io.ktor.server.application.*
import io.ktor.server.netty.*
import com.v39a.omni.plugins.configureFrameworks
import com.v39a.omni.plugins.configureHTTP
import com.v39a.omni.plugins.configureRouting
import com.v39a.omni.plugins.configureSerialization

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureFrameworks()
    configureSerialization()
    configureHTTP()
    configureRouting()
    configureDatabase()
}
