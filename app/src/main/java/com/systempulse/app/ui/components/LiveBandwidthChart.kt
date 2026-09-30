package com.systempulse.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.systempulse.app.ui.theme.DarkSurfaceVariant
import com.systempulse.app.ui.theme.NeonCyan
import com.systempulse.app.ui.theme.TextMuted
import kotlin.math.max

@Composable
fun LiveBandwidthChart(
    samples: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (samples.isEmpty()) {
            Text(
                text = "Live throughput graph will plot here...",
                color = TextMuted,
                fontSize = 12.sp
            )
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val maxSpeed = max(10.0, (samples.maxOfOrNull { it.second } ?: 10.0) * 1.15)
            val minTime = samples.first().first
            val maxTime = max(minTime + 1, samples.last().first)
            val timeRange = (maxTime - minTime).toDouble()

            // Horizontal Grid Lines
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = height * (i.toFloat() / gridLines)
                drawLine(
                    color = DarkSurfaceVariant.copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val path = Path()
            val fillPath = Path()

            samples.forEachIndexed { index, (time, speed) ->
                val x = if (timeRange > 0) {
                    ((time - minTime) / timeRange * width).toFloat()
                } else {
                    width * (index.toFloat() / samples.size)
                }
                val y = (height - (speed / maxSpeed * height)).toFloat().coerceIn(0f, height)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            // Draw area gradient fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.35f),
                        NeonCyan.copy(alpha = 0.0f)
                    )
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = NeonCyan,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
