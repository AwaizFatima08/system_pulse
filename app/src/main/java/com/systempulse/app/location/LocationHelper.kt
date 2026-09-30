package com.systempulse.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.systempulse.app.data.model.GeoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationHelper(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): GeoLocation? = withContext(Dispatchers.IO) {
        try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            ).await() ?: fusedLocationClient.lastLocation.await()

            if (location != null) {
                // Round coordinates to ~0.005 degrees (~500m) to enforce user privacy
                val roundedLat = Math.round(location.latitude * 200.0) / 200.0
                val roundedLon = Math.round(location.longitude * 200.0) / 200.0
                val hash = GeohashHelper.encode(roundedLat, roundedLon, precision = 6)

                var city = ""
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // For modern APIs, synchronous fallback or standard getFromLocation
                        val addresses = geocoder.getFromLocation(roundedLat, roundedLon, 1)
                        city = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea ?: ""
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(roundedLat, roundedLon, 1)
                        city = addresses?.firstOrNull()?.locality ?: ""
                    }
                } catch (_: Exception) {}

                return@withContext GeoLocation(
                    latitude = roundedLat,
                    longitude = roundedLon,
                    geohash = hash,
                    city = city
                )
            }
        } catch (_: Exception) {}

        null
    }
}
