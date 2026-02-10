package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.AddVirtualMemberRequest
import com.artifactkeeper.client.models.ArtifactListResponse
import com.artifactkeeper.client.models.ArtifactResponse
import com.artifactkeeper.client.models.CreateRepositoryRequest
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.RepositoryListResponse
import com.artifactkeeper.client.models.RepositoryResponse
import com.artifactkeeper.client.models.TreeResponse
import com.artifactkeeper.client.models.UpdateRepositoryRequest
import com.artifactkeeper.client.models.UpdateVirtualMembersRequest
import com.artifactkeeper.client.models.VirtualMemberResponse
import com.artifactkeeper.client.models.VirtualMembersListResponse

interface RepositoriesApi {
    /**
     * POST api/v1/repositories/{key}/members
     * Add a member to a virtual repository
     * 
     * Responses:
     *  - 200: Member added
     *  - 401: Authentication required
     *  - 404: Repository or member not found
     *
     * @param key Repository key
     * @param addVirtualMemberRequest 
     * @return [VirtualMemberResponse]
     */
    @POST("api/v1/repositories/{key}/members")
    suspend fun addVirtualMember(@Path("key") key: kotlin.String, @Body addVirtualMemberRequest: AddVirtualMemberRequest): Response<VirtualMemberResponse>

    /**
     * POST api/v1/repositories
     * Create a new repository
     * 
     * Responses:
     *  - 200: Repository created
     *  - 401: Authentication required
     *  - 409: Repository key already exists
     *
     * @param createRepositoryRequest 
     * @return [RepositoryResponse]
     */
    @POST("api/v1/repositories")
    suspend fun createRepository(@Body createRepositoryRequest: CreateRepositoryRequest): Response<RepositoryResponse>

    /**
     * DELETE api/v1/repositories/{key}/artifacts/{path}
     * Delete artifact
     * 
     * Responses:
     *  - 200: Artifact deleted
     *  - 401: Authentication required
     *  - 404: Artifact not found
     *
     * @param key Repository key
     * @param path Artifact path
     * @return [Unit]
     */
    @DELETE("api/v1/repositories/{key}/artifacts/{path}")
    suspend fun deleteArtifact(@Path("key") key: kotlin.String, @Path("path") path: kotlin.String): Response<Unit>

    /**
     * DELETE api/v1/repositories/{key}
     * Delete repository
     * 
     * Responses:
     *  - 200: Repository deleted
     *  - 401: Authentication required
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [Unit]
     */
    @DELETE("api/v1/repositories/{key}")
    suspend fun deleteRepository(@Path("key") key: kotlin.String): Response<Unit>

    /**
     * GET api/v1/repositories/{key}/download/{path}
     * Download artifact
     * 
     * Responses:
     *  - 200: Artifact binary content
     *  - 302: Redirect to S3 presigned URL
     *  - 404: Artifact not found
     *
     * @param key Repository key
     * @param path Artifact path
     * @return [Unit]
     */
    @GET("api/v1/repositories/{key}/download/{path}")
    suspend fun downloadArtifact(@Path("key") key: kotlin.String, @Path("path") path: kotlin.String): Response<Unit>

    /**
     * GET api/v1/repositories/{key}
     * Get repository details
     * 
     * Responses:
     *  - 200: Repository details
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [RepositoryResponse]
     */
    @GET("api/v1/repositories/{key}")
    suspend fun getRepository(@Path("key") key: kotlin.String): Response<RepositoryResponse>

    /**
     * GET api/v1/repositories/{key}/artifacts/{path}
     * Get artifact metadata
     * 
     * Responses:
     *  - 200: Artifact metadata
     *  - 404: Artifact not found
     *
     * @param key Repository key
     * @param path Artifact path
     * @return [ArtifactResponse]
     */
    @GET("api/v1/repositories/{key}/artifacts/{path}")
    suspend fun getRepositoryArtifactMetadata(@Path("key") key: kotlin.String, @Path("path") path: kotlin.String): Response<ArtifactResponse>

