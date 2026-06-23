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
     * Returns the converted SBOM as a [&#x60;SbomContentResponse&#x60;]: the metadata row plus the full converted document under &#x60;content&#x60;. The &#x60;content&#x60; is load-bearing here. A consumer that asked for &#x60;target_format&#x3D;spdx&#x60; needs the SPDX document (&#x60;content.spdxVersion&#x60;, &#x60;content.SPDXID&#x60;, ...) to feed downstream attestation tooling, and a &#x60;target_format&#x3D;cyclonedx&#x60; request needs &#x60;content.bomFormat &#x3D;&#x3D; \&quot;CycloneDX\&quot;&#x60;. Returning metadata-only (&#x60;SbomResponse&#x60;) dropped the converted document entirely, so callers could not tell an SPDX result from a CycloneDX one and round-trip conversion appeared to lose the document shape. (release-gate &#x60;test-sbom-convert.sh&#x60; 2.5.a / 2.5.b.)
     * Responses:
     *  - 200: Converted SBOM with content
     *  - 404: SBOM not found
     *  - 422: Validation error
     *
     * @param id SBOM ID
     * @param convertSbomRequest 
     * @return [SbomContentResponse]
     */
    @POST("api/v1/sbom/{id}/convert")
    suspend fun convertSbom(@Path("id") id: java.util.UUID, @Body convertSbomRequest: ConvertSbomRequest): Response<SbomContentResponse>

    /**
     * DELETE api/v1/sbom/license-policies/{id}
     * Delete a license policy
     * 
     * Responses:
     *  - 200: License policy deleted
     *  - 404: License policy not found
     *
     * @param id License policy ID
     * @return [kotlin.Any]
     */
    @DELETE("api/v1/sbom/license-policies/{id}")
    suspend fun deleteLicensePolicy(@Path("id") id: java.util.UUID): Response<kotlin.Any>

    /**
     * DELETE api/v1/sbom/{id}
     * Delete an SBOM
     * 
     * Responses:
     *  - 200: SBOM deleted
     *  - 404: SBOM not found
     *
     * @param id SBOM ID
     * @return [kotlin.Any]
     */
    @DELETE("api/v1/sbom/{id}")
    suspend fun deleteSbom(@Path("id") id: java.util.UUID): Response<kotlin.Any>

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
     * GET api/v1/sbom/cve/history/{id}
     * Get CVE history by artifact UUID or CVE identifier (legacy overload).
     * The path param accepts either:   - A UUID &#x60;artifact_id&#x60; (legacy shape, returns all CVEs for one artifact)   - A CVE id like &#x60;CVE-2019-10744&#x60; (returns this CVE across every artifact     the caller can access)  # URL design decision (#1385 round-2)  Overloading a single &#x60;{id}&#x60; path parameter to mean two different lookups is a REST anti-pattern: the route&#39;s behavior changes based on a runtime content sniff. We considered splitting into two routes vs documenting the overload and chose **both**: the split routes &#x60;GET /cve/history/by-artifact/{uuid}&#x60; and &#x60;GET /cve/history/by-cve/{cve_id}&#x60; are the canonical shape for new clients (typed path params, no sniff), while this overload remains so the v1.2.0 SDKs that already shipped against the single-route shape keep working. New code should prefer the split routes; the overload may be deprecated in v1.3.  Issue #1375: prior to this fix the route was typed &#x60;Path&lt;Uuid&gt;&#x60;, so any CVE-id call (e.g. the release-gate &#x60;GET /sbom/cve/history/CVE-2019-10744&#x60;) failed Axum&#39;s path extractor with a bare HTTP 400, leaving consumers unable to look up history by CVE.
     * Responses:
     *  - 200: CVE history entries
     *  - 400: Path id is neither a valid UUID nor a valid CVE identifier
     *
     * @param id Artifact UUID or CVE identifier (e.g. CVE-2019-10744). Prefer the typed routes /cve/history/by-artifact/{uuid} or /cve/history/by-cve/{cve_id}.
     * @return [kotlin.collections.List<CveHistoryEntry>]
     */
    @GET("api/v1/sbom/cve/history/{id}")
    suspend fun getCveHistory(@Path("id") id: kotlin.String): Response<kotlin.collections.List<CveHistoryEntry>>

    /**
     * GET api/v1/sbom/cve/history/by-artifact/{artifact_id}
     * Get CVE history for one artifact (typed UUID variant).
     * Canonical replacement for the UUID branch of the overloaded &#x60;/cve/history/{id}&#x60; route. Returns every CVE ever detected against the given artifact, deduped across curated &#x60;cve_history&#x60; rows and live &#x60;scan_findings&#x60; projections.
     * Responses:
     *  - 200: CVE history entries
     *  - 403: Caller does not have access to this artifact's repository
     *  - 404: Artifact not found
     *
     * @param artifactId Artifact UUID
     * @return [kotlin.collections.List<CveHistoryEntry>]
     */
    @GET("api/v1/sbom/cve/history/by-artifact/{artifact_id}")
    suspend fun getCveHistoryByArtifact(@Path("artifact_id") artifactId: java.util.UUID): Response<kotlin.collections.List<CveHistoryEntry>>

    /**
     * GET api/v1/sbom/cve/history/by-cve/{cve_id}
     * Get CVE history for one CVE identifier across artifacts (typed CVE-id variant).
     * Canonical replacement for the CVE-id branch of the overloaded &#x60;/cve/history/{id}&#x60; route. Returns every artifact the caller can access where the given CVE has been detected.
     * Responses:
     *  - 200: CVE history entries
     *  - 400: Path id is not a valid CVE identifier
     *
     * @param cveId CVE identifier (e.g. CVE-2019-10744)
     * @return [kotlin.collections.List<CveHistoryEntry>]
     */
    @GET("api/v1/sbom/cve/history/by-cve/{cve_id}")
    suspend fun getCveHistoryByCve(@Path("cve_id") cveId: kotlin.String): Response<kotlin.collections.List<CveHistoryEntry>>

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
     * POST api/v1/sbom/cve/status/by-artifact/{artifact_id}/by-cve/{cve_id}
     * Update CVE status for a synth (scan_findings-derived) Security tab row.
     * Background (#1426): the Security tab read path projects &#x60;scan_findings&#x60; into &#x60;CveHistoryEntry&#x60; rows whose &#x60;id&#x60; is a deterministic SHA-256 hash (see &#x60;synth_cve_id&#x60;). Those ids have no corresponding row in the &#x60;cve_history&#x60; table, so calls to &#x60;POST /cve/status/{id}&#x60; always 404 -- a dead acknowledge path. This route operates on the only stable identity a synth row carries, the (artifact_id, cve_id) pair, and writes the underlying &#x60;scan_findings&#x60; rows instead.  The wider design choice between (A) populating &#x60;cve_history&#x60; from the scanner loop and (B) treating &#x60;scan_findings&#x60; as the source of truth for the Security tab is settled here in favour of B: less code, less risk of data drift between two parallel tables, and &#x60;cve_history&#x60; remains in place for the rare curated/admin write path via the legacy &#x60;POST /cve/status/{id}&#x60; route.
     * Responses:
     *  - 200: Updated synth CVE entry
     *  - 400: Validation error (e.g. invalid CVE id or unsupported status)
     *  - 403: Caller does not have access to this artifact's repository
     *  - 404: No scan_findings rows match (artifact_id, cve_id)
     *
     * @param artifactId Artifact UUID
     * @param cveId CVE identifier (e.g. CVE-2019-10744)
     * @param updateCveStatusRequest 
     * @return [CveHistoryEntry]
     */
    @POST("api/v1/sbom/cve/status/by-artifact/{artifact_id}/by-cve/{cve_id}")
    suspend fun updateCveStatusByArtifactCve(@Path("artifact_id") artifactId: java.util.UUID, @Path("cve_id") cveId: kotlin.String, @Body updateCveStatusRequest: UpdateCveStatusRequest): Response<CveHistoryEntry>

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
