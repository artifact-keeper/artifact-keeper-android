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
import com.artifactkeeper.client.models.RotateWebhookSecretResponse
import com.artifactkeeper.client.models.TestWebhookResponse
import com.artifactkeeper.client.models.WebhookListResponse
import com.artifactkeeper.client.models.WebhookResponse
import com.artifactkeeper.client.models.WebhookSecretCreatedResponse

interface WebhooksApi {
    /**
     * POST api/v1/webhooks
     * Create webhook.
     * Generates a fresh signing secret (or accepts a caller-supplied one), encrypts it at rest, and returns the raw secret in the response body **once**. After this call, GET on the webhook returns only &#x60;secret_digest&#x60;, never the raw secret.
     * Responses:
     *  - 200: Webhook created. Body includes the raw secret exactly once (omitted when created unsigned).
     *  - 422: Validation error
     *  - 500: Internal server error
     *  - 503: A secret was supplied but AK_WEBHOOK_SECRET_KEY is not configured, so the secret cannot be encrypted at rest
     *
     * @param createWebhookRequest 
     * @return [WebhookSecretCreatedResponse]
     */
    @POST("api/v1/webhooks")
    suspend fun createWebhook(@Body createWebhookRequest: CreateWebhookRequest): Response<WebhookSecretCreatedResponse>

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
     * POST api/v1/webhooks/{id}/rotate-secret
     * Rotate the signing secret for a webhook.
     * Generates a new raw secret, encrypts it, moves the existing &#x60;secret_encrypted&#x60; into &#x60;secret_previous_encrypted&#x60;, and stamps an expiry 24 hours in the future. The new raw secret is returned in the response body **once**. The HMAC signing path (added in a later ticket) signs deliveries with both secrets while the previous one is within its expiry window so consumers can rotate without dropped events.  If a previous-secret window is still active when the rotate request arrives, the request is REJECTED with HTTP 409 Conflict. This prevents two near-simultaneous rotations from clobbering the original &#x60;secret_previous_encrypted&#x60; material before the operator has finished distributing the previous new key. The 409 body is structured: &#x60;{\&quot;error\&quot;: \&quot;rotation_already_in_progress\&quot;, \&quot;expires_at\&quot;: \&quot;&lt;RFC3339&gt;\&quot;}&#x60;.
     * Responses:
     *  - 200: Secret rotated. Body includes the new raw secret exactly once.
     *  - 404: Webhook not found
     *  - 409: A previous rotation overlap window is still active
     *  - 500: Encryption key not configured
     *
     * @param id Webhook ID
     * @return [RotateWebhookSecretResponse]
     */
    @POST("api/v1/webhooks/{id}/rotate-secret")
    suspend fun rotateWebhookSecret(@Path("id") id: java.util.UUID): Response<RotateWebhookSecretResponse>

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
