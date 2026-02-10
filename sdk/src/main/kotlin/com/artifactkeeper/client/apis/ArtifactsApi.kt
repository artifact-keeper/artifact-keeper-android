package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.ArtifactMetadataResponse
import com.artifactkeeper.client.models.ArtifactResponse
import com.artifactkeeper.client.models.ArtifactStatsResponse
import com.artifactkeeper.client.models.ErrorResponse

interface ArtifactsApi {
    /**
     * GET api/v1/artifacts/{id}
     * Get artifact by ID
     * 
     * Responses:
     *  - 200: Artifact details
     *  - 404: Artifact not found
     *
     * @param id Artifact ID
     * @return [ArtifactResponse]
     */
    @GET("api/v1/artifacts/{id}")
    suspend fun getArtifact(@Path("id") id: java.util.UUID): Response<ArtifactResponse>

    /**
     * GET api/v1/artifacts/{id}/metadata
     * Get artifact metadata by artifact ID
     * 
     * Responses:
     *  - 200: Artifact metadata
     *  - 404: Artifact or metadata not found
     *
     * @param id Artifact ID
     * @return [ArtifactMetadataResponse]
     */
    @GET("api/v1/artifacts/{id}/metadata")
    suspend fun getArtifactMetadata(@Path("id") id: java.util.UUID): Response<ArtifactMetadataResponse>

    /**
     * GET api/v1/artifacts/{id}/stats
     * Get artifact download statistics
     * 
     * Responses:
     *  - 200: Artifact download statistics
     *  - 404: Artifact not found
     *
     * @param id Artifact ID
     * @return [ArtifactStatsResponse]
     */
    @GET("api/v1/artifacts/{id}/stats")
    suspend fun getArtifactStats(@Path("id") id: java.util.UUID): Response<ArtifactStatsResponse>

}
