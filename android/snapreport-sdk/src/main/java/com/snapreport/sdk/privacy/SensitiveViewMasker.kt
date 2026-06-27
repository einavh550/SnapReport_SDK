package com.snapreport.sdk.privacy

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

/**
 * Traverses the view tree of the current activity and paints solid black rectangles
 * over any view that matches the configured privacy rules, obscuring sensitive data
 * in the captured screenshot.
 *
 * M4 skeleton — full tag/class matching and blur support is wired in M5.
 */
internal class SensitiveViewMasker(private val config: PrivacyConfig = PrivacyConfig()) {

    private val maskPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    /**
     * Draws mask rectangles over sensitive views onto [bitmap].
     *
     * @param rootView The root view of the activity window (decorView.rootView).
     * @param bitmap   The already-captured screenshot bitmap to mutate.
     */
    fun applyMasks(rootView: View, bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        collectSensitiveRects(rootView, canvas)
    }

    private fun collectSensitiveRects(view: View, canvas: Canvas) {
        if (isSensitive(view)) {
            val rect = getViewRect(view)
            canvas.drawRect(rect, maskPaint)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectSensitiveRects(view.getChildAt(i), canvas)
            }
        }
    }

    private fun isSensitive(view: View): Boolean {
        // Match by view tag string
        val tag = view.tag?.toString()
        if (tag != null && config.maskedViewTags.contains(tag)) return true
        // Match by simple class name
        val simpleName = view.javaClass.simpleName
        if (config.maskedViewClasses.contains(simpleName)) return true
        return false
    }

    private fun getViewRect(view: View): Rect {
        val pos = IntArray(2)
        view.getLocationOnScreen(pos)
        return Rect(pos[0], pos[1], pos[0] + view.width, pos[1] + view.height)
    }
}

