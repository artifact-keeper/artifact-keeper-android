package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.AddPeerLabelRequest
import com.artifactkeeper.client.models.PeerLabelResponse
import com.artifactkeeper.client.models.PeerLabelsListResponse
import com.artifactkeeper.client.models.SetPeerLabelsRequest

interface PeerInstanceLabelsApi {
    /**
     * POST api/v1/peers/{id}/labels/{label_key}
     * Add or update a single label on a peer instance
     * 
     * Responses:
     *  - 200: Label added/updated
     *  - 404: Peer instance not found
     *
     * @param id Peer instance ID
     * @param labelKey Label key to set
     * @param addPeerLabelRequest 
     * @return [PeerLabelResponse]
     */
    @POST("api/v1/peers/{id}/labels/{label_key}")
    suspend fun addLabel(@Path("id") id: java.util.UUID, @Path("label_key") labelKey: kotlin.String, @Body addPeerLabelRequest: AddPeerLabelRequest): Response<PeerLabelResponse>

    /**
     * DELETE api/v1/peers/{id}/labels/{label_key}
     * Delete a label by key from a peer instance
     * 
     * Responses:
     *  - 204: Label removed
     *  - 404: Peer instance or label not found
     *
     * @param id Peer instance ID
     * @param labelKey Label key to remove
     * @return [Unit]
     */
    @DELETE("api/v1/peers/{id}/labels/{label_key}")
    suspend fun deleteLabel(@Path("id") id: java.util.UUID, @Path("label_key") labelKey: kotlin.String): Response<Unit>

    /**
     * GET api/v1/peers/{id}/labels
     * List all labels on a peer instance
     * 
     * Responses:
     *  - 200: Labels retrieved
     *  - 404: Peer instance not found
     *
     * @param id Peer instance ID
     * @return [PeerLabelsListResponse]
     */
    @GET("api/v1/peers/{id}/labels")
    suspend fun listLabels(@Path("id") id: java.util.UUID): Response<PeerLabelsListResponse>

    /**
     * PUT api/v1/peers/{id}/labels
     * Set all labels on a peer instance (replaces existing)
     * 
     * Responses:
     *  - 200: Labels updated
     *  - 404: Peer instance not found
     *
     * @param id Peer instance ID
     * @param setPeerLabelsRequest 
     * @return [PeerLabelsListResponse]
     */
    @PUT("api/v1/peers/{id}/labels")
    suspend fun setLabels(@Path("id") id: java.util.UUID, @Body setPeerLabelsRequest: SetPeerLabelsRequest): Response<PeerLabelsListResponse>

}
