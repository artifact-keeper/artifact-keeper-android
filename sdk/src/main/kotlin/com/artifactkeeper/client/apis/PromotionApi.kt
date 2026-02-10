package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.BulkPromoteRequest
import com.artifactkeeper.client.models.BulkPromotionResponse
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.PromoteArtifactRequest
import com.artifactkeeper.client.models.PromotionHistoryResponse
import com.artifactkeeper.client.models.PromotionResponse

interface PromotionApi {
    /**
     * POST api/v1/promotion/repositories/{key}/artifacts/{artifact_id}/promote
     * 
     * 
     * Responses:
     *  - 200: Artifact promotion result
     *  - 404: Artifact or repository not found
     *  - 409: Artifact already exists in target
     *  - 422: Validation error (repo type/format mismatch)
     *
     * @param key Source repository key
     * @param artifactId Artifact ID to promote
     * @param promoteArtifactRequest 
     * @return [PromotionResponse]
     */
    @POST("api/v1/promotion/repositories/{key}/artifacts/{artifact_id}/promote")
    suspend fun promoteArtifact(@Path("key") key: kotlin.String, @Path("artifact_id") artifactId: java.util.UUID, @Body promoteArtifactRequest: PromoteArtifactRequest): Response<PromotionResponse>

    /**
     * POST api/v1/promotion/repositories/{key}/promote
     * 
     * 
     * Responses:
     *  - 200: Bulk promotion results
     *  - 404: Repository not found
     *  - 422: Validation error (repo type/format mismatch)
     *
     * @param key Source repository key
     * @param bulkPromoteRequest 
     * @return [BulkPromotionResponse]
     */
    @POST("api/v1/promotion/repositories/{key}/promote")
    suspend fun promoteArtifactsBulk(@Path("key") key: kotlin.String, @Body bulkPromoteRequest: BulkPromoteRequest): Response<BulkPromotionResponse>

    /**
     * GET api/v1/promotion/repositories/{key}/promotion-history
     * 
     * 
     * Responses:
     *  - 200: Promotion history for repository
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param page Page number (1-indexed) (optional)
     * @param perPage Items per page (max 100) (optional)
     * @param artifactId Filter by artifact ID (optional)
     * @return [PromotionHistoryResponse]
     */
    @GET("api/v1/promotion/repositories/{key}/promotion-history")
    suspend fun promotionHistory(@Path("key") key: kotlin.String, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null, @Query("artifact_id") artifactId: java.util.UUID? = null): Response<PromotionHistoryResponse>

}
