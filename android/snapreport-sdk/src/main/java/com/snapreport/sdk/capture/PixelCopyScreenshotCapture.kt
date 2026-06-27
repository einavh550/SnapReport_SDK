package com.snapreport.sdk.capture

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.WindowManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Primary screenshot strategy using [PixelCopy] (API 26+).
 *
 * Captures hardware-accelerated window content accurately, including SurfaceView/TextureView.
 * Falls back to [ViewCanvasScreenshotCapture] if PixelCopy itself fails (but not on FLAG_SECURE).
 */
@RequiresApi(Build.VERSION_CODES.O)
class PixelCopyScreenshotCapture : ScreenshotCapture {

    private val canvasFallback = ViewCanvasScreenshotCapture()

    override suspend fun capture(activity: Activity): Bitmap? {
        // Enforce FLAG_SECURE — must throw so the SDK records the blocked reason
        if (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0) {
            throw ScreenshotBlockedException("Window has FLAG_SECURE set.")
        }

        return try {
            captureWithPixelCopy(activity)
        } catch (e: ScreenshotBlockedException) {
            throw e                           // Propagate — do NOT fall back
        } catch (_: Exception) {
            canvasFallback.capture(activity)  // PixelCopy failure → canvas fallback
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun captureWithPixelCopy(activity: Activity): Bitmap {
        val window = activity.window
        val decor  = window.decorView
        val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        val handler = Handler(Looper.getMainLooper())

        return suspendCancellableCoroutine { cont ->
            PixelCopy.request(window, bitmap, { result ->
                if (result == PixelCopy.SUCCESS) {
                    cont.resume(bitmap)
                } else {
                    cont.resumeWithException(RuntimeException("PixelCopy failed with code $result"))
                }
            }, handler)
        }
    }
}

