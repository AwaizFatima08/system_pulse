package com.systempulse.app

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds

class SystemPulseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Google Mobile Ads SDK on a background thread
        try {
            MobileAds.initialize(this) { status ->
                Log.d("SystemPulseApp", "MobileAds initialized: $status")
            }
        } catch (e: Exception) {
            Log.w("SystemPulseApp", "MobileAds init deferred: ${e.message}")
        }
    }

    companion object {
        lateinit var instance: SystemPulseApp
            private set
    }
}
