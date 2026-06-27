package com.snapreport.sdk.capture

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.WindowManager

/**
 * Fallback screenshot strategy using [Canvas] drawing.
 *
 * Works on all API levels ≥ 24 (our minSdk). Used automatically on API < 26 and as
 * the PixelCopy fallback. Does **not** capture SurfaceView/TextureView content.
 */
class ViewCanvasScreenshotCapture : ScreenshotCapture {

    override suspend fun capture(activity: Activity): Bitmap? {
        // Enforce FLAG_SECURE
        if (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0) {
            throw ScreenshotBlockedException("Window has FLAG_SECURE set.")
        }

        val rootView = activity.window.decorView.rootView
        if (rootView.width <= 0 || rootView.height <= 0) return null

        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        rootView.draw(Canvas(bitmap))
        return bitmap
    }
}

