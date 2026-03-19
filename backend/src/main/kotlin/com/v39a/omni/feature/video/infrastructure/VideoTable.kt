package com.v39a.omni.feature.video.infrastructure

import com.v39a.omni.core.util.nowUTC
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object VideoTable : Table("videos") {
    // Используем UUID в качестве первичного ключа
    @OptIn(ExperimentalUuidApi::class)
    val id = uuid("id")
    val fileName = varchar("file_name", 255)
    val s3Path = varchar("s3_path", 512)
    val status = varchar("status", 50)
    val createdAt = datetime("created_at").clientDefault { nowUTC()
    }
    val updatedAt = datetime("updated_at").clientDefault { nowUTC()
    }

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(id)
}