package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.AddBuildArtifactsRequest
import com.artifactkeeper.client.models.AddBuildArtifactsResponse
import com.artifactkeeper.client.models.BuildDiffResponse
import com.artifactkeeper.client.models.BuildListResponse
import com.artifactkeeper.client.models.BuildResponse
import com.artifactkeeper.client.models.CreateBuildRequest
import com.artifactkeeper.client.models.UpdateBuildRequest

interface BuildsApi {
    /**
     * POST api/v1/builds/{id}/artifacts
     * Attach artifacts to a build (POST /api/v1/builds/:id/artifacts)
     * 
     * Responses:
     *  - 200: Artifacts added to build
     *  - 401: Authentication required
     *  - 404: Build not found
     *
     * @param id Build ID
     * @param addBuildArtifactsRequest 
     * @return [AddBuildArtifactsResponse]
     */
    @POST("api/v1/builds/{id}/artifacts")
    suspend fun addBuildArtifacts(@Path("id") id: java.util.UUID, @Body addBuildArtifactsRequest: AddBuildArtifactsRequest): Response<AddBuildArtifactsResponse>

    /**
     * POST api/v1/builds
     * Create a new build (POST /api/v1/builds)
     * 
     * Responses:
     *  - 200: Build created successfully
     *  - 401: Authentication required
     *
     * @param createBuildRequest 
     * @return [BuildResponse]
     */
    @POST("api/v1/builds")
    suspend fun createBuild(@Body createBuildRequest: CreateBuildRequest): Response<BuildResponse>

    /**
     * GET api/v1/builds/{id}
     * Get a build by ID
     * 
     * Responses:
     *  - 200: Build details
     *  - 404: Build not found
     *
     * @param id Build ID
     * @return [BuildResponse]
     */
    @GET("api/v1/builds/{id}")
    suspend fun getBuild(@Path("id") id: java.util.UUID): Response<BuildResponse>

    /**
     * GET api/v1/builds/diff
     * Get diff between two builds
     * 
     * Responses:
     *  - 200: Diff between two builds
     *
     * @param buildA 
     * @param buildB 
     * @return [BuildDiffResponse]
     */
    @GET("api/v1/builds/diff")
    suspend fun getBuildDiff(@Query("build_a") buildA: java.util.UUID, @Query("build_b") buildB: java.util.UUID): Response<BuildDiffResponse>

    /**
     * GET api/v1/builds
     * List builds
     * 
     * Responses:
     *  - 200: List of builds
     *
     * @param page  (optional)
     * @param perPage  (optional)
     * @param status  (optional)
     * @param search  (optional)
     * @param sortBy  (optional)
     * @param sortOrder  (optional)
     * @return [BuildListResponse]
     */
    @GET("api/v1/builds")
    suspend fun listBuilds(@Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null, @Query("status") status: kotlin.String? = null, @Query("search") search: kotlin.String? = null, @Query("sort_by") sortBy: kotlin.String? = null, @Query("sort_order") sortOrder: kotlin.String? = null): Response<BuildListResponse>

    /**
     * PUT api/v1/builds/{id}
     * Update build status (PUT /api/v1/builds/:id)
     * 
     * Responses:
     *  - 200: Build updated successfully
     *  - 401: Authentication required
     *  - 404: Build not found
     *
     * @param id Build ID
     * @param updateBuildRequest 
     * @return [BuildResponse]
     */
    @PUT("api/v1/builds/{id}")
    suspend fun updateBuild(@Path("id") id: java.util.UUID, @Body updateBuildRequest: UpdateBuildRequest): Response<BuildResponse>

}
