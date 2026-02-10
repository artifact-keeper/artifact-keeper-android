package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.AssessmentResult
import com.artifactkeeper.client.models.ConnectionResponse
import com.artifactkeeper.client.models.ConnectionTestResult
import com.artifactkeeper.client.models.CreateConnectionRequest
import com.artifactkeeper.client.models.CreateMigrationRequest
import com.artifactkeeper.client.models.MigrationItemResponse
import com.artifactkeeper.client.models.MigrationJobResponse
import com.artifactkeeper.client.models.MigrationReportResponse
import com.artifactkeeper.client.models.SourceRepository

interface MigrationApi {
    /**
     * POST api/v1/migrations/{id}/cancel
     * Cancel a migration job
     * 
     * Responses:
     *  - 200: Migration job cancelled
     *  - 409: Migration cannot be cancelled (wrong state)
     *  - 500: Internal server error
     *
     * @param id Migration job ID
     * @return [MigrationJobResponse]
     */
    @POST("api/v1/migrations/{id}/cancel")
    suspend fun cancelMigration(@Path("id") id: java.util.UUID): Response<MigrationJobResponse>

    /**
     * POST api/v1/migrations/connections
     * Create a new source connection
     * 
     * Responses:
     *  - 201: Connection created successfully
     *  - 500: Internal server error
     *
     * @param createConnectionRequest 
     * @return [ConnectionResponse]
     */
    @POST("api/v1/migrations/connections")
    suspend fun createConnection(@Body createConnectionRequest: CreateConnectionRequest): Response<ConnectionResponse>

    /**
     * POST api/v1/migrations
     * Create a new migration job
     * 
     * Responses:
     *  - 201: Migration job created successfully
     *  - 500: Internal server error
     *
     * @param createMigrationRequest 
     * @return [MigrationJobResponse]
     */
    @POST("api/v1/migrations")
    suspend fun createMigration(@Body createMigrationRequest: CreateMigrationRequest): Response<MigrationJobResponse>

