package com.systempulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.systempulse.app.data.model.BufferbloatGrade
import com.systempulse.app.ui.MainViewModel
import com.systempulse.app.ui.components.AdBannerView
import com.systempulse.app.ui.components.BottleneckBanner
import com.systempulse.app.ui.components.LiveBandwidthChart
import com.systempulse.app.ui.components.QoEGrid
import com.systempulse.app.ui.theme.AmberWarning
import com.systempulse.app.ui.theme.CrimsonAlert
import com.systempulse.app.ui.theme.CyberGreen
import com.systempulse.app.ui.theme.DarkBackground
import com.systempulse.app.ui.theme.DarkSurface
import com.systempulse.app.ui.theme.DarkSurfaceVariant
import com.systempulse.app.ui.theme.ElectricViolet
import com.systempulse.app.ui.theme.NeonCyan
import com.systempulse.app.ui.theme.TextMuted
import com.systempulse.app.ui.theme.TextPrimary

@Composable
fun AnalyticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val result = state.latestResult

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "DIAGNOSTIC ANALYTICS",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // 1. Throughput Chart Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Throughput Graph",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.downloadMbps > 0) "Peak: ${String.format("%.1f", state.downloadMbps)} Mbps" else "",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LiveBandwidthChart(samples = state.liveSamples)
                }
            }

            // 2. Wi-Fi vs. WAN Disambiguation Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Dual-Tier Latency Disambiguation",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Isolating local Wi-Fi router delay from upstream ISP delay",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LatencyBox(
                            title = "Local Gateway Ping",
                            value = "${state.gatewayPingMs.toInt()} ms",
                            subtext = "Wi-Fi Hop",
                            color = if (state.gatewayPingMs < 10) CyberGreen else AmberWarning,
                            modifier = Modifier.weight(1f)
                        )
                        LatencyBox(
                            title = "Remote WAN Ping",
                            value = "${state.wanIdlePingMs.toInt()} ms",
                            subtext = "ISP CDN Hop",
                            color = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )
                        LatencyBox(
                            title = "Jitter",
                            value = "±${state.jitterMs} ms",
                            subtext = "Variance",
                            color = if (state.jitterMs < 5) CyberGreen else CrimsonAlert,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Bufferbloat & Loaded Latency Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val grade = state.bufferbloatGrade
                    val gradeColor = when (grade) {
                        BufferbloatGrade.A_PLUS, BufferbloatGrade.A -> CyberGreen
                        BufferbloatGrade.B -> NeonCyan
                        BufferbloatGrade.C -> AmberWarning
                        BufferbloatGrade.D, BufferbloatGrade.F -> CrimsonAlert
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(gradeColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.5.dp, gradeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = grade.label,
                            color = gradeColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bufferbloat Rating: Grade ${grade.label}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = grade.description,
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // 4. Bottleneck Verdict Banner
            BottleneckBanner(
                verdict = state.qoeRating.bottleneckVerdict,
                explanation = state.qoeRating.bottleneckExplanation
            )

            // 5. QoE Scoring Grid
            Text(
                text = "QUALITY OF EXPERIENCE (QoE) INDICES",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            QoEGrid(
                streamingScore = state.qoeRating.streaming4kScore,
                gamingScore = state.qoeRating.gamingScore,
                videoCallScore = state.qoeRating.videoConferenceScore
            )
        }

        // 6. Bottom Adaptive AdMob Banner
        AdBannerView()
    }
}

@Composable
fun LatencyBox(
    title: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtext,
                color = TextMuted.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
        }
    }
}