    /**
     * GET api/v1/tree
     * 
     * 
     * Responses:
     *  - 200: Virtual folder tree for the repository
     *  - 400: Validation error (e.g. missing repository_key)
     *  - 404: Repository not found
     *
     * @param repositoryKey Repository key to browse (optional)
     * @param path Path prefix to browse within the repository (optional)
     * @param includeMetadata Whether to include metadata in the response (optional)
     * @return [TreeResponse]
     */
    @GET("api/v1/tree")
    suspend fun getTree(@Query("repository_key") repositoryKey: kotlin.String? = null, @Query("path") path: kotlin.String? = null, @Query("include_metadata") includeMetadata: kotlin.Boolean? = null): Response<TreeResponse>

    /**
     * GET api/v1/repositories/{key}/artifacts
     * List artifacts in repository
     * 
     * Responses:
     *  - 200: List of artifacts
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param page  (optional)
     * @param perPage  (optional)
     * @param q  (optional)
     * @param pathPrefix  (optional)
     * @return [ArtifactListResponse]
     */
    @GET("api/v1/repositories/{key}/artifacts")
    suspend fun listArtifacts(@Path("key") key: kotlin.String, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null, @Query("q") q: kotlin.String? = null, @Query("path_prefix") pathPrefix: kotlin.String? = null): Response<ArtifactListResponse>

    /**
     * GET api/v1/repositories
     * List repositories
     * 
     * Responses:
     *  - 200: List of repositories
     *
     * @param page  (optional)
     * @param perPage  (optional)
     * @param format  (optional)
     * @param type  (optional)
     * @param q  (optional)
     * @return [RepositoryListResponse]
     */
    @GET("api/v1/repositories")
    suspend fun listRepositories(@Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null, @Query("format") format: kotlin.String? = null, @Query("type") type: kotlin.String? = null, @Query("q") q: kotlin.String? = null): Response<RepositoryListResponse>

    /**
     * GET api/v1/repositories/{key}/members
     * List virtual repository members
     * 
     * Responses:
     *  - 200: List of virtual repository members
     *  - 400: Repository is not virtual
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [VirtualMembersListResponse]
     */
    @GET("api/v1/repositories/{key}/members")
    suspend fun listVirtualMembers(@Path("key") key: kotlin.String): Response<VirtualMembersListResponse>

    /**
     * DELETE api/v1/repositories/{key}/members/{member_key}
     * Remove a member from a virtual repository
     * 
     * Responses:
     *  - 200: Member removed
     *  - 400: Repository is not virtual
     *  - 401: Authentication required
     *  - 404: Repository or member not found
     *
     * @param key Repository key
     * @param memberKey Member repository key
     * @return [Unit]
     */
    @DELETE("api/v1/repositories/{key}/members/{member_key}")
    suspend fun removeVirtualMember(@Path("key") key: kotlin.String, @Path("member_key") memberKey: kotlin.String): Response<Unit>

    /**
     * PATCH api/v1/repositories/{key}
     * Update repository
     * 
     * Responses:
     *  - 200: Repository updated
     *  - 401: Authentication required
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param updateRepositoryRequest 
     * @return [RepositoryResponse]
     */
    @PATCH("api/v1/repositories/{key}")
    suspend fun updateRepository(@Path("key") key: kotlin.String, @Body updateRepositoryRequest: UpdateRepositoryRequest): Response<RepositoryResponse>

    /**
     * PUT api/v1/repositories/{key}/members
     * Update priorities for all members (bulk reorder)
     * 
     * Responses:
     *  - 200: Members updated
     *  - 400: Repository is not virtual
     *  - 401: Authentication required
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param updateVirtualMembersRequest 
     * @return [VirtualMembersListResponse]
     */
    @PUT("api/v1/repositories/{key}/members")
    suspend fun updateVirtualMembers(@Path("key") key: kotlin.String, @Body updateVirtualMembersRequest: UpdateVirtualMembersRequest): Response<VirtualMembersListResponse>

    /**
     * PUT api/v1/repositories/{key}/artifacts/{path}
     * Upload artifact
     * 
     * Responses:
     *  - 200: Artifact uploaded
     *  - 401: Authentication required
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param path Artifact path
     * @param requestBody 
     * @return [ArtifactResponse]
     */
    @PUT("api/v1/repositories/{key}/artifacts/{path}")
    suspend fun uploadArtifact(@Path("key") key: kotlin.String, @Path("path") path: kotlin.String, @Body requestBody: kotlin.collections.List<kotlin.Int>): Response<ArtifactResponse>

}
