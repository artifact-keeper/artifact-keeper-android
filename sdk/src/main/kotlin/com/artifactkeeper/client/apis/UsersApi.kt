package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.ApiTokenCreatedResponse
import com.artifactkeeper.client.models.ApiTokenListResponse
import com.artifactkeeper.client.models.AssignRoleRequest
import com.artifactkeeper.client.models.ChangePasswordRequest
import com.artifactkeeper.client.models.CreateApiTokenRequest
import com.artifactkeeper.client.models.CreateUserRequest
import com.artifactkeeper.client.models.CreateUserResponse
import com.artifactkeeper.client.models.ResetPasswordResponse
import com.artifactkeeper.client.models.RoleListResponse
import com.artifactkeeper.client.models.UpdateUserRequest
import com.artifactkeeper.client.models.UserListResponse
import com.artifactkeeper.client.models.UserResponse

interface UsersApi {
    /**
     * POST api/v1/users/{id}/roles
     * Assign role to user
     * 
     * Responses:
     *  - 200: Role assigned successfully
     *
     * @param id User ID
     * @param assignRoleRequest 
     * @return [Unit]
     */
    @POST("api/v1/users/{id}/roles")
    suspend fun assignRole(@Path("id") id: java.util.UUID, @Body assignRoleRequest: AssignRoleRequest): Response<Unit>

    /**
     * POST api/v1/users/{id}/password
     * Change user password
     * 
     * Responses:
     *  - 200: Password changed successfully
     *  - 401: Current password is incorrect
     *  - 403: Cannot change other users' passwords
     *  - 404: User not found
     *  - 422: Validation error
     *
     * @param id User ID
     * @param changePasswordRequest 
     * @return [Unit]
     */
    @POST("api/v1/users/{id}/password")
    suspend fun changePassword(@Path("id") id: java.util.UUID, @Body changePasswordRequest: ChangePasswordRequest): Response<Unit>

    /**
     * POST api/v1/users
     * Create user
     * 
     * Responses:
     *  - 200: User created successfully
     *  - 409: User already exists
     *  - 422: Validation error
     *
     * @param createUserRequest 
     * @return [CreateUserResponse]
     */
    @POST("api/v1/users")
    suspend fun createUser(@Body createUserRequest: CreateUserRequest): Response<CreateUserResponse>

    /**
     * POST api/v1/users/{id}/tokens
     * Create API token
     * 
     * Responses:
     *  - 200: API token created successfully
     *  - 403: Cannot create tokens for other users
     *
     * @param id User ID
     * @param createApiTokenRequest 
     * @return [ApiTokenCreatedResponse]
     */
    @POST("api/v1/users/{id}/tokens")
    suspend fun createUserApiToken(@Path("id") id: java.util.UUID, @Body createApiTokenRequest: CreateApiTokenRequest): Response<ApiTokenCreatedResponse>

    /**
     * DELETE api/v1/users/{id}
     * Delete user
     * 
     * Responses:
     *  - 200: User deleted successfully
     *  - 404: User not found
     *  - 422: Cannot delete yourself
     *
     * @param id User ID
     * @return [Unit]
     */
    @DELETE("api/v1/users/{id}")
    suspend fun deleteUser(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/users/{id}
     * Get user details
     * 
     * Responses:
     *  - 200: User details
     *  - 404: User not found
     *
     * @param id User ID
     * @return [UserResponse]
     */
    @GET("api/v1/users/{id}")
    suspend fun getUser(@Path("id") id: java.util.UUID): Response<UserResponse>

    /**
     * GET api/v1/users/{id}/roles
     * Get user roles
     * 
     * Responses:
     *  - 200: List of user roles
     *
     * @param id User ID
     * @return [RoleListResponse]
     */
    @GET("api/v1/users/{id}/roles")
    suspend fun getUserRoles(@Path("id") id: java.util.UUID): Response<RoleListResponse>

    /**
     * GET api/v1/users/{id}/tokens
     * List user&#39;s API tokens
     * 
     * Responses:
     *  - 200: List of API tokens
     *  - 403: Cannot view other users' tokens
     *
     * @param id User ID
     * @return [ApiTokenListResponse]
     */
    @GET("api/v1/users/{id}/tokens")
    suspend fun listUserTokens(@Path("id") id: java.util.UUID): Response<ApiTokenListResponse>

    /**
     * GET api/v1/users
     * List users
     * 
     * Responses:
     *  - 200: List of users
     *
     * @param search  (optional)
     * @param isActive  (optional)
     * @param isAdmin  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [UserListResponse]
     */
    @GET("api/v1/users")
    suspend fun listUsers(@Query("search") search: kotlin.String? = null, @Query("is_active") isActive: kotlin.Boolean? = null, @Query("is_admin") isAdmin: kotlin.Boolean? = null, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null): Response<UserListResponse>

    /**
     * POST api/v1/users/{id}/password/reset
     * Reset user password (admin only) Generates a new temporary password and sets must_change_password&#x3D;true
     * 
     * Responses:
     *  - 200: Password reset successfully
     *  - 403: Only administrators can reset passwords
     *  - 404: User not found
     *  - 422: Validation error
     *
     * @param id User ID
     * @return [ResetPasswordResponse]
     */
    @POST("api/v1/users/{id}/password/reset")
    suspend fun resetPassword(@Path("id") id: java.util.UUID): Response<ResetPasswordResponse>

    /**
     * DELETE api/v1/users/{id}/roles/{role_id}
     * Revoke role from user
     * 
     * Responses:
     *  - 200: Role revoked successfully
     *  - 404: Role assignment not found
     *
     * @param id User ID
     * @param roleId Role ID
     * @return [Unit]
     */
    @DELETE("api/v1/users/{id}/roles/{role_id}")
    suspend fun revokeRole(@Path("id") id: java.util.UUID, @Path("role_id") roleId: java.util.UUID): Response<Unit>

    /**
     * DELETE api/v1/users/{id}/tokens/{token_id}
     * Revoke API token
     * 
     * Responses:
     *  - 200: API token revoked successfully
     *  - 403: Cannot revoke other users' tokens
     *
     * @param id User ID
     * @param tokenId API token ID
     * @return [Unit]
     */
    @DELETE("api/v1/users/{id}/tokens/{token_id}")
    suspend fun revokeUserApiToken(@Path("id") id: java.util.UUID, @Path("token_id") tokenId: java.util.UUID): Response<Unit>

    /**
     * PATCH api/v1/users/{id}
     * Update user
     * 
     * Responses:
     *  - 200: User updated successfully
     *  - 404: User not found
     *
     * @param id User ID
     * @param updateUserRequest 
     * @return [UserResponse]
     */
    @PATCH("api/v1/users/{id}")
    suspend fun updateUser(@Path("id") id: java.util.UUID, @Body updateUserRequest: UpdateUserRequest): Response<UserResponse>

}
