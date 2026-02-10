package com.artifactkeeper.android.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Local models that have no direct SDK equivalent.
// These are kept locally until the SDK adds support for them.
// =============================================================================

// --- Multi-server (app-only, not from the API) ---
@Serializable
data class SavedServer(
    val id: String,
    val name: String,
    val url: String,
    val addedAt: Long  // epoch millis
)

// --- Enums used by the app UI ---

enum class RepositoryType {
    LOCAL,
    REMOTE,
    VIRTUAL,
    STAGING;

    companion object {
        fun fromString(value: String): RepositoryType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: LOCAL
        }
    }
}

enum class PolicyStatus {
    PASSING,
    FAILING,
    WARNING,
    PENDING;

    companion object {
        fun fromString(value: String): PolicyStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

// --- Staging models (the SDK uses /promotion/ endpoints but the app
//     uses staging-specific response shapes not in the generated SDK) ---

@Serializable
data class CveSummary(
    val total: Int = 0,
    @SerialName("critical_count") val criticalCount: Int = 0,
    @SerialName("high_count") val highCount: Int = 0,
    @SerialName("medium_count") val mediumCount: Int = 0,
    @SerialName("low_count") val lowCount: Int = 0,
)

@Serializable
data class LicenseSummary(
    val total: Int = 0,
    @SerialName("allowed_count") val allowedCount: Int = 0,
    @SerialName("denied_count") val deniedCount: Int = 0,
    @SerialName("unknown_count") val unknownCount: Int = 0,
    val licenses: List<String> = emptyList(),
)

@Serializable
data class PolicyViolation(
    val id: String,
    @SerialName("policy_id") val policyId: String,
    @SerialName("policy_name") val policyName: String,
    val severity: String,
    val message: String,
    val rule: String? = null,
    @SerialName("artifact_id") val artifactId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class StagingArtifact(
    val id: String,
    @SerialName("repository_key") val repositoryKey: String? = null,
    val name: String,
    val path: String,
    val version: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    @SerialName("checksum_sha256") val checksumSha256: String? = null,
    @SerialName("policy_status") val policyStatus: String = "pending",
    @SerialName("cve_summary") val cveSummary: CveSummary? = null,
    @SerialName("license_summary") val licenseSummary: LicenseSummary? = null,
    @SerialName("policy_violations") val policyViolations: List<PolicyViolation> = emptyList(),
    @SerialName("can_promote") val canPromote: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class StagingArtifactListResponse(
    val items: List<StagingArtifact>,
    val pagination: com.artifactkeeper.client.models.Pagination? = null,
)

@Serializable
data class StagingRepository(
    val id: String,
    val key: String,
    val name: String,
    val format: String,
    @SerialName("repo_type") val repoType: String = "staging",
    @SerialName("is_public") val isPublic: Boolean = false,
    val description: String? = null,
    @SerialName("storage_used_bytes") val storageUsedBytes: Long = 0,
    @SerialName("artifact_count") val artifactCount: Int = 0,
    @SerialName("pending_count") val pendingCount: Int = 0,
    @SerialName("passing_count") val passingCount: Int = 0,
    @SerialName("failing_count") val failingCount: Int = 0,
    @SerialName("target_repository_key") val targetRepositoryKey: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class StagingRepositoryListResponse(
    val items: List<StagingRepository>,
    val pagination: com.artifactkeeper.client.models.Pagination? = null,
)

// --- Analytics models not in SDK ---

@Serializable
data class DownloadTrendItem(
    val date: String,
    val count: Long
)

@Serializable
data class StorageGrowthItem(
    val date: String,
    @SerialName("total_bytes") val totalBytes: Long
)

// --- SBOM extra models not in SDK ---

@Serializable
data class SbomComponentsResponse(
    val components: List<com.artifactkeeper.client.models.ComponentResponse>,
    val total: Int,
)

@Serializable
data class SbomListResponse(
    val items: List<com.artifactkeeper.client.models.SbomResponse>,
    val total: Int,
)

@Serializable
data class CveTrendDataPoint(
    val date: String,
    val detected: Int,
    val resolved: Int,
)

// --- License policy create/update requests (SDK uses UpsertLicensePolicyRequest) ---

@Serializable
data class CreateLicensePolicyRequest(
    val name: String,
    val description: String? = null,
    @SerialName("allowed_licenses") val allowedLicenses: List<String> = emptyList(),
    @SerialName("denied_licenses") val deniedLicenses: List<String> = emptyList(),
    val action: String = "warn",
    @SerialName("allow_unknown") val allowUnknown: Boolean = true,
)

@Serializable
data class UpdateLicensePolicyRequest(
    val name: String,
    val description: String? = null,
    @SerialName("allowed_licenses") val allowedLicenses: List<String> = emptyList(),
    @SerialName("denied_licenses") val deniedLicenses: List<String> = emptyList(),
    val action: String = "warn",
    @SerialName("allow_unknown") val allowUnknown: Boolean = true,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
)

// --- Reorder members ---
@Serializable
data class MemberPriority(
    @SerialName("member_key") val memberKey: String,
    val priority: Int,
)

@Serializable
data class ReorderMembersRequest(
    val members: List<MemberPriority>,
)

// --- Health (local models for OkHttp-based /health endpoint deserialization) ---
// The SDK's HealthResponse uses a typed HealthChecks object with named fields,
// but the /health endpoint returns checks as a dynamic map, so we keep a local model.

@Serializable
data class LocalHealthResponse(
    val status: String,
    val checks: Map<String, HealthCheck> = emptyMap()
)

@Serializable
data class HealthCheck(
    val status: String,
    @SerialName("response_time_ms") val responseTimeMs: Long? = null
)

// --- Staging / Promotion models ---
// The SDK's promotion endpoints use different field names and types than the
// staging-specific endpoints (/api/v1/staging/...), so these must remain local.

@Serializable
data class PromoteArtifactRequest(
    @SerialName("target_repository_key") val targetRepositoryKey: String,
    @SerialName("force") val force: Boolean = false,
    val comment: String? = null,
)

@Serializable
data class PromotionResponse(
    val success: Boolean,
    val message: String,
    @SerialName("artifact_id") val artifactId: String,
    @SerialName("target_repository_key") val targetRepositoryKey: String,
    @SerialName("promoted_at") val promotedAt: String? = null,
)

@Serializable
data class BulkPromoteRequest(
    @SerialName("artifact_ids") val artifactIds: List<String>,
    @SerialName("target_repository_key") val targetRepositoryKey: String,
    @SerialName("force") val force: Boolean = false,
    val comment: String? = null,
)

@Serializable
data class BulkPromotionResult(
    @SerialName("artifact_id") val artifactId: String,
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class BulkPromotionResponse(
    @SerialName("total_requested") val totalRequested: Int,
    @SerialName("total_succeeded") val totalSucceeded: Int,
    @SerialName("total_failed") val totalFailed: Int,
    val results: List<BulkPromotionResult>,
)

@Serializable
data class PromotionHistoryEntry(
    val id: String,
    @SerialName("artifact_id") val artifactId: String,
    @SerialName("artifact_name") val artifactName: String? = null,
    @SerialName("artifact_version") val artifactVersion: String? = null,
    @SerialName("source_repository_key") val sourceRepositoryKey: String,
    @SerialName("target_repository_key") val targetRepositoryKey: String,
    @SerialName("promoted_by") val promotedBy: String? = null,
    @SerialName("promoted_by_username") val promotedByUsername: String? = null,
    val comment: String? = null,
    val forced: Boolean = false,
    @SerialName("promoted_at") val promotedAt: String,
)

@Serializable
data class PromotionHistoryResponse(
    val items: List<PromotionHistoryEntry>,
    val pagination: com.artifactkeeper.client.models.Pagination? = null,
)
