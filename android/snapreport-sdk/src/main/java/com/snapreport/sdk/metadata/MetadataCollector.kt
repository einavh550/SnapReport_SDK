package com.snapreport.sdk.metadata

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import com.snapreport.sdk.util.TimeProvider
import java.util.Locale
import java.util.TimeZone

/**
 * Collects device and application metadata at the moment a report is triggered.
 *
 * All fields are collected on the calling thread (IO dispatcher in [com.snapreport.sdk.SnapReportSdk]).
 * Only non-sensitive, non-personal data is gathered; no location, contacts, or files are read.
 */
internal class MetadataCollector(
    private val context: Context,
    private val timeProvider: TimeProvider = TimeProvider.System,
) {

    // ── Public API ────────────────────────────────────────────────────────────

    fun collectDeviceMetadata(): DeviceMetadata = DeviceMetadata(
        manufacturer       = Build.MANUFACTURER,
        model              = Build.MODEL,
        androidVersion     = Build.VERSION.RELEASE,
        sdkInt             = Build.VERSION.SDK_INT,
        batteryPercent     = getBatteryPercent(),
        isCharging         = isCharging(),
        totalRamMb         = getTotalRam(),
        availableRamMb     = getAvailableRam(),
        totalStorageMb     = getTotalStorage(),
        availableStorageMb = getAvailableStorage(),
        networkType        = getNetworkType(),
        locale             = Locale.getDefault().toLanguageTag(),
        timezone           = TimeZone.getDefault().id,
    )

    fun collectAppMetadata(): AppMetadata {
        val pm = context.packageManager
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) { null }

        val versionName = packageInfo?.versionName ?: "unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode ?: 0L
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toLong() ?: 0L
        }

        val (width, height) = getScreenSize()

        return AppMetadata(
            packageName    = context.packageName,
            appVersionName = versionName,
            appVersionCode = versionCode,
            timestampMicros = timeProvider.nowMicros(),
            screenWidthPx  = width,
            screenHeightPx = height,
            orientation    = getOrientation(),
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun batteryIntent(): Intent? =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    private fun getBatteryPercent(): Int? {
        val intent = batteryIntent() ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) level * 100 / scale else null
    }

    private fun isCharging(): Boolean? {
        val status = batteryIntent()?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: return null
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun memInfo(): ActivityManager.MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    }

    private fun getTotalRam(): Long     = memInfo().totalMem     / (1024 * 1024)
    private fun getAvailableRam(): Long = memInfo().availMem     / (1024 * 1024)

    private fun getTotalStorage(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.blockCountLong * stat.blockSizeLong / (1024 * 1024)
    }

    private fun getAvailableStorage(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.availableBlocksLong * stat.blockSizeLong / (1024 * 1024)
    }

    private fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "NONE"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
    }

    @Suppress("DEPRECATION")
    private fun getScreenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val bounds = wm.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val size = android.graphics.Point()
            wm.defaultDisplay.getRealSize(size)
            Pair(size.x, size.y)
        }
    }

    private fun getOrientation(): String =
        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            "landscape" else "portrait"
}

