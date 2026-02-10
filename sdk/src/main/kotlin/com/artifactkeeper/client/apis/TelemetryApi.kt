package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.CrashListResponse
import com.artifactkeeper.client.models.CrashReport
import com.artifactkeeper.client.models.SubmitCrashesRequest
import com.artifactkeeper.client.models.SubmitResponse
import com.artifactkeeper.client.models.TelemetrySettings

interface TelemetryApi {
    /**
     * DELETE api/v1/admin/telemetry/crashes/{id}
     * DELETE /api/v1/admin/telemetry/crashes/:id
     * 
     * Responses:
     *  - 200: Crash report deleted
     *
     * @param id Crash report ID
     * @return [Unit]
     */
    @DELETE("api/v1/admin/telemetry/crashes/{id}")
    suspend fun deleteCrash(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/admin/telemetry/crashes/{id}
     * GET /api/v1/admin/telemetry/crashes/:id
     * 
     * Responses:
     *  - 200: Crash report details
     *
     * @param id Crash report ID
     * @return [CrashReport]
     */
    @GET("api/v1/admin/telemetry/crashes/{id}")
    suspend fun getCrash(@Path("id") id: java.util.UUID): Response<CrashReport>

    /**
     * GET api/v1/admin/telemetry/settings
     * GET /api/v1/admin/telemetry/settings
     * 
     * Responses:
     *  - 200: Current telemetry settings
     *
     * @return [TelemetrySettings]
     */
    @GET("api/v1/admin/telemetry/settings")
    suspend fun getTelemetrySettings(): Response<TelemetrySettings>

    /**
     * GET api/v1/admin/telemetry/crashes
     * GET /api/v1/admin/telemetry/crashes
     * 
     * Responses:
     *  - 200: Paginated crash reports
     *
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [CrashListResponse]
     */
    @GET("api/v1/admin/telemetry/crashes")
    suspend fun listCrashes(@Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null): Response<CrashListResponse>

    /**
     * GET api/v1/admin/telemetry/crashes/pending
     * GET /api/v1/admin/telemetry/crashes/pending
     * 
     * Responses:
     *  - 200: Pending crash reports
     *
     * @return [kotlin.collections.List<CrashReport>]
     */
    @GET("api/v1/admin/telemetry/crashes/pending")
    suspend fun listPendingCrashes(): Response<kotlin.collections.List<CrashReport>>

    /**
     * POST api/v1/admin/telemetry/crashes/submit
     * POST /api/v1/admin/telemetry/crashes/submit
     * 
     * Responses:
     *  - 200: Crashes submitted
     *
     * @param submitCrashesRequest 
     * @return [SubmitResponse]
     */
    @POST("api/v1/admin/telemetry/crashes/submit")
    suspend fun submitCrashes(@Body submitCrashesRequest: SubmitCrashesRequest): Response<SubmitResponse>

    /**
     * POST api/v1/admin/telemetry/settings
     * POST /api/v1/admin/telemetry/settings
     * 
     * Responses:
     *  - 200: Settings updated
     *
     * @param telemetrySettings 
     * @return [TelemetrySettings]
     */
    @POST("api/v1/admin/telemetry/settings")
    suspend fun updateTelemetrySettings(@Body telemetrySettings: TelemetrySettings): Response<TelemetrySettings>

}
