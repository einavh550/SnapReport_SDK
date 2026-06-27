package com.snapreport.sdk.metadata

import com.google.gson.annotations.SerializedName

/**
 * Snapshot of the host application state captured at report time.
 */
data class AppMetadata(
    @SerializedName("packageName")      val packageName: String,
    @SerializedName("appVersionName")   val appVersionName: String,
    @SerializedName("appVersionCode")   val appVersionCode: Long,
    @SerializedName("timestampMicros")  val timestampMicros: Long,
    @SerializedName("screenWidthPx")    val screenWidthPx: Int,
    @SerializedName("screenHeightPx")   val screenHeightPx: Int,
    /** `"portrait"` or `"landscape"` */
    @SerializedName("orientation")      val orientation: String,
)

