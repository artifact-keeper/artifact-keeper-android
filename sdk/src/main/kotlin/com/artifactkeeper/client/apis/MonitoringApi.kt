package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.AlertState
import com.artifactkeeper.client.models.ServiceHealthEntry
import com.artifactkeeper.client.models.SuppressRequest

interface MonitoringApi {
    /**
     * GET api/v1/admin/monitoring/alerts
     * GET /api/v1/admin/monitoring/alerts
     * 
     * Responses:
     *  - 200: Current alert states
     *
     * @return [kotlin.collections.List<AlertState>]
     */
    @GET("api/v1/admin/monitoring/alerts")
    suspend fun getAlertStates(): Response<kotlin.collections.List<AlertState>>

    /**
     * GET api/v1/admin/monitoring/health-log
     * GET /api/v1/admin/monitoring/health-log
     * 
     * Responses:
     *  - 200: Health log entries
     *
     * @param service  (optional)
     * @param limit  (optional)
     * @return [kotlin.collections.List<ServiceHealthEntry>]
     */
    @GET("api/v1/admin/monitoring/health-log")
    suspend fun getHealthLog(@Query("service") service: kotlin.String? = null, @Query("limit") limit: kotlin.Long? = null): Response<kotlin.collections.List<ServiceHealthEntry>>

    /**
     * POST api/v1/admin/monitoring/check
     * POST /api/v1/admin/monitoring/check - manually trigger health checks
     * 
     * Responses:
     *  - 200: Health check results
     *
     * @return [kotlin.collections.List<ServiceHealthEntry>]
     */
    @POST("api/v1/admin/monitoring/check")
    suspend fun runHealthCheck(): Response<kotlin.collections.List<ServiceHealthEntry>>

    /**
     * POST api/v1/admin/monitoring/alerts/suppress
     * POST /api/v1/admin/monitoring/alerts/suppress
     * 
     * Responses:
     *  - 200: Alert suppressed
     *
     * @param suppressRequest 
     * @return [Unit]
     */
    @POST("api/v1/admin/monitoring/alerts/suppress")
    suspend fun suppressAlert(@Body suppressRequest: SuppressRequest): Response<Unit>

}
