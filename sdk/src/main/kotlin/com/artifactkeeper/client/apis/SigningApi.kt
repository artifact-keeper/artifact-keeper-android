package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.CreateKeyPayload
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.KeyListResponse
import com.artifactkeeper.client.models.RepositorySigningConfig
import com.artifactkeeper.client.models.SigningConfigResponse
import com.artifactkeeper.client.models.SigningKeyPublic
import com.artifactkeeper.client.models.UpdateSigningConfigPayload

interface SigningApi {
    /**
     * POST api/v1/signing/keys
     * Create a new signing key.
     * 
     * Responses:
     *  - 200: Created signing key
     *  - 401: Unauthorized
     *
     * @param createKeyPayload 
     * @return [SigningKeyPublic]
     */
    @POST("api/v1/signing/keys")
    suspend fun createKey(@Body createKeyPayload: CreateKeyPayload): Response<SigningKeyPublic>

    /**
     * DELETE api/v1/signing/keys/{key_id}
     * Delete a signing key.
     * 
     * Responses:
     *  - 200: Key deleted
     *  - 401: Unauthorized
     *  - 404: Key not found
     *
     * @param keyId Signing key ID
     * @return [kotlinx.serialization.json.JsonElement]
     */
    @DELETE("api/v1/signing/keys/{key_id}")
    suspend fun deleteKey(@Path("key_id") keyId: java.util.UUID): Response<kotlinx.serialization.json.JsonElement>

    /**
     * GET api/v1/signing/keys/{key_id}
     * Get a signing key by ID.
     * 
     * Responses:
     *  - 200: Signing key details
     *  - 401: Unauthorized
     *  - 404: Key not found
     *
     * @param keyId Signing key ID
     * @return [SigningKeyPublic]
     */
    @GET("api/v1/signing/keys/{key_id}")
    suspend fun getKey(@Path("key_id") keyId: java.util.UUID): Response<SigningKeyPublic>

    /**
     * GET api/v1/signing/keys/{key_id}/public
     * Get the public key in PEM format (for client import).
     * 
     * Responses:
     *  - 200: Public key in PEM format
     *  - 404: Key not found
     *
     * @param keyId Signing key ID
     * @return [kotlin.String]
     */
    @GET("api/v1/signing/keys/{key_id}/public")
    suspend fun getPublicKey(@Path("key_id") keyId: java.util.UUID): Response<kotlin.String>

    /**
     * GET api/v1/signing/repositories/{repo_id}/public-key
     * Get the public key for a repository (convenience endpoint).
     * 
     * Responses:
     *  - 200: Public key in PEM format
     *  - 404: No active signing key for repository
     *
     * @param repoId Repository ID
     * @return [kotlin.String]
     */
    @GET("api/v1/signing/repositories/{repo_id}/public-key")
    suspend fun getRepoPublicKey(@Path("repo_id") repoId: java.util.UUID): Response<kotlin.String>

    /**
     * GET api/v1/signing/repositories/{repo_id}/config
     * Get signing configuration for a repository.
     * 
     * Responses:
     *  - 200: Repository signing configuration
     *  - 401: Unauthorized
     *  - 404: Repository not found
     *
     * @param repoId Repository ID
     * @return [SigningConfigResponse]
     */
    @GET("api/v1/signing/repositories/{repo_id}/config")
    suspend fun getRepoSigningConfig(@Path("repo_id") repoId: java.util.UUID): Response<SigningConfigResponse>

    /**
     * GET api/v1/signing/keys
     * List all signing keys, optionally filtered by repository.
     * 
     * Responses:
     *  - 200: List of signing keys
     *  - 401: Unauthorized
     *
     * @param repositoryId Filter by repository ID (optional)
     * @return [KeyListResponse]
     */
    @GET("api/v1/signing/keys")
    suspend fun listKeys(@Query("repository_id") repositoryId: java.util.UUID? = null): Response<KeyListResponse>

    /**
     * POST api/v1/signing/keys/{key_id}/revoke
     * Revoke (deactivate) a signing key.
     * 
     * Responses:
     *  - 200: Key revoked
     *  - 401: Unauthorized
     *  - 404: Key not found
     *
     * @param keyId Signing key ID
     * @return [kotlinx.serialization.json.JsonElement]
     */
    @POST("api/v1/signing/keys/{key_id}/revoke")
    suspend fun revokeKey(@Path("key_id") keyId: java.util.UUID): Response<kotlinx.serialization.json.JsonElement>

    /**
     * POST api/v1/signing/keys/{key_id}/rotate
     * Rotate a signing key — generates new key, deactivates old one.
     * 
     * Responses:
     *  - 200: Newly generated signing key
     *  - 401: Unauthorized
     *  - 404: Key not found
     *
     * @param keyId Signing key ID to rotate
     * @return [SigningKeyPublic]
     */
    @POST("api/v1/signing/keys/{key_id}/rotate")
    suspend fun rotateKey(@Path("key_id") keyId: java.util.UUID): Response<SigningKeyPublic>

    /**
     * POST api/v1/signing/repositories/{repo_id}/config
     * Update signing configuration for a repository.
     * 
     * Responses:
     *  - 200: Updated signing configuration
     *  - 401: Unauthorized
     *  - 404: Repository not found
     *
     * @param repoId Repository ID
     * @param updateSigningConfigPayload 
     * @return [RepositorySigningConfig]
     */
    @POST("api/v1/signing/repositories/{repo_id}/config")
    suspend fun updateRepoSigningConfig(@Path("repo_id") repoId: java.util.UUID, @Body updateSigningConfigPayload: UpdateSigningConfigPayload): Response<RepositorySigningConfig>

}
