package com.artifactkeeper.android.data.api

import com.artifactkeeper.android.data.models.AdminGroupListResponse
import com.artifactkeeper.android.data.models.AdminStats
import com.artifactkeeper.android.data.models.AdminUserListResponse
import com.artifactkeeper.android.data.models.AlertState
import com.artifactkeeper.android.data.models.ArtifactListResponse
import com.artifactkeeper.android.data.models.AssignRepoRequest
import com.artifactkeeper.android.data.models.BuildListResponse
import com.artifactkeeper.android.data.models.ChangePasswordRequest
import com.artifactkeeper.android.data.models.CreateGroupRequest
import com.artifactkeeper.android.data.models.TotpCodeRequest
import com.artifactkeeper.android.data.models.TotpDisableRequest
import com.artifactkeeper.android.data.models.TotpEnableResponse
import com.artifactkeeper.android.data.models.TotpSetupResponse
import com.artifactkeeper.android.data.models.TotpVerifyRequest
import com.artifactkeeper.android.data.models.CreatePolicyRequest
import com.artifactkeeper.android.data.models.CreateUserRequest
import com.artifactkeeper.android.data.models.CreateUserResponse
import com.artifactkeeper.android.data.models.CreateWebhookRequest
import com.artifactkeeper.android.data.models.AdminGroup
import com.artifactkeeper.android.data.models.DownloadTrendItem
import com.artifactkeeper.android.data.models.HealthLogEntry
import com.artifactkeeper.android.data.models.HealthResponse
import com.artifactkeeper.android.data.models.LoginRequest
import com.artifactkeeper.android.data.models.LoginResponse
import com.artifactkeeper.android.data.models.SetupStatusResponse
import com.artifactkeeper.android.data.models.UserInfo
import com.artifactkeeper.android.data.models.PackageListResponse
import com.artifactkeeper.android.data.models.PeerConnection
import com.artifactkeeper.android.data.models.PeerInstance
import com.artifactkeeper.android.data.models.PeerListResponse
import com.artifactkeeper.android.data.models.RegisterPeerRequest
import com.artifactkeeper.android.data.models.RepoSecurityScore
import com.artifactkeeper.android.data.models.Repository
import com.artifactkeeper.android.data.models.RepositoryListResponse
import com.artifactkeeper.android.data.models.SSOProvider
import com.artifactkeeper.android.data.models.ScanFindingListResponse
import com.artifactkeeper.android.data.models.ScanListResponse
import com.artifactkeeper.android.data.models.SecurityPolicy
import com.artifactkeeper.android.data.models.StorageBreakdownItem
import com.artifactkeeper.android.data.models.StorageGrowthItem
import com.artifactkeeper.android.data.models.TestWebhookResponse
import com.artifactkeeper.android.data.models.TriggerScanRequest
import com.artifactkeeper.android.data.models.TriggerScanResponse
import com.artifactkeeper.android.data.models.UpdatePolicyRequest
import com.artifactkeeper.android.data.models.Webhook
import com.artifactkeeper.android.data.models.WebhookListResponse
import com.artifactkeeper.android.data.models.SbomListResponse
import com.artifactkeeper.android.data.models.SbomContentResponse
import com.artifactkeeper.android.data.models.SbomComponentsResponse
import com.artifactkeeper.android.data.models.CveHistoryEntry
import com.artifactkeeper.android.data.models.CveTrends
import com.artifactkeeper.android.data.models.GenerateSbomRequest
import com.artifactkeeper.android.data.models.SbomResponse
import com.artifactkeeper.android.data.models.LicensePolicyListResponse
import com.artifactkeeper.android.data.models.LicensePolicy
import com.artifactkeeper.android.data.models.CreateLicensePolicyRequest
import com.artifactkeeper.android.data.models.UpdateLicensePolicyRequest
import com.artifactkeeper.android.data.models.StagingRepositoryListResponse
import com.artifactkeeper.android.data.models.StagingArtifactListResponse
import com.artifactkeeper.android.data.models.PromoteArtifactRequest
import com.artifactkeeper.android.data.models.PromotionResponse
import com.artifactkeeper.android.data.models.BulkPromoteRequest
import com.artifactkeeper.android.data.models.BulkPromotionResponse
import com.artifactkeeper.android.data.models.PromotionHistoryResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ArtifactKeeperApi {
    @GET("/api/v1/packages")
    suspend fun listPackages(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 24,
        @Query("search") search: String? = null,
        @Query("format") format: String? = null,
    ): PackageListResponse

    @GET("/api/v1/builds")
    suspend fun listBuilds(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("sort_by") sortBy: String = "created_at",
        @Query("sort_order") sortOrder: String = "desc",
    ): BuildListResponse

    @GET("/api/v1/repositories")
    suspend fun listRepositories(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("search") search: String? = null,
    ): RepositoryListResponse

    @GET("/api/v1/repositories/{key}")
    suspend fun getRepository(@Path("key") key: String): Repository

    @GET("/api/v1/repositories/{key}/artifacts")
    suspend fun listArtifacts(
        @Path("key") repoKey: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("search") search: String? = null,
    ): ArtifactListResponse

    @GET("/api/v1/security/scores")
    suspend fun getSecurityScores(): List<RepoSecurityScore>

    @GET("/api/v1/security/scans")
    suspend fun listScans(
        @Query("repository_id") repositoryId: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): ScanListResponse

    @GET("/api/v1/security/scans/{scanId}/findings")
    suspend fun getScanFindings(
        @Path("scanId") scanId: String,
        @Query("per_page") perPage: Int = 200,
    ): ScanFindingListResponse

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("/api/v1/auth/me")
    suspend fun getMe(): UserInfo

    @GET("/api/v1/setup/status")
    suspend fun getSetupStatus(): SetupStatusResponse

    // --- Admin: Users ---
    @GET("api/v1/users")
    suspend fun listUsers(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50
    ): AdminUserListResponse

    @POST("api/v1/users")
    suspend fun createUser(@Body request: CreateUserRequest): CreateUserResponse

    // --- Admin: Groups ---
    @GET("api/v1/groups")
    suspend fun listGroups(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50
    ): AdminGroupListResponse

    @POST("api/v1/groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): AdminGroup

    // --- Admin: SSO ---
    @GET("api/v1/admin/sso/providers")
    suspend fun listSSOProviders(): List<SSOProvider>

    // --- Admin: Stats ---
    @GET("api/v1/admin/stats")
    suspend fun getAdminStats(): AdminStats

    // --- Peers ---
    @GET("api/v1/peers")
    suspend fun listPeers(): PeerListResponse

    @POST("api/v1/peers")
    suspend fun registerPeer(@Body request: RegisterPeerRequest): PeerInstance

    @DELETE("api/v1/peers/{id}")
    suspend fun deletePeer(@Path("id") id: String)

    @GET("api/v1/peers/{id}/connections")
    suspend fun getPeerConnections(@Path("id") id: String): List<PeerConnection>

    @GET("api/v1/peers/{id}/repositories")
    suspend fun getPeerRepositories(@Path("id") id: String): List<String>

    @POST("api/v1/peers/{id}/repositories")
    suspend fun assignPeerRepository(@Path("id") id: String, @Body request: AssignRepoRequest)

    // --- Webhooks ---
    @GET("api/v1/webhooks")
    suspend fun listWebhooks(): WebhookListResponse

    @POST("api/v1/webhooks")
    suspend fun createWebhook(@Body request: CreateWebhookRequest): Webhook

    @DELETE("api/v1/webhooks/{id}")
    suspend fun deleteWebhook(@Path("id") id: String)

    @POST("api/v1/webhooks/{id}/enable")
    suspend fun enableWebhook(@Path("id") id: String)

    @POST("api/v1/webhooks/{id}/disable")
    suspend fun disableWebhook(@Path("id") id: String)

    @POST("api/v1/webhooks/{id}/test")
    suspend fun testWebhook(@Path("id") id: String): TestWebhookResponse

    // --- Security Policies ---
    @GET("api/v1/security/policies")
    suspend fun listPolicies(): List<SecurityPolicy>

    @POST("api/v1/security/policies")
    suspend fun createPolicy(@Body request: CreatePolicyRequest): SecurityPolicy

    @PUT("api/v1/security/policies/{id}")
    suspend fun updatePolicy(@Path("id") id: String, @Body request: UpdatePolicyRequest): SecurityPolicy

    @DELETE("api/v1/security/policies/{id}")
    suspend fun deletePolicy(@Path("id") id: String)

    // --- Security Scans ---
    @POST("api/v1/security/scan")
    suspend fun triggerScan(@Body request: TriggerScanRequest = TriggerScanRequest()): TriggerScanResponse

    // --- Operations: Analytics ---
    @GET("api/v1/admin/analytics/storage/breakdown")
    suspend fun getStorageBreakdown(): List<StorageBreakdownItem>

    @GET("api/v1/admin/analytics/downloads/trend")
    suspend fun getDownloadTrend(@Query("days") days: Int = 30): List<DownloadTrendItem>

    @GET("api/v1/admin/analytics/storage/growth")
    suspend fun getStorageGrowth(@Query("days") days: Int = 30): List<StorageGrowthItem>

    // --- Operations: Monitoring ---
    @GET("api/v1/admin/monitoring/health-log")
    suspend fun getHealthLog(@Query("limit") limit: Int = 20): List<HealthLogEntry>

    @GET("api/v1/admin/monitoring/alerts")
    suspend fun getAlerts(): List<AlertState>

    @GET("api/v1/admin/monitoring/metrics")
    @retrofit2.http.Headers("Accept: text/plain")
    suspend fun getPrometheusMetrics(): okhttp3.ResponseBody

    // --- TOTP ---
    @POST("api/v1/auth/totp/setup")
    suspend fun totpSetup(): TotpSetupResponse

    @POST("api/v1/auth/totp/enable")
    suspend fun totpEnable(@Body request: TotpCodeRequest): TotpEnableResponse

    @POST("api/v1/auth/totp/verify")
    suspend fun totpVerify(@Body request: TotpVerifyRequest): LoginResponse

    @POST("api/v1/auth/totp/disable")
    suspend fun totpDisable(@Body request: TotpDisableRequest)

    // --- Password ---
    @POST("api/v1/users/{id}/password")
    suspend fun changePassword(@Path("id") userId: String, @Body request: ChangePasswordRequest)

    // --- Health ---
    @GET("health")
    suspend fun getHealth(): HealthResponse

    // --- SBOM ---
    @GET("api/v1/sbom")
    suspend fun listSboms(
        @Query("artifact_id") artifactId: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): SbomListResponse

    @POST("api/v1/sbom")
    suspend fun generateSbom(@Body request: GenerateSbomRequest): SbomResponse

    @GET("api/v1/sbom/{id}")
    suspend fun getSbom(@Path("id") id: String): SbomContentResponse

    @GET("api/v1/sbom/{id}/components")
    suspend fun getSbomComponents(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100,
    ): SbomComponentsResponse

    @GET("api/v1/sbom/cve/history/{artifactId}")
    suspend fun getCveHistory(@Path("artifactId") artifactId: String): List<CveHistoryEntry>

    @GET("api/v1/sbom/cve/trends")
    suspend fun getCveTrends(@Query("days") days: Int = 30): CveTrends

    // --- License Policies ---
    @GET("api/v1/sbom/license-policies")
    suspend fun listLicensePolicies(): LicensePolicyListResponse

    @POST("api/v1/sbom/license-policies")
    suspend fun createLicensePolicy(@Body request: CreateLicensePolicyRequest): LicensePolicy

    @PUT("api/v1/sbom/license-policies/{id}")
    suspend fun updateLicensePolicy(@Path("id") id: String, @Body request: UpdateLicensePolicyRequest): LicensePolicy

    @DELETE("api/v1/sbom/license-policies/{id}")
    suspend fun deleteLicensePolicy(@Path("id") id: String)

    // --- Staging Repositories ---
    @GET("api/v1/staging/repositories")
    suspend fun listStagingRepos(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
    ): StagingRepositoryListResponse

    @GET("api/v1/staging/repositories/{key}/artifacts")
    suspend fun listStagingArtifacts(
        @Path("key") repoKey: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("policy_status") policyStatus: String? = null,
    ): StagingArtifactListResponse

    @POST("api/v1/staging/repositories/{key}/artifacts/{artifactId}/promote")
    suspend fun promoteArtifact(
        @Path("key") repoKey: String,
        @Path("artifactId") artifactId: String,
        @Body request: PromoteArtifactRequest,
    ): PromotionResponse

    @POST("api/v1/staging/repositories/{key}/promote-bulk")
    suspend fun promoteBulk(
        @Path("key") repoKey: String,
        @Body request: BulkPromoteRequest,
    ): BulkPromotionResponse

    @GET("api/v1/staging/repositories/{key}/promotion-history")
    suspend fun getPromotionHistory(
        @Path("key") repoKey: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
    ): PromotionHistoryResponse
}
