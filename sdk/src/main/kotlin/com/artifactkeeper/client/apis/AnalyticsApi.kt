package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.DownloadTrend
import com.artifactkeeper.client.models.GrowthSummary
import com.artifactkeeper.client.models.RepositorySnapshot
import com.artifactkeeper.client.models.RepositoryStorageBreakdown
import com.artifactkeeper.client.models.StaleArtifact
import com.artifactkeeper.client.models.StorageSnapshot

interface AnalyticsApi {
    /**
     * POST api/v1/admin/analytics/snapshot
     * POST /api/v1/admin/analytics/snapshot - manually trigger a snapshot
     * 
     * Responses:
     *  - 200: Snapshot captured successfully
     *
     * @return [StorageSnapshot]
     */
    @POST("api/v1/admin/analytics/snapshot")
    suspend fun captureSnapshot(): Response<StorageSnapshot>

    /**
     * GET api/v1/admin/analytics/downloads/trend
     * GET /api/v1/admin/analytics/downloads/trend
     * 
     * Responses:
     *  - 200: Download trends over date range
     *
     * @param from  (optional)
     * @param to  (optional)
     * @return [kotlin.collections.List<DownloadTrend>]
     */
    @GET("api/v1/admin/analytics/downloads/trend")
    suspend fun getDownloadTrends(@Query("from") from: kotlin.String? = null, @Query("to") to: kotlin.String? = null): Response<kotlin.collections.List<DownloadTrend>>

    /**
     * GET api/v1/admin/analytics/storage/growth
     * GET /api/v1/admin/analytics/storage/growth
     * 
     * Responses:
     *  - 200: Growth summary for date range
     *
     * @param from  (optional)
     * @param to  (optional)
     * @return [GrowthSummary]
     */
    @GET("api/v1/admin/analytics/storage/growth")
    suspend fun getGrowthSummary(@Query("from") from: kotlin.String? = null, @Query("to") to: kotlin.String? = null): Response<GrowthSummary>

    /**
     * GET api/v1/admin/analytics/repositories/{id}/trend
     * GET /api/v1/admin/analytics/repositories/{id}/trend
     * 
     * Responses:
     *  - 200: Repository storage trend over date range
     *
     * @param id Repository ID
     * @param from  (optional)
     * @param to  (optional)
     * @return [kotlin.collections.List<RepositorySnapshot>]
     */
    @GET("api/v1/admin/analytics/repositories/{id}/trend")
    suspend fun getRepositoryTrend(@Path("id") id: java.util.UUID, @Query("from") from: kotlin.String? = null, @Query("to") to: kotlin.String? = null): Response<kotlin.collections.List<RepositorySnapshot>>

    /**
     * GET api/v1/admin/analytics/artifacts/stale
     * GET /api/v1/admin/analytics/artifacts/stale
     * 
     * Responses:
     *  - 200: List of stale artifacts
     *
     * @param days  (optional)
     * @param limit  (optional)
     * @return [kotlin.collections.List<StaleArtifact>]
     */
    @GET("api/v1/admin/analytics/artifacts/stale")
    suspend fun getStaleArtifacts(@Query("days") days: kotlin.Int? = null, @Query("limit") limit: kotlin.Long? = null): Response<kotlin.collections.List<StaleArtifact>>

    /**
     * GET api/v1/admin/analytics/storage/breakdown
     * GET /api/v1/admin/analytics/storage/breakdown
     * 
     * Responses:
     *  - 200: Per-repository storage breakdown
     *
     * @return [kotlin.collections.List<RepositoryStorageBreakdown>]
     */
    @GET("api/v1/admin/analytics/storage/breakdown")
    suspend fun getStorageBreakdown(): Response<kotlin.collections.List<RepositoryStorageBreakdown>>

    /**
     * GET api/v1/admin/analytics/storage/trend
     * GET /api/v1/admin/analytics/storage/trend
     * 
     * Responses:
     *  - 200: Storage trend over date range
     *
     * @param from  (optional)
     * @param to  (optional)
     * @return [kotlin.collections.List<StorageSnapshot>]
     */
    @GET("api/v1/admin/analytics/storage/trend")
    suspend fun getStorageTrend(@Query("from") from: kotlin.String? = null, @Query("to") to: kotlin.String? = null): Response<kotlin.collections.List<StorageSnapshot>>

}
