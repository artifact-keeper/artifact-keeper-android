package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.CreatePolicyRequest
import com.artifactkeeper.client.models.LifecyclePolicy
import com.artifactkeeper.client.models.PolicyExecutionResult
import com.artifactkeeper.client.models.UpdatePolicyRequest

interface LifecycleApi {
    /**
     * POST api/v1/admin/lifecycle
     * POST /api/v1/admin/lifecycle
     * 
     * Responses:
     *  - 200: Policy created successfully
     *
     * @param createPolicyRequest 
     * @return [LifecyclePolicy]
     */
    @POST("api/v1/admin/lifecycle")
    suspend fun createLifecyclePolicy(@Body createPolicyRequest: CreatePolicyRequest): Response<LifecyclePolicy>

    /**
     * DELETE api/v1/admin/lifecycle/{id}
     * DELETE /api/v1/admin/lifecycle/:id
     * 
     * Responses:
     *  - 200: Policy deleted
     *
     * @param id Policy ID
     * @return [Unit]
     */
    @DELETE("api/v1/admin/lifecycle/{id}")
    suspend fun deleteLifecyclePolicy(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/admin/lifecycle/execute-all
     * POST /api/v1/admin/lifecycle/execute-all
     * 
     * Responses:
     *  - 200: All enabled policies executed
     *
     * @return [kotlin.collections.List<PolicyExecutionResult>]
     */
    @POST("api/v1/admin/lifecycle/execute-all")
    suspend fun executeAllPolicies(): Response<kotlin.collections.List<PolicyExecutionResult>>

    /**
     * POST api/v1/admin/lifecycle/{id}/execute
     * POST /api/v1/admin/lifecycle/:id/execute
     * 
     * Responses:
     *  - 200: Policy executed
     *
     * @param id Policy ID
     * @return [PolicyExecutionResult]
     */
    @POST("api/v1/admin/lifecycle/{id}/execute")
    suspend fun executePolicy(@Path("id") id: java.util.UUID): Response<PolicyExecutionResult>

    /**
     * GET api/v1/admin/lifecycle/{id}
     * GET /api/v1/admin/lifecycle/:id
     * 
     * Responses:
     *  - 200: Lifecycle policy details
     *
     * @param id Policy ID
     * @return [LifecyclePolicy]
     */
    @GET("api/v1/admin/lifecycle/{id}")
    suspend fun getLifecyclePolicy(@Path("id") id: java.util.UUID): Response<LifecyclePolicy>

    /**
     * GET api/v1/admin/lifecycle
     * GET /api/v1/admin/lifecycle
     * 
     * Responses:
     *  - 200: List lifecycle policies
     *
     * @param repositoryId  (optional)
     * @return [kotlin.collections.List<LifecyclePolicy>]
     */
    @GET("api/v1/admin/lifecycle")
    suspend fun listLifecyclePolicies(@Query("repository_id") repositoryId: java.util.UUID? = null): Response<kotlin.collections.List<LifecyclePolicy>>

    /**
     * POST api/v1/admin/lifecycle/{id}/preview
     * POST /api/v1/admin/lifecycle/:id/preview - dry-run
     * 
     * Responses:
     *  - 200: Policy preview (dry-run)
     *
     * @param id Policy ID
     * @return [PolicyExecutionResult]
     */
    @POST("api/v1/admin/lifecycle/{id}/preview")
    suspend fun previewPolicy(@Path("id") id: java.util.UUID): Response<PolicyExecutionResult>

    /**
     * PATCH api/v1/admin/lifecycle/{id}
     * PATCH /api/v1/admin/lifecycle/:id
     * 
     * Responses:
     *  - 200: Policy updated successfully
     *
     * @param id Policy ID
     * @param updatePolicyRequest 
     * @return [LifecyclePolicy]
     */
    @PATCH("api/v1/admin/lifecycle/{id}")
    suspend fun updateLifecyclePolicy(@Path("id") id: java.util.UUID, @Body updatePolicyRequest: UpdatePolicyRequest): Response<LifecyclePolicy>

}
