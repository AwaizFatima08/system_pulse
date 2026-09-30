package com.systempulse.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.systempulse.app.data.model.BottleneckVerdict
import com.systempulse.app.ui.theme.AmberWarning
import com.systempulse.app.ui.theme.CrimsonAlert
import com.systempulse.app.ui.theme.CyberGreen
import com.systempulse.app.ui.theme.DarkSurface
import com.systempulse.app.ui.theme.ElectricViolet
import com.systempulse.app.ui.theme.TextMuted
import com.systempulse.app.ui.theme.TextPrimary

@Composable
fun BottleneckBanner(
    verdict: BottleneckVerdict,
    explanation: String,
    modifier: Modifier = Modifier
) {
    val (title, icon, color) = when (verdict) {
        BottleneckVerdict.HEALTHY -> Triple("Clean Network Path", Icons.Default.CheckCircle, CyberGreen)
        BottleneckVerdict.WIFI_BOTTLENECK -> Triple("Local Wi-Fi Bottleneck", Icons.Default.WifiOff, AmberWarning)
        BottleneckVerdict.ISP_BOTTLENECK -> Triple("ISP WAN Constraint", Icons.Default.Router, CrimsonAlert)
        BottleneckVerdict.BUFFERBLOAT_CONGESTION -> Triple("Bufferbloat Congestion", Icons.Default.Warning, ElectricViolet)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = color,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = explanation,
                    color = TextPrimary.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
