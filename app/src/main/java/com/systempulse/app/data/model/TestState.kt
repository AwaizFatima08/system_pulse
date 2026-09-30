package com.systempulse.app.data.model

sealed class TestPhase(val displayName: String) {
    data object Idle : TestPhase("Ready")
    data object InspectingNetwork : TestPhase("Analyzing Interface & Gateway...")
    data object ResolvingIsp : TestPhase("Identifying ISP & Geo-tag...")
    data object MeasuringIdlePing : TestPhase("Measuring Idle Latency & Jitter...")
    data object TestingDownload : TestPhase("Testing Download Throughput...")
    data object TestingUpload : TestPhase("Testing Upload Throughput...")
    data object TestingBufferbloat : TestPhase("Diagnosing Loaded Bufferbloat...")
    data object AnalyzingQoE : TestPhase("Computing QoE Scores...")
    data object Finished : TestPhase("Diagnostic Complete")
    data class Failed(val error: String) : TestPhase("Test Failed")
}

data class LiveSample(
    val timestampMs: Long,
    val speedMbps: Double,
    val phase: String
)
