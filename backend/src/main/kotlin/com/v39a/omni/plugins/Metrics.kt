package com.v39a.omni.plugins

import com.v39a.omni.feature.video.domain.VideoStatus
import com.v39a.omni.feature.video.port.VideoRepository
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Gauge
import io.minio.MinioClient
import io.minio.ListObjectsArgs
import org.koin.ktor.ext.inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers

object MetricsManager {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val videosUploaded: Counter = Counter.builder("omnisearch_videos_uploaded_total")
        .description("Total videos uploaded")
        .register(registry)

    val searchRequests: Counter = Counter.builder("omnisearch_search_requests_total")
        .description("Total search requests")
        .register(registry)

    val pipelineDuration: Timer = Timer.builder("omnisearch_pipeline_duration_seconds")
        .description("Full video processing pipeline duration")
        .publishPercentiles(0.5, 0.95)
        .register(registry)

    val searchLatency: Timer = Timer.builder("omnisearch_search_latency_seconds")
        .description("Search latency in seconds")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry)

    val searchResultsCount: DistributionSummary = DistributionSummary.builder("omnisearch_search_results_count")
        .description("Number of search results returned")
        .register(registry)
}

fun Application.configureMetrics() {
    val videoRepository by inject<VideoRepository>()
    val minioClient by inject<MinioClient>()
    val config = environment.config
    val bucketName = config.propertyOrNull("minio.bucket")?.getString() ?: "videos"

    // Регистрация Gauge
    Gauge.builder("omnisearch_videos_active_count") {
        runBlocking {
            try {
                videoRepository.getAll().count { it.status == VideoStatus.READY }.toDouble()
            } catch (e: Exception) {
                0.0
            }
        }
    }.register(MetricsManager.registry)

    Gauge.builder("omnisearch_videos_processing_count") {
        runBlocking {
            try {
                videoRepository.getAll().count {
                    it.status != VideoStatus.READY && it.status != VideoStatus.ERROR && it.status != VideoStatus.UNKNOWN
                }.toDouble()
            } catch (e: Exception) {
                0.0
            }
        }
    }.register(MetricsManager.registry)

    Gauge.builder("omnisearch_storage_used_bytes") {
        runBlocking(Dispatchers.IO) {
            try {
                val objects = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).recursive(true).build()
                )
                var totalSize = 0L
                for (objResult in objects) {
                    try {
                        val item = objResult.get()
                        totalSize += item.size()
                    } catch (e: Exception) {
                        // ignore errors reading single items
                    }
                }
                totalSize.toDouble()
            } catch (e: Exception) {
                0.0
            }
        }
    }.register(MetricsManager.registry)

    install(MicrometerMetrics) {
        registry = MetricsManager.registry
    }

    routing {
        get("/metrics") {
            call.respondText(MetricsManager.registry.scrape())
        }
    }
}
