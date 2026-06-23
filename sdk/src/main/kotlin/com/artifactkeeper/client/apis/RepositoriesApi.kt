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
import com.artifactkeeper.client.models.CacheTtlResponse
import com.artifactkeeper.client.models.CreateRepositoryRequest
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.InvalidateCacheResponse
import com.artifactkeeper.client.models.PypiTrackRequest
import com.artifactkeeper.client.models.PypiTrackResponse
import com.artifactkeeper.client.models.PypiTracksListResponse
import com.artifactkeeper.client.models.RepositoryListResponse
import com.artifactkeeper.client.models.RepositoryResponse
import com.artifactkeeper.client.models.RoutingRulesResponse
import com.artifactkeeper.client.models.SetCacheTtlRequest
import com.artifactkeeper.client.models.SetRoutingRulesRequest
import com.artifactkeeper.client.models.TreeResponse
import com.artifactkeeper.client.models.UpdateRepositoryRequest
import com.artifactkeeper.client.models.UpdateVirtualMembersRequest
import com.artifactkeeper.client.models.UpstreamAuthRequest
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
     *  - 409: Member already exists in virtual repository
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
     *  - 403: Insufficient permissions
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
     *  - 409: Artifact is immutable (released) and cannot be deleted
     *
     * @param key Repository key
     * @param path Artifact path
     * @return [Unit]
     */
    @DELETE("api/v1/repositories/{key}/artifacts/{path}")
    suspend fun deleteArtifact(@Path("key") key: kotlin.String, @Path("path") path: kotlin.String): Response<Unit>

    /**
     * DELETE api/v1/repositories/{key}/pypi-tracks/{project}
     * Remove a PEP 708 &#x60;tracks&#x60; declaration, restoring local-precedence isolation for that project name (#1600).
     * 
     * Responses:
     *  - 204: tracks declaration removed (idempotent)
     *  - 401: Authentication required
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param project Project name (PEP 503 normalized server-side)
     * @return [Unit]
     */
    @DELETE("api/v1/repositories/{key}/pypi-tracks/{project}")
    suspend fun deletePypiTrack(@Path("key") key: kotlin.String, @Path("project") project: kotlin.String): Response<Unit>

    /**
     * DELETE api/v1/repositories/{key}
     * Delete repository
     * 
     * Responses:
     *  - 200: Repository deleted
     *  - 401: Authentication required
     *  - 403: Insufficient permissions
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [Unit]
     */
    @DELETE("api/v1/repositories/{key}")
    suspend fun deleteRepository(@Path("key") key: kotlin.String): Response<Unit>

    /**
     * DELETE api/v1/repositories/{key}/routing-rules
     * Delete all routing rules for a repository
     * 
     * Responses:
     *  - 200: Routing rules deleted
     *  - 401: Authentication required
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [Unit]
     */
    @DELETE("api/v1/repositories/{key}/routing-rules")
    suspend fun deleteRoutingRules(@Path("key") key: kotlin.String): Response<Unit>

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
     * GET api/v1/repositories/{key}/cache-ttl
     * Get the proxy cache TTL for a repository
     * 
     * Responses:
     *  - 200: Current cache TTL
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [CacheTtlResponse]
     */
    @GET("api/v1/repositories/{key}/cache-ttl")
    suspend fun getCacheTtl(@Path("key") key: kotlin.String): Response<CacheTtlResponse>

    /**
     * GET api/v1/tree/content
     * 
     * 
     * Responses:
     *  - 200: Artifact file content
     *  - 400: Validation error
     *  - 404: Artifact not found
     *
     * @param repositoryKey Repository key containing the artifact
     * @param path Full artifact path within the repository
     * @param maxBytes Optional maximum number of bytes to return (truncates the response) (optional)
     * @return [Unit]
     */
    @GET("api/v1/tree/content")
    suspend fun getContent(@Query("repository_key") repositoryKey: kotlin.String, @Query("path") path: kotlin.String, @Query("max_bytes") maxBytes: kotlin.Long? = null): Response<Unit>

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
     * GET api/v1/repositories/{key}/routing-rules
     * Get routing rules for a repository
     * 
     * Responses:
     *  - 200: Current routing rules
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [RoutingRulesResponse]
     */
    @GET("api/v1/repositories/{key}/routing-rules")
    suspend fun getRoutingRules(@Path("key") key: kotlin.String): Response<RoutingRulesResponse>

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
     * POST api/v1/repositories/{key}/cache/invalidate
     * Invalidate a single cached artifact entry on a Remote (proxy) repository (#1539).
     * Mirrors the auth + repo-access pattern of &#x60;set_cache_ttl&#x60;. Idempotent: invalidating a path that was never cached (or was already evicted) still returns 200, matching the underlying &#x60;ProxyService::invalidate_cache&#x60; contract (which ignores delete-of-missing on the storage backend).
     * Responses:
     *  - 200: Cache entry invalidated (or was already absent)
     *  - 400: Validation error (e.g. non-remote repo or invalid path)
     *  - 401: Authentication required
     *  - 404: Repository not found
     *  - 503: Proxy service not configured on this deployment
     *
     * @param key Repository key
     * @param path Artifact path to evict from the proxy cache. Same shape as the path segment of &#x60;GET /api/v1/repositories/{key}/artifacts/{path}&#x60;. Path-traversal segments such as &#x60;..&#x60; are rejected by &#x60;ProxyService::cache_storage_key&#x60; (covered by &#x60;test_invalidate_cache_by_key_rejects_invalid_path&#x60;).
     * @return [InvalidateCacheResponse]
     */
    @POST("api/v1/repositories/{key}/cache/invalidate")
    suspend fun invalidateCache(@Path("key") key: kotlin.String, @Query("path") path: kotlin.String): Response<InvalidateCacheResponse>

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
     * @param groupBy Server-side artifact grouping.  Supported values: - &#x60;maven_component&#x60;: Maven/Gradle artifacts are grouped by   groupId, artifactId, and version.  Individual files (jar, pom,   checksums) appear in the &#x60;artifact_files&#x60; array of each component. - &#x60;docker_tag&#x60;: Docker/OCI artifacts are grouped by (image, tag),   with &#x60;total_size_bytes&#x60; summed across the manifest config and   referenced layer blobs.  The grouped rows are returned in the   &#x60;docker_tags&#x60; array. (optional)
     * @return [ArtifactListResponse]
     */
    @GET("api/v1/repositories/{key}/artifacts")
    suspend fun listArtifacts(@Path("key") key: kotlin.String, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null, @Query("q") q: kotlin.String? = null, @Query("path_prefix") pathPrefix: kotlin.String? = null, @Query("group_by") groupBy: kotlin.String? = null): Response<ArtifactListResponse>

    /**
     * GET api/v1/repositories/{key}/pypi-tracks
     * List the PEP 708 &#x60;tracks&#x60; declarations on a repository.
     * 
     * Responses:
     *  - 200: tracks declarations
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [PypiTracksListResponse]
     */
    @GET("api/v1/repositories/{key}/pypi-tracks")
    suspend fun listPypiTracks(@Path("key") key: kotlin.String): Response<PypiTracksListResponse>

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
     *  - 200: List of virtual repository members (filtered to caller-visible members)
     *  - 400: Repository is not virtual
     *  - 401: Authentication required
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [VirtualMembersListResponse]
     */
    @GET("api/v1/repositories/{key}/members")
    suspend fun listVirtualMembers(@Path("key") key: kotlin.String): Response<VirtualMembersListResponse>

    /**
     * PUT api/v1/repositories/{key}/pypi-tracks/{project}
     * Declare (upsert) that a locally-owned PyPI project tracks an upstream one, allowing a virtual repository to merge versions across members for that name instead of isolating it (PEP 708, #1600).
     * 
     * Responses:
     *  - 200: tracks declaration stored
     *  - 400: Invalid request or repository type
     *  - 401: Authentication required
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param project Project name (PEP 503 normalized server-side)
     * @param pypiTrackRequest 
     * @return [PypiTrackResponse]
     */
    @PUT("api/v1/repositories/{key}/pypi-tracks/{project}")
    suspend fun putPypiTrack(@Path("key") key: kotlin.String, @Path("project") project: kotlin.String, @Body pypiTrackRequest: PypiTrackRequest): Response<PypiTrackResponse>

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
     * PUT api/v1/repositories/{key}/cache-ttl
     * Set the proxy cache TTL for a repository
     * 
     * Responses:
     *  - 200: Cache TTL updated
     *  - 400: Invalid TTL value
     *  - 401: Authentication required
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param setCacheTtlRequest 
     * @return [CacheTtlResponse]
     */
    @PUT("api/v1/repositories/{key}/cache-ttl")
    suspend fun setCacheTtl(@Path("key") key: kotlin.String, @Body setCacheTtlRequest: SetCacheTtlRequest): Response<CacheTtlResponse>

    /**
     * POST api/v1/repositories/{key}/routing-rules
     * Set routing rules for a repository
     * Routing rules rewrite the request path before it is forwarded to the upstream server. This is useful for proxying resources like GitHub Releases where the client-facing path structure differs from the upstream URL layout. Rules are evaluated in order and the first match wins.
     * Responses:
     *  - 200: Routing rules saved
     *  - 400: Invalid rule (bad regex or capture reference)
     *  - 401: Authentication required
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param setRoutingRulesRequest 
     * @return [RoutingRulesResponse]
     */
    @POST("api/v1/repositories/{key}/routing-rules")
    suspend fun setRoutingRules(@Path("key") key: kotlin.String, @Body setRoutingRulesRequest: SetRoutingRulesRequest): Response<RoutingRulesResponse>

    /**
     * PUT api/v1/repositories/{key}/upstream-auth
     * Set or remove upstream auth for a remote repository
     * 
     * Responses:
     *  - 200: Upstream auth updated
     *  - 400: Invalid auth type or missing fields
     *  - 401: Authentication required
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param upstreamAuthRequest 
     * @return [Unit]
     */
    @PUT("api/v1/repositories/{key}/upstream-auth")
    suspend fun setUpstreamAuth(@Path("key") key: kotlin.String, @Body upstreamAuthRequest: UpstreamAuthRequest): Response<Unit>

    /**
     * POST api/v1/repositories/{key}/test-upstream
     * Test connectivity to the upstream URL of a remote repository
     * 
     * Responses:
     *  - 200: Upstream reachable
     *  - 400: Repository is not remote or has no upstream URL
     *  - 401: Authentication required
     *  - 404: Repository not found
     *  - 502: Upstream unreachable
     *
     * @param key Repository key
     * @return [Unit]
     */
    @POST("api/v1/repositories/{key}/test-upstream")
    suspend fun testUpstream(@Path("key") key: kotlin.String): Response<Unit>

    /**
     * PATCH api/v1/repositories/{key}
     * Update repository
     * 
     * Responses:
     *  - 200: Repository updated
     *  - 401: Authentication required
     *  - 403: Insufficient permissions
     *  - 404: Repository not found
     *  - 409: Repository key already exists
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
