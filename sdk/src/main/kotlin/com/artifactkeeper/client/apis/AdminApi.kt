package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.BackupListResponse
import com.artifactkeeper.client.models.BackupResponse
import com.artifactkeeper.client.models.CleanupRequest
import com.artifactkeeper.client.models.CleanupResponse
import com.artifactkeeper.client.models.CreateBackupRequest
import com.artifactkeeper.client.models.CreateInstanceRequest
import com.artifactkeeper.client.models.ReindexResponse
import com.artifactkeeper.client.models.RemoteInstanceResponse
import com.artifactkeeper.client.models.RestoreRequest
import com.artifactkeeper.client.models.RestoreResponse
import com.artifactkeeper.client.models.SystemSettings
import com.artifactkeeper.client.models.SystemStats

interface AdminApi {
    /**
     * POST api/v1/admin/backups/{id}/cancel
     * Cancel a running backup
     * 
     * Responses:
     *  - 200: Backup cancelled
     *  - 404: Backup not found
     *  - 500: Internal server error
     *
     * @param id Backup ID
     * @return [Unit]
     */
    @POST("api/v1/admin/backups/{id}/cancel")
    suspend fun cancelBackup(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/admin/backups
     * Create backup
     * 
     * Responses:
     *  - 200: Backup created
     *  - 500: Internal server error
     *
     * @param createBackupRequest 
     * @return [BackupResponse]
     */
    @POST("api/v1/admin/backups")
    suspend fun createBackup(@Body createBackupRequest: CreateBackupRequest): Response<BackupResponse>

    /**
     * POST api/v1/instances
     * Create a new remote instance
     * 
     * Responses:
     *  - 200: Created remote instance
     *
     * @param createInstanceRequest 
     * @return [RemoteInstanceResponse]
     */
    @POST("api/v1/instances")
    suspend fun createInstance(@Body createInstanceRequest: CreateInstanceRequest): Response<RemoteInstanceResponse>

    /**
     * DELETE api/v1/admin/backups/{id}
     * Delete a backup
     * 
     * Responses:
     *  - 200: Backup deleted
     *  - 404: Backup not found
     *  - 500: Internal server error
     *
     * @param id Backup ID
     * @return [Unit]
     */
    @DELETE("api/v1/admin/backups/{id}")
    suspend fun deleteBackup(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * DELETE api/v1/instances/{id}
     * Delete a remote instance
     * 
     * Responses:
     *  - 200: Instance deleted
     *
     * @param id Remote instance ID
     * @return [Unit]
     */
    @DELETE("api/v1/instances/{id}")
    suspend fun deleteInstance(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/admin/backups/{id}/execute
     * Execute a pending backup
     * 
     * Responses:
     *  - 200: Backup executed
     *  - 404: Backup not found
     *  - 500: Internal server error
     *
     * @param id Backup ID
     * @return [BackupResponse]
     */
    @POST("api/v1/admin/backups/{id}/execute")
    suspend fun executeBackup(@Path("id") id: java.util.UUID): Response<BackupResponse>

    /**
     * GET api/v1/admin/backups/{id}
     * Get backup by ID
     * 
     * Responses:
     *  - 200: Backup details
     *  - 404: Backup not found
     *  - 500: Internal server error
     *
     * @param id Backup ID
     * @return [BackupResponse]
     */
    @GET("api/v1/admin/backups/{id}")
    suspend fun getBackup(@Path("id") id: java.util.UUID): Response<BackupResponse>

    /**
     * GET api/v1/admin/settings
     * Get system settings
     * 
     * Responses:
     *  - 200: System settings
     *  - 500: Internal server error
     *
     * @return [SystemSettings]
     */
    @GET("api/v1/admin/settings")
    suspend fun getSettings(): Response<SystemSettings>

    /**
     * GET api/v1/admin/stats
     * Get system statistics
     * 
     * Responses:
     *  - 200: System statistics
     *  - 500: Internal server error
     *
     * @return [SystemStats]
     */
    @GET("api/v1/admin/stats")
    suspend fun getSystemStats(): Response<SystemStats>

    /**
     * GET api/v1/admin/backups
     * List backups
     * 
     * Responses:
     *  - 200: List of backups
     *  - 500: Internal server error
     *
     * @param status  (optional)
     * @param type  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [BackupListResponse]
     */
    @GET("api/v1/admin/backups")
    suspend fun listBackups(@Query("status") status: kotlin.String? = null, @Query("type") type: kotlin.String? = null, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null): Response<BackupListResponse>

    /**
     * GET api/v1/instances
     * List all remote instances for the authenticated user
     * 
     * Responses:
     *  - 200: List of remote instances
     *
     * @return [kotlin.collections.List<RemoteInstanceResponse>]
     */
    @GET("api/v1/instances")
    suspend fun listInstances(): Response<kotlin.collections.List<RemoteInstanceResponse>>

    /**
     * DELETE api/v1/instances/{id}/proxy/{path}
     * Proxy a DELETE request to a remote instance
     * 
     * Responses:
     *  - 200: Proxied response
     *
     * @param id Remote instance ID
     * @param path Sub-path to proxy
     * @return [Unit]
     */
    @DELETE("api/v1/instances/{id}/proxy/{path}")
    suspend fun proxyDelete(@Path("id") id: java.util.UUID, @Path("path") path: kotlin.String): Response<Unit>

    /**
     * GET api/v1/instances/{id}/proxy/{path}
     * Proxy a GET request to a remote instance
     * 
     * Responses:
     *  - 200: Proxied response
     *
     * @param id Remote instance ID
     * @param path Sub-path to proxy
     * @return [Unit]
     */
    @GET("api/v1/instances/{id}/proxy/{path}")
    suspend fun proxyGet(@Path("id") id: java.util.UUID, @Path("path") path: kotlin.String): Response<Unit>

    /**
     * POST api/v1/instances/{id}/proxy/{path}
     * Proxy a POST request to a remote instance
     * 
     * Responses:
     *  - 200: Proxied response
     *
     * @param id Remote instance ID
     * @param path Sub-path to proxy
     * @param body 
     * @return [Unit]
     */
    @POST("api/v1/instances/{id}/proxy/{path}")
    suspend fun proxyPost(@Path("id") id: java.util.UUID, @Path("path") path: kotlin.String, @Body body: kotlin.String): Response<Unit>

    /**
     * PUT api/v1/instances/{id}/proxy/{path}
     * Proxy a PUT request to a remote instance
     * 
     * Responses:
     *  - 200: Proxied response
     *
     * @param id Remote instance ID
     * @param path Sub-path to proxy
     * @param body 
     * @return [Unit]
     */
    @PUT("api/v1/instances/{id}/proxy/{path}")
    suspend fun proxyPut(@Path("id") id: java.util.UUID, @Path("path") path: kotlin.String, @Body body: kotlin.String): Response<Unit>

    /**
     * POST api/v1/admin/backups/{id}/restore
     * Restore from backup
     * 
     * Responses:
     *  - 200: Backup restored
     *  - 404: Backup not found
     *  - 500: Internal server error
     *
     * @param id Backup ID
     * @param restoreRequest 
     * @return [RestoreResponse]
     */
    @POST("api/v1/admin/backups/{id}/restore")
    suspend fun restoreBackup(@Path("id") id: java.util.UUID, @Body restoreRequest: RestoreRequest): Response<RestoreResponse>

    /**
     * POST api/v1/admin/cleanup
     * Run cleanup tasks
     * 
     * Responses:
     *  - 200: Cleanup completed
     *  - 500: Internal server error
     *
     * @param cleanupRequest 
     * @return [CleanupResponse]
     */
    @POST("api/v1/admin/cleanup")
    suspend fun runCleanup(@Body cleanupRequest: CleanupRequest): Response<CleanupResponse>

    /**
     * POST api/v1/admin/reindex
     * Trigger a full Meilisearch reindex of all artifacts and repositories.
     * Requires admin privileges and Meilisearch to be configured.
     * Responses:
     *  - 200: Reindex completed
     *  - 401: Admin privileges required
     *  - 500: Internal server error
     *
     * @return [ReindexResponse]
     */
    @POST("api/v1/admin/reindex")
    suspend fun triggerReindex(): Response<ReindexResponse>

    /**
     * POST api/v1/admin/settings
     * Update system settings
     * 
     * Responses:
     *  - 200: Settings updated
     *  - 500: Internal server error
     *
     * @param systemSettings 
     * @return [SystemSettings]
     */
    @POST("api/v1/admin/settings")
    suspend fun updateSettings(@Body systemSettings: SystemSettings): Response<SystemSettings>

}
