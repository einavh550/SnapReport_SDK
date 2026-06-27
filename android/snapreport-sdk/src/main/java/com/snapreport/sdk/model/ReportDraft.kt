package com.snapreport.sdk.model

import com.snapreport.sdk.metadata.AppMetadata
import com.snapreport.sdk.metadata.DeviceMetadata
import java.io.File

/**
 * Intermediate in-memory representation of a report assembled by the SDK
 * before it is serialized and uploaded (or persisted for offline retry in M5).
 */
data class ReportDraft(
    val userId: String?,
    val description: String?,
    val deviceMetadata: DeviceMetadata,
    val appMetadata: AppMetadata,
    /** Compressed screenshot temp file, or null if no screenshot was captured. */
    val screenshotFile: File?,
    /** Non-null when the screenshot was intentionally blocked (e.g. FLAG_SECURE). */
    val screenshotBlockedReason: String?,
    val timestampMs: Long = System.currentTimeMillis(),
)

