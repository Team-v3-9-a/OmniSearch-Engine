package com.v39a.omni.feature.video.infrastructure

import com.v39a.omni.core.util.dbQuery
import com.v39a.omni.core.util.nowUTC
import com.v39a.omni.feature.video.domain.command.UpdateVideoMetadataCommand
import com.v39a.omni.feature.video.domain.Video
import com.v39a.omni.feature.video.domain.VideoRepository
import com.v39a.omni.feature.video.domain.VideoStatus
import com.v39a.omni.feature.video.infrastructure.VideoTable.id
import org.jetbrains.exposed.sql.*
import java.util.*

class PostgresVideoRepository : VideoRepository {
    override suspend fun create(video: Video): Unit = dbQuery {
        VideoTable.insert {
            it[id] = video.id
            it[fileName] = video.path.substringAfterLast("/")
            it[s3Path] = video.path
            it[status] = video.status?.name ?: VideoStatus.UPLOADED.name

            it[title] = video.title
            it[durationSeconds] = video.durationSeconds
            it[thumbnailPath] = video.thumbnailPath
            it[fps] = video.fps
            it[resolution] = video.resolution
        }
    }

    override suspend fun updateStatus(id: UUID, newStatus: VideoStatus): Unit = dbQuery {
        VideoTable.update({ VideoTable.id eq id }) {
            it[status] = newStatus.name
            it[updatedAt] = nowUTC()
        }
    }

    override suspend fun getById(id: UUID): Video? = dbQuery {
        VideoTable.selectAll()
            .where { VideoTable.id eq id }
            .mapNotNull { toDomainModel(it) }
            .singleOrNull()
    }

    override suspend fun patchVideo(videoId: UUID, command: UpdateVideoMetadataCommand): Unit = dbQuery {
        VideoTable.update({ id eq videoId }) { statement ->
            command.status?.let { statement[status] = it.name }
            command.durationSeconds?.let { statement[durationSeconds] = it.toInt() }
            command.thumbnailPath?.let { statement[thumbnailPath] = it }
            command.fps?.let { statement[fps] = it }
            command.resolution?.let { statement[resolution] = it }
            statement[updatedAt] = nowUTC()
        }
    }

    override suspend fun getAll(): List<Video> =  dbQuery {
        VideoTable
            .selectAll()
            .orderBy(VideoTable.createdAt to SortOrder.DESC)
            .map { toDomainModel(it) }
    }

    override suspend fun getByIds(ids: Collection<UUID>): List<Video> = dbQuery {
        if (ids.isEmpty()) emptyList()
        else {
            VideoTable.selectAll()
                .where { id inList ids }
                .map { toDomainModel(it) }
        }
    }

    // маппинг строки БД в доменную модель
    private fun toDomainModel(row: ResultRow): Video {
        val dbStatus = row[VideoTable.status]
        val status = VideoStatus.entries.find { it.name == dbStatus } ?: VideoStatus.UNKNOWN

        return Video(
            id = row[id],
            path = row[VideoTable.s3Path],
            title = row[VideoTable.title],
            thumbnailPath = row[VideoTable.thumbnailPath],
            durationSeconds = row[VideoTable.durationSeconds],
            createdAt = row[VideoTable.createdAt],
            status = status,
            updatedAt = row[VideoTable.updatedAt],
            fps = row[VideoTable.fps],
            resolution = row[VideoTable.resolution],
        )
    }
}