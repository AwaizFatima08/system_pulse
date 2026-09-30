# System Pulse (`system_pulse`)

**System Pulse** is a next-generation Android ISP speed tester, home network bottleneck diagnostician, and crowd-sourced ISP performance mapper built natively with **Kotlin** and **Jetpack Compose (Material 3)**.

Targeted for Google Play Store publication under package `com.systempulse.app`.

---

## ⚡ Key Capabilities

1. **Dual-Tier Network Disambiguation**:
   - **Local Gateway Ping**: Measures latency to local Wi-Fi router (default gateway) via socket inspection.
   - **Remote WAN Ping & Jitter**: Multi-sample ICMP/TCP RTT and RFC 3550 jitter calculation.
   - **Bottleneck Diagnosis**: Disambiguates whether performance degradation is caused by local Wi-Fi interference vs. ISP throttling.

2. **Loaded Latency & Bufferbloat Grading**:
   - Measures latency under full download/upload load to identify router queue bufferbloat.
   - Computes automated Bufferbloat Grades (**A+**, **A**, **B**, **C**, **D**, **F**).

3. **Quality of Experience (QoE) Scoring**:
   - **4K Ultra HD Streaming**: Real-time evaluation based on download throughput and loaded latency.
   - **Competitive Online Gaming**: Score penalizing latency spikes, packet jitter, and bufferbloat.
   - **Video Conferencing / VoIP**: Score evaluating symmetric upstream bandwidth and latency variance.

4. **Crowdsourced Geo-Tagging & Coverage Map**:
   - **500m Privacy Geohash Grid**: Rounds user coordinates to ~500m to preserve location privacy while indexing regional coverage.
   - **ISP Identification**: Automatically retrieves Public IP and ASN metadata.
   - **Interactive Google Map**: Displays regional speed distribution pins (Maps Compose).

5. **Monetization (Google AdMob SDK)**:
   - **Adaptive Banners**: Unobtrusive banner at the bottom of the Detailed Analytics screen.
   - **Capped Interstitials**: Frequency-capped (1 per session after 3 diagnostic runs).
   - **Rewarded Video Ads**: Unlocks historical competitor ISP outage maps.

---

## 🏗️ Architecture & Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.2.20 |
| **UI Framework** | Jetpack Compose + Material Design 3 (Compose BOM 2025.10.01) |
| **Network Engine** | OkHttp 4.12.0 + Native TCP Sockets |
| **Location & Maps** | Google Play Services Fused Location + Google Maps Compose 6.4.1 |
| **Monetization** | Google Mobile Ads SDK (AdMob 23.6.0) |
| **Backend & Sync** | Firebase Firestore (BOM 34.4.0) |
| **Architecture** | Clean Architecture / MVVM (`MainViewModel`, `StateFlow`, Repository Pattern) |

---

## 🚀 Building the Project

### Prerequisites
- JDK 17
- Android SDK (API 34/35/36)

### Compile & Assemble APK
```bash
# Build Debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 Google Play Store Release Checklist

1. **AdMob Live Units**: Replace test ad unit IDs in `AdManager.kt` and `AndroidManifest.xml` with your approved live AdMob unit IDs.
2. **Maps API Key**: Add your Google Maps Android API key in `local.properties`:
   ```properties
   MAPS_API_KEY=YOUR_KEY_HERE
   ```
3. **Firebase Services**: Add your production `google-services.json` inside the `app/` folder (the build script will automatically detect and apply the Google Services plugin).
4. **App Bundle (`.aab`)**:
   ```bash
   ./gradlew bundleRelease
   ```
