package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.AddArtifactLabelRequest
import com.artifactkeeper.client.models.ArtifactLabelResponse
import com.artifactkeeper.client.models.ArtifactLabelsListResponse
import com.artifactkeeper.client.models.SetArtifactLabelsRequest

interface ArtifactLabelsApi {
    /**
     * POST api/v1/artifacts/{id}/labels/{label_key}
     * Add or update a single label on an artifact
     * 
     * Responses:
     *  - 200: Label added/updated
     *  - 404: Artifact not found
     *
     * @param id Artifact ID
     * @param labelKey Label key to set
     * @param addArtifactLabelRequest 
     * @return [ArtifactLabelResponse]
     */
    @POST("api/v1/artifacts/{id}/labels/{label_key}")
    suspend fun addArtifactLabel(@Path("id") id: java.util.UUID, @Path("label_key") labelKey: kotlin.String, @Body addArtifactLabelRequest: AddArtifactLabelRequest): Response<ArtifactLabelResponse>

    /**
     * DELETE api/v1/artifacts/{id}/labels/{label_key}
     * Delete a label by key from an artifact
     * 
     * Responses:
     *  - 204: Label removed
     *  - 404: Artifact not found
     *
     * @param id Artifact ID
     * @param labelKey Label key to remove
     * @return [Unit]
     */
    @DELETE("api/v1/artifacts/{id}/labels/{label_key}")
    suspend fun deleteArtifactLabel(@Path("id") id: java.util.UUID, @Path("label_key") labelKey: kotlin.String): Response<Unit>

    /**
     * GET api/v1/artifacts/{id}/labels
     * List all labels on an artifact
     * 
     * Responses:
     *  - 200: Labels retrieved
     *  - 404: Artifact not found
     *
     * @param id Artifact ID
     * @return [ArtifactLabelsListResponse]
     */
    @GET("api/v1/artifacts/{id}/labels")
    suspend fun listArtifactLabels(@Path("id") id: java.util.UUID): Response<ArtifactLabelsListResponse>

    /**
     * PUT api/v1/artifacts/{id}/labels
     * Set all labels on an artifact (replaces existing)
     * 
     * Responses:
     *  - 200: Labels updated
     *  - 404: Artifact not found
     *
     * @param id Artifact ID
     * @param setArtifactLabelsRequest 
     * @return [ArtifactLabelsListResponse]
     */
    @PUT("api/v1/artifacts/{id}/labels")
    suspend fun setArtifactLabels(@Path("id") id: java.util.UUID, @Body setArtifactLabelsRequest: SetArtifactLabelsRequest): Response<ArtifactLabelsListResponse>

}
