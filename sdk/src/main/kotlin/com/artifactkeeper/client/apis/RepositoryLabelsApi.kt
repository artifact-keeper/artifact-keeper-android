package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.AddLabelRequest
import com.artifactkeeper.client.models.LabelResponse
import com.artifactkeeper.client.models.LabelsListResponse
import com.artifactkeeper.client.models.SetLabelsRequest

interface RepositoryLabelsApi {
    /**
     * POST api/v1/repositories/{key}/labels/{label_key}
     * Add or update a single label
     * 
     * Responses:
     *  - 200: Label added/updated
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param labelKey Label key to set
     * @param addLabelRequest 
     * @return [LabelResponse]
     */
    @POST("api/v1/repositories/{key}/labels/{label_key}")
    suspend fun addRepoLabel(@Path("key") key: kotlin.String, @Path("label_key") labelKey: kotlin.String, @Body addLabelRequest: AddLabelRequest): Response<LabelResponse>

    /**
     * DELETE api/v1/repositories/{key}/labels/{label_key}
     * Delete a label by key
     * 
     * Responses:
     *  - 204: Label removed
     *  - 404: Repository or label not found
     *
     * @param key Repository key
     * @param labelKey Label key to remove
     * @return [Unit]
     */
    @DELETE("api/v1/repositories/{key}/labels/{label_key}")
    suspend fun deleteRepoLabel(@Path("key") key: kotlin.String, @Path("label_key") labelKey: kotlin.String): Response<Unit>

    /**
     * GET api/v1/repositories/{key}/labels
     * List all labels on a repository
     * 
     * Responses:
     *  - 200: Labels retrieved
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [LabelsListResponse]
     */
    @GET("api/v1/repositories/{key}/labels")
    suspend fun listRepoLabels(@Path("key") key: kotlin.String): Response<LabelsListResponse>

    /**
     * PUT api/v1/repositories/{key}/labels
     * Set all labels on a repository (replaces existing)
     * 
     * Responses:
     *  - 200: Labels updated
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param setLabelsRequest 
     * @return [LabelsListResponse]
     */
    @PUT("api/v1/repositories/{key}/labels")
    suspend fun setRepoLabels(@Path("key") key: kotlin.String, @Body setLabelsRequest: SetLabelsRequest): Response<LabelsListResponse>

}
