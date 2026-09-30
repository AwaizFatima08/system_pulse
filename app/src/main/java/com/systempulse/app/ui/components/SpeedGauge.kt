package com.systempulse.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.systempulse.app.ui.theme.CyberGreen
import com.systempulse.app.ui.theme.DarkSurfaceVariant
import com.systempulse.app.ui.theme.ElectricViolet
import com.systempulse.app.ui.theme.NeonCyan
import com.systempulse.app.ui.theme.TextMuted
import com.systempulse.app.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedGauge(
    speedMbps: Double,
    maxSpeedMbps: Double = 500.0,
    label: String = "DOWNLOAD",
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speedMbps.toFloat(),
        animationSpec = tween(durationMillis = 200),
        label = "gaugeSpeed"
    )

    // Gauge sweeps 240 degrees (from 150 deg to 390 deg)
    val startAngle = 150f
    val totalSweep = 240f
    val fraction = (animatedSpeed / maxSpeedMbps.toFloat()).coerceIn(0f, 1f)
    val currentSweep = totalSweep * fraction

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            val strokeWidth = 18.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

            // 1. Background Track Arc
            drawArc(
                color = DarkSurfaceVariant,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Glowing Gradient Active Arc
            val gradientBrush = Brush.sweepGradient(
                colors = listOf(NeonCyan, ElectricViolet, CyberGreen),
                center = center
            )

            if (currentSweep > 1f) {
                drawArc(
                    brush = gradientBrush,
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Tick Marks
            val tickRadius = diameter / 2f - 18.dp.toPx()
            val numTicks = 9
            for (i in 0 until numTicks) {
                val tickAngle = startAngle + (totalSweep * (i.toFloat() / (numTicks - 1)))
                val rad = tickAngle * (PI / 180.0)
                val tickStart = Offset(
                    center.x + (tickRadius * cos(rad)).toFloat(),
                    center.y + (tickRadius * sin(rad)).toFloat()
                )
                val tickEnd = Offset(
                    center.x + ((tickRadius - 8.dp.toPx()) * cos(rad)).toFloat(),
                    center.y + ((tickRadius - 8.dp.toPx()) * sin(rad)).toFloat()
                )
                drawLine(
                    color = if (i.toFloat() / (numTicks - 1) <= fraction) NeonCyan else TextMuted.copy(alpha = 0.5f),
                    start = tickStart,
                    end = tickEnd,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Center Digital Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(
                text = String.format("%.1f", animatedSpeed),
                color = TextPrimary,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Mbps",
                color = NeonCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label.uppercase(),
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            )
        }
    }
}
