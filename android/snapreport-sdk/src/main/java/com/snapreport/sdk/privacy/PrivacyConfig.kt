package com.snapreport.sdk.privacy

/**
 * Privacy rules for the [SensitiveViewMasker].
 *
 * M4 skeleton — will be extended in M5 with per-view-tag and class-name rules.
 *
 * @param maskedViewTags     Set of view `tag` strings whose regions are obscured in screenshots.
 * @param maskedViewClasses  Simple class names that are always masked (default: `EditText`).
 * @param maskBlurRadius     Blur intensity applied to masked regions (1-25, default 10).
 */
data class PrivacyConfig(
    val maskedViewTags: Set<String>    = emptySet(),
    val maskedViewClasses: Set<String> = setOf("EditText"),
    val maskBlurRadius: Int            = 10,
)

