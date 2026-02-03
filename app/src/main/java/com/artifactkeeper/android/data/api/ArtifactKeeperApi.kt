package com.artifactkeeper.android.data.api

import com.artifactkeeper.android.data.models.BuildListResponse
import com.artifactkeeper.android.data.models.PackageListResponse
import com.artifactkeeper.android.data.models.RepositoryListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ArtifactKeeperApi {
    @GET("/api/v1/packages")
    suspend fun listPackages(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 24,
        @Query("search") search: String? = null,
        @Query("format") format: String? = null,
    ): PackageListResponse

    @GET("/api/v1/builds")
    suspend fun listBuilds(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("sort_by") sortBy: String = "created_at",
        @Query("sort_order") sortOrder: String = "desc",
    ): BuildListResponse

    @GET("/api/v1/repositories")
    suspend fun listRepositories(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
    ): RepositoryListResponse
}
