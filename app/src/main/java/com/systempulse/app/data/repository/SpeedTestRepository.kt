package com.systempulse.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.systempulse.app.data.model.GeoLocation
import com.systempulse.app.data.model.IspInfo
import com.systempulse.app.data.model.QoERating
import com.systempulse.app.data.model.SpeedTestResult
import com.systempulse.app.data.model.TestMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class SpeedTestRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("system_pulse_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _historyFlow = MutableStateFlow<List<SpeedTestResult>>(emptyList())
    val historyFlow: Flow<List<SpeedTestResult>> = _historyFlow.asStateFlow()

    private val _communityTestsFlow = MutableStateFlow<List<SpeedTestResult>>(emptyList())
    val communityTestsFlow: Flow<List<SpeedTestResult>> = _communityTestsFlow.asStateFlow()

    init {
        loadHistory()
        loadSampleCommunityMapData()
    }

    private fun loadHistory() {
        val json = prefs.getString(KEY_TEST_HISTORY, null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<SpeedTestResult>>() {}.type
            try {
                val list: List<SpeedTestResult> = gson.fromJson(json, type)
                _historyFlow.value = list.sortedByDescending { it.timestamp }
            } catch (_: Exception) {}
        }
    }

    suspend fun saveTest(result: SpeedTestResult) = withContext(Dispatchers.IO) {
        val current = _historyFlow.value.toMutableList()
        current.add(0, result)
        _historyFlow.value = current

        // Save to SharedPreferences
        val json = gson.toJson(current)
        prefs.edit().putString(KEY_TEST_HISTORY, json).apply()

        // Sync to community flow
        val community = _communityTestsFlow.value.toMutableList()
        community.add(0, result)
        _communityTestsFlow.value = community

        // Sync to Firebase Firestore if available
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("speed_tests")
                .document(result.testId)
                .set(result)
        } catch (_: Exception) {
            // Firebase not configured yet or offline; stored safely locally
        }
    }

    /**
     * Seeds initial crowd-sourced data points so the interactive map and comparison
     * immediately illustrate speed distributions across ISPs and regions.
     */
    private fun loadSampleCommunityMapData() {
        val samplePoints = listOf(
            SpeedTestResult(
                testId = "seed-1",
                timestamp = System.currentTimeMillis() - 3600_000,
                ispInfo = IspInfo(asn = "AS15169", ispName = "Google Fiber", ipPublic = "192.0.2.1", city = "Metro Area"),
                metrics = TestMetrics(downloadMbps = 450.2, uploadMbps = 320.8, pingMs = 12.0, jitterMs = 2.1, loadedPingMs = 16.0, bufferbloatGrade = "A", gatewayPingMs = 1.8),
                location = GeoLocation(latitude = 37.7749, longitude = -122.4194, geohash = "9q8yyk", city = "San Francisco")
            ),
            SpeedTestResult(
                testId = "seed-2",
                timestamp = System.currentTimeMillis() - 7200_000,
                ispInfo = IspInfo(asn = "AS7922", ispName = "Comcast XFINITY", ipPublic = "192.0.2.2", city = "Metro Area"),
                metrics = TestMetrics(downloadMbps = 280.5, uploadMbps = 24.1, pingMs = 22.0, jitterMs = 4.5, loadedPingMs = 65.0, bufferbloatGrade = "C", gatewayPingMs = 3.2),
                location = GeoLocation(latitude = 37.7833, longitude = -122.4167, geohash = "9q8yyx", city = "San Francisco")
            ),
            SpeedTestResult(
                testId = "seed-3",
                timestamp = System.currentTimeMillis() - 12000_000,
                ispInfo = IspInfo(asn = "AS14593", ispName = "Starlink Satellite", ipPublic = "192.0.2.3", city = "Suburbs"),
                metrics = TestMetrics(downloadMbps = 145.0, uploadMbps = 18.5, pingMs = 42.0, jitterMs = 8.2, loadedPingMs = 78.0, bufferbloatGrade = "B", gatewayPingMs = 2.1),
                location = GeoLocation(latitude = 37.7650, longitude = -122.4400, geohash = "9q8yvh", city = "Twin Peaks")
            ),
            SpeedTestResult(
                testId = "seed-4",
                timestamp = System.currentTimeMillis() - 15000_000,
                ispInfo = IspInfo(asn = "AS7018", ispName = "AT&T Fiber", ipPublic = "192.0.2.4", city = "Downtown"),
                metrics = TestMetrics(downloadMbps = 520.0, uploadMbps = 490.0, pingMs = 9.0, jitterMs = 1.4, loadedPingMs = 12.0, bufferbloatGrade = "A+", gatewayPingMs = 1.2),
                location = GeoLocation(latitude = 37.7900, longitude = -122.4000, geohash = "9q8yzn", city = "Financial District")
            )
        )
        _communityTestsFlow.value = samplePoints
    }

    companion object {
        private const val KEY_TEST_HISTORY = "key_test_history_json"
    }
}
