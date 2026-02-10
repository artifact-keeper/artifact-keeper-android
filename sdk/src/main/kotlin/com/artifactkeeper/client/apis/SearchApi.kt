package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.AdvancedSearchResponse
import com.artifactkeeper.client.models.ChecksumSearchResponse
import com.artifactkeeper.client.models.QuickSearchResponse
import com.artifactkeeper.client.models.SearchResultItem
import com.artifactkeeper.client.models.SuggestResponse

interface SearchApi {
    /**
     * GET api/v1/search/advanced
     * 
     * 
     * Responses:
     *  - 200: Advanced search results with pagination and facets
     *
     * @param query  (optional)
     * @param format  (optional)
     * @param repositoryKey  (optional)
     * @param name  (optional)
     * @param path  (optional)
     * @param version  (optional)
     * @param minSize  (optional)
     * @param maxSize  (optional)
     * @param createdAfter  (optional)
     * @param createdBefore  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @param sortBy  (optional)
     * @param sortOrder  (optional)
     * @return [AdvancedSearchResponse]
     */
    @GET("api/v1/search/advanced")
    suspend fun advancedSearch(@Query("query") query: kotlin.String? = null, @Query("format") format: kotlin.String? = null, @Query("repository_key") repositoryKey: kotlin.String? = null, @Query("name") name: kotlin.String? = null, @Query("path") path: kotlin.String? = null, @Query("version") version: kotlin.String? = null, @Query("min_size") minSize: kotlin.Long? = null, @Query("max_size") maxSize: kotlin.Long? = null, @Query("created_after") createdAfter: kotlin.String? = null, @Query("created_before") createdBefore: kotlin.String? = null, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null, @Query("sort_by") sortBy: kotlin.String? = null, @Query("sort_order") sortOrder: kotlin.String? = null): Response<AdvancedSearchResponse>

    /**
     * GET api/v1/search/checksum
     * 
     * 
     * Responses:
     *  - 200: Artifacts matching the given checksum
     *  - 422: Unsupported checksum algorithm
     *
     * @param checksum 
     * @param algorithm  (optional)
     * @return [ChecksumSearchResponse]
     */
    @GET("api/v1/search/checksum")
    suspend fun checksumSearch(@Query("checksum") checksum: kotlin.String, @Query("algorithm") algorithm: kotlin.String? = null): Response<ChecksumSearchResponse>

    /**
     * GET api/v1/search/quick
     * 
     * 
     * Responses:
     *  - 200: Quick search results
     *
     * @param q  (optional)
     * @param limit  (optional)
     * @param types  (optional)
     * @return [QuickSearchResponse]
     */
    @GET("api/v1/search/quick")
    suspend fun quickSearch(@Query("q") q: kotlin.String? = null, @Query("limit") limit: kotlin.Long? = null, @Query("types") types: kotlin.String? = null): Response<QuickSearchResponse>

    /**
     * GET api/v1/search/recent
     * 
     * 
     * Responses:
     *  - 200: Recently uploaded artifacts
     *
     * @param limit  (optional)
     * @return [kotlin.collections.List<SearchResultItem>]
     */
    @GET("api/v1/search/recent")
    suspend fun recent(@Query("limit") limit: kotlin.Long? = null): Response<kotlin.collections.List<SearchResultItem>>

    /**
     * GET api/v1/search/suggest
     * 
     * 
     * Responses:
     *  - 200: Autocomplete suggestions for the given prefix
     *
     * @param prefix 
     * @param limit  (optional)
     * @return [SuggestResponse]
     */
    @GET("api/v1/search/suggest")
    suspend fun suggest(@Query("prefix") prefix: kotlin.String, @Query("limit") limit: kotlin.Long? = null): Response<SuggestResponse>

    /**
     * GET api/v1/search/trending
     * 
     * 
     * Responses:
     *  - 200: Trending artifacts by download count
     *
     * @param days  (optional)
     * @param limit  (optional)
     * @return [kotlin.collections.List<SearchResultItem>]
     */
    @GET("api/v1/search/trending")
    suspend fun trending(@Query("days") days: kotlin.Int? = null, @Query("limit") limit: kotlin.Long? = null): Response<kotlin.collections.List<SearchResultItem>>

}
