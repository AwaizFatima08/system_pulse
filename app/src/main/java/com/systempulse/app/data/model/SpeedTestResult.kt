package com.systempulse.app.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class SpeedTestResult(
    @SerializedName("test_id")
    val testId: String = UUID.randomUUID().toString(),

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @SerializedName("user_id")
    val userId: String = "anon",

    @SerializedName("isp_info")
    val ispInfo: IspInfo = IspInfo(),

    @SerializedName("metrics")
    val metrics: TestMetrics = TestMetrics(),

    @SerializedName("wifi_metrics")
    val wifiMetrics: WifiMetrics? = null,

    @SerializedName("location")
    val location: GeoLocation? = null,

    @SerializedName("qoe")
    val qoe: QoERating = QoERating()
)

data class IspInfo(
    @SerializedName("asn")
    val asn: String = "AS0000",

    @SerializedName("isp_name")
    val ispName: String = "Detecting ISP...",

    @SerializedName("ip_public")
    val ipPublic: String = "0.0.0.0",

    @SerializedName("city")
    val city: String = "",

    @SerializedName("country")
    val country: String = ""
)

data class TestMetrics(
    @SerializedName("download_mbps")
    val downloadMbps: Double = 0.0,

    @SerializedName("upload_mbps")
    val uploadMbps: Double = 0.0,

    @SerializedName("ping_ms")
    val pingMs: Double = 0.0,

    @SerializedName("jitter_ms")
    val jitterMs: Double = 0.0,

    @SerializedName("loaded_ping_ms")
    val loadedPingMs: Double = 0.0,

    @SerializedName("bufferbloat_grade")
    val bufferbloatGrade: String = "A",

    @SerializedName("gateway_ping_ms")
    val gatewayPingMs: Double = 0.0
)

data class WifiMetrics(
    @SerializedName("ssid")
    val ssid: String = "",

    @SerializedName("bssid")
    val bssid: String = "",

    @SerializedName("rssi_dbm")
    val rssiDbm: Int = 0,

    @SerializedName("frequency_mhz")
    val frequencyMhz: Int = 0,

    @SerializedName("frequency_band")
    val frequencyBand: String = "Wi-Fi", // "2.4 GHz", "5 GHz", "6 GHz"

    @SerializedName("link_speed_mbps")
    val linkSpeedMbps: Int = 0
)

data class GeoLocation(
    @SerializedName("latitude")
    val latitude: Double = 0.0,

    @SerializedName("longitude")
    val longitude: Double = 0.0,

    @SerializedName("geohash")
    val geohash: String = "",

    @SerializedName("city")
    val city: String = ""
)

data class QoERating(
    @SerializedName("streaming_4k")
    val streaming4kScore: Int = 0, // 0-100

    @SerializedName("gaming")
    val gamingScore: Int = 0, // 0-100

    @SerializedName("video_conference")
    val videoConferenceScore: Int = 0, // 0-100

    @SerializedName("overall_score")
    val overallScore: Int = 0, // 0-100

    @SerializedName("bottleneck_verdict")
    val bottleneckVerdict: BottleneckVerdict = BottleneckVerdict.HEALTHY,

    @SerializedName("bottleneck_explanation")
    val bottleneckExplanation: String = "Connection is healthy."
)

enum class BottleneckVerdict {
    HEALTHY,
    WIFI_BOTTLENECK,
    ISP_BOTTLENECK,
    BUFFERBLOAT_CONGESTION
}
