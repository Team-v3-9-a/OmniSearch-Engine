package com.v39a.omni.plugins

import com.v39a.omni.feature.video.infrastructure.VideoTable
import io.ktor.server.application.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

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
        transaction {
            // CREATE TABLE IF NOT EXISTS)
            SchemaUtils.create(VideoTable)

            // Сид базы данных моками
            if (VideoTable.selectAll().empty()) {

                // Mock-video 1
                VideoTable.insert {
                    it[id] = UUID.randomUUID()
                    it[fileName] = "funny_cats.mp4"
                    it[s3Path] = "videos/funny_cats.mp4"
                    it[status] = "READY"
                }

                // Mock-video 2
                VideoTable.insert {
                    it[id] = UUID.randomUUID()
                    it[fileName] = "ml_lecture.mp4"
                    it[s3Path] = "videos/ml_lecture.mp4"
                    it[status] = "PROCESSING"
                }

                environment.log.info("Database seeded with mock videos!")
            }
        }
    } catch (e: Exception) {
        environment.log.error("! Failed to connect to database: ${e.message}")
        throw e;
    }

}