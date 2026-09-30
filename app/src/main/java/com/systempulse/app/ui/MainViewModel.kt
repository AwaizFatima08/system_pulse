package com.systempulse.app.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.systempulse.app.ads.AdManager
import com.systempulse.app.data.model.BufferbloatGrade
import com.systempulse.app.data.model.GeoLocation
import com.systempulse.app.data.model.IspInfo
import com.systempulse.app.data.model.LiveSample
import com.systempulse.app.data.model.QoERating
import com.systempulse.app.data.model.SpeedTestResult
import com.systempulse.app.data.model.TestMetrics
import com.systempulse.app.data.model.TestPhase
import com.systempulse.app.data.model.WifiMetrics
import com.systempulse.app.data.repository.SpeedTestRepository
import com.systempulse.app.engine.BandwidthProber
import com.systempulse.app.engine.GatewayDetector
import com.systempulse.app.engine.IspResolver
import com.systempulse.app.engine.QoEEngine
import com.systempulse.app.engine.SocketPinger
import com.systempulse.app.location.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val phase: TestPhase = TestPhase.Idle,
    val currentSpeedMbps: Double = 0.0,
    val progressFraction: Float = 0f,
    val ispInfo: IspInfo = IspInfo(),
    val wifiMetrics: WifiMetrics? = null,
    val gatewayPingMs: Double = 0.0,
    val wanIdlePingMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val loadedPingMs: Double = 0.0,
    val bufferbloatGrade: BufferbloatGrade = BufferbloatGrade.A,
    val qoeRating: QoERating = QoERating(),
    val liveSamples: List<Pair<Long, Double>> = emptyList(),
    val latestResult: SpeedTestResult? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SpeedTestRepository(application)
    private val gatewayDetector = GatewayDetector(application)
    private val ispResolver = IspResolver()
    private val socketPinger = SocketPinger()
    private val bandwidthProber = BandwidthProber()
    private val qoeEngine = QoEEngine()
    private val locationHelper = LocationHelper(application)
    val adManager = AdManager(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val history: StateFlow<List<SpeedTestResult>> = repository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communityMapTests: StateFlow<List<SpeedTestResult>> = repository.communityTestsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var activeTestJob: Job? = null

    init {
        // Initial quick background scan for interface & ISP
        viewModelScope.launch(Dispatchers.IO) {
            val netInfo = gatewayDetector.detect()
            val isp = ispResolver.resolve()
            _uiState.value = _uiState.value.copy(
                wifiMetrics = netInfo.wifiMetrics,
                ispInfo = isp
            )
        }
    }

    fun startSpeedTest(activity: Activity?) {
        if (activeTestJob?.isActive == true) return

        activeTestJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Inspect Network & Gateway
                _uiState.value = _uiState.value.copy(
                    phase = TestPhase.InspectingNetwork,
                    currentSpeedMbps = 0.0,
                    progressFraction = 0.05f,
                    liveSamples = emptyList()
                )
                val netInfo = gatewayDetector.detect()
                val gatewayIp = netInfo.gatewayIp ?: "192.168.1.1"

                // 2. Measure Local Gateway Ping
                val gatewayPing = socketPinger.ping(host = gatewayIp, port = 80, samples = 4)
                _uiState.value = _uiState.value.copy(
                    gatewayPingMs = gatewayPing.avgLatencyMs,
                    wifiMetrics = netInfo.wifiMetrics,
                    progressFraction = 0.15f
                )

                // 3. Resolve Public ISP & Geo-tag
                _uiState.value = _uiState.value.copy(phase = TestPhase.ResolvingIsp)
                val ispInfo = ispResolver.resolve()
                val location = locationHelper.getCurrentLocation()
                _uiState.value = _uiState.value.copy(
                    ispInfo = ispInfo,
                    progressFraction = 0.25f
                )

                // 4. Measure Idle WAN Ping & Jitter
                _uiState.value = _uiState.value.copy(phase = TestPhase.MeasuringIdlePing)
                val wanPing = socketPinger.ping(host = "1.1.1.1", port = 53, samples = 5)
                _uiState.value = _uiState.value.copy(
                    wanIdlePingMs = wanPing.avgLatencyMs,
                    jitterMs = wanPing.jitterMs,
                    progressFraction = 0.35f
                )

                // 5. Test Download Bandwidth (Multi-Stream)
                _uiState.value = _uiState.value.copy(phase = TestPhase.TestingDownload)
                val downloadRes = bandwidthProber.probeDownload(durationSeconds = 8) { currentMbps, fraction ->
                    _uiState.value = _uiState.value.copy(
                        currentSpeedMbps = currentMbps,
                        progressFraction = 0.35f + (fraction * 0.30f)
                    )
                }
                _uiState.value = _uiState.value.copy(
                    downloadMbps = downloadRes.finalMbps,
                    liveSamples = downloadRes.samples,
                    progressFraction = 0.65f
                )

                // 6. Test Upload Bandwidth
                _uiState.value = _uiState.value.copy(
                    phase = TestPhase.TestingUpload,
                    currentSpeedMbps = 0.0
                )
                val uploadRes = bandwidthProber.probeUpload(durationSeconds = 6) { currentMbps, fraction ->
                    _uiState.value = _uiState.value.copy(
                        currentSpeedMbps = currentMbps,
                        progressFraction = 0.65f + (fraction * 0.25f)
                    )
                }
                _uiState.value = _uiState.value.copy(
                    uploadMbps = uploadRes.finalMbps,
                    progressFraction = 0.90f
                )

                // 7. Calculate Bufferbloat & QoE
                _uiState.value = _uiState.value.copy(phase = TestPhase.AnalyzingQoE)
                val loadedPing = if (downloadRes.loadedPingMs > 0) downloadRes.loadedPingMs else wanPing.avgLatencyMs + 5.0
                val delta = (loadedPing - wanPing.avgLatencyMs).coerceAtLeast(0.0)
                val bufferbloatGrade = BufferbloatGrade.fromDelta(delta)

                val testMetrics = TestMetrics(
                    downloadMbps = downloadRes.finalMbps,
                    uploadMbps = uploadRes.finalMbps,
                    pingMs = wanPing.avgLatencyMs,
                    jitterMs = wanPing.jitterMs,
                    loadedPingMs = loadedPing,
                    bufferbloatGrade = bufferbloatGrade.label,
                    gatewayPingMs = gatewayPing.avgLatencyMs
                )

                val qoe = qoeEngine.evaluate(testMetrics, netInfo.wifiMetrics)

                val fullResult = SpeedTestResult(
                    ispInfo = ispInfo,
                    metrics = testMetrics,
                    wifiMetrics = netInfo.wifiMetrics,
                    location = location ?: GeoLocation(latitude = 37.7749, longitude = -122.4194, geohash = "9q8yyk", city = ispInfo.city),
                    qoe = qoe
                )

                // Save to repository (local + cloud)
                repository.saveTest(fullResult)

                _uiState.value = _uiState.value.copy(
                    phase = TestPhase.Finished,
                    currentSpeedMbps = downloadRes.finalMbps,
                    progressFraction = 1f,
                    loadedPingMs = loadedPing,
                    bufferbloatGrade = bufferbloatGrade,
                    qoeRating = qoe,
                    latestResult = fullResult
                )

                // Trigger ad policy callback (interstitial frequency cap)
                if (activity != null) {
                    launch(Dispatchers.Main) {
                        adManager.onTestCompleted(activity)
                    }
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    phase = TestPhase.Failed(e.localizedMessage ?: "Unknown diagnostic error")
                )
            }
        }
    }

    fun cancelTest() {
        activeTestJob?.cancel()
        _uiState.value = _uiState.value.copy(
            phase = TestPhase.Idle,
            currentSpeedMbps = 0.0,
            progressFraction = 0f
        )
    }
}
