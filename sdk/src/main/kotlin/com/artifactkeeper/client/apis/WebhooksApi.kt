package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.CreateWebhookRequest
import com.artifactkeeper.client.models.DeliveryListResponse
import com.artifactkeeper.client.models.DeliveryResponse
import com.artifactkeeper.client.models.TestWebhookResponse
import com.artifactkeeper.client.models.WebhookListResponse
import com.artifactkeeper.client.models.WebhookResponse

interface WebhooksApi {
    /**
     * POST api/v1/webhooks
     * Create webhook
     * 
     * Responses:
     *  - 200: Webhook created successfully
     *  - 422: Validation error
     *  - 500: Internal server error
     *
     * @param createWebhookRequest 
     * @return [WebhookResponse]
     */
    @POST("api/v1/webhooks")
    suspend fun createWebhook(@Body createWebhookRequest: CreateWebhookRequest): Response<WebhookResponse>

    /**
     * DELETE api/v1/webhooks/{id}
     * Delete webhook
     * 
     * Responses:
     *  - 200: Webhook deleted successfully
     *  - 404: Webhook not found
     *
     * @param id Webhook ID
     * @return [Unit]
     */
    @DELETE("api/v1/webhooks/{id}")
    suspend fun deleteWebhook(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/webhooks/{id}/disable
     * Disable webhook
     * 
     * Responses:
     *  - 200: Webhook disabled successfully
     *  - 404: Webhook not found
     *
     * @param id Webhook ID
     * @return [Unit]
     */
    @POST("api/v1/webhooks/{id}/disable")
    suspend fun disableWebhook(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/webhooks/{id}/enable
     * Enable webhook
     * 
     * Responses:
     *  - 200: Webhook enabled successfully
     *  - 404: Webhook not found
     *
     * @param id Webhook ID
     * @return [Unit]
     */
    @POST("api/v1/webhooks/{id}/enable")
    suspend fun enableWebhook(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/webhooks/{id}
     * Get webhook by ID
     * 
     * Responses:
     *  - 200: Webhook details
     *  - 404: Webhook not found
     *
     * @param id Webhook ID
     * @return [WebhookResponse]
     */
    @GET("api/v1/webhooks/{id}")
    suspend fun getWebhook(@Path("id") id: java.util.UUID): Response<WebhookResponse>

    /**
     * GET api/v1/webhooks/{id}/deliveries
     * List webhook deliveries
     * 
     * Responses:
     *  - 200: List of webhook deliveries
     *  - 500: Internal server error
     *
     * @param id Webhook ID
     * @param status  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [DeliveryListResponse]
     */
    @GET("api/v1/webhooks/{id}/deliveries")
    suspend fun listDeliveries(@Path("id") id: java.util.UUID, @Query("status") status: kotlin.String? = null, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null): Response<DeliveryListResponse>

    /**
     * GET api/v1/webhooks
     * List webhooks
     * 
     * Responses:
     *  - 200: List of webhooks
     *  - 500: Internal server error
     *
     * @param repositoryId  (optional)
     * @param enabled  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [WebhookListResponse]
     */
    @GET("api/v1/webhooks")
    suspend fun listWebhooks(@Query("repository_id") repositoryId: java.util.UUID? = null, @Query("enabled") enabled: kotlin.Boolean? = null, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null): Response<WebhookListResponse>

    /**
     * POST api/v1/webhooks/{id}/deliveries/{delivery_id}/redeliver
     * Redeliver a failed webhook
     * 
     * Responses:
     *  - 200: Redelivery result
     *  - 404: Webhook or delivery not found
     *
     * @param id Webhook ID
     * @param deliveryId Delivery ID
     * @return [DeliveryResponse]
     */
    @POST("api/v1/webhooks/{id}/deliveries/{delivery_id}/redeliver")
    suspend fun redeliver(@Path("id") id: java.util.UUID, @Path("delivery_id") deliveryId: java.util.UUID): Response<DeliveryResponse>

    /**
     * POST api/v1/webhooks/{id}/test
     * Test webhook by sending a test payload
     * 
     * Responses:
     *  - 200: Test delivery result
     *  - 404: Webhook not found
     *
     * @param id Webhook ID
     * @return [TestWebhookResponse]
     */
    @POST("api/v1/webhooks/{id}/test")
    suspend fun testWebhook(@Path("id") id: java.util.UUID): Response<TestWebhookResponse>

}
