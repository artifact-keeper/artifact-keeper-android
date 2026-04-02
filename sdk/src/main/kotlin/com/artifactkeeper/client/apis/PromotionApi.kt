package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.BulkEvaluationResponse
import com.artifactkeeper.client.models.BulkPromoteRequest
import com.artifactkeeper.client.models.BulkPromotionResponse
import com.artifactkeeper.client.models.CreateRuleRequest
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.PromoteArtifactRequest
import com.artifactkeeper.client.models.PromotionHistoryResponse
import com.artifactkeeper.client.models.PromotionResponse
import com.artifactkeeper.client.models.PromotionRuleListResponse
import com.artifactkeeper.client.models.PromotionRuleResponse
import com.artifactkeeper.client.models.RejectArtifactRequest
import com.artifactkeeper.client.models.RejectionResponse
import com.artifactkeeper.client.models.UpdateRuleRequest

interface PromotionApi {
    /**
     * POST api/v1/promotion-rules
     * Create a promotion rule
     * 
     * Responses:
     *  - 200: Promotion rule created
     *  - 400: Validation error
     *  - 500: Internal server error
     *
     * @param createRuleRequest 
     * @return [PromotionRuleResponse]
     */
    @POST("api/v1/promotion-rules")
    suspend fun createRule(@Body createRuleRequest: CreateRuleRequest): Response<PromotionRuleResponse>

    /**
     * DELETE api/v1/promotion-rules/{id}
     * Delete a promotion rule
     * 
     * Responses:
     *  - 200: Promotion rule deleted
     *  - 404: Rule not found
     *
     * @param id Promotion rule ID
     * @return [Unit]
     */
    @DELETE("api/v1/promotion-rules/{id}")
    suspend fun deleteRule(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/promotion-rules/{id}/evaluate
     * Dry-run evaluate a rule against all artifacts in its source repository
     * 
     * Responses:
     *  - 200: Evaluation results
     *  - 404: Rule not found
     *
     * @param id Promotion rule ID to evaluate
     * @return [BulkEvaluationResponse]
     */
    @POST("api/v1/promotion-rules/{id}/evaluate")
    suspend fun evaluateRule(@Path("id") id: java.util.UUID): Response<BulkEvaluationResponse>

    /**
     * GET api/v1/promotion-rules/{id}
     * Get a promotion rule by ID
     * 
     * Responses:
     *  - 200: Promotion rule details
     *  - 404: Rule not found
     *
     * @param id Promotion rule ID
     * @return [PromotionRuleResponse]
     */
    @GET("api/v1/promotion-rules/{id}")
    suspend fun getRule(@Path("id") id: java.util.UUID): Response<PromotionRuleResponse>

    /**
     * GET api/v1/promotion-rules
     * List all promotion rules
     * 
     * Responses:
     *  - 200: List of promotion rules
     *  - 500: Internal server error
     *
     * @param sourceRepoId  (optional)
     * @return [PromotionRuleListResponse]
     */
    @GET("api/v1/promotion-rules")
    suspend fun listRules(@Query("source_repo_id") sourceRepoId: java.util.UUID? = null): Response<PromotionRuleListResponse>

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
     * @param status Filter by status (promoted, rejected, pending_approval) (optional)
     * @return [PromotionHistoryResponse]
     */
    @GET("api/v1/promotion/repositories/{key}/promotion-history")
    suspend fun promotionHistory(@Path("key") key: kotlin.String, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null, @Query("artifact_id") artifactId: java.util.UUID? = null, @Query("status") status: kotlin.String? = null): Response<PromotionHistoryResponse>

    /**
     * POST api/v1/promotion/repositories/{key}/artifacts/{artifact_id}/reject
     * 
     * 
     * Responses:
     *  - 200: Artifact rejection result
     *  - 404: Artifact or repository not found
     *  - 422: Validation error
     *
     * @param key Source repository key
     * @param artifactId Artifact ID to reject
     * @param rejectArtifactRequest 
     * @return [RejectionResponse]
     */
    @POST("api/v1/promotion/repositories/{key}/artifacts/{artifact_id}/reject")
    suspend fun rejectArtifact(@Path("key") key: kotlin.String, @Path("artifact_id") artifactId: java.util.UUID, @Body rejectArtifactRequest: RejectArtifactRequest): Response<RejectionResponse>

    /**
     * PUT api/v1/promotion-rules/{id}
     * Update a promotion rule
     * 
     * Responses:
     *  - 200: Promotion rule updated
     *  - 404: Rule not found
     *
     * @param id Promotion rule ID
     * @param updateRuleRequest 
     * @return [PromotionRuleResponse]
     */
    @PUT("api/v1/promotion-rules/{id}")
    suspend fun updateRule(@Path("id") id: java.util.UUID, @Body updateRuleRequest: UpdateRuleRequest): Response<PromotionRuleResponse>

}
