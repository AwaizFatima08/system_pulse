package com.systempulse.app.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

class BandwidthProber(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {

    // Fast global edge test endpoints (Cloudflare speed test chunks)
    private val downloadEndpoints = listOf(
        "https://speed.cloudflare.com/__down?bytes=25000000", // 25 MB
        "https://speed.cloudflare.com/__down?bytes=50000000", // 50 MB
        "https://speed.cloudflare.com/__down?bytes=25000000",
        "https://speed.cloudflare.com/__down?bytes=50000000"
    )

    private val uploadEndpoint = "https://speed.cloudflare.com/__up"

    data class ThroughputResult(
        val finalMbps: Double,
        val peakMbps: Double,
        val loadedPingMs: Double,
        val samples: List<Pair<Long, Double>>
    )

    /**
     * Executes multi-stream download test for [durationSeconds], streaming live progress via [onProgress].
     */
    suspend fun probeDownload(
        durationSeconds: Int = 10,
        onProgress: (currentMbps: Double, progressFraction: Float) -> Unit
    ): ThroughputResult = withContext(Dispatchers.IO) {
        val totalBytes = AtomicLong(0L)
        val isRunning = AtomicBoolean(true)
        val loadedPings = mutableListOf<Double>()
        val samples = mutableListOf<Pair<Long, Double>>()
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationSeconds * 1000L)
        var peakSpeed = 0.0

        coroutineScope {
            // Worker 1: Real-time speed sampling & ping prober
            val monitorJob = async {
                var lastBytes = 0L
                var lastTime = System.currentTimeMillis()

                while (isRunning.get() && System.currentTimeMillis() < endTime) {
                    delay(250)
                    val now = System.currentTimeMillis()
                    val currentTotalBytes = totalBytes.get()
                    val bytesDelta = currentTotalBytes - lastBytes
                    val timeDeltaSec = (now - lastTime) / 1000.0

                    if (timeDeltaSec > 0) {
                        val currentMbps = (bytesDelta * 8.0) / (timeDeltaSec * 1_000_000.0)
                        peakSpeed = max(peakSpeed, currentMbps)
                        val elapsed = now - startTime
                        val fraction = (elapsed.toFloat() / (durationSeconds * 1000f)).coerceIn(0f, 1f)

                        samples.add(Pair(now, currentMbps))
                        withContext(Dispatchers.Main) {
                            onProgress(currentMbps, fraction)
                        }
                    }

                    lastBytes = currentTotalBytes
                    lastTime = now

                    // Sample loaded latency (bufferbloat)
                    try {
                        val pingStart = System.nanoTime()
                        val socket = java.net.Socket()
                        socket.connect(java.net.InetSocketAddress("1.1.1.1", 53), 500)
                        val pingMs = (System.nanoTime() - pingStart) / 1_000_000.0
                        loadedPings.add(pingMs)
                        socket.close()
                    } catch (_: Exception) {}
                }
                isRunning.set(false)
            }

            // Workers 2-5: Parallel chunk download streams
            val downloadWorkers = (0 until 4).map { index ->
                async {
                    val url = downloadEndpoints[index % downloadEndpoints.size]
                    val buffer = ByteArray(32 * 1024)

                    while (isRunning.get() && System.currentTimeMillis() < endTime) {
                        try {
                            val request = Request.Builder()
                                .url(url)
                                .header("Cache-Control", "no-cache")
                                .build()

                            client.newCall(request).execute().use { response ->
                                val stream = response.body?.byteStream() ?: return@use
                                while (isRunning.get() && System.currentTimeMillis() < endTime) {
                                    val read = stream.read(buffer)
                                    if (read == -1) break
                                    totalBytes.addAndGet(read.toLong())
                                }
                            }
                        } catch (e: IOException) {
                            delay(100)
                        }
                    }
                }
            }

            // Wait for duration to elapse
            while (System.currentTimeMillis() < endTime && isActive) {
                delay(100)
            }
            isRunning.set(false)
            monitorJob.await()
            downloadWorkers.awaitAll()
        }

        val totalTimeSec = (System.currentTimeMillis() - startTime) / 1000.0
        val avgMbps = if (totalTimeSec > 0) {
            (totalBytes.get() * 8.0) / (totalTimeSec * 1_000_000.0)
        } else 0.0

        val avgLoadedPing = if (loadedPings.isNotEmpty()) loadedPings.average() else 0.0

        ThroughputResult(
            finalMbps = String.format("%.2f", avgMbps).toDouble(),
            peakMbps = String.format("%.2f", peakSpeed).toDouble(),
            loadedPingMs = String.format("%.1f", avgLoadedPing).toDouble(),
            samples = samples
        )
    }

    /**
     * Executes streaming upload test for [durationSeconds], streaming live progress via [onProgress].
     */
    suspend fun probeUpload(
        durationSeconds: Int = 8,
        onProgress: (currentMbps: Double, progressFraction: Float) -> Unit
    ): ThroughputResult = withContext(Dispatchers.IO) {
        val totalBytes = AtomicLong(0L)
        val isRunning = AtomicBoolean(true)
        val loadedPings = mutableListOf<Double>()
        val samples = mutableListOf<Pair<Long, Double>>()
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationSeconds * 1000L)
        var peakSpeed = 0.0

        coroutineScope {
            val monitorJob = async {
                var lastBytes = 0L
                var lastTime = System.currentTimeMillis()

                while (isRunning.get() && System.currentTimeMillis() < endTime) {
                    delay(250)
                    val now = System.currentTimeMillis()
                    val currentTotalBytes = totalBytes.get()
                    val bytesDelta = currentTotalBytes - lastBytes
                    val timeDeltaSec = (now - lastTime) / 1000.0

                    if (timeDeltaSec > 0) {
                        val currentMbps = (bytesDelta * 8.0) / (timeDeltaSec * 1_000_000.0)
                        peakSpeed = max(peakSpeed, currentMbps)
                        val elapsed = now - startTime
                        val fraction = (elapsed.toFloat() / (durationSeconds * 1000f)).coerceIn(0f, 1f)

                        samples.add(Pair(now, currentMbps))
                        withContext(Dispatchers.Main) {
                            onProgress(currentMbps, fraction)
                        }
                    }

                    lastBytes = currentTotalBytes
                    lastTime = now

                    try {
                        val pingStart = System.nanoTime()
                        val socket = java.net.Socket()
                        socket.connect(java.net.InetSocketAddress("1.1.1.1", 53), 500)
                        val pingMs = (System.nanoTime() - pingStart) / 1_000_000.0
                        loadedPings.add(pingMs)
                        socket.close()
                    } catch (_: Exception) {}
                }
                isRunning.set(false)
            }

            // Workers for parallel upload
            val uploadWorkers = (0 until 2).map {
                async {
                    val dummyChunk = ByteArray(64 * 1024)
                    while (isRunning.get() && System.currentTimeMillis() < endTime) {
                        try {
                            val requestBody = object : RequestBody() {
                                override fun contentType() = "application/octet-stream".toMediaType()
                                override fun contentLength() = 10L * 1024 * 1024 // 10MB chunk

                                override fun writeTo(sink: BufferedSink) {
                                    var written = 0L
                                    while (written < 10L * 1024 * 1024 && isRunning.get()) {
                                        sink.write(dummyChunk)
                                        written += dummyChunk.size
                                        totalBytes.addAndGet(dummyChunk.size.toLong())
                                    }
                                }
                            }

                            val request = Request.Builder()
                                .url(uploadEndpoint)
                                .post(requestBody)
                                .build()

                            client.newCall(request).execute().use { _ -> }
                        } catch (_: Exception) {
                            delay(100)
                        }
                    }
                }
            }

            while (System.currentTimeMillis() < endTime && isActive) {
                delay(100)
            }
            isRunning.set(false)
            monitorJob.await()
            uploadWorkers.awaitAll()
        }

        val totalTimeSec = (System.currentTimeMillis() - startTime) / 1000.0
        val avgMbps = if (totalTimeSec > 0) {
            (totalBytes.get() * 8.0) / (totalTimeSec * 1_000_000.0)
        } else 0.0

        val avgLoadedPing = if (loadedPings.isNotEmpty()) loadedPings.average() else 0.0

        ThroughputResult(
            finalMbps = String.format("%.2f", avgMbps).toDouble(),
            peakMbps = String.format("%.2f", peakSpeed).toDouble(),
            loadedPingMs = String.format("%.1f", avgLoadedPing).toDouble(),
            samples = samples
        )
    }
}
