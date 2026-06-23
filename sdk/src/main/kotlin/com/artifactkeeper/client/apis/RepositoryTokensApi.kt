package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.CreateRepoTokenRequest
import com.artifactkeeper.client.models.CreateRepoTokenResponse
import com.artifactkeeper.client.models.RepoTokenListResponse
import com.artifactkeeper.client.models.RepoTokenResponse

interface RepositoryTokensApi {
    /**
     * POST api/v1/repositories/{key}/tokens
     * Create a new access token scoped to a repository.
     * The token is automatically restricted to this repository. The plaintext token value is returned only in this response and cannot be retrieved later.
     * Responses:
     *  - 200: Token created (value shown once)
     *  - 400: Validation error
     *  - 401: Not authenticated
     *  - 403: Insufficient permissions
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param createRepoTokenRequest 
     * @return [CreateRepoTokenResponse]
     */
    @POST("api/v1/repositories/{key}/tokens")
    suspend fun createRepoToken(@Path("key") key: kotlin.String, @Body createRepoTokenRequest: CreateRepoTokenRequest): Response<CreateRepoTokenResponse>

    /**
     * GET api/v1/repositories/{key}/tokens/{token_id}
     * Get details of a specific token on a repository.
     * 
     * Responses:
     *  - 200: Token details
     *  - 401: Not authenticated
     *  - 403: Insufficient permissions
     *  - 404: Repository or token not found
     *
     * @param key Repository key
     * @param tokenId Token ID
     * @return [RepoTokenResponse]
     */
    @GET("api/v1/repositories/{key}/tokens/{token_id}")
    suspend fun getRepoToken(@Path("key") key: kotlin.String, @Path("token_id") tokenId: java.util.UUID): Response<RepoTokenResponse>

    /**
     * GET api/v1/repositories/{key}/tokens
     * List all access tokens configured on a repository.
     * 
     * Responses:
     *  - 200: List of tokens on this repository
     *  - 401: Not authenticated
     *  - 403: Insufficient permissions
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [RepoTokenListResponse]
     */
    @GET("api/v1/repositories/{key}/tokens")
    suspend fun listRepoTokens(@Path("key") key: kotlin.String): Response<RepoTokenListResponse>

    /**
     * DELETE api/v1/repositories/{key}/tokens/{token_id}
     * Revoke an access token from a repository.
     * This soft-revokes the token by setting &#x60;revoked_at&#x60;. The token will immediately stop working for authentication.
     * Responses:
     *  - 204: Token revoked
     *  - 401: Not authenticated
     *  - 403: Insufficient permissions
     *  - 404: Repository or token not found
     *
     * @param key Repository key
     * @param tokenId Token ID
     * @return [Unit]
     */
    @DELETE("api/v1/repositories/{key}/tokens/{token_id}")
    suspend fun revokeRepoToken(@Path("key") key: kotlin.String, @Path("token_id") tokenId: java.util.UUID): Response<Unit>

}
