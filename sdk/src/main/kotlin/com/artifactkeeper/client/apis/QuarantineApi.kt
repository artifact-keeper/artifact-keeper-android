package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.QuarantineActionResponse
import com.artifactkeeper.client.models.QuarantineStatusResponse
import com.artifactkeeper.client.models.RejectRequest

interface QuarantineApi {
    /**
     * GET api/v1/quarantine/{artifact_id}
     * Get quarantine status for an artifact
     * 
     * Responses:
     *  - 200: Quarantine status
     *  - 401: Authentication required
     *  - 404: Artifact not found
     *
     * @param artifactId Artifact ID
     * @return [QuarantineStatusResponse]
     */
    @GET("api/v1/quarantine/{artifact_id}")
    suspend fun getQuarantineStatus(@Path("artifact_id") artifactId: java.util.UUID): Response<QuarantineStatusResponse>

    /**
     * POST api/v1/quarantine/{artifact_id}/reject
     * Reject a quarantined artifact (admin only)
     * 
     * Responses:
     *  - 200: Artifact rejected
     *  - 401: Authentication required
     *  - 403: Admin access required
     *  - 404: Artifact not found
     *  - 409: Artifact is not in quarantined state
     *
     * @param artifactId Artifact ID
     * @param rejectRequest 
     * @return [QuarantineActionResponse]
     */
    @POST("api/v1/quarantine/{artifact_id}/reject")
    suspend fun rejectQuarantinedArtifact(@Path("artifact_id") artifactId: java.util.UUID, @Body rejectRequest: RejectRequest): Response<QuarantineActionResponse>

    /**
     * POST api/v1/quarantine/{artifact_id}/release
     * Release an artifact from quarantine (admin only)
     * 
     * Responses:
     *  - 200: Artifact released
     *  - 401: Authentication required
     *  - 403: Admin access required
     *  - 404: Artifact not found
     *  - 409: Artifact is not in quarantined state
     *
     * @param artifactId Artifact ID
     * @return [QuarantineActionResponse]
     */
    @POST("api/v1/quarantine/{artifact_id}/release")
    suspend fun releaseArtifact(@Path("artifact_id") artifactId: java.util.UUID): Response<QuarantineActionResponse>

}
