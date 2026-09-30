package com.systempulse.app.engine

import com.systempulse.app.data.model.BottleneckVerdict
import com.systempulse.app.data.model.BufferbloatGrade
import com.systempulse.app.data.model.QoERating
import com.systempulse.app.data.model.TestMetrics
import com.systempulse.app.data.model.WifiMetrics
import kotlin.math.max
import kotlin.math.min

class QoEEngine {

    fun evaluate(
        metrics: TestMetrics,
        wifiMetrics: WifiMetrics?
    ): QoERating {
        val down = metrics.downloadMbps
        val up = metrics.uploadMbps
        val ping = metrics.pingMs
        val jitter = metrics.jitterMs
        val loadedPing = metrics.loadedPingMs
        val gatewayPing = metrics.gatewayPingMs

        // 1. 4K Streaming (High bandwidth, tolerant of moderate ping, sensitive to heavy bufferbloat)
        val streamBandwidthScore = when {
            down >= 50.0 -> 100
            down >= 25.0 -> 90
            down >= 15.0 -> 75
            down >= 7.0 -> 50
            else -> (down * 7).toInt().coerceIn(10, 40)
        }
        val streamPingPenalty = if (loadedPing > 120.0) 25 else if (loadedPing > 60.0) 10 else 0
        val streamingScore = (streamBandwidthScore - streamPingPenalty).coerceIn(10, 100)

        // 2. Competitive Gaming (Extremely sensitive to ping, jitter, and bufferbloat; low bandwidth required)
        var gameScore = 100
        if (ping > 20.0) gameScore -= ((ping - 20) * 0.8).toInt()
        if (jitter > 3.0) gameScore -= ((jitter - 3) * 3.5).toInt()
        val bufferbloatDelta = max(0.0, loadedPing - ping)
        if (bufferbloatDelta > 15.0) gameScore -= ((bufferbloatDelta - 15) * 0.6).toInt()
        val gamingScore = gameScore.coerceIn(5, 100)

        // 3. Video Conferencing (Requires balanced upload, low jitter, moderate latency)
        var confScore = 100
        if (up < 5.0) confScore -= ((5.0 - up) * 12).toInt()
        if (down < 5.0) confScore -= ((5.0 - down) * 8).toInt()
        if (jitter > 10.0) confScore -= ((jitter - 10) * 2.5).toInt()
        if (ping > 80.0) confScore -= ((ping - 80) * 0.5).toInt()
        val videoConferenceScore = confScore.coerceIn(10, 100)

        // Overall QoE
        val overallScore = ((streamingScore * 0.35) + (gamingScore * 0.35) + (videoConferenceScore * 0.30)).toInt()

        // Bottleneck Isolation
        val (verdict, explanation) = isolateBottleneck(
            gatewayPing = gatewayPing,
            wanPing = ping,
            downMbps = down,
            loadedPing = loadedPing,
            wifiMetrics = wifiMetrics
        )

        return QoERating(
            streaming4kScore = streamingScore,
            gamingScore = gamingScore,
            videoConferenceScore = videoConferenceScore,
            overallScore = overallScore,
            bottleneckVerdict = verdict,
            bottleneckExplanation = explanation
        )
    }

    private fun isolateBottleneck(
        gatewayPing: Double,
        wanPing: Double,
        downMbps: Double,
        loadedPing: Double,
        wifiMetrics: WifiMetrics?
    ): Pair<BottleneckVerdict, String> {
        val rssi = wifiMetrics?.rssiDbm ?: -50
        val linkSpeed = wifiMetrics?.linkSpeedMbps ?: 100

        // Local Wi-Fi check
        if (gatewayPing > 20.0 || rssi < -78 || linkSpeed < 30) {
            val reason = when {
                rssi < -78 -> "Weak Wi-Fi signal (${rssi} dBm). Move closer to your router or switch to 5 GHz."
                gatewayPing > 20.0 -> "Local router latency is high (${gatewayPing} ms). Local network or router interference detected."
                else -> "Wi-Fi link speed is throttled to ${linkSpeed} Mbps."
            }
            return Pair(BottleneckVerdict.WIFI_BOTTLENECK, reason)
        }

        // Bufferbloat check
        val bufferbloatDelta = loadedPing - wanPing
        if (bufferbloatDelta > 70.0) {
            return Pair(
                BottleneckVerdict.BUFFERBLOAT_CONGESTION,
                "Router queue bufferbloat spike (+${bufferbloatDelta.toInt()} ms under load). Smart Queue Management (SQM/CAKE) recommended."
            )
        }

        // ISP check
        if (wanPing > 80.0 || downMbps < 15.0) {
            return Pair(
                BottleneckVerdict.ISP_BOTTLENECK,
                "Local Wi-Fi is fast, but ISP delivery speed or upstream routing is constrained."
            )
        }

        return Pair(BottleneckVerdict.HEALTHY, "Network is performing optimally with low latency across Wi-Fi and WAN.")
    }
}
