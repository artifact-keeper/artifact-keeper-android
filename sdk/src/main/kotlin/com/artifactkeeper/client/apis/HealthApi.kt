package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.HealthResponse
import com.artifactkeeper.client.models.LivezResponse
import com.artifactkeeper.client.models.ReadyzResponse

interface HealthApi {
    /**
     * GET health
     * Health check endpoint -- rich status page for dashboards.
     * Checks database, storage (real write/read probe), optional services (Trivy, OpenSearch), and exposes DB connection pool statistics.
     * Responses:
     *  - 200: Service is healthy
     *  - 503: Service is unhealthy
     *
     * @return [HealthResponse]
     */
    @GET("health")
    suspend fun healthCheck(): Response<HealthResponse>

    /**
     * GET livez
     * Liveness probe - confirms the process is alive and can serve HTTP.
     * Takes no State parameter. If Axum can route the request and execute this function, the process is alive. External service failures cannot trigger pod restarts.
     * Responses:
     *  - 200: Process is alive
     *
     * @return [LivezResponse]
     */
    @GET("livez")
    suspend fun livenessCheck(): Response<LivezResponse>

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
     * GET readyz
     * Readiness probe - is the service ready to accept traffic?
     * Returns 200 once the database is reachable and migrations have applied successfully. Initial-setup state (whether the default admin password has been changed) is reported as an informational field on the response but does NOT influence the status code: a 503 here would make Kubernetes restart the pod, terminating any &#x60;kubectl exec&#x60; session an operator is using to complete setup. See issue #889.  API mutations are separately gated by the setup middleware (&#x60;api::middleware::setup&#x60;) until setup is complete, so a 200 from this endpoint does not imply that write traffic will be accepted.
     * Responses:
     *  - 200: Service is ready
     *  - 503: Service is not ready
     *
     * @return [ReadyzResponse]
     */
    @GET("readyz")
    suspend fun readinessCheck(): Response<ReadyzResponse>

}
