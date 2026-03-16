package com.v39a.omni.plugins

import com.v39a.omni.feature.video.infrastructure.VideoTable
import io.ktor.server.application.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils

fun Application.configureDatabase() {

    val dbUrl = environment.config.property("storage.jdbcUrl").getString()
    val dbUser = environment.config.property("storage.username").getString()
    val dbPassword = environment.config.property("storage.password").getString()

    val config = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = dbUser
        password = dbPassword
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    try {
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        environment.log.info("Database connected successfully!")
    } catch (e: Exception) {
        environment.log.error("! Failed to connect to database: ${e.message}")
        throw e;
    }

    SchemaUtils.create(VideoTable)
}