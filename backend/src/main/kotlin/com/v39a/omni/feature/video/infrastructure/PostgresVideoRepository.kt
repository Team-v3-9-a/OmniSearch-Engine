package com.v39a.omni.feature.video.infrastructure

import com.v39a.omni.core.util.nowUTC
import com.v39a.omni.feature.video.domain.UpdateVideoMetadataCommand
import com.v39a.omni.feature.video.domain.Video
import com.v39a.omni.feature.video.domain.VideoRepository
import com.v39a.omni.feature.video.domain.VideoStatus
import com.v39a.omni.feature.video.infrastructure.VideoTable.id
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PostgresVideoRepository : VideoRepository {
    override suspend fun create(video: Video): Unit = withContext(Dispatchers.IO) {
        transaction {
            VideoTable.insert {
                it[id] = video.id
                    // todo здесь я беру название файла из пути. Нужно уточнить,
                    //  нужно ли отдельное поле для названия, или этот вариант допустим
                it[fileName] = video.path.substringAfterLast("/")
                it[s3Path] = video.path
                it[status] = video.status!!.name
            }
        }
    }

    override suspend fun updateStatus(id: UUID, newStatus: VideoStatus): Unit = withContext(Dispatchers.IO) {
        transaction {
            VideoTable.update({ VideoTable.id eq id }) {
                it[status] = newStatus.name
                it[updatedAt] = nowUTC()
            }
        }
    }

    override suspend fun getById(id: UUID): Video? = withContext(Dispatchers.IO) {
        transaction {
            VideoTable.select(column = Expression.build { VideoTable.id eq id })
                .mapNotNull { toDomainModel(it) }
                .singleOrNull()
        }
    }

    override suspend fun patchVideo(videoId: UUID, command: UpdateVideoMetadataCommand) {
        transaction {
            VideoTable.update({ VideoTable.id eq videoId }) { statement ->

                command.status?.let { statement[status] = it.name }
                command.durationSeconds?.let { statement[durationSeconds] = it.toInt() }
                command.thumbnailPath?.let { statement[thumbnailPath] = it }

                statement[updatedAt] = nowUTC()
            }
        }
    }

    // маппинг строки БД в доменную модель
    private fun toDomainModel(row: ResultRow): Video {
        return Video(
            id = row[id],
            path = row[VideoTable.s3Path],
            title = row[VideoTable.title],
            thumbnailPath = row[VideoTable.thumbnailPath],
            durationSeconds = row[VideoTable.durationSeconds],
            createdAt = row[VideoTable.createdAt],
            status = VideoStatus.valueOf(row[VideoTable.status])
        )
    }
}