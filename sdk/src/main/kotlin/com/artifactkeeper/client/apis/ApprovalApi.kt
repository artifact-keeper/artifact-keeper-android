package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.ApprovalListResponse
import com.artifactkeeper.client.models.ApprovalRequest
import com.artifactkeeper.client.models.ApprovalResponse
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.ReviewRequest

interface ApprovalApi {
    /**
     * POST api/v1/approval/{id}/approve
     * Approve a pending promotion request. Admin-only.
     * This copies the artifact from the staging repo to the release repo, inserts the new artifact record, records promotion history, and updates the approval status to \&quot;approved\&quot;.
     * Responses:
     *  - 200: Promotion approved and executed
     *  - 403: Admin access required
     *  - 404: Approval request not found
     *  - 409: Approval already reviewed
     *
     * @param id Approval request ID
     * @param reviewRequest 
     * @return [ApprovalResponse]
     */
    @POST("api/v1/approval/{id}/approve")
    suspend fun approvePromotion(@Path("id") id: java.util.UUID, @Body reviewRequest: ReviewRequest): Response<ApprovalResponse>

    /**
     * GET api/v1/approval/{id}
     * Get a single approval request by ID.
     * 
     * Responses:
     *  - 200: Approval request details
     *  - 404: Approval request not found
     *
     * @param id Approval request ID
     * @return [ApprovalResponse]
     */
    @GET("api/v1/approval/{id}")
    suspend fun getApproval(@Path("id") id: java.util.UUID): Response<ApprovalResponse>

    /**
     * GET api/v1/approval/history
     * List approval history with optional filtering by status or source repository.
     * 
     * Responses:
     *  - 200: Approval history
     *
     * @param page Page number (1-indexed) (optional)
     * @param perPage Items per page (max 100) (optional)
     * @param status Filter by status (pending, approved, rejected) (optional)
     * @param sourceRepository Filter by source repository key (optional)
     * @return [ApprovalListResponse]
     */
    @GET("api/v1/approval/history")
    suspend fun listApprovalHistory(@Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null, @Query("status") status: kotlin.String? = null, @Query("source_repository") sourceRepository: kotlin.String? = null): Response<ApprovalListResponse>

    /**
     * GET api/v1/approval/pending
     * List pending approval requests. Optionally filter by source repository.
     * 
     * Responses:
     *  - 200: Pending approval requests
     *
     * @param page Page number (1-indexed) (optional)
     * @param perPage Items per page (max 100) (optional)
     * @param sourceRepository Filter by source repository key (optional)
     * @return [ApprovalListResponse]
     */
    @GET("api/v1/approval/pending")
    suspend fun listPendingApprovals(@Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null, @Query("source_repository") sourceRepository: kotlin.String? = null): Response<ApprovalListResponse>

    /**
     * POST api/v1/approval/{id}/reject
     * Reject a pending promotion request. Admin-only.
     * 
     * Responses:
     *  - 200: Promotion rejected
     *  - 403: Admin access required
     *  - 404: Approval request not found
     *  - 409: Approval already reviewed
     *
     * @param id Approval request ID
     * @param reviewRequest 
     * @return [ApprovalResponse]
     */
    @POST("api/v1/approval/{id}/reject")
    suspend fun rejectPromotion(@Path("id") id: java.util.UUID, @Body reviewRequest: ReviewRequest): Response<ApprovalResponse>

    /**
     * POST api/v1/approval/request
     * Request approval for promoting an artifact from staging to release.
     * 
     * Responses:
     *  - 201: Approval request created
     *  - 404: Artifact or repository not found
     *  - 409: Pending approval already exists
     *  - 422: Validation error
     *
     * @param approvalRequest 
     * @return [ApprovalResponse]
     */
    @POST("api/v1/approval/request")
    suspend fun requestApproval(@Body approvalRequest: ApprovalRequest): Response<ApprovalResponse>

}
