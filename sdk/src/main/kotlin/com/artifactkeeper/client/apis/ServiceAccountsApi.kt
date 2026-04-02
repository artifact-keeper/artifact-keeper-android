package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.CreateServiceAccountRequest
import com.artifactkeeper.client.models.CreateTokenRequest
import com.artifactkeeper.client.models.CreateTokenResponse
import com.artifactkeeper.client.models.PreviewRepoSelectorRequest
import com.artifactkeeper.client.models.PreviewRepoSelectorResponse
import com.artifactkeeper.client.models.ServiceAccountListResponse
import com.artifactkeeper.client.models.ServiceAccountResponse
import com.artifactkeeper.client.models.TokenListResponse
import com.artifactkeeper.client.models.UpdateServiceAccountRequest

interface ServiceAccountsApi {
    /**
     * POST api/v1/service-accounts
     * Create a new service account
     * 
     * Responses:
     *  - 201: Service account created
     *  - 400: Validation error
     *  - 403: Not admin
     *
     * @param createServiceAccountRequest 
     * @return [ServiceAccountResponse]
     */
    @POST("api/v1/service-accounts")
    suspend fun createServiceAccount(@Body createServiceAccountRequest: CreateServiceAccountRequest): Response<ServiceAccountResponse>

    /**
     * POST api/v1/service-accounts/{id}/tokens
     * Create a token for a service account
     * 
     * Responses:
     *  - 200: Token created (value shown once)
     *  - 404: Service account not found
     *
     * @param id Service account ID
     * @param createTokenRequest 
     * @return [CreateTokenResponse]
     */
    @POST("api/v1/service-accounts/{id}/tokens")
    suspend fun createToken(@Path("id") id: java.util.UUID, @Body createTokenRequest: CreateTokenRequest): Response<CreateTokenResponse>

    /**
     * DELETE api/v1/service-accounts/{id}
     * Delete a service account
     * 
     * Responses:
     *  - 204: Deleted
     *  - 404: Not found
     *
     * @param id Service account ID
     * @return [Unit]
     */
    @DELETE("api/v1/service-accounts/{id}")
    suspend fun deleteServiceAccount(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/service-accounts/{id}
     * Get a service account by ID
     * 
     * Responses:
     *  - 200: Service account details
     *  - 404: Not found
     *
     * @param id Service account ID
     * @return [ServiceAccountResponse]
     */
    @GET("api/v1/service-accounts/{id}")
    suspend fun getServiceAccount(@Path("id") id: java.util.UUID): Response<ServiceAccountResponse>

    /**
     * GET api/v1/service-accounts
     * List all service accounts
     * 
     * Responses:
     *  - 200: List of service accounts
     *  - 403: Not admin
     *
     * @return [ServiceAccountListResponse]
     */
    @GET("api/v1/service-accounts")
    suspend fun listServiceAccounts(): Response<ServiceAccountListResponse>

    /**
     * GET api/v1/service-accounts/{id}/tokens
     * List tokens for a service account
     * 
     * Responses:
     *  - 200: Token list
     *  - 404: Service account not found
     *
     * @param id Service account ID
     * @return [TokenListResponse]
     */
    @GET("api/v1/service-accounts/{id}/tokens")
    suspend fun listTokens(@Path("id") id: java.util.UUID): Response<TokenListResponse>

    /**
     * POST api/v1/service-accounts/repo-selector/preview
     * Preview which repositories match a given repo selector.
     * Does not create or modify anything. Useful for testing selectors before attaching them to a token.
     * Responses:
     *  - 200: Matched repositories
     *  - 400: Invalid selector
     *  - 403: Not admin
     *
     * @param previewRepoSelectorRequest 
     * @return [PreviewRepoSelectorResponse]
     */
    @POST("api/v1/service-accounts/repo-selector/preview")
    suspend fun previewRepoSelector(@Body previewRepoSelectorRequest: PreviewRepoSelectorRequest): Response<PreviewRepoSelectorResponse>

    /**
     * DELETE api/v1/service-accounts/{id}/tokens/{token_id}
     * Revoke a token from a service account
     * 
     * Responses:
     *  - 204: Token revoked
     *  - 404: Not found
     *
     * @param id Service account ID
     * @param tokenId Token ID
     * @return [Unit]
     */
    @DELETE("api/v1/service-accounts/{id}/tokens/{token_id}")
    suspend fun revokeToken(@Path("id") id: java.util.UUID, @Path("token_id") tokenId: java.util.UUID): Response<Unit>

    /**
     * PATCH api/v1/service-accounts/{id}
     * Update a service account
     * 
     * Responses:
     *  - 200: Updated
     *  - 404: Not found
     *
     * @param id Service account ID
     * @param updateServiceAccountRequest 
     * @return [ServiceAccountResponse]
     */
    @PATCH("api/v1/service-accounts/{id}")
    suspend fun updateServiceAccount(@Path("id") id: java.util.UUID, @Body updateServiceAccountRequest: UpdateServiceAccountRequest): Response<ServiceAccountResponse>

}
