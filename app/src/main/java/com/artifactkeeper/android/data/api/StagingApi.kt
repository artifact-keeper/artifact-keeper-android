package com.artifactkeeper.android.data.api

import com.artifactkeeper.android.data.models.BulkPromoteRequest
import com.artifactkeeper.android.data.models.BulkPromotionResponse
import com.artifactkeeper.android.data.models.PromoteArtifactRequest
import com.artifactkeeper.android.data.models.PromotionHistoryResponse
import com.artifactkeeper.android.data.models.PromotionResponse
import com.artifactkeeper.android.data.models.StagingArtifactListResponse
import com.artifactkeeper.android.data.models.StagingRepositoryListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Staging-specific endpoints that are not part of the generated SDK.
 * These use /api/v1/staging/ paths which differ from the SDK's /api/v1/promotion/ paths.
 */
interface StagingApi {

    @GET("api/v1/staging/repositories")
    suspend fun listStagingRepos(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
    ): Response<StagingRepositoryListResponse>

    @GET("api/v1/staging/repositories/{key}/artifacts")
    suspend fun listStagingArtifacts(
        @Path("key") repoKey: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("policy_status") policyStatus: String? = null,
    ): Response<StagingArtifactListResponse>

    @POST("api/v1/staging/repositories/{key}/artifacts/{artifactId}/promote")
    suspend fun promoteArtifact(
        @Path("key") repoKey: String,
        @Path("artifactId") artifactId: String,
        @Body request: PromoteArtifactRequest,
    ): Response<PromotionResponse>

    @POST("api/v1/staging/repositories/{key}/promote-bulk")
    suspend fun promoteBulk(
        @Path("key") repoKey: String,
        @Body request: BulkPromoteRequest,
    ): Response<BulkPromotionResponse>

    @GET("api/v1/staging/repositories/{key}/promotion-history")
    suspend fun getPromotionHistory(
        @Path("key") repoKey: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
    ): Response<PromotionHistoryResponse>
}
