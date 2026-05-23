package com.v39a.omni.feature.video.infrastructure

import com.v39a.omni.core.util.nowUTC
import kotlin.uuid.ExperimentalUuidApi
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object VideoTable : Table("videos") {
    // Используем UUID в качестве первичного ключа
    @OptIn(ExperimentalUuidApi::class)
    val id = uuid("id")
    val fileName = varchar("file_name", 255)
    val title = varchar("title", 255)
    val thumbnailPath = varchar("thumbnail_path", 512)
    val durationSeconds = integer("duration_seconds")
    val s3Path = varchar("s3_path", 512)
    val status = varchar("status", 50)
    val createdAt = datetime("created_at").clientDefault { nowUTC()
    }
    val updatedAt = datetime("updated_at").clientDefault { nowUTC()
    }

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(id)
}