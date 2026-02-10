package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.CheckLicenseComplianceRequest
import com.artifactkeeper.client.models.ComponentResponse
import com.artifactkeeper.client.models.ConvertSbomRequest
import com.artifactkeeper.client.models.CveHistoryEntry
import com.artifactkeeper.client.models.CveTrends
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.GenerateSbomRequest
import com.artifactkeeper.client.models.LicenseCheckResult
import com.artifactkeeper.client.models.LicensePolicyResponse
import com.artifactkeeper.client.models.SbomContentResponse
import com.artifactkeeper.client.models.SbomResponse
import com.artifactkeeper.client.models.UpdateCveStatusRequest
import com.artifactkeeper.client.models.UpsertLicensePolicyRequest

interface SbomApi {
    /**
     * POST api/v1/sbom/check-compliance
     * Check license compliance against policies
     * 
     * Responses:
     *  - 200: License compliance result
     *  - 404: No license policy configured
     *
     * @param checkLicenseComplianceRequest 
     * @return [LicenseCheckResult]
     */
    @POST("api/v1/sbom/check-compliance")
    suspend fun checkLicenseCompliance(@Body checkLicenseComplianceRequest: CheckLicenseComplianceRequest): Response<LicenseCheckResult>

    /**
     * POST api/v1/sbom/{id}/convert
     * Convert an SBOM to a different format
     * 
     * Responses:
     *  - 200: Converted SBOM
     *  - 404: SBOM not found
     *  - 422: Validation error
     *
     * @param id SBOM ID
     * @param convertSbomRequest 
     * @return [SbomResponse]
     */
    @POST("api/v1/sbom/{id}/convert")
    suspend fun convertSbom(@Path("id") id: java.util.UUID, @Body convertSbomRequest: ConvertSbomRequest): Response<SbomResponse>

    /**
     * DELETE api/v1/sbom/license-policies/{id}
     * Delete a license policy
     * 
     * Responses:
     *  - 200: License policy deleted
     *  - 404: License policy not found
     *
     * @param id License policy ID
     * @return [kotlinx.serialization.json.JsonElement]
     */
    @DELETE("api/v1/sbom/license-policies/{id}")
    suspend fun deleteLicensePolicy(@Path("id") id: java.util.UUID): Response<kotlinx.serialization.json.JsonElement>

    /**
     * DELETE api/v1/sbom/{id}
     * Delete an SBOM
     * 
     * Responses:
     *  - 200: SBOM deleted
     *  - 404: SBOM not found
     *
     * @param id SBOM ID
     * @return [kotlinx.serialization.json.JsonElement]
     */
    @DELETE("api/v1/sbom/{id}")
    suspend fun deleteSbom(@Path("id") id: java.util.UUID): Response<kotlinx.serialization.json.JsonElement>

    /**
     * POST api/v1/sbom
     * Generate an SBOM for an artifact
     * 
     * Responses:
     *  - 200: Generated SBOM
     *  - 404: Artifact not found
     *  - 422: Validation error
     *
     * @param generateSbomRequest 
     * @return [SbomResponse]
     */
    @POST("api/v1/sbom")
    suspend fun generateSbom(@Body generateSbomRequest: GenerateSbomRequest): Response<SbomResponse>

    /**
     * GET api/v1/sbom/cve/history/{artifact_id}
     * Get CVE history for an artifact
     * 
     * Responses:
     *  - 200: CVE history entries
     *
     * @param artifactId Artifact ID
     * @return [kotlin.collections.List<CveHistoryEntry>]
     */
    @GET("api/v1/sbom/cve/history/{artifact_id}")
    suspend fun getCveHistory(@Path("artifact_id") artifactId: java.util.UUID): Response<kotlin.collections.List<CveHistoryEntry>>

    /**
     * GET api/v1/sbom/cve/trends
     * Get CVE trends and statistics
     * 
     * Responses:
     *  - 200: CVE trends
     *
     * @param repositoryId  (optional)
     * @param days  (optional)
     * @return [CveTrends]
     */
    @GET("api/v1/sbom/cve/trends")
    suspend fun getCveTrends(@Query("repository_id") repositoryId: java.util.UUID? = null, @Query("days") days: kotlin.Int? = null): Response<CveTrends>

