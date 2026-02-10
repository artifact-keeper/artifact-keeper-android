package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.CreatePermissionRequest
import com.artifactkeeper.client.models.PermissionListResponse
import com.artifactkeeper.client.models.PermissionResponse

interface PermissionsApi {
    /**
     * POST api/v1/permissions
     * Create a permission
     * 
     * Responses:
     *  - 200: Permission created successfully
     *  - 409: Permission already exists
     *  - 500: Internal server error
     *
     * @param createPermissionRequest 
     * @return [PermissionResponse]
     */
    @POST("api/v1/permissions")
    suspend fun createPermission(@Body createPermissionRequest: CreatePermissionRequest): Response<PermissionResponse>

    /**
     * DELETE api/v1/permissions/{id}
     * Delete a permission
     * 
     * Responses:
     *  - 200: Permission deleted successfully
     *  - 404: Permission not found
     *  - 500: Internal server error
     *
     * @param id Permission ID
     * @return [Unit]
     */
    @DELETE("api/v1/permissions/{id}")
    suspend fun deletePermission(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/permissions/{id}
     * Get a permission by ID
     * 
     * Responses:
     *  - 200: Permission details
     *  - 404: Permission not found
     *  - 500: Internal server error
     *
     * @param id Permission ID
     * @return [PermissionResponse]
     */
    @GET("api/v1/permissions/{id}")
    suspend fun getPermission(@Path("id") id: java.util.UUID): Response<PermissionResponse>

    /**
     * GET api/v1/permissions
     * List permissions
     * 
     * Responses:
     *  - 200: List of permissions
     *  - 500: Internal server error
     *
     * @param principalType  (optional)
     * @param principalId  (optional)
     * @param targetType  (optional)
     * @param targetId  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [PermissionListResponse]
     */
    @GET("api/v1/permissions")
    suspend fun listPermissions(@Query("principal_type") principalType: kotlin.String? = null, @Query("principal_id") principalId: java.util.UUID? = null, @Query("target_type") targetType: kotlin.String? = null, @Query("target_id") targetId: java.util.UUID? = null, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null): Response<PermissionListResponse>

    /**
     * PUT api/v1/permissions/{id}
     * Update a permission
     * 
     * Responses:
     *  - 200: Permission updated successfully
     *  - 404: Permission not found
     *  - 500: Internal server error
     *
     * @param id Permission ID
     * @param createPermissionRequest 
     * @return [PermissionResponse]
     */
    @PUT("api/v1/permissions/{id}")
    suspend fun updatePermission(@Path("id") id: java.util.UUID, @Body createPermissionRequest: CreatePermissionRequest): Response<PermissionResponse>

}
