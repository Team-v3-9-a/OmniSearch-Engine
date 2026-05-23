package com.v39a.omni.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabase() {

    val dbUrl = environment.config.property("storage.jdbcUrl").getString()
    val dbUser = environment.config.property("storage.username").getString()
    val dbPassword = environment.config.property("storage.password").getString()
    val dbDriver = environment.config.property("storage.driverClassName").getString()

    val config = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = dbUser
        password = dbPassword
        driverClassName = dbDriver
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    try {
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        environment.log.info("Database connected successfully!")
        // todo mock? later
//        transaction {
//            SchemaUtils.create(VideoTable)
//
//        }
    } catch (e: Exception) {
        environment.log.error("! Failed to connect to database: ${e.message}")
        throw e
    }

}