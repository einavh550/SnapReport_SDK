package com.snapreport.sdk.network

import com.snapreport.sdk.config.SnapReportConfig
import com.snapreport.sdk.util.Logger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Creates a configured [OkHttpClient] + [Retrofit] pair and returns a ready [SnapReportApi].
 *
 * Call once during SDK initialization and cache the result.
 */
internal object NetworkClientFactory {

    fun createApi(config: SnapReportConfig): SnapReportApi {
        val loggingInterceptor = HttpLoggingInterceptor { msg -> Logger.d("OkHttp", msg) }.apply {
            level = if (config.debugLogging) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(config.httpTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.httpTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.httpTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SnapReportApi::class.java)
    }
}

