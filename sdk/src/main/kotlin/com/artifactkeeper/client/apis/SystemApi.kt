package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.SystemConfigResponse

interface SystemApi {
    /**
     * GET api/v1/system/config
     * Return public runtime configuration.
     * No authentication required. This endpoint exposes only non-sensitive configuration values that help frontends adapt their behavior (e.g. showing upload limits, conditionally rendering scanner UI, initiating OIDC flows).
     * Responses:
     *  - 200: Public runtime configuration
     *
     * @return [SystemConfigResponse]
     */
    @GET("api/v1/system/config")
    suspend fun getSystemConfig(): Response<SystemConfigResponse>

}
