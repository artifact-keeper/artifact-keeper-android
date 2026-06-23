package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.BulkStatusRequest
import com.artifactkeeper.client.models.CreateRuleRequest
import com.artifactkeeper.client.models.PackageResponse
import com.artifactkeeper.client.models.ReEvaluateRequest
import com.artifactkeeper.client.models.RuleResponse
import com.artifactkeeper.client.models.StatsResponse
import com.artifactkeeper.client.models.UpdateRuleRequest

interface CurationApi {
    /**
     * POST api/v1/curation/packages/{id}/approve
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id Package ID
     * @return [PackageResponse]
     */
    @POST("api/v1/curation/packages/{id}/approve")
    suspend fun approvePackage(@Path("id") id: java.util.UUID): Response<PackageResponse>

    /**
     * POST api/v1/curation/packages/{id}/block
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id Package ID
     * @return [PackageResponse]
     */
    @POST("api/v1/curation/packages/{id}/block")
    suspend fun blockPackage(@Path("id") id: java.util.UUID): Response<PackageResponse>

    /**
     * POST api/v1/curation/packages/bulk-approve
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param bulkStatusRequest 
     * @return [kotlin.Long]
     */
    @POST("api/v1/curation/packages/bulk-approve")
    suspend fun bulkApprove(@Body bulkStatusRequest: BulkStatusRequest): Response<kotlin.Long>

    /**
     * POST api/v1/curation/packages/bulk-block
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param bulkStatusRequest 
     * @return [kotlin.Long]
     */
    @POST("api/v1/curation/packages/bulk-block")
    suspend fun bulkBlock(@Body bulkStatusRequest: BulkStatusRequest): Response<kotlin.Long>

    /**
     * POST api/v1/curation/rules
     * 
     * 
     * Responses:
     *  - 201: 
     *
     * @param createRuleRequest 
     * @return [RuleResponse]
     */
    @POST("api/v1/curation/rules")
    suspend fun createCurationRule(@Body createRuleRequest: CreateRuleRequest): Response<RuleResponse>

    /**
     * DELETE api/v1/curation/rules/{id}
     * 
     * 
     * Responses:
     *  - 204: 
     *
     * @param id Rule ID
     * @return [Unit]
     */
    @DELETE("api/v1/curation/rules/{id}")
    suspend fun deleteCurationRule(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/curation/packages/{id}
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id Package ID
     * @return [PackageResponse]
     */
    @GET("api/v1/curation/packages/{id}")
    suspend fun getCurationPackage(@Path("id") id: java.util.UUID): Response<PackageResponse>

    /**
     * GET api/v1/curation/packages
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param stagingRepoId 
     * @param status  (optional)
     * @param limit  (optional)
     * @param offset  (optional)
     * @return [kotlin.collections.List<PackageResponse>]
     */
    @GET("api/v1/curation/packages")
    suspend fun listCurationPackages(@Query("staging_repo_id") stagingRepoId: java.util.UUID, @Query("status") status: kotlin.String? = null, @Query("limit") limit: kotlin.Long? = null, @Query("offset") offset: kotlin.Long? = null): Response<kotlin.collections.List<PackageResponse>>

    /**
     * GET api/v1/curation/rules
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param stagingRepoId Filter by staging repo (optional)
     * @return [kotlin.collections.List<RuleResponse>]
     */
    @GET("api/v1/curation/rules")
    suspend fun listCurationRules(@Query("staging_repo_id") stagingRepoId: java.util.UUID? = null): Response<kotlin.collections.List<RuleResponse>>

    /**
     * POST api/v1/curation/packages/re-evaluate
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param reEvaluateRequest 
     * @return [kotlin.Long]
     */
    @POST("api/v1/curation/packages/re-evaluate")
    suspend fun reEvaluate(@Body reEvaluateRequest: ReEvaluateRequest): Response<kotlin.Long>

    /**
     * GET api/v1/curation/stats
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param stagingRepoId 
     * @return [StatsResponse]
     */
    @GET("api/v1/curation/stats")
    suspend fun stats(@Query("staging_repo_id") stagingRepoId: java.util.UUID): Response<StatsResponse>

    /**
     * PUT api/v1/curation/rules/{id}
     * 
     * 
     * Responses:
     *  - 200: 
     *
     * @param id Rule ID
     * @param updateRuleRequest 
     * @return [RuleResponse]
     */
    @PUT("api/v1/curation/rules/{id}")
    suspend fun updateCurationRule(@Path("id") id: java.util.UUID, @Body updateRuleRequest: UpdateRuleRequest): Response<RuleResponse>

}
