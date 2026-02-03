package com.artifactkeeper.android.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object ApiClient {
    private val json = Json { ignoreUnknownKeys = true }
    private var _baseUrl = "http://10.0.2.2:30080/"
    private var _token: String? = null

    private fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder()
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
            .build()
    }

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(_baseUrl)
            .client(buildClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    var api: ArtifactKeeperApi = buildRetrofit().create(ArtifactKeeperApi::class.java)
        private set

    val baseUrl: String get() = _baseUrl

    val token: String? get() = _token

    fun configure(baseUrl: String, token: String? = null) {
        _baseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        _token = token
        api = buildRetrofit().create(ArtifactKeeperApi::class.java)
    }

    fun setToken(token: String?) {
        _token = token
        api = buildRetrofit().create(ArtifactKeeperApi::class.java)
    }
}
