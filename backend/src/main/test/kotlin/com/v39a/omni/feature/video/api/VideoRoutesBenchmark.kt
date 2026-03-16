package com.v39a.omni.feature.video.api

import com.v39a.omni.module
import io.ktor.client.request.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.system.measureTimeMillis

class VideoRoutesBenchmark {

    @Test
    fun benchmarkSearchEndpoint() = testApplication {
        application {
            module()
        }

        val warmupRounds = 100
        val benchmarkRounds = 1000

        // Warmup
        repeat(warmupRounds) {
            client.get("/api/v1/videos/search?query=test")
        }

        val time = measureTimeMillis {
            repeat(benchmarkRounds) {
                client.get("/api/v1/videos/search?query=test")
            }
        }

        println("BENCHMARK_RESULT: Average time per request: ${time.toDouble() / benchmarkRounds} ms")
    }
}