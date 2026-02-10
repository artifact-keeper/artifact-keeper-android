package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.FormatHandlerResponse
import com.artifactkeeper.client.models.InstallFromGitRequest
import com.artifactkeeper.client.models.InstallFromLocalRequest
import com.artifactkeeper.client.models.PluginConfigResponse
import com.artifactkeeper.client.models.PluginInstallResponse
import com.artifactkeeper.client.models.PluginListResponse
import com.artifactkeeper.client.models.PluginResponse
import com.artifactkeeper.client.models.TestFormatRequest
import com.artifactkeeper.client.models.TestFormatResponse
import com.artifactkeeper.client.models.UpdatePluginConfigRequest
import com.artifactkeeper.client.models.WasmPluginResponse

interface PluginsApi {
    /**
     * POST api/v1/formats/{format_key}/disable
     * Disable a format handler (T042)
     * 
     * Responses:
     *  - 200: Format handler disabled
     *
     * @param formatKey Format handler key
     * @return [FormatHandlerResponse]
     */
    @POST("api/v1/formats/{format_key}/disable")
    suspend fun disableFormatHandler(@Path("format_key") formatKey: kotlin.String): Response<FormatHandlerResponse>

    /**
     * POST api/v1/plugins/{id}/disable
     * Disable plugin
     * 
     * Responses:
     *  - 200: Plugin disabled successfully
     *  - 404: Plugin not found
     *
     * @param id Plugin ID
     * @return [Unit]
     */
    @POST("api/v1/plugins/{id}/disable")
    suspend fun disablePlugin(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/formats/{format_key}/enable
     * Enable a format handler (T041)
     * 
     * Responses:
     *  - 200: Format handler enabled
     *
     * @param formatKey Format handler key
     * @return [FormatHandlerResponse]
     */
    @POST("api/v1/formats/{format_key}/enable")
    suspend fun enableFormatHandler(@Path("format_key") formatKey: kotlin.String): Response<FormatHandlerResponse>

    /**
     * POST api/v1/plugins/{id}/enable
     * Enable plugin
     * 
     * Responses:
     *  - 200: Plugin enabled successfully
     *  - 404: Plugin not found
     *
     * @param id Plugin ID
     * @return [Unit]
     */
    @POST("api/v1/plugins/{id}/enable")
    suspend fun enablePlugin(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/formats/{format_key}
     * Get a format handler by key (T040)
     * 
     * Responses:
     *  - 200: Format handler details
     *  - 404: Format handler not found
     *
     * @param formatKey Format handler key
     * @return [FormatHandlerResponse]
     */
    @GET("api/v1/formats/{format_key}")
    suspend fun getFormatHandler(@Path("format_key") formatKey: kotlin.String): Response<FormatHandlerResponse>

    /**
     * GET api/v1/plugins/{id}
     * Get plugin details
     * 
     * Responses:
     *  - 200: Plugin details
     *  - 404: Plugin not found
     *
     * @param id Plugin ID
     * @return [PluginResponse]
     */
    @GET("api/v1/plugins/{id}")
    suspend fun getPlugin(@Path("id") id: java.util.UUID): Response<PluginResponse>

    /**
     * GET api/v1/plugins/{id}/config
     * Get plugin configuration
     * 
     * Responses:
     *  - 200: Plugin configuration
     *  - 404: Plugin not found
     *
     * @param id Plugin ID
     * @return [PluginConfigResponse]
     */
    @GET("api/v1/plugins/{id}/config")
    suspend fun getPluginConfig(@Path("id") id: java.util.UUID): Response<PluginConfigResponse>

    /**
     * GET api/v1/plugins/{id}/events
     * Get plugin events (T026)
     * 
     * Responses:
     *  - 200: Plugin events
     *
     * @param id Plugin ID
     * @param limit  (optional)
     * @return [kotlin.collections.List<kotlinx.serialization.json.JsonElement>]
     */
    @GET("api/v1/plugins/{id}/events")
    suspend fun getPluginEvents(@Path("id") id: java.util.UUID, @Query("limit") limit: kotlin.Long? = null): Response<kotlin.collections.List<kotlinx.serialization.json.JsonElement>>

    /**
     * POST api/v1/plugins/install/git
     * Install a plugin from a Git repository (T021)
     * 
     * Responses:
     *  - 200: Plugin installed from Git
     *
     * @param installFromGitRequest 
     * @return [PluginInstallResponse]
     */
    @POST("api/v1/plugins/install/git")
    suspend fun installFromGit(@Body installFromGitRequest: InstallFromGitRequest): Response<PluginInstallResponse>

    /**
     * POST api/v1/plugins/install/local
     * Install a plugin from local filesystem path (T063) This endpoint is intended for development use only.
     * 
     * Responses:
     *  - 200: Plugin installed from local path
     *  - 400: Invalid path
     *
     * @param installFromLocalRequest 
     * @return [PluginInstallResponse]
     */
    @POST("api/v1/plugins/install/local")
    suspend fun installFromLocal(@Body installFromLocalRequest: InstallFromLocalRequest): Response<PluginInstallResponse>

    /**
     * POST api/v1/plugins/install/zip
     * Install a plugin from a ZIP file (T034)
     * 
     * Responses:
     *  - 200: Plugin installed from ZIP
     *  - 400: Missing or invalid ZIP file
     *
     * @return [PluginInstallResponse]
     */
    @POST("api/v1/plugins/install/zip")
    suspend fun installFromZip(): Response<PluginInstallResponse>

    /**
     * POST api/v1/plugins
     * Install plugin from uploaded package
     * 
     * Responses:
     *  - 200: Plugin installed successfully
     *  - 400: Invalid plugin manifest
     *  - 409: Plugin already installed
     *
     * @return [PluginResponse]
     */
    @POST("api/v1/plugins")
    suspend fun installPlugin(): Response<PluginResponse>

    /**
     * GET api/v1/formats
     * List all format handlers (T039)
     * 
     * Responses:
     *  - 200: List of format handlers
     *
     * @param type  (optional)
     * @param enabled  (optional)
     * @return [kotlin.collections.List<FormatHandlerResponse>]
     */
    @GET("api/v1/formats")
    suspend fun listFormatHandlers(@Query("type") type: kotlin.String? = null, @Query("enabled") enabled: kotlin.Boolean? = null): Response<kotlin.collections.List<FormatHandlerResponse>>

    /**
     * GET api/v1/plugins
     * List installed plugins
     * 
     * Responses:
     *  - 200: List of installed plugins
     *
     * @param status  (optional)
     * @param type  (optional)
     * @return [PluginListResponse]
     */
    @GET("api/v1/plugins")
    suspend fun listPlugins(@Query("status") status: kotlin.String? = null, @Query("type") type: kotlin.String? = null): Response<PluginListResponse>

    /**
     * POST api/v1/plugins/{id}/reload
     * Reload a plugin (hot-reload) (T048)
     * 
     * Responses:
     *  - 200: Plugin reloaded successfully
     *
     * @param id Plugin ID
     * @return [WasmPluginResponse]
     */
    @POST("api/v1/plugins/{id}/reload")
    suspend fun reloadPlugin(@Path("id") id: java.util.UUID): Response<WasmPluginResponse>

    /**
     * POST api/v1/formats/{format_key}/test
     * Test a format handler with sample content (T062)
     * 
     * Responses:
     *  - 200: Format handler test results
     *
     * @param formatKey Format handler key
     * @param testFormatRequest 
     * @return [TestFormatResponse]
     */
    @POST("api/v1/formats/{format_key}/test")
    suspend fun testFormatHandler(@Path("format_key") formatKey: kotlin.String, @Body testFormatRequest: TestFormatRequest): Response<TestFormatResponse>

    /**
     * DELETE api/v1/plugins/{id}
     * Uninstall plugin
     * 
     * Responses:
     *  - 200: Plugin uninstalled successfully
     *  - 404: Plugin not found
     *
     * @param id Plugin ID
     * @return [Unit]
     */
    @DELETE("api/v1/plugins/{id}")
    suspend fun uninstallPlugin(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/plugins/{id}/config
     * Update plugin configuration
     * 
     * Responses:
     *  - 200: Plugin configuration updated
     *  - 404: Plugin not found
     *
     * @param id Plugin ID
     * @param updatePluginConfigRequest 
     * @return [PluginConfigResponse]
     */
    @POST("api/v1/plugins/{id}/config")
    suspend fun updatePluginConfig(@Path("id") id: java.util.UUID, @Body updatePluginConfigRequest: UpdatePluginConfigRequest): Response<PluginConfigResponse>

}
