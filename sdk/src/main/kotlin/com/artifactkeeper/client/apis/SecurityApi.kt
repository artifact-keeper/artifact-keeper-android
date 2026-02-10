package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.AcknowledgeRequest
import com.artifactkeeper.client.models.CreatePolicyRequest
import com.artifactkeeper.client.models.DashboardResponse
import com.artifactkeeper.client.models.DtAnalysisResponse
import com.artifactkeeper.client.models.DtComponentFull
import com.artifactkeeper.client.models.DtFinding
import com.artifactkeeper.client.models.DtPolicyFull
import com.artifactkeeper.client.models.DtPolicyViolation
import com.artifactkeeper.client.models.DtPortfolioMetrics
import com.artifactkeeper.client.models.DtProject
import com.artifactkeeper.client.models.DtProjectMetrics
import com.artifactkeeper.client.models.DtStatusResponse
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.FindingListResponse
import com.artifactkeeper.client.models.FindingResponse
import com.artifactkeeper.client.models.PolicyResponse
import com.artifactkeeper.client.models.RepoSecurityResponse
import com.artifactkeeper.client.models.ScanConfigResponse
import com.artifactkeeper.client.models.ScanListResponse
import com.artifactkeeper.client.models.ScanResponse
import com.artifactkeeper.client.models.ScoreResponse
import com.artifactkeeper.client.models.TriggerScanRequest
import com.artifactkeeper.client.models.TriggerScanResponse
import com.artifactkeeper.client.models.UpdateAnalysisBody
import com.artifactkeeper.client.models.UpdatePolicyRequest
import com.artifactkeeper.client.models.UpsertScanConfigRequest

interface SecurityApi {
    /**
     * POST api/v1/security/findings/{id}/acknowledge
     * 
     * 
     * Responses:
     *  - 200: Finding acknowledged
     *  - 404: Finding not found
     *
     * @param id Finding ID
     * @param acknowledgeRequest 
     * @return [FindingResponse]
     */
    @POST("api/v1/security/findings/{id}/acknowledge")
    suspend fun acknowledgeFinding(@Path("id") id: java.util.UUID, @Body acknowledgeRequest: AcknowledgeRequest): Response<FindingResponse>

    /**
     * POST api/v1/security/policies
     * 
     * 
     * Responses:
     *  - 200: Policy created
     *  - 422: Validation error
     *
     * @param createPolicyRequest 
     * @return [PolicyResponse]
     */
    @POST("api/v1/security/policies")
    suspend fun createPolicy(@Body createPolicyRequest: CreatePolicyRequest): Response<PolicyResponse>

    /**
     * DELETE api/v1/security/policies/{id}
     * 
     * 
     * Responses:
     *  - 200: Policy deleted
     *  - 404: Policy not found
     *
     * @param id Policy ID
     * @return [kotlinx.serialization.json.JsonElement]
     */
    @DELETE("api/v1/security/policies/{id}")
    suspend fun deletePolicy(@Path("id") id: java.util.UUID): Response<kotlinx.serialization.json.JsonElement>

    /**
     * GET api/v1/dependency-track/status
     * Get Dependency-Track integration status
     * 
     * Responses:
     *  - 200: Dependency-Track status
     *
     * @return [DtStatusResponse]
     */
    @GET("api/v1/dependency-track/status")
    suspend fun dtStatus(): Response<DtStatusResponse>

    /**
     * GET api/v1/security/scores
     * 
     * 
     * Responses:
     *  - 200: All repository security scores
     *
     * @return [kotlin.collections.List<ScoreResponse>]
     */
    @GET("api/v1/security/scores")
    suspend fun getAllScores(): Response<kotlin.collections.List<ScoreResponse>>

