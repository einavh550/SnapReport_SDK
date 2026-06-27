package com.snapreport.sdk.model

/**
 * Result of a report upload attempt.
 */
sealed class UploadResult {
    /** Report was accepted by the server. */
    data class Success(val ticketId: String) : UploadResult()

    /**
     * Upload failed.
     * @param errorCode  Structured error code from the server (e.g. `INVALID_API_KEY`), or null on network error.
     * @param message    Human-readable detail.
     * @param retryable  True for transient (network/5xx) failures that should be retried offline.
     */
    data class Failure(
        val errorCode: String?,
        val message: String,
        val retryable: Boolean,
    ) : UploadResult()
}

