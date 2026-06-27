package com.snapreport.sdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.snapreport.sdk.capture.PixelCopyScreenshotCapture
import com.snapreport.sdk.capture.ScreenshotBlockedException
import com.snapreport.sdk.capture.ScreenshotCapture
import com.snapreport.sdk.capture.ViewCanvasScreenshotCapture
import com.snapreport.sdk.config.SnapReportConfig
import com.snapreport.sdk.metadata.MetadataCollector
import com.snapreport.sdk.model.ReportDraft
import com.snapreport.sdk.model.UploadResult
import com.snapreport.sdk.network.NetworkClientFactory
import com.snapreport.sdk.network.ReportUploader
import com.snapreport.sdk.privacy.PrivacyConfig
import com.snapreport.sdk.privacy.SensitiveViewMasker
import com.snapreport.sdk.util.ImageCompressor
import com.snapreport.sdk.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

private const val TAG = "SnapReportSdk"

/**
 * # SnapReport SDK
 *
 * Public singleton entry-point.  Usage:
 *
 * ```kotlin
 * // In Application.onCreate():
 * SnapReportSdk.init(this, SnapReportConfig(apiKey = "sr_live_…"))
 *
 * // Anywhere in the app:
 * SnapReportSdk.triggerReport(description = "Button does nothing")
 * ```
 *
 * The SDK is designed to **never crash** the host application.  Every entry-point
 * swallows all exceptions, logs them, and returns silently.
 */
object SnapReportSdk {

    // ── Internal state ────────────────────────────────────────────────────────

    @Volatile private var config: SnapReportConfig? = null
    @Volatile private var userId: String? = null
    @Volatile private var initialized = false

    private val sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Weak reference to the currently-resumed foreground Activity
    private var currentActivityRef: WeakReference<Activity> = WeakReference(null)

