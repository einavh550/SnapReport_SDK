package com.example.snapreportdemo

import android.app.Application
import com.snapreport.sdk.SnapReportSdk
import com.snapreport.sdk.config.SnapReportConfig
import com.snapreport.sdk.config.SnapReportEnvironment

/**
 * Host application class.  Initialises the SnapReport SDK with development settings.
 *
 * The API key below is a placeholder — replace it with a real key generated via the portal.
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = SnapReportConfig(
            apiKey         = BuildConfig.SNAPREPORT_API_KEY.ifBlank { "sr_live_REPLACE_ME" },
            baseUrl        = "http://10.0.2.2:8000/",   // Android emulator → host machine
            environment    = SnapReportEnvironment.DEVELOPMENT,
            debugLogging   = BuildConfig.DEBUG,
            enableShakeTrigger = false,                  // Shake wired in M5
        )

        SnapReportSdk.init(this, config)

        // Optional: attach a demo user ID (never use real PII here)
        SnapReportSdk.setUserId("demo-user-001")
    }
}

