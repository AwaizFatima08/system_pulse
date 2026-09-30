package com.systempulse.app.data.model

enum class BufferbloatGrade(val label: String, val description: String) {
    A_PLUS("A+", "Negligible loaded latency (< 5ms increase). Outstanding."),
    A("A", "Minimal bufferbloat (< 15ms increase). Great for competitive gaming."),
    B("B", "Moderate loaded latency (< 30ms increase). Good for general usage."),
    C("C", "Noticeable latency spike (< 60ms increase). May cause lag in video calls."),
    D("D", "Severe latency spike (< 150ms increase). Streaming may buffer."),
    F("F", "Extreme bufferbloat (> 150ms increase). High packet queuing.");

    companion object {
        fun fromDelta(deltaMs: Double): BufferbloatGrade = when {
            deltaMs <= 5.0 -> A_PLUS
            deltaMs <= 15.0 -> A
            deltaMs <= 30.0 -> B
            deltaMs <= 60.0 -> C
            deltaMs <= 150.0 -> D
            else -> F
        }
    }
}
