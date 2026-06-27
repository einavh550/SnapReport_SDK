package com.snapreport.sdk.config

/**
 * Immutable configuration object passed to [com.snapreport.sdk.SnapReportSdk.init].
 *
 * All fields have sensible defaults; only [apiKey] is required.
 *
 * @param apiKey           Project API key from the SnapReport portal (format: `sr_live_…`). **Required.**
 * @param baseUrl          Base URL of the SnapReport backend server.
 * @param environment      Environment hint (informational only; does not auto-configure [baseUrl]).
 * @param maxScreenshotWidthPx  Max width in pixels to which screenshots are scaled down.
 * @param screenshotQuality     JPEG/WebP quality 0–100.
 * @param maxScreenshotBytes    Target max compressed screenshot size in bytes.
 * @param enableScreenshotCapture  Whether to attach a screenshot to each report.
 * @param enableShakeTrigger       Whether shake-to-report is active (wired in M5).
 * @param shakeThreshold           Shake magnitude threshold in m/s² above gravity.
 * @param shakeCooldownMs          Min milliseconds between two shake events.
 * @param requireWifiForUpload     If true, uploads are deferred until Wi-Fi is available.
 * @param httpTimeoutSeconds       HTTP connect/read/write timeout in seconds.
 * @param debugLogging             Enable verbose logcat output. **Disable in production.**
 */
data class SnapReportConfig(
    val apiKey: String,
    val baseUrl: String = SnapReportEnvironment.DEFAULT_DEV_BASE_URL,
    val environment: SnapReportEnvironment = SnapReportEnvironment.DEVELOPMENT,
    val maxScreenshotWidthPx: Int = 1280,
    val screenshotQuality: Int = 80,
    val maxScreenshotBytes: Long = 512 * 1024L,
    val enableScreenshotCapture: Boolean = true,
    val enableShakeTrigger: Boolean = true,
    val shakeThreshold: Float = 12.0f,
    val shakeCooldownMs: Long = 3_000L,
    val requireWifiForUpload: Boolean = false,
    val httpTimeoutSeconds: Long = 30L,
    val debugLogging: Boolean = false,
) {
    init {
        require(apiKey.isNotBlank())           { "SnapReportConfig: apiKey must not be blank." }
        require(maxScreenshotWidthPx > 0)      { "SnapReportConfig: maxScreenshotWidthPx must be > 0." }
        require(screenshotQuality in 0..100)   { "SnapReportConfig: screenshotQuality must be 0-100." }
        require(maxScreenshotBytes > 0)        { "SnapReportConfig: maxScreenshotBytes must be > 0." }
        require(httpTimeoutSeconds > 0)        { "SnapReportConfig: httpTimeoutSeconds must be > 0." }
    }
}