    private lateinit var metadataCollector: MetadataCollector
    private lateinit var imageCompressor: ImageCompressor
    private lateinit var reportUploader: ReportUploader
    private lateinit var sensitiveViewMasker: SensitiveViewMasker
    private lateinit var screenshotCapture: ScreenshotCapture

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initialises the SDK.  Must be called from [Application.onCreate] before
     * any other SDK method.  Safe to call multiple times — subsequent calls are
     * ignored.
     *
     * @param application The host [Application] instance.
     * @param config      Immutable SDK configuration.
     * @param privacyConfig  Optional privacy masking rules.
     */
    @JvmStatic
    fun init(
        application: Application,
        config: SnapReportConfig,
        privacyConfig: PrivacyConfig = PrivacyConfig(),
    ) {
        if (initialized) {
            Logger.w(TAG, "init() called more than once — ignoring subsequent call.")
            return
        }
        try {
            this.config = config
            Logger.enabled = config.debugLogging
            Logger.i(TAG, "Initializing SnapReport SDK (env=${config.environment}, baseUrl=${config.baseUrl})")

            metadataCollector  = MetadataCollector(application)
            imageCompressor    = ImageCompressor(
                maxWidthPx    = config.maxScreenshotWidthPx,
                quality       = config.screenshotQuality,
                maxSizeBytes  = config.maxScreenshotBytes,
                cacheDir      = application.cacheDir,
            )
            sensitiveViewMasker = SensitiveViewMasker(privacyConfig)
            screenshotCapture   = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                      PixelCopyScreenshotCapture()
                                  else
                                      ViewCanvasScreenshotCapture()

            val api          = NetworkClientFactory.createApi(config)
            reportUploader   = ReportUploader(api, config.apiKey)

            // Register lifecycle callbacks to track the foreground Activity
            application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)

            initialized = true
            Logger.i(TAG, "SnapReport SDK initialized successfully.")
        } catch (e: Exception) {
            Logger.e(TAG, "SDK initialization failed", e)
        }
    }

    /**
     * Sets an anonymous user identifier that will be attached to every subsequent report.
     * Pass `null` to clear the stored user ID.
     *
     * The value is stored in memory only — it is never persisted to disk.
     *
     * @param id An opaque, non-personal identifier such as a database row UUID.
     *           Must not be an email address or other PII.
     */
    @JvmStatic
    fun setUserId(id: String?) {
        userId = sanitizeUserId(id)
        Logger.d(TAG, "userId updated")
    }

    /**
     * Manually triggers a bug report.
     *
     * Captures a screenshot of the current foreground activity, collects device/app
     * metadata, compresses the image, and uploads the report to the SnapReport backend.
     *
     * The entire pipeline runs on [Dispatchers.IO]; this method returns immediately and
     * will never block the calling thread.
     *
     * @param description Optional text description provided by the user.
     */
    @JvmStatic
    fun triggerReport(description: String? = null) {
        if (!assertInitialized()) return
        sdkScope.launch {
            triggerReportInternal(description)
        }
    }

    /**
     * Enables or disables verbose logcat output at runtime.
     * **Always disabled in release builds.**
     */
    @JvmStatic
    fun enableDebugLogging(enable: Boolean) {
        Logger.enabled = enable
    }

    /**
     * Shuts down the SDK and releases resources.
     * After calling this the SDK cannot be re-initialized within the same process.
     */
    @JvmStatic
    fun shutdown() {
        try {
            initialized = false
            currentActivityRef.clear()
            Logger.i(TAG, "SnapReport SDK shut down.")
        } catch (e: Exception) {
            Logger.e(TAG, "Error during shutdown", e)
        }
    }

    // ── Internal pipeline ─────────────────────────────────────────────────────

    /**
     * Full report pipeline: capture → compress → mask → build draft → upload.
     * All errors are swallowed so the host app is never affected.
     */
    internal suspend fun triggerReportInternal(description: String?) {
        val cfg = config ?: return
        try {
            Logger.d(TAG, "triggerReport: starting pipeline")

            val deviceMeta = metadataCollector.collectDeviceMetadata()
            val appMeta    = metadataCollector.collectAppMetadata()

            // Screenshot capture (main thread required for PixelCopy)
            var screenshotFile: java.io.File? = null
            var blockedReason: String?         = null

            if (cfg.enableScreenshotCapture) {
                val activity = currentActivityRef.get()
                if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                    try {
                        val bitmap = withContext(Dispatchers.Main) {
                            screenshotCapture.capture(activity)
                        }
                        if (bitmap != null) {
                            // Apply privacy masks and compress (back on IO thread)
                            val rootView = activity.window.decorView.rootView
                            withContext(Dispatchers.Main) {
                                sensitiveViewMasker.applyMasks(rootView, bitmap)
                            }
                            screenshotFile = imageCompressor.compress(bitmap)
                            bitmap.recycle()
                        }
                    } catch (e: ScreenshotBlockedException) {
                        blockedReason = "FLAG_SECURE"
                        Logger.d(TAG, "Screenshot blocked: ${e.message}")
                    } catch (e: Exception) {
                        Logger.w(TAG, "Screenshot capture failed: ${e.message}")
                    }
                } else {
                    Logger.d(TAG, "No foreground activity available for screenshot.")
                }
            }

            val draft = ReportDraft(
                userId                  = userId,
                description             = description,
                deviceMetadata          = deviceMeta,
                appMetadata             = appMeta,
                screenshotFile          = screenshotFile,
                screenshotBlockedReason = blockedReason,
            )

            // Upload immediately — offline queue added in M5
            val result = reportUploader.upload(draft)

            // Clean up temp file regardless of outcome
            screenshotFile?.delete()

            when (result) {
                is UploadResult.Success ->
                    Logger.i(TAG, "Report submitted — ticketId=${result.ticketId}")
                is UploadResult.Failure ->
                    Logger.w(TAG, "Report upload failed (retryable=${result.retryable}): ${result.message}")
            }
        } catch (e: Exception) {
            // Absolute safety net — SDK must never crash the host app
            Logger.e(TAG, "Unexpected error in report pipeline", e)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun assertInitialized(): Boolean {
        if (!initialized) {
            Logger.e(TAG, "SnapReportSdk.init() has not been called. Report skipped.")
            return false
        }
        return true
    }

    /** Strips leading/trailing whitespace; returns null for blank or excessively long IDs. */
    private fun sanitizeUserId(id: String?): String? {
        if (id == null) return null
        val trimmed = id.trim()
        return if (trimmed.isBlank() || trimmed.length > 256) null else trimmed
    }

    // ── Lifecycle tracking ────────────────────────────────────────────────────

    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity)  { currentActivityRef = WeakReference(activity) }
        override fun onActivityPaused(activity: Activity)   { if (currentActivityRef.get() === activity) currentActivityRef.clear() }
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) { if (currentActivityRef.get() === activity) currentActivityRef.clear() }
    }
}

