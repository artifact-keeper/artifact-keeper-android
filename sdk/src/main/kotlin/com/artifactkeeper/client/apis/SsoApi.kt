package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.CreateLdapConfigRequest
import com.artifactkeeper.client.models.CreateOidcConfigRequest
import com.artifactkeeper.client.models.CreateSamlConfigRequest
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.ExchangeCodeRequest
import com.artifactkeeper.client.models.ExchangeCodeResponse
import com.artifactkeeper.client.models.LdapConfigResponse
import com.artifactkeeper.client.models.LdapLoginRequest
import com.artifactkeeper.client.models.LdapTestResult
import com.artifactkeeper.client.models.OidcConfigResponse
import com.artifactkeeper.client.models.SamlConfigResponse
import com.artifactkeeper.client.models.SsoProviderInfo
import com.artifactkeeper.client.models.ToggleRequest
import com.artifactkeeper.client.models.UpdateLdapConfigRequest
import com.artifactkeeper.client.models.UpdateOidcConfigRequest
import com.artifactkeeper.client.models.UpdateSamlConfigRequest

interface SsoApi {
    /**
     * POST api/v1/admin/sso/ldap
     * Create a new LDAP provider configuration
     * 
     * Responses:
     *  - 200: LDAP configuration created
     *  - 401: Unauthorized
     *
     * @param createLdapConfigRequest 
     * @return [LdapConfigResponse]
     */
    @POST("api/v1/admin/sso/ldap")
    suspend fun createLdap(@Body createLdapConfigRequest: CreateLdapConfigRequest): Response<LdapConfigResponse>

    /**
     * POST api/v1/admin/sso/oidc
     * Create a new OIDC provider configuration
     * 
     * Responses:
     *  - 200: OIDC configuration created
     *  - 401: Unauthorized
     *
     * @param createOidcConfigRequest 
     * @return [OidcConfigResponse]
     */
    @POST("api/v1/admin/sso/oidc")
    suspend fun createOidc(@Body createOidcConfigRequest: CreateOidcConfigRequest): Response<OidcConfigResponse>

    /**
     * POST api/v1/admin/sso/saml
     * Create a new SAML provider configuration
     * 
     * Responses:
     *  - 200: SAML configuration created
     *  - 401: Unauthorized
     *
     * @param createSamlConfigRequest 
     * @return [SamlConfigResponse]
     */
    @POST("api/v1/admin/sso/saml")
    suspend fun createSaml(@Body createSamlConfigRequest: CreateSamlConfigRequest): Response<SamlConfigResponse>

