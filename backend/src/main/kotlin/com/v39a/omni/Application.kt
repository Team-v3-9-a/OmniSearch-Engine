package com.v39a.omni
import com.v39a.omni.plugins.*
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.server.netty.*


fun main(args: Array<String>) {

    // Загрузка переменных окружения из /backend/.env
    try {
        val dotenv = dotenv {
            directory = "./backend"
            ignoreIfMissing = true
        }
        dotenv.entries().forEach { entry ->
            System.setProperty(entry.key, entry.value)
        }
    } catch (e: Exception) {
        println("Error loading .env file: ${e.message}")
    }

    EngineMain.main(args)
}

fun Application.module() {
    configureFrameworks()
    configureSerialization()
    configureHTTP()
    configureRouting()
    configureDatabase()
}
