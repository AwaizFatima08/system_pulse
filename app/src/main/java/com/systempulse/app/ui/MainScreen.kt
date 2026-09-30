package com.systempulse.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.systempulse.app.ui.screens.AnalyticsScreen
import com.systempulse.app.ui.screens.CoverageMapScreen
import com.systempulse.app.ui.screens.HomeScreen
import com.systempulse.app.ui.theme.DarkBackground
import com.systempulse.app.ui.theme.DarkSurface
import com.systempulse.app.ui.theme.NeonCyan
import com.systempulse.app.ui.theme.TextMuted
import com.systempulse.app.ui.theme.TextPrimary

enum class AppDestination(val title: String, val icon: ImageVector) {
    HOME("Pulse", Icons.Default.Speed),
    ANALYTICS("Analytics", Icons.Default.Analytics),
    MAP("Coverage", Icons.Default.Map)
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val destinations = AppDestination.values()

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                destinations.forEachIndexed { index, dest ->
                    val selected = selectedIndex == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedIndex = index },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.title,
                                tint = if (selected) NeonCyan else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = dest.title,
                                fontSize = 11.sp,
                                color = if (selected) NeonCyan else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NeonCyan.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedIndex) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAnalytics = { selectedIndex = 1 }
                )
                1 -> AnalyticsScreen(viewModel = viewModel)
                2 -> CoverageMapScreen(viewModel = viewModel)
            }
        }
    }
}
