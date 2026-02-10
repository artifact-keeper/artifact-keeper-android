package com.artifactkeeper.android.data.api

import com.artifactkeeper.android.BuildConfig
import com.artifactkeeper.client.apis.AdminApi
import com.artifactkeeper.client.apis.AnalyticsApi
import com.artifactkeeper.client.apis.AuthApi
import com.artifactkeeper.client.apis.BuildsApi
import com.artifactkeeper.client.apis.GroupsApi
import com.artifactkeeper.client.apis.HealthApi
import com.artifactkeeper.client.apis.MonitoringApi
import com.artifactkeeper.client.apis.PackagesApi
import com.artifactkeeper.client.apis.PeersApi
import com.artifactkeeper.client.apis.PromotionApi
import com.artifactkeeper.client.apis.RepositoriesApi
import com.artifactkeeper.client.apis.SbomApi
import com.artifactkeeper.client.apis.SecurityApi
import com.artifactkeeper.client.apis.SsoApi
import com.artifactkeeper.client.apis.UsersApi
import com.artifactkeeper.client.apis.WebhooksApi
import com.artifactkeeper.client.infrastructure.ApiClient as SdkApiClient
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Unwrap a Retrofit [Response], returning the body on success
 * or throwing an exception with the error body on failure.
 */
suspend fun <T> Response<T>.unwrap(): T {
    if (isSuccessful) return body()!!
    throw Exception(errorBody()?.string() ?: "HTTP ${code()}")
}

object ApiClient {
    private var _baseUrl = ""
    private var _token: String? = null
    private var sdkClient: SdkApiClient? = null

    val isConfigured: Boolean get() = _baseUrl.isNotBlank()
    val baseUrl: String get() = _baseUrl
    val token: String? get() = _token

    // --- Typed API services ---
    lateinit var authApi: AuthApi private set
    lateinit var reposApi: RepositoriesApi private set
    lateinit var packagesApi: PackagesApi private set
    lateinit var buildsApi: BuildsApi private set
    lateinit var securityApi: SecurityApi private set
    lateinit var adminApi: AdminApi private set
    lateinit var usersApi: UsersApi private set
    lateinit var groupsApi: GroupsApi private set
    lateinit var peersApi: PeersApi private set
    lateinit var webhooksApi: WebhooksApi private set
    lateinit var analyticsApi: AnalyticsApi private set
    lateinit var monitoringApi: MonitoringApi private set
    lateinit var healthApi: HealthApi private set
    lateinit var sbomApi: SbomApi private set
    lateinit var ssoApi: SsoApi private set
    lateinit var promotionApi: PromotionApi private set
    lateinit var stagingApi: StagingApi private set

    // Keep a raw OkHttpClient for cases that need direct HTTP access
    val httpClient: OkHttpClient get() = buildOkHttpClientBuilder().build()

    private fun buildOkHttpClientBuilder(): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })

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

        return builder
    }

    private fun rebuildServices() {
        val url = _baseUrl.ifBlank { "http://localhost/" }
        val okBuilder = buildOkHttpClientBuilder()

        val client = if (_token != null) {
            SdkApiClient(
                baseUrl = url,
                okHttpClientBuilder = okBuilder,
                authName = "bearer_auth",
                bearerToken = _token!!
            )
        } else {
            SdkApiClient(
                baseUrl = url,
                okHttpClientBuilder = okBuilder,
                authNames = arrayOf("bearer_auth")
            )
        }
        sdkClient = client

        authApi = client.createService(AuthApi::class.java)
        reposApi = client.createService(RepositoriesApi::class.java)
        packagesApi = client.createService(PackagesApi::class.java)
        buildsApi = client.createService(BuildsApi::class.java)
        securityApi = client.createService(SecurityApi::class.java)
        adminApi = client.createService(AdminApi::class.java)
        usersApi = client.createService(UsersApi::class.java)
        groupsApi = client.createService(GroupsApi::class.java)
        peersApi = client.createService(PeersApi::class.java)
        webhooksApi = client.createService(WebhooksApi::class.java)
        analyticsApi = client.createService(AnalyticsApi::class.java)
        monitoringApi = client.createService(MonitoringApi::class.java)
        healthApi = client.createService(HealthApi::class.java)
        sbomApi = client.createService(SbomApi::class.java)
        ssoApi = client.createService(SsoApi::class.java)
        promotionApi = client.createService(PromotionApi::class.java)
        stagingApi = client.createService(StagingApi::class.java)
    }

    init {
        rebuildServices()
    }

    fun configure(baseUrl: String, token: String? = null) {
        _baseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        _token = token
        rebuildServices()
    }

    fun clearConfig() {
        _baseUrl = ""
        _token = null
        rebuildServices()
    }

    fun setToken(token: String?) {
        _token = token
        rebuildServices()
    }
}
