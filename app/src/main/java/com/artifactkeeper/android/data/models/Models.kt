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
    @SerialName("repository_id") val repositoryId: String,
    val name: String,
    val path: String,
    val version: String? = null,
    @SerialName("content_type") val contentType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("download_count") val downloadCount: Int,
    @SerialName("checksum_sha256") val checksumSha256: String,
    @SerialName("created_at") val createdAt: String,
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
    val user: UserInfo,
)

@Serializable
data class UserInfo(
    val id: String,
    val username: String,
    val email: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
)