    /**
     * GET api/v1/security/dashboard
     * 
     * 
     * Responses:
     *  - 200: Security dashboard summary
     *
     * @return [DashboardResponse]
     */
    @GET("api/v1/security/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    /**
     * GET api/v1/security/policies/{id}
     * 
     * 
     * Responses:
     *  - 200: Policy details
     *  - 404: Policy not found
     *
     * @param id Policy ID
     * @return [PolicyResponse]
     */
    @GET("api/v1/security/policies/{id}")
    suspend fun getPolicy(@Path("id") id: java.util.UUID): Response<PolicyResponse>

    /**
     * GET api/v1/dependency-track/metrics/portfolio
     * Get portfolio-level metrics
     * 
     * Responses:
     *  - 200: Portfolio metrics
     *
     * @return [DtPortfolioMetrics]
     */
    @GET("api/v1/dependency-track/metrics/portfolio")
    suspend fun getPortfolioMetrics(): Response<DtPortfolioMetrics>

    /**
     * GET api/v1/dependency-track/projects/{project_uuid}
     * Get project findings by project UUID
     * 
     * Responses:
     *  - 200: Project findings
     *
     * @param projectUuid Project UUID
     * @return [kotlin.collections.List<DtFinding>]
     */
    @GET("api/v1/dependency-track/projects/{project_uuid}")
    suspend fun getProject(@Path("project_uuid") projectUuid: kotlin.String): Response<kotlin.collections.List<DtFinding>>

    /**
     * GET api/v1/dependency-track/projects/{project_uuid}/components
     * Get components for a project
     * 
     * Responses:
     *  - 200: Project components
     *
     * @param projectUuid Project UUID
     * @return [kotlin.collections.List<DtComponentFull>]
     */
    @GET("api/v1/dependency-track/projects/{project_uuid}/components")
    suspend fun getProjectComponents(@Path("project_uuid") projectUuid: kotlin.String): Response<kotlin.collections.List<DtComponentFull>>

    /**
     * GET api/v1/dependency-track/projects/{project_uuid}/findings
     * Get vulnerability findings for a project
     * 
     * Responses:
     *  - 200: Project vulnerability findings
     *
     * @param projectUuid Project UUID
     * @return [kotlin.collections.List<DtFinding>]
     */
    @GET("api/v1/dependency-track/projects/{project_uuid}/findings")
    suspend fun getProjectFindings(@Path("project_uuid") projectUuid: kotlin.String): Response<kotlin.collections.List<DtFinding>>

    /**
     * GET api/v1/dependency-track/projects/{project_uuid}/metrics
     * Get metrics for a project
     * 
     * Responses:
     *  - 200: Project metrics
     *
     * @param projectUuid Project UUID
     * @return [DtProjectMetrics]
     */
    @GET("api/v1/dependency-track/projects/{project_uuid}/metrics")
    suspend fun getProjectMetrics(@Path("project_uuid") projectUuid: kotlin.String): Response<DtProjectMetrics>

    /**
     * GET api/v1/dependency-track/projects/{project_uuid}/metrics/history
     * Get metrics history for a project
     * 
     * Responses:
     *  - 200: Project metrics history
     *
     * @param projectUuid Project UUID
     * @param days  (optional)
     * @return [kotlin.collections.List<DtProjectMetrics>]
     */
    @GET("api/v1/dependency-track/projects/{project_uuid}/metrics/history")
    suspend fun getProjectMetricsHistory(@Path("project_uuid") projectUuid: kotlin.String, @Query("days") days: kotlin.Int? = null): Response<kotlin.collections.List<DtProjectMetrics>>

    /**
     * GET api/v1/dependency-track/projects/{project_uuid}/violations
     * Get policy violations for a project
     * 
     * Responses:
     *  - 200: Project policy violations
     *
     * @param projectUuid Project UUID
     * @return [kotlin.collections.List<DtPolicyViolation>]
     */
    @GET("api/v1/dependency-track/projects/{project_uuid}/violations")
    suspend fun getProjectViolations(@Path("project_uuid") projectUuid: kotlin.String): Response<kotlin.collections.List<DtPolicyViolation>>

    /**
     * GET api/v1/repositories/{key}/security
     * 
     * 
     * Responses:
     *  - 200: Repository security config and score
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [RepoSecurityResponse]
     */
    @GET("api/v1/repositories/{key}/security")
    suspend fun getRepoSecurity(@Path("key") key: kotlin.String): Response<RepoSecurityResponse>

    /**
     * GET api/v1/security/scans/{id}
     * 
     * 
     * Responses:
     *  - 200: Scan details
     *  - 404: Scan not found
     *
     * @param id Scan result ID
     * @return [ScanResponse]
     */
    @GET("api/v1/security/scans/{id}")
    suspend fun getScan(@Path("id") id: java.util.UUID): Response<ScanResponse>

    /**
     * GET api/v1/security/artifacts/{artifact_id}/scans
     * 
     * 
     * Responses:
     *  - 200: Paginated list of scans for an artifact
     *
     * @param artifactId Artifact ID
     * @param status Filter by scan status (optional)
     * @param page Page number (default: 1) (optional)
     * @param perPage Items per page (default: 20, max: 100) (optional)
     * @return [ScanListResponse]
     */
    @GET("api/v1/security/artifacts/{artifact_id}/scans")
    suspend fun listArtifactScans(@Path("artifact_id") artifactId: java.util.UUID, @Query("status") status: kotlin.String? = null, @Query("page") page: kotlin.Long? = null, @Query("per_page") perPage: kotlin.Long? = null): Response<ScanListResponse>

    /**
     * GET api/v1/dependency-track/policies
     * List all policies
     * 
     * Responses:
     *  - 200: List of policies
     *
     * @return [kotlin.collections.List<DtPolicyFull>]
     */
    @GET("api/v1/dependency-track/policies")
    suspend fun listDependencyTrackPolicies(): Response<kotlin.collections.List<DtPolicyFull>>

    /**
     * GET api/v1/security/scans/{id}/findings
     * 
     * 
     * Responses:
     *  - 200: Paginated list of findings for a scan
     *  - 404: Scan not found
     *
     * @param id Scan result ID
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [FindingListResponse]
     */
    @GET("api/v1/security/scans/{id}/findings")
    suspend fun listFindings(@Path("id") id: java.util.UUID, @Query("page") page: kotlin.Long? = null, @Query("per_page") perPage: kotlin.Long? = null): Response<FindingListResponse>

    /**
     * GET api/v1/security/policies
     * 
     * 
     * Responses:
     *  - 200: List of security policies
     *
     * @return [kotlin.collections.List<PolicyResponse>]
     */
    @GET("api/v1/security/policies")
    suspend fun listPolicies(): Response<kotlin.collections.List<PolicyResponse>>

    /**
     * GET api/v1/dependency-track/projects
     * List all Dependency-Track projects
     * 
     * Responses:
     *  - 200: List of projects
     *
     * @return [kotlin.collections.List<DtProject>]
     */
    @GET("api/v1/dependency-track/projects")
    suspend fun listProjects(): Response<kotlin.collections.List<DtProject>>

    /**
     * GET api/v1/repositories/{key}/security/scans
     * 
     * 
     * Responses:
     *  - 200: Paginated list of scans for a repository
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param repositoryId  (optional)
     * @param artifactId  (optional)
     * @param status  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [ScanListResponse]
     */
    @GET("api/v1/repositories/{key}/security/scans")
    suspend fun listRepoScans(@Path("key") key: kotlin.String, @Query("repository_id") repositoryId: java.util.UUID? = null, @Query("artifact_id") artifactId: java.util.UUID? = null, @Query("status") status: kotlin.String? = null, @Query("page") page: kotlin.Long? = null, @Query("per_page") perPage: kotlin.Long? = null): Response<ScanListResponse>

    /**
     * GET api/v1/security/configs
     * 
     * 
     * Responses:
     *  - 200: List of scan configurations
     *
     * @return [kotlin.collections.List<ScanConfigResponse>]
     */
    @GET("api/v1/security/configs")
    suspend fun listScanConfigs(): Response<kotlin.collections.List<ScanConfigResponse>>

    /**
     * GET api/v1/security/scans
     * 
     * 
     * Responses:
     *  - 200: Paginated list of scans
     *
     * @param repositoryId  (optional)
     * @param artifactId  (optional)
     * @param status  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [ScanListResponse]
     */
    @GET("api/v1/security/scans")
    suspend fun listScans(@Query("repository_id") repositoryId: java.util.UUID? = null, @Query("artifact_id") artifactId: java.util.UUID? = null, @Query("status") status: kotlin.String? = null, @Query("page") page: kotlin.Long? = null, @Query("per_page") perPage: kotlin.Long? = null): Response<ScanListResponse>

    /**
     * DELETE api/v1/security/findings/{id}/acknowledge
     * 
     * 
     * Responses:
     *  - 200: Acknowledgment revoked
     *  - 404: Finding not found
     *
     * @param id Finding ID
     * @return [FindingResponse]
     */
    @DELETE("api/v1/security/findings/{id}/acknowledge")
    suspend fun revokeAcknowledgment(@Path("id") id: java.util.UUID): Response<FindingResponse>

    /**
     * POST api/v1/security/scan
     * 
     * 
     * Responses:
     *  - 200: Scan triggered successfully
     *  - 400: Validation error
     *  - 500: Scanner service not configured
     *
     * @param triggerScanRequest 
     * @return [TriggerScanResponse]
     */
    @POST("api/v1/security/scan")
    suspend fun triggerScan(@Body triggerScanRequest: TriggerScanRequest): Response<TriggerScanResponse>

    /**
     * PUT api/v1/dependency-track/analysis
     * Update analysis (triage) for a finding
     * 
     * Responses:
     *  - 200: Updated analysis
     *
     * @param updateAnalysisBody 
     * @return [DtAnalysisResponse]
     */
    @PUT("api/v1/dependency-track/analysis")
    suspend fun updateAnalysis(@Body updateAnalysisBody: UpdateAnalysisBody): Response<DtAnalysisResponse>

    /**
     * PUT api/v1/security/policies/{id}
     * 
     * 
     * Responses:
     *  - 200: Policy updated
     *  - 404: Policy not found
     *
     * @param id Policy ID
     * @param updatePolicyRequest 
     * @return [PolicyResponse]
     */
    @PUT("api/v1/security/policies/{id}")
    suspend fun updatePolicy(@Path("id") id: java.util.UUID, @Body updatePolicyRequest: UpdatePolicyRequest): Response<PolicyResponse>

    /**
     * PUT api/v1/repositories/{key}/security
     * 
     * 
     * Responses:
     *  - 200: Repository security config updated
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param upsertScanConfigRequest 
     * @return [ScanConfigResponse]
     */
    @PUT("api/v1/repositories/{key}/security")
    suspend fun updateRepoSecurity(@Path("key") key: kotlin.String, @Body upsertScanConfigRequest: UpsertScanConfigRequest): Response<ScanConfigResponse>

}
