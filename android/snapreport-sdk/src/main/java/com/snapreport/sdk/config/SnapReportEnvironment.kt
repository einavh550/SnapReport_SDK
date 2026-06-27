package com.snapreport.sdk.config

/**
 * Defines the target server environment for SDK uploads.
 */
enum class SnapReportEnvironment {
    /** Points to the Android emulator loopback address (http://10.0.2.2:8000). */
    DEVELOPMENT,
    /** Points to a staging environment; configure baseUrl manually via SnapReportConfig. */
    STAGING,
    /** Points to the production server. */
    PRODUCTION;

    companion object {
        /** Default base URL for the Android emulator pointing at localhost:8000. */
        const val DEFAULT_DEV_BASE_URL = "http://10.0.2.2:8000/"
    }
}

