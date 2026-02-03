package com.artifactkeeper.android.data.api

import com.artifactkeeper.android.data.models.ArtifactListResponse
import com.artifactkeeper.android.data.models.BuildListResponse
import com.artifactkeeper.android.data.models.LoginRequest
import com.artifactkeeper.android.data.models.LoginResponse
import com.artifactkeeper.android.data.models.PackageListResponse
import com.artifactkeeper.android.data.models.RepoSecurityScore
import com.artifactkeeper.android.data.models.Repository
import com.artifactkeeper.android.data.models.RepositoryListResponse
import com.artifactkeeper.android.data.models.ScanListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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

    @GET("/api/v1/repositories/{key}")
    suspend fun getRepository(@Path("key") key: String): Repository

    @GET("/api/v1/repositories/{key}/artifacts")
    suspend fun listArtifacts(
        @Path("key") repoKey: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("search") search: String? = null,
    ): ArtifactListResponse

    @GET("/api/v1/security/scores")
    suspend fun getSecurityScores(): List<RepoSecurityScore>

    @GET("/api/v1/security/scans")
    suspend fun listScans(
        @Query("repository_id") repositoryId: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): ScanListResponse

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}
