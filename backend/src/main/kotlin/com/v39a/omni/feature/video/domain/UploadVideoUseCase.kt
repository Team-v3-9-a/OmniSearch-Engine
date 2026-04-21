package com.v39a.omni.feature.video.domain

import com.v39a.omni.core.util.nowUTC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

data class UploadVideoCommand(
    val fileName: String,
    val contentType: String,
    val title: String,
    val thumbnailPath: String,
    val durationSeconds: Int,
    val contentStream: InputStream
)

class UploadVideoUseCase(
    private val videoRepository: VideoRepository,
    private val videoStorage: VideoStorage,
) {
    suspend fun execute(command: UploadVideoCommand): Video = withContext(Dispatchers.IO) {
        val videoId = UUID.randomUUID()

        // Названия файлов не обсуждались, для избежания коллизий добавляю id в название
        val s3Path = videoStorage.upload(
            fileName = "${videoId}_${command.fileName}",
            stream = command.contentStream,
            contentType = command.contentType
        )

        // Доменная сущность
        val video = Video(
            id = videoId,
            path = s3Path,
            title = command.title,
            durationSeconds = command.durationSeconds,
            createdAt = nowUTC(),
            thumbnailPath = command.thumbnailPath,
            status = VideoStatus.UPLOADED
        )

        videoRepository.create(video)

        video
    }
}