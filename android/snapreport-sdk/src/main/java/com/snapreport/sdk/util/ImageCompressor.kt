package com.snapreport.sdk.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream
import java.io.File

private const val TAG = "ImageCompressor"

/**
 * Scales and compresses a [Bitmap] to satisfy the size constraints defined in
 * [com.snapreport.sdk.config.SnapReportConfig].
 *
 * The compressed bytes are written to a temp file inside [cacheDir].
 */
internal class ImageCompressor(
    private val maxWidthPx: Int,
    private val quality: Int,
    private val maxSizeBytes: Long,
    private val cacheDir: File,
) {

    /**
     * Scales [bitmap] to at most [maxWidthPx] wide (preserving aspect ratio),
     * encodes it as JPEG at [quality], and iteratively lowers quality until the
     * result fits within [maxSizeBytes].
     *
     * @return A temp [File] containing the compressed JPEG bytes.
     */
    fun compress(bitmap: Bitmap): File {
        val scaled = scaleBitmap(bitmap)
        val bytes  = encodeToBytes(scaled, quality, maxSizeBytes)

        val outFile = File.createTempFile("sr_screenshot_", ".jpg", cacheDir)
        outFile.writeBytes(bytes)
        Logger.d(TAG, "Compressed screenshot: ${bytes.size / 1024} KB  (${scaled.width}×${scaled.height})")
        return outFile
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun scaleBitmap(src: Bitmap): Bitmap {
        if (src.width <= maxWidthPx) return src
        val scale  = maxWidthPx.toFloat() / src.width
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    private fun encodeToBytes(bitmap: Bitmap, initialQuality: Int, maxBytes: Long): ByteArray {
        var q = initialQuality.coerceIn(10, 100)
        var out: ByteArrayOutputStream
        while (true) {
            out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, q, out)
            val bytes = out.toByteArray()
            if (bytes.size <= maxBytes || q <= 10) return bytes
            q = (q - 10).coerceAtLeast(10)
        }
    }

    companion object {
        /** Re-reads a JPEG file back into a [Bitmap] — used for testing/preview. */
        fun decode(file: File): Bitmap? =
            BitmapFactory.decodeFile(file.absolutePath)
    }
}

