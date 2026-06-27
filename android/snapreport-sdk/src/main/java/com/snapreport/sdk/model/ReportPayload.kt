package com.snapreport.sdk.model

import com.google.gson.annotations.SerializedName
import com.snapreport.sdk.metadata.AppMetadata
import com.snapreport.sdk.metadata.DeviceMetadata

/**
 * The serialized JSON object sent as the `metadata` multipart field to the backend.
 */
data class ReportPayload(
    @SerializedName("userId")                  val userId: String?,
    @SerializedName("description")             val description: String?,
    @SerializedName("deviceMetadata")          val deviceMetadata: DeviceMetadata,
    @SerializedName("appMetadata")             val appMetadata: AppMetadata,
    @SerializedName("screenshotBlockedReason") val screenshotBlockedReason: String?,
)

