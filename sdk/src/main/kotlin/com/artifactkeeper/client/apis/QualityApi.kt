package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.ArtifactHealthResponse
import com.artifactkeeper.client.models.CheckResponse
import com.artifactkeeper.client.models.CreateGateRequest
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.GateEvaluationResponse
import com.artifactkeeper.client.models.GateResponse
import com.artifactkeeper.client.models.HealthDashboardResponse
import com.artifactkeeper.client.models.IssueResponse
import com.artifactkeeper.client.models.RepoHealthResponse
import com.artifactkeeper.client.models.SuppressIssueRequest
import com.artifactkeeper.client.models.TriggerChecksRequest
import com.artifactkeeper.client.models.TriggerChecksResponse
import com.artifactkeeper.client.models.UpdateGateRequest

interface QualityApi {
    /**
     * POST api/v1/quality/gates
     * 
     * 
     * Responses:
     *  - 200: Quality gate created
     *  - 422: Validation error
     *
     * @param createGateRequest 
     * @return [GateResponse]
     */
    @POST("api/v1/quality/gates")
    suspend fun createGate(@Body createGateRequest: CreateGateRequest): Response<GateResponse>

    /**
     * DELETE api/v1/quality/gates/{id}
     * 
     * 
     * Responses:
     *  - 200: Quality gate deleted
     *  - 404: Quality gate not found
     *
     * @param id Quality gate ID
     * @return [kotlinx.serialization.json.JsonElement]
     */
    @DELETE("api/v1/quality/gates/{id}")
    suspend fun deleteGate(@Path("id") id: java.util.UUID): Response<kotlinx.serialization.json.JsonElement>

    /**
     * POST api/v1/quality/gates/evaluate/{artifact_id}
     * 
     * 
     * Responses:
     *  - 200: Gate evaluation result
     *  - 404: Artifact or gate not found
     *
     * @param artifactId Artifact ID to evaluate
     * @param repositoryId  (optional)
     * @return [GateEvaluationResponse]
     */
    @POST("api/v1/quality/gates/evaluate/{artifact_id}")
    suspend fun evaluateGate(@Path("artifact_id") artifactId: java.util.UUID, @Query("repository_id") repositoryId: java.util.UUID? = null): Response<GateEvaluationResponse>

    /**
     * GET api/v1/quality/health/artifacts/{artifact_id}
     * 
     * 
     * Responses:
     *  - 200: Artifact health score
     *  - 404: Artifact not found
     *
     * @param artifactId Artifact ID
     * @return [ArtifactHealthResponse]
     */
    @GET("api/v1/quality/health/artifacts/{artifact_id}")
    suspend fun getArtifactHealth(@Path("artifact_id") artifactId: java.util.UUID): Response<ArtifactHealthResponse>

    /**
     * GET api/v1/quality/checks/{id}
     * 
     * 
     * Responses:
     *  - 200: Check result details
     *  - 404: Check result not found
     *
     * @param id Check result ID
     * @return [CheckResponse]
     */
    @GET("api/v1/quality/checks/{id}")
    suspend fun getCheck(@Path("id") id: java.util.UUID): Response<CheckResponse>

    /**
     * GET api/v1/quality/gates/{id}
     * 
     * 
     * Responses:
     *  - 200: Quality gate details
     *  - 404: Quality gate not found
     *
     * @param id Quality gate ID
     * @return [GateResponse]
     */
    @GET("api/v1/quality/gates/{id}")
    suspend fun getGate(@Path("id") id: java.util.UUID): Response<GateResponse>

    /**
     * GET api/v1/quality/health/dashboard
     * 
     * 
     * Responses:
     *  - 200: Health dashboard summary
     *
     * @return [HealthDashboardResponse]
     */
    @GET("api/v1/quality/health/dashboard")
    suspend fun getHealthDashboard(): Response<HealthDashboardResponse>

    /**
     * GET api/v1/quality/health/repositories/{key}
     * 
     * 
     * Responses:
     *  - 200: Repository health score
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [RepoHealthResponse]
     */
    @GET("api/v1/quality/health/repositories/{key}")
    suspend fun getRepoHealth(@Path("key") key: kotlin.String): Response<RepoHealthResponse>

    /**
     * GET api/v1/quality/checks/{id}/issues
     * 
     * 
     * Responses:
     *  - 200: List of issues for a check result
     *  - 404: Check result not found
     *
     * @param id Check result ID
     * @return [kotlin.collections.List<IssueResponse>]
     */
    @GET("api/v1/quality/checks/{id}/issues")
    suspend fun listCheckIssues(@Path("id") id: java.util.UUID): Response<kotlin.collections.List<IssueResponse>>

    /**
     * GET api/v1/quality/checks
     * 
     * 
     * Responses:
     *  - 200: List of quality check results
     *
     * @param artifactId  (optional)
     * @param repositoryId  (optional)
     * @return [kotlin.collections.List<CheckResponse>]
     */
    @GET("api/v1/quality/checks")
    suspend fun listChecks(@Query("artifact_id") artifactId: java.util.UUID? = null, @Query("repository_id") repositoryId: java.util.UUID? = null): Response<kotlin.collections.List<CheckResponse>>

    /**
     * GET api/v1/quality/gates
     * 
     * 
     * Responses:
     *  - 200: List of quality gates
     *
     * @return [kotlin.collections.List<GateResponse>]
     */
    @GET("api/v1/quality/gates")
    suspend fun listGates(): Response<kotlin.collections.List<GateResponse>>

    /**
     * POST api/v1/quality/issues/{id}/suppress
     * 
     * 
     * Responses:
     *  - 200: Issue suppressed
     *  - 404: Issue not found
     *
     * @param id Issue ID
     * @param suppressIssueRequest 
     * @return [IssueResponse]
     */
    @POST("api/v1/quality/issues/{id}/suppress")
    suspend fun suppressIssue(@Path("id") id: java.util.UUID, @Body suppressIssueRequest: SuppressIssueRequest): Response<IssueResponse>

    /**
     * POST api/v1/quality/checks/trigger
     * 
     * 
     * Responses:
     *  - 200: Quality checks triggered
     *  - 400: Validation error
     *
     * @param triggerChecksRequest 
     * @return [TriggerChecksResponse]
     */
    @POST("api/v1/quality/checks/trigger")
    suspend fun triggerChecks(@Body triggerChecksRequest: TriggerChecksRequest): Response<TriggerChecksResponse>

    /**
     * DELETE api/v1/quality/issues/{id}/suppress
     * 
     * 
     * Responses:
     *  - 200: Issue unsuppressed
     *  - 404: Issue not found
     *
     * @param id Issue ID
     * @return [IssueResponse]
     */
    @DELETE("api/v1/quality/issues/{id}/suppress")
    suspend fun unsuppressIssue(@Path("id") id: java.util.UUID): Response<IssueResponse>

    /**
     * PUT api/v1/quality/gates/{id}
     * 
     * 
     * Responses:
     *  - 200: Quality gate updated
     *  - 404: Quality gate not found
     *
     * @param id Quality gate ID
     * @param updateGateRequest 
     * @return [GateResponse]
     */
    @PUT("api/v1/quality/gates/{id}")
    suspend fun updateGate(@Path("id") id: java.util.UUID, @Body updateGateRequest: UpdateGateRequest): Response<GateResponse>

}
