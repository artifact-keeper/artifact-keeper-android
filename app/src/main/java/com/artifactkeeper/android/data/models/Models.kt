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
