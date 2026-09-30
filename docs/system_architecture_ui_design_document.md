# Software Design Document (SDD)

## App Name: Network Pulse (Working Title)
**Platform:** Android (Mobile)  
**Monetization:** Google AdMob (Banners, Interstitials, Rewarded)  
**Core Purpose:** Next-generation ISP speed tester, home network bottleneck diagnostician, and crowd-sourced ISP performance mapper.

---

## 1. System Overview & Architecture

### 1.1 Architecture Blueprint
```
[ User Android Device ]
  ├── Local Sensor Engine (Ping, Traffic Inspection, Wi-Fi Signal)
  ├── GPS Location Service (Fused Location Provider API)
  ├── Ad Engine (AdMob SDK)
  └── UI Layer (Jetpack Compose / Material Design 3)
           │
           │ HTTPS / REST API
           ▼
[ Cloud Backend (Firebase / Supabase) ]
  ├── Firestore DB (Speed Tests, Geo-Coordinates, ISP Records)
  ├── Cloud Functions (Data Aggregation & Heatmap Generation)
  └── Analytics & BigQuery (ISP SLA & Coverage Performance)
```

---

## 2. Technical Toolchain & Libraries

| Layer | Recommended Technology | Reason |
| :--- | :--- | :--- |
| **Language / Framework** | Kotlin + Jetpack Compose | Native performance, modern declarative UI, efficient background tasks. |
| **Location Services** | Google Fused Location Provider API | High-accuracy GPS with minimal battery drain. |
| **Maps & Heatmaps** | Google Maps SDK for Android + Utility Library | Seamless heatmap layer rendering for regional ISP speeds. |
| **Backend & Database** | Firebase (Firestore + Cloud Functions) | Serverless, real-time database, scales automatically. |
| **Network Probing Engine** | `OkHttp3` + Native Sockets | Raw socket handling for precise ping, latency, and throughput measurement. |
| **Monetization** | Google AdMob SDK | Adaptive Banner Ads, Native In-Feed Ads, and Rewarded Video Ads. |
| **Data Visualization** | Vico / MPAndroidChart | Lightweight vector charts for live bandwidth and quality-of-experience (QoE) metrics. |

---

## 3. Core Feature Specification

### 3.1 Dual-Tier Diagnostic Speed Test
* **Wi-Fi vs. WAN Disambiguation:** Performs simultaneous ping/speed checks to the local gateway (router) and remote CDN servers to isolate Wi-Fi interference from ISP throttling.
* **QoE Scoring:** Computes instant usability indices for 4K Streaming, Online Gaming, and Video Conferencing based on jitter and loaded latency (Bufferbloat).

### 3.2 GPS Geo-Tagging & Crowdsourced ISP Mapping
* **Automatic ISP Identification:** Fetches Public IP and Autonomous System Number (ASN) to tag ISP names (e.g., Comcast, Starlink, Jio).
* **Location Indexing:** Coordinates (Latitude/Longitude) are rounded to a 500m geohash to protect user privacy while mapping regional speed variations.
* **Speed Heatmap:** Interactive map displaying crowd-sourced average speeds and reported outages by area and provider.

---

## 4. UI/UX Wireframe Structure

```
+-----------------------------------------------------------------------+
|                            APP STRUCTURE                              |
+-----------------------+-----------------------+-----------------------+
|  1. Home (Pulse)      |  2. Detailed Analytics|  3. ISP Coverage Map  |
|                       |                       |                       |
|  * Radial Speed Gauge |  * Live Bandwidth     |  * Interactive Map    |
|  * ISP & Location Tag |  * Loaded Ping/Jitter |  * Area Pin Heatmap   |
|  * Wi-Fi/WAN Toggle   |  * QoE Index Cards    |  * Area Search Filter |
|  * Diagnostic Button  |  * Ad Banner Area     |  * ISP Comparison     |
+-----------------------+-----------------------+-----------------------+
```

---

## 5. Ad Integration Strategy

1. **Adaptive Banner Ads:** Positioned unobtrusively at the bottom of the *Detailed Insights / Analytics* screen.
2. **Interstitial Ads:** Triggered conditionally after every 3 speed tests or upon exporting a PDF SLA report (capped at 1 per session).
3. **Rewarded Video Ads:** Allows users to unlock high-resolution historical coverage maps for specific competitor ISPs in their city.

---

## 6. Database Schema (Firestore)

### Collection: `speed_tests`
```json
{
  "test_id": "string",
  "timestamp": "ISO-8601",
  "user_id": "string",
  "isp_info": {
    "asn": "AS15169",
    "isp_name": "Google Fiber",
    "ip_public": "192.0.2.1"
  },
  "metrics": {
    "download_mbps": 450.2,
    "upload_mbps": 320.8,
    "ping_ms": 12,
    "jitter_ms": 2.1,
    "bufferbloat_grade": "A"
  },
  "location": {
    "latitude": 31.5204,
    "longitude": 74.3587,
    "geohash": "stq4x1"
  }
}
```