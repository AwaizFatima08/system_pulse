package com.systempulse.app.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.systempulse.app.data.model.WifiMetrics
import java.net.Inet4Address
import java.net.InetAddress

class GatewayDetector(private val context: Context) {

    data class GatewayInfo(
        val gatewayIp: String?,
        val isWifi: Boolean,
        val isCellular: Boolean,
        val wifiMetrics: WifiMetrics?
    )

    fun detect(): GatewayInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)

        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        var gatewayIp: String? = null

        // 1. Try resolving gateway from LinkProperties
        if (activeNetwork != null && cm != null) {
            val linkProps = cm.getLinkProperties(activeNetwork)
            if (linkProps != null) {
                for (route in linkProps.routes) {
                    val gateway = route.gateway
                    if (gateway != null && !gateway.isAnyLocalAddress && route.isDefaultRoute) {
                        gatewayIp = gateway.hostAddress
                        break
                    }
                }
            }
        }

        // 2. Fallback to WifiManager DHCP info if Wi-Fi
        var wifiMetrics: WifiMetrics? = null
        if (isWifi) {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wm != null) {
                if (gatewayIp == null) {
                    val dhcp = wm.dhcpInfo
                    if (dhcp != null && dhcp.gateway != 0) {
                        gatewayIp = intToIp(dhcp.gateway)
                    }
                }

                val connInfo: WifiInfo? = wm.connectionInfo
                if (connInfo != null) {
                    val freq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) connInfo.frequency else 0
                    val band = when {
                        freq in 2400..2495 -> "2.4 GHz"
                        freq in 5150..5895 -> "5 GHz"
                        freq in 5925..7125 -> "6 GHz (Wi-Fi 6E/7)"
                        else -> "Wi-Fi"
                    }
                    val ssid = connInfo.ssid?.replace("\"", "") ?: "Connected Wi-Fi"

                    wifiMetrics = WifiMetrics(
                        ssid = if (ssid != "<unknown ssid>") ssid else "Wi-Fi Network",
                        bssid = connInfo.bssid ?: "",
                        rssiDbm = connInfo.rssi,
                        frequencyMhz = freq,
                        frequencyBand = band,
                        linkSpeedMbps = connInfo.linkSpeed
                    )
                }
            }
        }

        return GatewayInfo(
            gatewayIp = gatewayIp ?: if (isWifi) "192.168.1.1" else null,
            isWifi = isWifi,
            isCellular = isCellular,
            wifiMetrics = wifiMetrics
        )
    }

    private fun intToIp(i: Int): String {
        return (i and 0xFF).toString() + "." +
                (i shr 8 and 0xFF) + "." +
                (i shr 16 and 0xFF) + "." +
                (i shr 24 and 0xFF)
    }
}
