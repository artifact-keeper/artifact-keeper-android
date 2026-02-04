package com.artifactkeeper.android.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pagination(
    val page: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Long,
    @SerialName("total_pages") val totalPages: Int,
)

@Serializable
data class PackageItem(
    val id: String,
    @SerialName("repository_key") val repositoryKey: String,
    val name: String,
    val version: String,
    val format: String,
    val description: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("download_count") val downloadCount: Long,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class PackageListResponse(
    val items: List<PackageItem>,
    val pagination: Pagination? = null,
)

@Serializable
data class BuildItem(
    val id: String,
    val name: String,
    val number: Int,
    val status: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    val agent: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("artifact_count") val artifactCount: Int? = null,
    @SerialName("vcs_branch") val vcsBranch: String? = null,
    @SerialName("vcs_revision") val vcsRevision: String? = null,
    @SerialName("vcs_message") val vcsMessage: String? = null,
)

@Serializable
data class BuildListResponse(
    val items: List<BuildItem>,
    val pagination: Pagination? = null,
)

@Serializable
data class Repository(
    val id: String,
    val key: String,
    val name: String,
    val format: String,
    @SerialName("repo_type") val repoType: String,
    @SerialName("is_public") val isPublic: Boolean,
    val description: String? = null,
    @SerialName("storage_used_bytes") val storageUsedBytes: Long = 0,
    @SerialName("artifact_count") val artifactCount: Int = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class RepositoryListResponse(
    val items: List<Repository>,
    val pagination: Pagination? = null,
)

@Serializable
data class Artifact(
    val id: String,
    @SerialName("repository_key") val repositoryKey: String? = null,
    val name: String,
    val path: String,
    val version: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("checksum_sha256") val checksumSha256: String? = null,
    @SerialName("created_at") val createdAt: String,
    val metadata: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
data class ArtifactListResponse(
    val items: List<Artifact>,
    val pagination: Pagination? = null,
)

@Serializable
data class RepoSecurityScore(
    val id: String,
    @SerialName("repository_id") val repositoryId: String,
    val grade: String,
    val score: Int,
    @SerialName("critical_count") val criticalCount: Int,
    @SerialName("high_count") val highCount: Int,
    @SerialName("medium_count") val mediumCount: Int,
    @SerialName("low_count") val lowCount: Int,
)

@Serializable
data class ScanResult(
    val id: String,
    @SerialName("artifact_id") val artifactId: String,
    @SerialName("scan_type") val scanType: String,
    val status: String,
    @SerialName("findings_count") val findingsCount: Int,
    @SerialName("critical_count") val criticalCount: Int,
    @SerialName("high_count") val highCount: Int,
    @SerialName("medium_count") val mediumCount: Int,
    @SerialName("low_count") val lowCount: Int,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("error_message") val errorMessage: String? = null,
)

@Serializable
data class ScanListResponse(
    val items: List<ScanResult>,
    val total: Int,
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 0,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("must_change_password") val mustChangePassword: Boolean = false,
)

@Serializable
data class UserInfo(
    val id: String,
    val username: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
)

// --- Admin ---
@Serializable
data class AdminUser(
    val id: String,
    val username: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("auth_provider") val authProvider: String? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("is_admin") val isAdmin: Boolean,
    @SerialName("must_change_password") val mustChangePassword: Boolean? = null,
    @SerialName("last_login_at") val lastLoginAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class AdminUserListResponse(
    val items: List<AdminUser>,
    val pagination: Pagination? = null
)

@Serializable
data class CreateUserRequest(
    val username: String,
    val email: String,
    val password: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean? = null
)

@Serializable
data class CreateUserResponse(
    val user: AdminUser,
    @SerialName("generated_password") val generatedPassword: String? = null
)

@Serializable
data class AdminGroup(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class AdminGroupListResponse(
    val items: List<AdminGroup>,
    val pagination: Pagination? = null
)

@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class SSOProvider(
    val id: String,
    val name: String,
    @SerialName("provider_type") val providerType: String,
    @SerialName("login_url") val loginUrl: String? = null,
)

@Serializable
data class SSOProviderListResponse(
    val items: List<SSOProvider>,
    val pagination: Pagination? = null
)

// --- Integration ---
@Serializable
data class PeerInstance(
    val id: String,
    val name: String,
    @SerialName("endpoint_url") val endpointUrl: String,
    val status: String,
    val region: String? = null,
    @SerialName("cache_size_bytes") val cacheSizeBytes: Long,
    @SerialName("cache_used_bytes") val cacheUsedBytes: Long,
    @SerialName("cache_usage_percent") val cacheUsagePercent: Double,
    @SerialName("last_heartbeat_at") val lastHeartbeatAt: String? = null,
    @SerialName("last_sync_at") val lastSyncAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("api_key") val apiKey: String,
    @SerialName("is_local") val isLocal: Boolean
)

@Serializable
data class PeerListResponse(
    val items: List<PeerInstance>,
    val total: Long
)

@Serializable
data class RegisterPeerRequest(
    val name: String,
    @SerialName("endpoint_url") val endpointUrl: String,
    val region: String? = null,
    @SerialName("api_key") val apiKey: String
)

@Serializable
data class PeerConnection(
    val id: String,
    @SerialName("target_peer_id") val targetPeerId: String,
    val status: String,
    @SerialName("latency_ms") val latencyMs: Int? = null,
    @SerialName("bandwidth_estimate_bps") val bandwidthEstimateBps: Long? = null,
    @SerialName("shared_artifacts_count") val sharedArtifactsCount: Int,
    @SerialName("shared_chunks_count") val sharedChunksCount: Int,
    @SerialName("bytes_transferred_total") val bytesTransferredTotal: Long,
    @SerialName("transfer_success_count") val transferSuccessCount: Int,
    @SerialName("transfer_failure_count") val transferFailureCount: Int,
    @SerialName("last_probed_at") val lastProbedAt: String? = null,
    @SerialName("last_transfer_at") val lastTransferAt: String? = null
)

@Serializable
data class Webhook(
    val id: String,
    val name: String,
    val url: String,
    val events: List<String>,
    @SerialName("is_enabled") val isEnabled: Boolean,
    @SerialName("repository_id") val repositoryId: String? = null,
    @SerialName("last_triggered_at") val lastTriggeredAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class WebhookListResponse(
    val items: List<Webhook>,
    val total: Long
)

@Serializable
data class CreateWebhookRequest(
    val name: String,
    val url: String,
    val events: List<String>,
    val secret: String? = null
)

@Serializable
data class TestWebhookResponse(
    val success: Boolean,
    @SerialName("status_code") val statusCode: Int? = null,
    @SerialName("response_body") val responseBody: String? = null,
    val error: String? = null
)

// --- Security Policies ---
@Serializable
data class SecurityPolicy(
    val id: String,
    val name: String,
    @SerialName("repository_id") val repositoryId: String? = null,
    @SerialName("max_severity") val maxSeverity: String,
    @SerialName("block_unscanned") val blockUnscanned: Boolean,
    @SerialName("block_on_fail") val blockOnFail: Boolean,
    @SerialName("is_enabled") val isEnabled: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class CreatePolicyRequest(
    val name: String,
    @SerialName("repository_id") val repositoryId: String? = null,
    @SerialName("max_severity") val maxSeverity: String,
    @SerialName("block_unscanned") val blockUnscanned: Boolean,
    @SerialName("block_on_fail") val blockOnFail: Boolean
)

@Serializable
data class UpdatePolicyRequest(
    val name: String,
    @SerialName("max_severity") val maxSeverity: String,
    @SerialName("block_unscanned") val blockUnscanned: Boolean,
    @SerialName("block_on_fail") val blockOnFail: Boolean,
    @SerialName("is_enabled") val isEnabled: Boolean
)

@Serializable
data class TriggerScanRequest(
    @SerialName("artifact_id") val artifactId: String? = null,
    @SerialName("repository_id") val repositoryId: String? = null
)

@Serializable
data class TriggerScanResponse(
    val message: String,
    @SerialName("artifacts_queued") val artifactsQueued: Int
)

// --- Operations ---
@Serializable
data class AdminStats(
    @SerialName("total_repositories") val totalRepositories: Int = 0,
    @SerialName("total_artifacts") val totalArtifacts: Int = 0,
    @SerialName("total_users") val totalUsers: Int = 0,
    @SerialName("total_groups") val totalGroups: Int = 0,
    @SerialName("total_storage_bytes") val totalStorageBytes: Long = 0,
    @SerialName("total_downloads") val totalDownloads: Long = 0
)

@Serializable
data class StorageBreakdownItem(
    @SerialName("repository_id") val repositoryId: String,
    @SerialName("repository_key") val repositoryKey: String,
    val format: String,
    @SerialName("storage_bytes") val storageBytes: Long,
    @SerialName("artifact_count") val artifactCount: Int
)

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

@Serializable
data class HealthResponse(
    val status: String,
    val checks: Map<String, HealthCheck> = emptyMap()
)

@Serializable
data class HealthCheck(
    val status: String,
    @SerialName("response_time_ms") val responseTimeMs: Long? = null
)

@Serializable
data class HealthLogEntry(
    @SerialName("service_name") val serviceName: String,
    val status: String,
    @SerialName("previous_status") val previousStatus: String? = null,
    val message: String? = null,
    @SerialName("response_time_ms") val responseTimeMs: Long? = null,
    @SerialName("checked_at") val checkedAt: String,
)

@Serializable
data class AlertState(
    @SerialName("service_name") val serviceName: String,
    @SerialName("current_status") val currentStatus: String,
    @SerialName("consecutive_failures") val consecutiveFailures: Int = 0,
    @SerialName("last_alert_sent_at") val lastAlertSentAt: String? = null,
    @SerialName("suppressed_until") val suppressedUntil: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)

// --- Multi-server ---
@Serializable
data class SavedServer(
    val id: String,
    val name: String,
    val url: String,
    val addedAt: Long  // epoch millis
)

@Serializable
data class AssignRepoRequest(
    @SerialName("repository_id") val repositoryId: String,
    @SerialName("replication_mode") val replicationMode: String = "pull"
)
