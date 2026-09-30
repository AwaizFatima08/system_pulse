package com.systempulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.systempulse.app.ui.theme.AmberWarning
import com.systempulse.app.ui.theme.CrimsonAlert
import com.systempulse.app.ui.theme.CyberGreen
import com.systempulse.app.ui.theme.DarkSurface
import com.systempulse.app.ui.theme.NeonCyan
import com.systempulse.app.ui.theme.TextMuted
import com.systempulse.app.ui.theme.TextPrimary

@Composable
fun QoEGrid(
    streamingScore: Int,
    gamingScore: Int,
    videoCallScore: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QoEItemCard(
            title = "4K Ultra HD Streaming",
            subtitle = "Bitrate stability, zero buffering bufferbloat headroom",
            score = streamingScore,
            icon = Icons.Default.Tv
        )
        QoEItemCard(
            title = "Low-Latency Online Gaming",
            subtitle = "Minimal jitter, loaded latency spike resilience",
            score = gamingScore,
            icon = Icons.Default.Gamepad
        )
        QoEItemCard(
            title = "Video Conferencing & VoIP",
            subtitle = "Symmetric upstream throughput and low packet jitter",
            score = videoCallScore,
            icon = Icons.Default.VideoCall
        )
    }
}

@Composable
fun QoEItemCard(
    title: String,
    subtitle: String,
    score: Int,
    icon: ImageVector
) {
    val (statusLabel, statusColor) = when {
        score >= 90 -> "EXCELLENT" to CyberGreen
        score >= 75 -> "GREAT" to NeonCyan
        score >= 50 -> "FAIR" to AmberWarning
        else -> "POOR" to CrimsonAlert
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(statusColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$score / 100",
                    color = statusColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
