package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.CreateGroupRequest
import com.artifactkeeper.client.models.GroupListResponse
import com.artifactkeeper.client.models.GroupResponse
import com.artifactkeeper.client.models.MembersRequest

interface GroupsApi {
    /**
     * POST api/v1/groups/{id}/members
     * Add members to a group
     * 
     * Responses:
     *  - 200: Members added successfully
     *  - 404: Group not found
     *  - 500: Internal server error
     *
     * @param id Group ID
     * @param membersRequest 
     * @return [Unit]
     */
    @POST("api/v1/groups/{id}/members")
    suspend fun addMembers(@Path("id") id: java.util.UUID, @Body membersRequest: MembersRequest): Response<Unit>

    /**
     * POST api/v1/groups
     * Create a group
     * 
     * Responses:
     *  - 200: Group created successfully
     *  - 409: Group name already exists
     *  - 500: Internal server error
     *
     * @param createGroupRequest 
     * @return [GroupResponse]
     */
    @POST("api/v1/groups")
    suspend fun createGroup(@Body createGroupRequest: CreateGroupRequest): Response<GroupResponse>

    /**
     * DELETE api/v1/groups/{id}
     * Delete a group
     * 
     * Responses:
     *  - 200: Group deleted successfully
     *  - 404: Group not found
     *  - 500: Internal server error
     *
     * @param id Group ID
     * @return [Unit]
     */
    @DELETE("api/v1/groups/{id}")
    suspend fun deleteGroup(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/groups/{id}
     * Get a group by ID
     * 
     * Responses:
     *  - 200: Group details
     *  - 404: Group not found
     *  - 500: Internal server error
     *
     * @param id Group ID
     * @return [GroupResponse]
     */
    @GET("api/v1/groups/{id}")
    suspend fun getGroup(@Path("id") id: java.util.UUID): Response<GroupResponse>

    /**
     * GET api/v1/groups
     * List groups
     * 
     * Responses:
     *  - 200: List of groups
     *  - 500: Internal server error
     *
     * @param search  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [GroupListResponse]
     */
    @GET("api/v1/groups")
    suspend fun listGroups(@Query("search") search: kotlin.String? = null, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null): Response<GroupListResponse>

    /**
     * DELETE api/v1/groups/{id}/members
     * Remove members from a group
     * 
     * Responses:
     *  - 200: Members removed successfully
     *  - 404: Group not found
     *  - 500: Internal server error
     *
     * @param id Group ID
     * @param membersRequest 
     * @return [Unit]
     */
    @DELETE("api/v1/groups/{id}/members")
    suspend fun removeMembers(@Path("id") id: java.util.UUID, @Body membersRequest: MembersRequest): Response<Unit>

    /**
     * PUT api/v1/groups/{id}
     * Update a group
     * 
     * Responses:
     *  - 200: Group updated successfully
     *  - 404: Group not found
     *  - 500: Internal server error
     *
     * @param id Group ID
     * @param createGroupRequest 
     * @return [GroupResponse]
     */
    @PUT("api/v1/groups/{id}")
    suspend fun updateGroup(@Path("id") id: java.util.UUID, @Body createGroupRequest: CreateGroupRequest): Response<GroupResponse>

}
