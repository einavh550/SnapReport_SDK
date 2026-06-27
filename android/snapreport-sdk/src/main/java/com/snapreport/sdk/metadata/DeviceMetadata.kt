package com.snapreport.sdk.metadata

import com.google.gson.annotations.SerializedName

/**
 * Snapshot of device hardware and OS state captured at report time.
 */
data class DeviceMetadata(
    @SerializedName("manufacturer")       val manufacturer: String,
    @SerializedName("model")              val model: String,
    @SerializedName("androidVersion")     val androidVersion: String,
    @SerializedName("sdkInt")             val sdkInt: Int,
    @SerializedName("batteryPercent")     val batteryPercent: Int?,
    @SerializedName("isCharging")         val isCharging: Boolean?,
    @SerializedName("totalRamMb")         val totalRamMb: Long,
    @SerializedName("availableRamMb")     val availableRamMb: Long,
    @SerializedName("totalStorageMb")     val totalStorageMb: Long,
    @SerializedName("availableStorageMb") val availableStorageMb: Long,
    @SerializedName("networkType")        val networkType: String,
    @SerializedName("locale")             val locale: String,
    @SerializedName("timezone")           val timezone: String,
)