    /**
     * DELETE api/v1/admin/sso/ldap/{id}
     * Delete an LDAP provider configuration
     * 
     * Responses:
     *  - 200: LDAP configuration deleted
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id LDAP configuration ID
     * @return [Unit]
     */
    @DELETE("api/v1/admin/sso/ldap/{id}")
    suspend fun deleteLdap(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * DELETE api/v1/admin/sso/oidc/{id}
     * Delete an OIDC provider configuration
     * 
     * Responses:
     *  - 200: OIDC configuration deleted
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id OIDC configuration ID
     * @return [Unit]
     */
    @DELETE("api/v1/admin/sso/oidc/{id}")
    suspend fun deleteOidc(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * DELETE api/v1/admin/sso/saml/{id}
     * Delete a SAML provider configuration
     * 
     * Responses:
     *  - 200: SAML configuration deleted
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id SAML configuration ID
     * @return [Unit]
     */
    @DELETE("api/v1/admin/sso/saml/{id}")
    suspend fun deleteSaml(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/auth/sso/exchange
     * Exchange a short-lived code for access and refresh tokens
     * 
     * Responses:
     *  - 200: Token exchange successful
     *  - 400: Invalid or expired exchange code
     *
     * @param exchangeCodeRequest 
     * @return [ExchangeCodeResponse]
     */
    @POST("api/v1/auth/sso/exchange")
    suspend fun exchangeCode(@Body exchangeCodeRequest: ExchangeCodeRequest): Response<ExchangeCodeResponse>

    /**
     * GET api/v1/admin/sso/ldap/{id}
     * Get LDAP provider configuration by ID
     * 
     * Responses:
     *  - 200: LDAP configuration details
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id LDAP configuration ID
     * @return [LdapConfigResponse]
     */
    @GET("api/v1/admin/sso/ldap/{id}")
    suspend fun getLdap(@Path("id") id: java.util.UUID): Response<LdapConfigResponse>

    /**
     * GET api/v1/admin/sso/oidc/{id}
     * Get OIDC provider configuration by ID
     * 
     * Responses:
     *  - 200: OIDC configuration details
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id OIDC configuration ID
     * @return [OidcConfigResponse]
     */
    @GET("api/v1/admin/sso/oidc/{id}")
    suspend fun getOidc(@Path("id") id: java.util.UUID): Response<OidcConfigResponse>

    /**
     * GET api/v1/admin/sso/saml/{id}
     * Get SAML provider configuration by ID
     * 
     * Responses:
     *  - 200: SAML configuration details
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id SAML configuration ID
     * @return [SamlConfigResponse]
     */
    @GET("api/v1/admin/sso/saml/{id}")
    suspend fun getSaml(@Path("id") id: java.util.UUID): Response<SamlConfigResponse>

    /**
     * POST api/v1/auth/sso/ldap/{id}/login
     * Authenticate via LDAP
     * 
     * Responses:
     *  - 200: Authentication successful with tokens
     *  - 401: Invalid credentials
     *  - 404: LDAP provider not found
     *
     * @param id LDAP provider configuration ID
     * @param ldapLoginRequest 
     * @return [Unit]
     */
    @POST("api/v1/auth/sso/ldap/{id}/login")
    suspend fun ldapLogin(@Path("id") id: java.util.UUID, @Body ldapLoginRequest: LdapLoginRequest): Response<Unit>

    /**
     * GET api/v1/admin/sso/ldap
     * List all LDAP provider configurations
     * 
     * Responses:
     *  - 200: List of LDAP configurations
     *  - 401: Unauthorized
     *
     * @return [kotlin.collections.List<LdapConfigResponse>]
     */
    @GET("api/v1/admin/sso/ldap")
    suspend fun listLdap(): Response<kotlin.collections.List<LdapConfigResponse>>

    /**
     * GET api/v1/admin/sso/oidc
     * List all OIDC provider configurations
     * 
     * Responses:
     *  - 200: List of OIDC configurations
     *  - 401: Unauthorized
     *
     * @return [kotlin.collections.List<OidcConfigResponse>]
     */
    @GET("api/v1/admin/sso/oidc")
    suspend fun listOidc(): Response<kotlin.collections.List<OidcConfigResponse>>

    /**
     * GET api/v1/auth/sso/providers
     * List all enabled SSO providers
     * 
     * Responses:
     *  - 200: List of enabled SSO providers
     *
     * @return [kotlin.collections.List<SsoProviderInfo>]
     */
    @GET("api/v1/auth/sso/providers")
    suspend fun listProviders(): Response<kotlin.collections.List<SsoProviderInfo>>

    /**
     * GET api/v1/admin/sso/saml
     * List all SAML provider configurations
     * 
     * Responses:
     *  - 200: List of SAML configurations
     *  - 401: Unauthorized
     *
     * @return [kotlin.collections.List<SamlConfigResponse>]
     */
    @GET("api/v1/admin/sso/saml")
    suspend fun listSaml(): Response<kotlin.collections.List<SamlConfigResponse>>

    /**
     * GET api/v1/admin/sso/providers
     * List all enabled SSO providers (admin view)
     * 
     * Responses:
     *  - 200: List of enabled SSO providers
     *  - 401: Unauthorized
     *
     * @return [kotlin.collections.List<SsoProviderInfo>]
     */
    @GET("api/v1/admin/sso/providers")
    suspend fun listSsoProvidersAdmin(): Response<kotlin.collections.List<SsoProviderInfo>>

    /**
     * GET api/v1/auth/sso/oidc/{id}/callback
     * Handle OIDC authorization callback
     * 
     * Responses:
     *  - 307: Redirect to frontend with exchange code
     *  - 400: Invalid callback parameters
     *
     * @param id OIDC provider configuration ID
     * @param code 
     * @param state 
     * @return [Unit]
     */
    @GET("api/v1/auth/sso/oidc/{id}/callback")
    suspend fun oidcCallback(@Path("id") id: java.util.UUID, @Query("code") code: kotlin.String, @Query("state") state: kotlin.String): Response<Unit>

    /**
     * GET api/v1/auth/sso/oidc/{id}/login
     * Initiate OIDC login redirect
     * 
     * Responses:
     *  - 307: Redirect to OIDC authorization endpoint
     *  - 404: OIDC provider not found
     *
     * @param id OIDC provider configuration ID
     * @return [Unit]
     */
    @GET("api/v1/auth/sso/oidc/{id}/login")
    suspend fun oidcLogin(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/auth/sso/saml/{id}/acs
     * Handle SAML Assertion Consumer Service (ACS) callback
     * 
     * Responses:
     *  - 307: Redirect to frontend with exchange code
     *  - 400: Invalid SAML response
     *
     * @param id SAML provider configuration ID
     * @param saMLResponse 
     * @param relayState  (optional)
     * @return [Unit]
     */
    @FormUrlEncoded
    @POST("api/v1/auth/sso/saml/{id}/acs")
    suspend fun samlAcs(@Path("id") id: java.util.UUID, @Field("SAMLResponse") saMLResponse: kotlin.String, @Field("RelayState") relayState: kotlin.String? = null): Response<Unit>

    /**
     * GET api/v1/auth/sso/saml/{id}/login
     * Initiate SAML login redirect
     * 
     * Responses:
     *  - 307: Redirect to SAML IdP SSO endpoint
     *  - 404: SAML provider not found
     *
     * @param id SAML provider configuration ID
     * @return [Unit]
     */
    @GET("api/v1/auth/sso/saml/{id}/login")
    suspend fun samlLogin(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/admin/sso/ldap/{id}/test
     * Test an LDAP provider connection
     * 
     * Responses:
     *  - 200: LDAP connection test result
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id LDAP configuration ID
     * @return [LdapTestResult]
     */
    @POST("api/v1/admin/sso/ldap/{id}/test")
    suspend fun testLdap(@Path("id") id: java.util.UUID): Response<LdapTestResult>

    /**
     * PATCH api/v1/admin/sso/ldap/{id}/toggle
     * Toggle an LDAP provider enabled/disabled
     * 
     * Responses:
     *  - 200: LDAP configuration toggled
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id LDAP configuration ID
     * @param toggleRequest 
     * @return [Unit]
     */
    @PATCH("api/v1/admin/sso/ldap/{id}/toggle")
    suspend fun toggleLdap(@Path("id") id: java.util.UUID, @Body toggleRequest: ToggleRequest): Response<Unit>

    /**
     * PATCH api/v1/admin/sso/oidc/{id}/toggle
     * Toggle an OIDC provider enabled/disabled
     * 
     * Responses:
     *  - 200: OIDC configuration toggled
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id OIDC configuration ID
     * @param toggleRequest 
     * @return [Unit]
     */
    @PATCH("api/v1/admin/sso/oidc/{id}/toggle")
    suspend fun toggleOidc(@Path("id") id: java.util.UUID, @Body toggleRequest: ToggleRequest): Response<Unit>

    /**
     * PATCH api/v1/admin/sso/saml/{id}/toggle
     * Toggle a SAML provider enabled/disabled
     * 
     * Responses:
     *  - 200: SAML configuration toggled
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id SAML configuration ID
     * @param toggleRequest 
     * @return [Unit]
     */
    @PATCH("api/v1/admin/sso/saml/{id}/toggle")
    suspend fun toggleSaml(@Path("id") id: java.util.UUID, @Body toggleRequest: ToggleRequest): Response<Unit>

    /**
     * PUT api/v1/admin/sso/ldap/{id}
     * Update an LDAP provider configuration
     * 
     * Responses:
     *  - 200: LDAP configuration updated
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id LDAP configuration ID
     * @param updateLdapConfigRequest 
     * @return [LdapConfigResponse]
     */
    @PUT("api/v1/admin/sso/ldap/{id}")
    suspend fun updateLdap(@Path("id") id: java.util.UUID, @Body updateLdapConfigRequest: UpdateLdapConfigRequest): Response<LdapConfigResponse>

    /**
     * PUT api/v1/admin/sso/oidc/{id}
     * Update an OIDC provider configuration
     * 
     * Responses:
     *  - 200: OIDC configuration updated
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id OIDC configuration ID
     * @param updateOidcConfigRequest 
     * @return [OidcConfigResponse]
     */
    @PUT("api/v1/admin/sso/oidc/{id}")
    suspend fun updateOidc(@Path("id") id: java.util.UUID, @Body updateOidcConfigRequest: UpdateOidcConfigRequest): Response<OidcConfigResponse>

    /**
     * PUT api/v1/admin/sso/saml/{id}
     * Update a SAML provider configuration
     * 
     * Responses:
     *  - 200: SAML configuration updated
     *  - 401: Unauthorized
     *  - 404: Configuration not found
     *
     * @param id SAML configuration ID
     * @param updateSamlConfigRequest 
     * @return [SamlConfigResponse]
     */
    @PUT("api/v1/admin/sso/saml/{id}")
    suspend fun updateSaml(@Path("id") id: java.util.UUID, @Body updateSamlConfigRequest: UpdateSamlConfigRequest): Response<SamlConfigResponse>

}
