package com.artifactkeeper.android.data.api

import com.artifactkeeper.android.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object ApiClient {
    private val json = Json { ignoreUnknownKeys = true }
    private var _baseUrl = ""
    private var _token: String? = null

    val isConfigured: Boolean get() = _baseUrl.isNotBlank()

    private fun buildClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor { chain ->
                val request = _token?.let {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $it")
                        .build()
                } ?: chain.request()
                chain.proceed(request)
            }

        if (BuildConfig.DEBUG) {
            val trustAllManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustAllManager)
            builder.hostnameVerifier { _, _ -> true }
        }

        return builder.build()
    }

    private fun buildRetrofit(): Retrofit {
        val url = _baseUrl.ifBlank { "http://localhost/" }
        return Retrofit.Builder()
            .baseUrl(url)
            .client(buildClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    var api: ArtifactKeeperApi = buildRetrofit().create(ArtifactKeeperApi::class.java)
        private set

    val httpClient: OkHttpClient get() = buildClient()

    val baseUrl: String get() = _baseUrl

    val token: String? get() = _token

    fun configure(baseUrl: String, token: String? = null) {
        _baseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        _token = token
        api = buildRetrofit().create(ArtifactKeeperApi::class.java)
    }

    fun clearConfig() {
        _baseUrl = ""
        _token = null
        api = buildRetrofit().create(ArtifactKeeperApi::class.java)
    }

    fun setToken(token: String?) {
        _token = token
        api = buildRetrofit().create(ArtifactKeeperApi::class.java)
    }

    fun getHealthUrl(): String {
        val base = _baseUrl.ifBlank { "http://localhost/" }
        return "${base}health"
    }
}
