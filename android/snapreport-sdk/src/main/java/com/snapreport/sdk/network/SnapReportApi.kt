package com.snapreport.sdk.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit interface for the SnapReport backend SDK upload endpoint.
 */
internal interface SnapReportApi {

    /**
     * Upload a bug report.
     *
     * @param apiKey     Project API key, sent in the `X-SnapReport-Api-Key` header.
     * @param metadata   JSON-encoded [com.snapreport.sdk.model.ReportPayload], sent as the `metadata` part.
     * @param screenshot Optional JPEG screenshot file part.
     */
    @Multipart
    @POST("api/sdk/reports")
    suspend fun uploadReport(
        @Header("X-SnapReport-Api-Key") apiKey: String,
        @Part("metadata") metadata: RequestBody,
        @Part screenshot: MultipartBody.Part?,
    ): Response<UploadResponse>
}

/**
 * JSON shape returned by a successful (2xx) upload or a structured error response.
 */
internal data class UploadResponse(
    val success: Boolean,
    val ticketId: String?,
    val error: String?,
    val message: String?,
)

