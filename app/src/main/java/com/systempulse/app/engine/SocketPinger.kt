package com.systempulse.app.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs

class SocketPinger {

    data class PingResult(
        val avgLatencyMs: Double,
        val minLatencyMs: Double,
        val maxLatencyMs: Double,
        val jitterMs: Double,
        val successCount: Int,
        val totalCount: Int
    )

    suspend fun ping(
        host: String,
        port: Int = 80,
        samples: Int = 6,
        timeoutMs: Int = 1500
    ): PingResult = withContext(Dispatchers.IO) {
        val latencies = mutableListOf<Double>()

        for (i in 0 until samples) {
            val startNs = System.nanoTime()
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val durationMs = (System.nanoTime() - startNs) / 1_000_000.0
                latencies.add(durationMs)
            } catch (e: Exception) {
                // If port 80 fails on local gateway, try port 53 (DNS)
                if (port == 80) {
                    try {
                        val dnsStart = System.nanoTime()
                        val dnsSocket = Socket()
                        dnsSocket.connect(InetSocketAddress(host, 53), timeoutMs)
                        val dur = (System.nanoTime() - dnsStart) / 1_000_000.0
                        latencies.add(dur)
                        dnsSocket.close()
                    } catch (_: Exception) {
                        // Socket unreachable
                    }
                }
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }

        if (latencies.isEmpty()) {
            return@withContext PingResult(
                avgLatencyMs = 999.0,
                minLatencyMs = 999.0,
                maxLatencyMs = 999.0,
                jitterMs = 0.0,
                successCount = 0,
                totalCount = samples
            )
        }

        val avg = latencies.average()
        val min = latencies.minOrNull() ?: avg
        val max = latencies.maxOrNull() ?: avg

        // Jitter: RFC 3550 / mean consecutive difference
        var jitterSum = 0.0
        if (latencies.size > 1) {
            for (i in 1 until latencies.size) {
                jitterSum += abs(latencies[i] - latencies[i - 1])
            }
            jitterSum /= (latencies.size - 1)
        }

        PingResult(
            avgLatencyMs = String.format("%.1f", avg).toDouble(),
            minLatencyMs = String.format("%.1f", min).toDouble(),
            maxLatencyMs = String.format("%.1f", max).toDouble(),
            jitterMs = String.format("%.1f", jitterSum).toDouble(),
            successCount = latencies.size,
            totalCount = samples
        )
    }
}