    /**
     * GET api/v1/sbom/license-policies/{id}
     * Get a license policy by ID
     * 
     * Responses:
     *  - 200: License policy details
     *  - 404: License policy not found
     *
     * @param id License policy ID
     * @return [LicensePolicyResponse]
     */
    @GET("api/v1/sbom/license-policies/{id}")
    suspend fun getLicensePolicy(@Path("id") id: java.util.UUID): Response<LicensePolicyResponse>

    /**
     * GET api/v1/sbom/{id}
     * Get SBOM by ID with full content
     * 
     * Responses:
     *  - 200: SBOM with content
     *  - 404: SBOM not found
     *
     * @param id SBOM ID
     * @return [SbomContentResponse]
     */
    @GET("api/v1/sbom/{id}")
    suspend fun getSbom(@Path("id") id: java.util.UUID): Response<SbomContentResponse>

    /**
     * GET api/v1/sbom/by-artifact/{artifact_id}
     * Get SBOM by artifact ID
     * 
     * Responses:
     *  - 200: SBOM for the artifact
     *  - 404: SBOM not found for artifact
     *
     * @param artifactId Artifact ID
     * @return [SbomContentResponse]
     */
    @GET("api/v1/sbom/by-artifact/{artifact_id}")
    suspend fun getSbomByArtifact(@Path("artifact_id") artifactId: java.util.UUID): Response<SbomContentResponse>

    /**
     * GET api/v1/sbom/{id}/components
     * Get components of an SBOM
     * 
     * Responses:
     *  - 200: List of SBOM components
     *  - 404: SBOM not found
     *
     * @param id SBOM ID
     * @return [kotlin.collections.List<ComponentResponse>]
     */
    @GET("api/v1/sbom/{id}/components")
    suspend fun getSbomComponents(@Path("id") id: java.util.UUID): Response<kotlin.collections.List<ComponentResponse>>

    /**
     * GET api/v1/sbom/license-policies
     * List all license policies
     * 
     * Responses:
     *  - 200: List of license policies
     *
     * @return [kotlin.collections.List<LicensePolicyResponse>]
     */
    @GET("api/v1/sbom/license-policies")
    suspend fun listLicensePolicies(): Response<kotlin.collections.List<LicensePolicyResponse>>

    /**
     * GET api/v1/sbom
     * List SBOMs with optional filters
     * 
     * Responses:
     *  - 200: List of SBOMs
     *
     * @param artifactId  (optional)
     * @param repositoryId  (optional)
     * @param format  (optional)
     * @return [kotlin.collections.List<SbomResponse>]
     */
    @GET("api/v1/sbom")
    suspend fun listSboms(@Query("artifact_id") artifactId: java.util.UUID? = null, @Query("repository_id") repositoryId: java.util.UUID? = null, @Query("format") format: kotlin.String? = null): Response<kotlin.collections.List<SbomResponse>>

    /**
     * POST api/v1/sbom/cve/status/{id}
     * Update CVE status
     * 
     * Responses:
     *  - 200: Updated CVE entry
     *  - 422: Validation error
     *
     * @param id CVE history entry ID
     * @param updateCveStatusRequest 
     * @return [CveHistoryEntry]
     */
    @POST("api/v1/sbom/cve/status/{id}")
    suspend fun updateCveStatus(@Path("id") id: java.util.UUID, @Body updateCveStatusRequest: UpdateCveStatusRequest): Response<CveHistoryEntry>

    /**
     * POST api/v1/sbom/license-policies
     * Create or update a license policy
     * 
     * Responses:
     *  - 200: Created or updated license policy
     *  - 422: Validation error
     *
     * @param upsertLicensePolicyRequest 
     * @return [LicensePolicyResponse]
     */
    @POST("api/v1/sbom/license-policies")
    suspend fun upsertLicensePolicy(@Body upsertLicensePolicyRequest: UpsertLicensePolicyRequest): Response<LicensePolicyResponse>

}
