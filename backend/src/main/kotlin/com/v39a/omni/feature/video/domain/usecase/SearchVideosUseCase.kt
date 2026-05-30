package com.v39a.omni.feature.video.domain.usecase

import com.v39a.omni.feature.video.domain.MLEngineClient
import com.v39a.omni.feature.video.domain.VideoRepository
import com.v39a.omni.feature.video.domain.VideoStatus
import com.v39a.omni.feature.video.domain.VideoStorage
import com.v39a.omni.feature.video.domain.VideoSearchResult
import com.v39a.omni.feature.video.domain.VideoSearchSegment
import java.util.UUID

class SearchVideosUseCase(
    private val mlEngineClient: MLEngineClient,
    private val videoRepository: VideoRepository,
    private val videoStorage: VideoStorage,
    private val externalUrl: String
) {
    suspend operator fun invoke(query: String): List<VideoSearchResult> {
        if (query.isBlank()) {
            return emptyList()
        }

        val mlResults = mlEngineClient.search(query)
        if (mlResults.isEmpty()) {
            return emptyList()
        }

        val videoIds = mlResults.mapNotNull {
            try {
                UUID.fromString(it.videoId)
            } catch (e: Exception) {
                null
            }
        }.toSet()

        if (videoIds.isEmpty()) {
            return emptyList()
        }

        val videos = videoRepository.getByIds(videoIds)
            .filter { it.status == VideoStatus.READY }
            .associateBy { it.id }

        val groupedSegments = LinkedHashMap<UUID, MutableList<VideoSearchSegment>>()
        val videoMaxScore = mutableMapOf<UUID, Double>()

        for (item in mlResults) {
            val uuid = try {
                UUID.fromString(item.videoId)
            } catch (e: Exception) {
                continue
            }

            val video = videos[uuid] ?: continue

            val currentMax = videoMaxScore[uuid] ?: 0.0
            if (item.score > currentMax) {
                videoMaxScore[uuid] = item.score
            }

            val segmentsList = groupedSegments.getOrPut(uuid) { mutableListOf() }
            segmentsList.add(
                VideoSearchSegment(
                    textSnippet = item.textSnippet ?: "",
                    startTime = item.startTime ?: 0.0,
                    endTime = item.endTime ?: 0.0
                )
            )
        }

        val results = mutableListOf<VideoSearchResult>()
        for ((videoId, segments) in groupedSegments) {
            val video = videos[videoId]!!
            val score = videoMaxScore[videoId] ?: 0.0

            val thumbnailUrl = if (video.thumbnailPath.isNotBlank()) {
                videoStorage.getPresignedUrl(video.thumbnailPath)
                    .replace("http://minio:9000", externalUrl)
            } else {
                ""
            }

            results.add(
                VideoSearchResult(
                    video = video,
                    score = score,
                    thumbnailUrl = thumbnailUrl,
                    segments = segments
                )
            )
        }

        return results
    }
}
