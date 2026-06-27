package com.snapreport.sdk.util

import android.util.Log

/**
 * Thin wrapper around [android.util.Log].
 *
 * All log calls are gated by [enabled] so that verbose output can be fully silenced
 * in production without removing log sites.  The SDK never logs API keys, JWTs, or
 * personally-identifiable information.
 */
internal object Logger {

    private const val PREFIX = "SnapReport"

    @Volatile
    var enabled: Boolean = false

    fun d(tag: String, msg: String) {
        if (enabled) Log.d("$PREFIX/$tag", msg)
    }

    fun i(tag: String, msg: String) {
        if (enabled) Log.i("$PREFIX/$tag", msg)
    }

    fun w(tag: String, msg: String) {
        if (enabled) Log.w("$PREFIX/$tag", msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        // Errors are always logged (without sensitive data) regardless of the enabled flag
        if (throwable != null) Log.e("$PREFIX/$tag", msg, throwable)
        else Log.e("$PREFIX/$tag", msg)
    }
}

