package com.snapreport.sdk.capture

import android.app.Activity
import android.graphics.Bitmap

/**
 * Strategy interface for capturing a screenshot of the current foreground activity.
 */
interface ScreenshotCapture {
    /**
     * Captures a screenshot of [activity].
     *
     * @return The captured [Bitmap], or `null` if the view has zero size.
     * @throws ScreenshotBlockedException if the window has [android.view.WindowManager.LayoutParams.FLAG_SECURE] set.
     */
    suspend fun capture(activity: Activity): Bitmap?
}

/**
 * Thrown when screenshot capture is blocked because the window has FLAG_SECURE set.
 * The SDK catches this and records `screenshotBlockedReason = "FLAG_SECURE"` instead.
 */
class ScreenshotBlockedException(message: String) : Exception(message)