    /**
     * DELETE api/v1/migrations/connections/{id}
     * Delete a source connection
     * 
     * Responses:
     *  - 204: Connection deleted successfully
     *  - 404: Connection not found
     *  - 500: Internal server error
     *
     * @param id Connection ID
     * @return [Unit]
     */
    @DELETE("api/v1/migrations/connections/{id}")
    suspend fun deleteConnection(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * DELETE api/v1/migrations/{id}
     * Delete a migration job
     * 
     * Responses:
     *  - 204: Migration job deleted successfully
     *  - 404: Migration job not found
     *  - 500: Internal server error
     *
     * @param id Migration job ID
     * @return [Unit]
     */
    @DELETE("api/v1/migrations/{id}")
    suspend fun deleteMigration(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/migrations/{id}/assessment
     * Get assessment results
     * 
     * Responses:
     *  - 200: Assessment results
     *  - 404: Migration job not found
     *  - 500: Internal server error
     *
     * @param id Migration job ID
     * @return [AssessmentResult]
     */
    @GET("api/v1/migrations/{id}/assessment")
    suspend fun getAssessment(@Path("id") id: java.util.UUID): Response<AssessmentResult>

    /**
     * GET api/v1/migrations/connections/{id}
     * Get a specific source connection
     * 
     * Responses:
     *  - 200: Connection details
     *  - 404: Connection not found
     *  - 500: Internal server error
     *
     * @param id Connection ID
     * @return [ConnectionResponse]
     */
    @GET("api/v1/migrations/connections/{id}")
    suspend fun getConnection(@Path("id") id: java.util.UUID): Response<ConnectionResponse>

    /**
     * GET api/v1/migrations/{id}
     * Get a specific migration job
     * 
     * Responses:
     *  - 200: Migration job details
     *  - 404: Migration job not found
     *  - 500: Internal server error
     *
     * @param id Migration job ID
     * @return [MigrationJobResponse]
     */
    @GET("api/v1/migrations/{id}")
    suspend fun getMigration(@Path("id") id: java.util.UUID): Response<MigrationJobResponse>

    /**
     * GET api/v1/migrations/{id}/report
     * Get migration report
     * 
     * Responses:
     *  - 200: Migration report
     *  - 404: Migration report not found
     *  - 500: Internal server error
     *
     * @param id Migration job ID
     * @param format  (optional)
     * @return [MigrationReportResponse]
     */
    @GET("api/v1/migrations/{id}/report")
    suspend fun getMigrationReport(@Path("id") id: java.util.UUID, @Query("format") format: kotlin.String? = null): Response<MigrationReportResponse>

    /**
     * GET api/v1/migrations/connections
     * List all source connections for the current user
     * 
     * Responses:
     *  - 200: List of source connections
     *  - 500: Internal server error
     *
     * @return [kotlin.collections.List<ConnectionResponse>]
     */
    @GET("api/v1/migrations/connections")
    suspend fun listConnections(): Response<kotlin.collections.List<ConnectionResponse>>

    /**
     * GET api/v1/migrations/{id}/items
     * List migration items for a job
     * 
     * Responses:
     *  - 200: List of migration items
     *  - 500: Internal server error
     *
     * @param id Migration job ID
     * @param status  (optional)
     * @param itemType  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [kotlin.collections.List<MigrationItemResponse>]
     */
    @GET("api/v1/migrations/{id}/items")
    suspend fun listMigrationItems(@Path("id") id: java.util.UUID, @Query("status") status: kotlin.String? = null, @Query("item_type") itemType: kotlin.String? = null, @Query("page") page: kotlin.Long? = null, @Query("per_page") perPage: kotlin.Long? = null): Response<kotlin.collections.List<MigrationItemResponse>>

    /**
     * GET api/v1/migrations
     * List migration jobs
     * 
     * Responses:
     *  - 200: List of migration jobs
     *  - 500: Internal server error
     *
     * @param status  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [kotlin.collections.List<MigrationJobResponse>]
     */
    @GET("api/v1/migrations")
    suspend fun listMigrations(@Query("status") status: kotlin.String? = null, @Query("page") page: kotlin.Long? = null, @Query("per_page") perPage: kotlin.Long? = null): Response<kotlin.collections.List<MigrationJobResponse>>

    /**
     * GET api/v1/migrations/connections/{id}/repositories
     * List repositories from Artifactory source
     * 
     * Responses:
     *  - 200: List of source repositories
     *  - 404: Connection not found
     *  - 500: Internal server error
     *
     * @param id Connection ID
     * @return [kotlin.collections.List<SourceRepository>]
     */
    @GET("api/v1/migrations/connections/{id}/repositories")
    suspend fun listSourceRepositories(@Path("id") id: java.util.UUID): Response<kotlin.collections.List<SourceRepository>>

    /**
     * POST api/v1/migrations/{id}/pause
     * Pause a migration job
     * 
     * Responses:
     *  - 200: Migration job paused
     *  - 409: Migration cannot be paused (wrong state)
     *  - 500: Internal server error
     *
     * @param id Migration job ID
     * @return [MigrationJobResponse]
     */
    @POST("api/v1/migrations/{id}/pause")
    suspend fun pauseMigration(@Path("id") id: java.util.UUID): Response<MigrationJobResponse>

    /**
     * POST api/v1/migrations/{id}/resume
     * Resume a paused migration job
     * 
     * Responses:
     *  - 200: Migration job resumed
     *  - 409: Migration cannot be resumed (wrong state)
     *  - 500: Internal server error
     *
     * @param id Migration job ID
     * @return [MigrationJobResponse]
     */
    @POST("api/v1/migrations/{id}/resume")
    suspend fun resumeMigration(@Path("id") id: java.util.UUID): Response<MigrationJobResponse>

    /**
     * POST api/v1/migrations/{id}/assess
     * Run pre-migration assessment
     * 
     * Responses:
     *  - 202: Assessment started
     *  - 409: Cannot start assessment (wrong state)
     *  - 500: Internal server error
     *
     * @param id Migration job ID
     * @return [MigrationJobResponse]
     */
    @POST("api/v1/migrations/{id}/assess")
    suspend fun runAssessment(@Path("id") id: java.util.UUID): Response<MigrationJobResponse>

    /**
     * POST api/v1/migrations/{id}/start
     * Start a migration job
     * 
     * Responses:
     *  - 200: Migration job started
     *  - 404: Migration job not found
     *  - 409: Migration cannot be started (wrong state)
     *  - 500: Internal server error
     *
     * @param id Migration job ID
     * @return [MigrationJobResponse]
     */
    @POST("api/v1/migrations/{id}/start")
    suspend fun startMigration(@Path("id") id: java.util.UUID): Response<MigrationJobResponse>

    /**
     * GET api/v1/migrations/{id}/stream
     * Stream migration progress via Server-Sent Events
     * 
     * Responses:
     *  - 200: SSE stream of migration progress
     *  - 404: Migration job not found
     *  - 500: Internal server error
     *
     * @param id Migration job ID
     * @return [Unit]
     */
    @GET("api/v1/migrations/{id}/stream")
    suspend fun streamMigrationProgress(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/migrations/connections/{id}/test
     * Test connection to Artifactory
     * 
     * Responses:
     *  - 200: Connection test result
     *  - 404: Connection not found
     *  - 500: Internal server error
     *
     * @param id Connection ID
     * @return [ConnectionTestResult]
     */
    @POST("api/v1/migrations/connections/{id}/test")
    suspend fun testConnection(@Path("id") id: java.util.UUID): Response<ConnectionTestResult>

}
