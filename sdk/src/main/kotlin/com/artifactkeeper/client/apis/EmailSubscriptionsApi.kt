package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.CreateEmailSubscriptionRequest
import com.artifactkeeper.client.models.EmailSubscriptionListResponse
import com.artifactkeeper.client.models.EmailSubscriptionResponse

interface EmailSubscriptionsApi {
    /**
     * POST api/v1/repositories/{key}/email-subscriptions
     * Create an email subscription scoped to a repository.
     * 
     * Responses:
     *  - 201: Subscription created
     *  - 400: Validation error
     *  - 401: Not authenticated
     *  - 403: Insufficient permissions
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @param createEmailSubscriptionRequest 
     * @return [EmailSubscriptionResponse]
     */
    @POST("api/v1/repositories/{key}/email-subscriptions")
    suspend fun createSubscription(@Path("key") key: kotlin.String, @Body createEmailSubscriptionRequest: CreateEmailSubscriptionRequest): Response<EmailSubscriptionResponse>

    /**
     * DELETE api/v1/repositories/{key}/email-subscriptions/{subscription_id}
     * Delete an email subscription by id.
     * 
     * Responses:
     *  - 204: Subscription deleted
     *  - 401: Not authenticated
     *  - 403: Insufficient permissions
     *  - 404: Subscription or repository not found
     *
     * @param key Repository key
     * @param subscriptionId Subscription ID
     * @return [Unit]
     */
    @DELETE("api/v1/repositories/{key}/email-subscriptions/{subscription_id}")
    suspend fun deleteSubscription(@Path("key") key: kotlin.String, @Path("subscription_id") subscriptionId: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/repositories/{key}/email-subscriptions
     * List the email subscriptions configured on a repository.
     * 
     * Responses:
     *  - 200: List of email subscriptions
     *  - 401: Not authenticated
     *  - 403: Insufficient permissions
     *  - 404: Repository not found
     *
     * @param key Repository key
     * @return [EmailSubscriptionListResponse]
     */
    @GET("api/v1/repositories/{key}/email-subscriptions")
    suspend fun listSubscriptions(@Path("key") key: kotlin.String): Response<EmailSubscriptionListResponse>

}
