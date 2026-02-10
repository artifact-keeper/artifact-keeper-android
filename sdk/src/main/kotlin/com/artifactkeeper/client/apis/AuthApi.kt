package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.CreateApiTokenRequest
import com.artifactkeeper.client.models.CreateApiTokenResponse
import com.artifactkeeper.client.models.CreateTicketRequest
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.LoginRequest
import com.artifactkeeper.client.models.LoginResponse
import com.artifactkeeper.client.models.RefreshTokenRequest
import com.artifactkeeper.client.models.SetupStatusResponse
import com.artifactkeeper.client.models.TicketResponse
import com.artifactkeeper.client.models.TotpCodeRequest
import com.artifactkeeper.client.models.TotpDisableRequest
import com.artifactkeeper.client.models.TotpEnableResponse
import com.artifactkeeper.client.models.TotpSetupResponse
import com.artifactkeeper.client.models.TotpVerifyRequest
import com.artifactkeeper.client.models.UserResponse

interface AuthApi {
    /**
     * POST api/v1/auth/tokens
     * Create a new API token for the current user
     * 
     * Responses:
     *  - 200: API token created
     *  - 401: Not authenticated
     *
     * @param createApiTokenRequest 
     * @return [CreateApiTokenResponse]
     */
    @POST("api/v1/auth/tokens")
    suspend fun createApiToken(@Body createApiTokenRequest: CreateApiTokenRequest): Response<CreateApiTokenResponse>

    /**
     * POST api/v1/auth/ticket
     * Create a short-lived, single-use download/stream ticket for the current user. The ticket can be passed as a &#x60;?ticket&#x3D;&#x60; query parameter on endpoints that cannot use &#x60;Authorization&#x60; headers (e.g. &#x60;&lt;a&gt;&#x60; downloads, &#x60;EventSource&#x60; SSE).
     * 
     * Responses:
     *  - 200: Download ticket created
     *  - 401: Not authenticated
     *
     * @param createTicketRequest 
     * @return [TicketResponse]
     */
    @POST("api/v1/auth/ticket")
    suspend fun createDownloadTicket(@Body createTicketRequest: CreateTicketRequest): Response<TicketResponse>

    /**
     * POST api/v1/auth/totp/disable
     * Disable TOTP for the authenticated user (requires password and current TOTP code)
     * 
     * Responses:
     *  - 200: TOTP disabled successfully
     *  - 401: Invalid password or TOTP code
     *
     * @param totpDisableRequest 
     * @return [Unit]
     */
    @POST("api/v1/auth/totp/disable")
    suspend fun disableTotp(@Body totpDisableRequest: TotpDisableRequest): Response<Unit>

    /**
     * POST api/v1/auth/totp/enable
     * Enable TOTP by verifying the initial code and generating backup codes
     * 
     * Responses:
     *  - 200: TOTP enabled with backup codes
     *  - 401: Unauthorized or invalid TOTP code
     *
     * @param totpCodeRequest 
     * @return [TotpEnableResponse]
     */
    @POST("api/v1/auth/totp/enable")
    suspend fun enableTotp(@Body totpCodeRequest: TotpCodeRequest): Response<TotpEnableResponse>

    /**
     * GET api/v1/auth/me
     * Get current user info
     * 
     * Responses:
     *  - 200: Current user info
     *  - 401: Not authenticated
     *  - 404: User not found
     *
     * @return [UserResponse]
     */
    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    /**
     * POST api/v1/auth/login
     * Login with credentials
     * 
     * Responses:
     *  - 200: Login successful
     *  - 401: Invalid credentials
     *
     * @param loginRequest 
     * @return [LoginResponse]
     */
    @POST("api/v1/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    /**
     * POST api/v1/auth/logout
     * Logout current session
     * 
     * Responses:
     *  - 200: Logout successful, auth cookies cleared
     *
     * @return [Unit]
     */
    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    /**
     * POST api/v1/auth/refresh
     * Refresh access token
     * 
     * Responses:
     *  - 200: Token refreshed successfully
     *  - 401: Invalid or expired refresh token
     *
     * @param refreshTokenRequest 
     * @return [LoginResponse]
     */
    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body refreshTokenRequest: RefreshTokenRequest): Response<LoginResponse>

    /**
     * DELETE api/v1/auth/tokens/{token_id}
     * Revoke an API token
     * 
     * Responses:
     *  - 200: API token revoked
     *  - 401: Not authenticated
     *  - 404: Token not found
     *
     * @param tokenId ID of the API token to revoke
     * @return [Unit]
     */
    @DELETE("api/v1/auth/tokens/{token_id}")
    suspend fun revokeApiToken(@Path("token_id") tokenId: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/setup/status
     * Returns whether initial setup (password change) is required.
     * 
     * Responses:
     *  - 200: Setup status retrieved
     *
     * @return [SetupStatusResponse]
     */
    @GET("api/v1/setup/status")
    suspend fun setupStatus(): Response<SetupStatusResponse>

    /**
     * POST api/v1/auth/totp/setup
     * Generate a new TOTP secret and QR code URL for the authenticated user
     * 
     * Responses:
     *  - 200: TOTP setup details with secret and QR code URL
     *  - 401: Unauthorized
     *
     * @return [TotpSetupResponse]
     */
    @POST("api/v1/auth/totp/setup")
    suspend fun setupTotp(): Response<TotpSetupResponse>

    /**
     * POST api/v1/auth/totp/verify
     * Verify TOTP code during login (exchanges totp_token + code for full auth tokens)
     * 
     * Responses:
     *  - 200: TOTP verified, authentication tokens returned
     *  - 401: Invalid TOTP code or token
     *
     * @param totpVerifyRequest 
     * @return [LoginResponse]
     */
    @POST("api/v1/auth/totp/verify")
    suspend fun verifyTotp(@Body totpVerifyRequest: TotpVerifyRequest): Response<LoginResponse>

}
