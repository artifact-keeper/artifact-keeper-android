package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.PackageListResponse
import com.artifactkeeper.client.models.PackageResponse
import com.artifactkeeper.client.models.PackageVersionsResponse

interface PackagesApi {
    /**
     * GET api/v1/packages/{id}
     * Get a package by ID
     * 
     * Responses:
     *  - 200: Package details
     *  - 404: Package not found
     *  - 500: Internal server error
     *
     * @param id Package ID
     * @return [PackageResponse]
     */
    @GET("api/v1/packages/{id}")
    suspend fun getPackage(@Path("id") id: java.util.UUID): Response<PackageResponse>

    /**
     * GET api/v1/packages/{id}/versions
     * Get package versions
     * 
     * Responses:
     *  - 200: List of package versions
     *  - 404: Package not found
     *  - 500: Internal server error
     *
     * @param id Package ID
     * @return [PackageVersionsResponse]
     */
    @GET("api/v1/packages/{id}/versions")
    suspend fun getPackageVersions(@Path("id") id: java.util.UUID): Response<PackageVersionsResponse>

    /**
     * GET api/v1/packages
     * List packages
     * 
     * Responses:
     *  - 200: Paginated list of packages
     *  - 500: Internal server error
     *
     * @param page  (optional)
     * @param perPage  (optional)
     * @param repositoryKey  (optional)
     * @param format  (optional)
     * @param search  (optional)
     * @return [PackageListResponse]
     */
    @GET("api/v1/packages")
    suspend fun listPackages(@Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null, @Query("repository_key") repositoryKey: kotlin.String? = null, @Query("format") format: kotlin.String? = null, @Query("search") search: kotlin.String? = null): Response<PackageListResponse>

}
