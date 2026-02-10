package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.HealthResponse

interface HealthApi {
    /**
     * GET health
     * Health check endpoint - basic liveness check
     * 
     * Responses:
     *  - 200: Service is healthy
     *  - 503: Service is unhealthy
     *
     * @return [HealthResponse]
     */
    @GET("health")
    suspend fun healthCheck(): Response<HealthResponse>

    /**
     * GET api/v1/admin/metrics
     * Prometheus metrics endpoint. Renders all registered metrics from the metrics-exporter-prometheus recorder.
     * 
     * Responses:
     *  - 200: Prometheus metrics in text format
     *
     * @return [Unit]
     */
    @GET("api/v1/admin/metrics")
    suspend fun metrics(): Response<Unit>

    /**
     * GET ready
     * Readiness check endpoint - is the service ready to accept traffic?
     * 
     * Responses:
     *  - 200: Service is ready to accept traffic
     *  - 503: Service is not ready
     *
     * @return [Unit]
     */
    @GET("ready")
    suspend fun readinessCheck(): Response<Unit>

}
