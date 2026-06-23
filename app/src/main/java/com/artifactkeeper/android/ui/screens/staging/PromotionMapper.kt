package com.artifactkeeper.android.ui.screens.staging

import com.artifactkeeper.android.data.models.BulkPromoteRequest
import com.artifactkeeper.android.data.models.BulkPromotionResponse
import com.artifactkeeper.android.data.models.BulkPromotionResult
import com.artifactkeeper.android.data.models.PromoteArtifactRequest
import com.artifactkeeper.android.data.models.PromotionHistoryEntry
import com.artifactkeeper.android.data.models.PromotionResponse
import java.util.UUID

/**
 * Maps between the app's local staging/promotion UI models and the generated
 * 1.2.1 SDK promotion models. The 1.2.1 backend dropped the staging endpoints
 * under api/v1/staging; promotion now lives under api/v1/promotion with a
 * different request and response shape. This object keeps the UI contract
 * stable while routing traffic to the supported SDK operations.
 */
object PromotionMapper {

    /**
     * Build the SDK promote-artifact request from the local UI request.
     * The local "force" flag maps to the SDK skip_policy_check flag and the
     * local "comment" maps to SDK notes. Target repository key carries over.
     */
    fun toSdkPromoteRequest(
        request: PromoteArtifactRequest,
    ): com.artifactkeeper.client.models.PromoteArtifactRequest =
        com.artifactkeeper.client.models.PromoteArtifactRequest(
            targetRepository = request.targetRepositoryKey,
            skipPolicyCheck = request.force,
            notes = request.comment,
        )

    /**
     * Build the SDK bulk-promote request. Artifact ids are UUIDs in 1.2.1;
     * any id that is not a valid UUID is dropped rather than crashing the call.
     */
    fun toSdkBulkRequest(
        request: BulkPromoteRequest,
    ): com.artifactkeeper.client.models.BulkPromoteRequest =
        com.artifactkeeper.client.models.BulkPromoteRequest(
            artifactIds = request.artifactIds.mapNotNull { it.toUuidOrNull() },
            targetRepository = request.targetRepositoryKey,
            skipPolicyCheck = request.force,
            notes = request.comment,
        )

    /**
     * Map a single SDK promotion result back to the local UI response.
     */
    fun toLocalPromotionResponse(
        sdk: com.artifactkeeper.client.models.PromotionResponse,
        artifactId: String,
    ): PromotionResponse =
        PromotionResponse(
            success = sdk.promoted,
            message = sdk.message ?: defaultPromotionMessage(sdk),
            artifactId = artifactId,
            targetRepositoryKey = sdk.target,
            promotedAt = null,
        )

    /**
     * Map an SDK bulk promotion response back to the local UI response. The SDK
     * reports total/promoted/failed counts and per-artifact results; the local
     * model wants total_requested/total_succeeded/total_failed plus results.
     */
    fun toLocalBulkResponse(
        sdk: com.artifactkeeper.client.models.BulkPromotionResponse,
    ): BulkPromotionResponse =
        BulkPromotionResponse(
            totalRequested = sdk.total,
            totalSucceeded = sdk.promoted,
            totalFailed = sdk.failed,
            results = sdk.results.map { result ->
                BulkPromotionResult(
                    artifactId = result.target,
                    success = result.promoted,
                    message = result.message,
                    error = if (!result.promoted) result.message else null,
                )
            },
        )

    /**
     * Map an SDK promotion history entry to the local UI entry. The SDK exposes
     * an artifact path rather than a name/version pair, so the trailing path
     * segment becomes the display name and version is left absent.
     */
    fun toLocalHistoryEntry(
        sdk: com.artifactkeeper.client.models.PromotionHistoryEntry,
    ): PromotionHistoryEntry =
        PromotionHistoryEntry(
            id = sdk.id.toString(),
            artifactId = sdk.artifactId.toString(),
            artifactName = sdk.artifactPath.substringAfterLast('/'),
            artifactVersion = null,
            sourceRepositoryKey = sdk.sourceRepoKey,
            targetRepositoryKey = sdk.targetRepoKey,
            promotedBy = sdk.promotedBy?.toString(),
            promotedByUsername = sdk.promotedByUsername,
            comment = sdk.notes,
            forced = false,
            promotedAt = sdk.createdAt.toString(),
        )

    private fun defaultPromotionMessage(
        sdk: com.artifactkeeper.client.models.PromotionResponse,
    ): String =
        if (sdk.promoted) {
            "Promoted to ${sdk.target}"
        } else {
            "Promotion blocked by ${sdk.policyViolations.size} policy violation(s)"
        }

    private fun String.toUuidOrNull(): UUID? =
        try {
            UUID.fromString(this)
        } catch (_: IllegalArgumentException) {
            null
        }
}
