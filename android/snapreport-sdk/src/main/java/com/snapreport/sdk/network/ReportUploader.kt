package com.snapreport.sdk.network

import com.google.gson.Gson
import com.snapreport.sdk.model.ReportDraft
import com.snapreport.sdk.model.ReportPayload
import com.snapreport.sdk.model.UploadResult
import com.snapreport.sdk.util.Logger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

private const val TAG = "ReportUploader"

/**
 * Converts a [ReportDraft] into a multipart HTTP request and uploads it via [SnapReportApi].
 */
internal class ReportUploader(
    private val api: SnapReportApi,
    private val apiKey: String,
    private val gson: Gson = Gson(),
) {

    /**
     * Performs the upload and returns a [UploadResult]. Never throws.
     */
    suspend fun upload(draft: ReportDraft): UploadResult {
        return try {
            val payload = ReportPayload(
                userId                  = draft.userId,
                description             = draft.description,
                deviceMetadata          = draft.deviceMetadata,
                appMetadata             = draft.appMetadata,
                screenshotBlockedReason = draft.screenshotBlockedReason,
            )
            val metadataBody = gson.toJson(payload)
                .toRequestBody("application/json".toMediaType())

            val screenshotPart = draft.screenshotFile
                ?.takeIf { it.exists() && it.length() > 0 }
                ?.let { file ->
                    MultipartBody.Part.createFormData(
                        "screenshot", file.name,
                        file.asRequestBody("image/jpeg".toMediaType())
                    )
                }

            val response = api.uploadReport(apiKey, metadataBody, screenshotPart)

            if (response.isSuccessful && response.body()?.success == true) {
                val ticketId = response.body()?.ticketId ?: "unknown"
                Logger.i(TAG, "Uploaded successfully — ticketId=$ticketId")
                UploadResult.Success(ticketId)
            } else {
                val body       = response.body()
                val errorCode  = body?.error ?: "SERVER_ERROR"
                val msg        = body?.message ?: "HTTP ${response.code()}"
                val retryable  = response.code() in 500..599
                Logger.w(TAG, "Upload failed: $errorCode — $msg")
                UploadResult.Failure(errorCode, msg, retryable)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Upload exception: ${e.message}", e)
            UploadResult.Failure(null, e.message ?: "Unknown network error", retryable = true)
        }
    }
}

