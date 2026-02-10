package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.AnnouncePeerRequest
import com.artifactkeeper.client.models.AssignRepoRequest
import com.artifactkeeper.client.models.ChunkAvailabilityResponse
import com.artifactkeeper.client.models.ChunkManifestResponse
import com.artifactkeeper.client.models.CompleteChunkBody
import com.artifactkeeper.client.models.DiscoverablePeerResponse
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.FailBody
import com.artifactkeeper.client.models.HeartbeatRequest
import com.artifactkeeper.client.models.IdentityResponse
import com.artifactkeeper.client.models.InitTransferBody
import com.artifactkeeper.client.models.NetworkProfileBody
import com.artifactkeeper.client.models.PeerInstanceListResponse
import com.artifactkeeper.client.models.PeerInstanceResponse
import com.artifactkeeper.client.models.PeerResponse
import com.artifactkeeper.client.models.ProbeBody
import com.artifactkeeper.client.models.RegisterPeerRequest
import com.artifactkeeper.client.models.ScoredPeerResponse
import com.artifactkeeper.client.models.SyncTaskResponse
import com.artifactkeeper.client.models.TransferSessionResponse
import com.artifactkeeper.client.models.UpdateChunkAvailabilityBody

interface PeersApi {
    /**
     * POST api/v1/peers/announce
     * POST /api/v1/peers/announce
     * 
     * Responses:
     *  - 200: Peer announcement accepted
     *  - 500: Internal server error
     *
     * @param announcePeerRequest 
     * @return [kotlinx.serialization.json.JsonElement]
     */
    @POST("api/v1/peers/announce")
    suspend fun announcePeer(@Body announcePeerRequest: AnnouncePeerRequest): Response<kotlinx.serialization.json.JsonElement>

    /**
     * POST api/v1/peers/{id}/repositories
     * Assign repository to peer instance
     * 
     * Responses:
     *  - 200: Repository assigned successfully
     *  - 404: Peer instance not found
     *  - 500: Internal server error
     *
     * @param id Peer instance ID
     * @param assignRepoRequest 
     * @return [Unit]
     */
    @POST("api/v1/peers/{id}/repositories")
    suspend fun assignRepo(@Path("id") id: java.util.UUID, @Body assignRepoRequest: AssignRepoRequest): Response<Unit>

    /**
     * POST api/v1/peers/{id}/transfer/{session_id}/chunk/{chunk_index}/complete
     * POST /api/v1/peers/:id/transfer/:session_id/chunk/:chunk_index/complete
     * 
     * Responses:
     *  - 200: Chunk marked as complete
     *
     * @param id Peer instance ID
     * @param sessionId Transfer session ID
     * @param chunkIndex Chunk index
     * @param completeChunkBody 
     * @return [Unit]
     */
    @POST("api/v1/peers/{id}/transfer/{session_id}/chunk/{chunk_index}/complete")
    suspend fun completeChunk(@Path("id") id: java.util.UUID, @Path("session_id") sessionId: java.util.UUID, @Path("chunk_index") chunkIndex: kotlin.Int, @Body completeChunkBody: CompleteChunkBody): Response<Unit>

    /**
     * POST api/v1/peers/{id}/transfer/{session_id}/complete
     * POST /api/v1/peers/:id/transfer/:session_id/complete
     * 
     * Responses:
     *  - 200: Transfer session marked as complete
     *
     * @param id Peer instance ID
     * @param sessionId Transfer session ID
     * @return [Unit]
     */
    @POST("api/v1/peers/{id}/transfer/{session_id}/complete")
    suspend fun completeSession(@Path("id") id: java.util.UUID, @Path("session_id") sessionId: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/peers/{id}/connections/discover
     * GET /api/v1/peers/:id/connections/discover
     * 
     * Responses:
     *  - 200: Discoverable peers
     *
     * @param id Peer instance ID
     * @return [kotlin.collections.List<DiscoverablePeerResponse>]
     */
    @GET("api/v1/peers/{id}/connections/discover")
    suspend fun discoverPeers(@Path("id") id: java.util.UUID): Response<kotlin.collections.List<DiscoverablePeerResponse>>

    /**
     * POST api/v1/peers/{id}/transfer/{session_id}/chunk/{chunk_index}/fail
     * POST /api/v1/peers/:id/transfer/:session_id/chunk/:chunk_index/fail
     * 
     * Responses:
     *  - 200: Chunk marked as failed
     *
     * @param id Peer instance ID
     * @param sessionId Transfer session ID
     * @param chunkIndex Chunk index
     * @param failBody 
     * @return [Unit]
     */
    @POST("api/v1/peers/{id}/transfer/{session_id}/chunk/{chunk_index}/fail")
    suspend fun failChunk(@Path("id") id: java.util.UUID, @Path("session_id") sessionId: java.util.UUID, @Path("chunk_index") chunkIndex: kotlin.Int, @Body failBody: FailBody): Response<Unit>

    /**
     * POST api/v1/peers/{id}/transfer/{session_id}/fail
     * POST /api/v1/peers/:id/transfer/:session_id/fail
     * 
     * Responses:
     *  - 200: Transfer session marked as failed
     *
     * @param id Peer instance ID
     * @param sessionId Transfer session ID
     * @param failBody 
     * @return [Unit]
     */
    @POST("api/v1/peers/{id}/transfer/{session_id}/fail")
    suspend fun failSession(@Path("id") id: java.util.UUID, @Path("session_id") sessionId: java.util.UUID, @Body failBody: FailBody): Response<Unit>

    /**
     * GET api/v1/peers/{id}/repositories
     * Get assigned repositories for peer instance
     * 
     * Responses:
     *  - 200: List of assigned repository IDs
     *  - 404: Peer instance not found
     *  - 500: Internal server error
     *
     * @param id Peer instance ID
     * @return [kotlin.collections.List<java.util.UUID>]
     */
    @GET("api/v1/peers/{id}/repositories")
    suspend fun getAssignedRepos(@Path("id") id: java.util.UUID): Response<kotlin.collections.List<java.util.UUID>>

    /**
     * GET api/v1/peers/{id}/chunks/{artifact_id}
     * GET /api/v1/peers/:id/chunks/:artifact_id
     * 
     * Responses:
     *  - 200: Chunk availability for this peer and artifact
     *  - 404: No chunk availability data
     *
     * @param id Peer instance ID
     * @param artifactId Artifact ID
     * @return [ChunkAvailabilityResponse]
     */
    @GET("api/v1/peers/{id}/chunks/{artifact_id}")
    suspend fun getChunkAvailability(@Path("id") id: java.util.UUID, @Path("artifact_id") artifactId: java.util.UUID): Response<ChunkAvailabilityResponse>

    /**
     * GET api/v1/peers/{id}/transfer/{session_id}/chunks
     * GET /api/v1/peers/:id/transfer/:session_id/chunks
     * 
     * Responses:
     *  - 200: Chunk manifest for the transfer session
     *
     * @param id Peer instance ID
     * @param sessionId Transfer session ID
     * @return [ChunkManifestResponse]
     */
    @GET("api/v1/peers/{id}/transfer/{session_id}/chunks")
    suspend fun getChunkManifest(@Path("id") id: java.util.UUID, @Path("session_id") sessionId: java.util.UUID): Response<ChunkManifestResponse>

    /**
     * GET api/v1/peers/identity
     * GET /api/v1/peers/identity
     * 
     * Responses:
     *  - 200: Local peer identity
     *  - 500: Internal server error
     *
     * @return [IdentityResponse]
     */
    @GET("api/v1/peers/identity")
    suspend fun getIdentity(): Response<IdentityResponse>

    /**
     * GET api/v1/peers/{id}
     * Get peer instance details
     * 
     * Responses:
     *  - 200: Peer instance details
     *  - 404: Peer instance not found
     *  - 500: Internal server error
     *
     * @param id Peer instance ID
     * @return [PeerInstanceResponse]
     */
    @GET("api/v1/peers/{id}")
    suspend fun getPeer(@Path("id") id: java.util.UUID): Response<PeerInstanceResponse>

    /**
     * GET api/v1/peers/{id}/chunks/{artifact_id}/peers
     * GET /api/v1/peers/:id/chunks/:artifact_id/peers
     * 
     * Responses:
     *  - 200: Peers that have chunks for this artifact
     *
     * @param id Peer instance ID
     * @param artifactId Artifact ID
     * @return [kotlin.collections.List<ChunkAvailabilityResponse>]
     */
    @GET("api/v1/peers/{id}/chunks/{artifact_id}/peers")
    suspend fun getPeersWithChunks(@Path("id") id: java.util.UUID, @Path("artifact_id") artifactId: java.util.UUID): Response<kotlin.collections.List<ChunkAvailabilityResponse>>

    /**
     * GET api/v1/peers/{id}/chunks/{artifact_id}/scored-peers
     * GET /api/v1/peers/:id/chunks/:artifact_id/scored-peers
     * 
     * Responses:
     *  - 200: Scored peers for artifact download
     *
     * @param id Peer instance ID
     * @param artifactId Artifact ID
     * @return [kotlin.collections.List<ScoredPeerResponse>]
     */
    @GET("api/v1/peers/{id}/chunks/{artifact_id}/scored-peers")
    suspend fun getScoredPeers(@Path("id") id: java.util.UUID, @Path("artifact_id") artifactId: java.util.UUID): Response<kotlin.collections.List<ScoredPeerResponse>>

    /**
     * GET api/v1/peers/{id}/transfer/{session_id}
     * GET /api/v1/peers/:id/transfer/:session_id
     * 
     * Responses:
     *  - 200: Transfer session details
     *
     * @param id Peer instance ID
     * @param sessionId Transfer session ID
     * @return [TransferSessionResponse]
     */
    @GET("api/v1/peers/{id}/transfer/{session_id}")
    suspend fun getSession(@Path("id") id: java.util.UUID, @Path("session_id") sessionId: java.util.UUID): Response<TransferSessionResponse>

    /**
     * GET api/v1/peers/{id}/sync/tasks
     * Get pending sync tasks for peer instance
     * 
     * Responses:
     *  - 200: List of pending sync tasks
     *  - 404: Peer instance not found
     *  - 500: Internal server error
     *
     * @param id Peer instance ID
     * @param status  (optional)
     * @param region  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [kotlin.collections.List<SyncTaskResponse>]
     */
    @GET("api/v1/peers/{id}/sync/tasks")
    suspend fun getSyncTasks(@Path("id") id: java.util.UUID, @Query("status") status: kotlin.String? = null, @Query("region") region: kotlin.String? = null, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null): Response<kotlin.collections.List<SyncTaskResponse>>

    /**
     * POST api/v1/peers/{id}/heartbeat
     * Heartbeat from peer instance
     * 
     * Responses:
     *  - 200: Heartbeat recorded successfully
     *  - 404: Peer instance not found
     *  - 500: Internal server error
     *
     * @param id Peer instance ID
     * @param heartbeatRequest 
     * @return [Unit]
     */
    @POST("api/v1/peers/{id}/heartbeat")
    suspend fun heartbeat(@Path("id") id: java.util.UUID, @Body heartbeatRequest: HeartbeatRequest): Response<Unit>

    /**
     * POST api/v1/peers/{id}/transfer/init
     * POST /api/v1/peers/:id/transfer/init
     * 
     * Responses:
     *  - 200: Transfer session initialized
     *
     * @param id Peer instance ID
     * @param initTransferBody 
     * @return [TransferSessionResponse]
     */
    @POST("api/v1/peers/{id}/transfer/init")
    suspend fun initTransfer(@Path("id") id: java.util.UUID, @Body initTransferBody: InitTransferBody): Response<TransferSessionResponse>

    /**
     * GET api/v1/peers/{id}/connections
     * GET /api/v1/peers/:id/connections
     * 
     * Responses:
     *  - 200: List of peer connections
     *  - 404: Peer not found
     *
     * @param id Peer instance ID
     * @param status Filter peers by status (active, probing, unreachable, disabled) (optional)
     * @return [kotlin.collections.List<PeerResponse>]
     */
    @GET("api/v1/peers/{id}/connections")
    suspend fun listPeerConnections(@Path("id") id: java.util.UUID, @Query("status") status: kotlin.String? = null): Response<kotlin.collections.List<PeerResponse>>

    /**
     * GET api/v1/peers
     * List peer instances
     * 
     * Responses:
     *  - 200: List of peer instances
     *  - 500: Internal server error
     *
     * @param status  (optional)
     * @param region  (optional)
     * @param page  (optional)
     * @param perPage  (optional)
     * @return [PeerInstanceListResponse]
     */
    @GET("api/v1/peers")
    suspend fun listPeers(@Query("status") status: kotlin.String? = null, @Query("region") region: kotlin.String? = null, @Query("page") page: kotlin.Int? = null, @Query("per_page") perPage: kotlin.Int? = null): Response<PeerInstanceListResponse>

    /**
     * POST api/v1/peers/{id}/connections/{target_id}/unreachable
     * POST /api/v1/peers/:id/connections/:target_id/unreachable
     * 
     * Responses:
     *  - 200: Peer marked as unreachable
     *
     * @param id Peer instance ID
     * @param targetId Target peer ID to mark unreachable
     * @return [Unit]
     */
    @POST("api/v1/peers/{id}/connections/{target_id}/unreachable")
    suspend fun markUnreachable(@Path("id") id: java.util.UUID, @Path("target_id") targetId: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/peers/{id}/connections/probe
     * POST /api/v1/peers/:id/connections/probe
     * 
     * Responses:
     *  - 200: Probe result recorded
     *
     * @param id Peer instance ID
     * @param probeBody 
     * @return [PeerResponse]
     */
    @POST("api/v1/peers/{id}/connections/probe")
    suspend fun probePeer(@Path("id") id: java.util.UUID, @Body probeBody: ProbeBody): Response<PeerResponse>

    /**
     * POST api/v1/peers
     * Register new peer instance
     * 
     * Responses:
     *  - 200: Peer instance registered successfully
     *  - 500: Internal server error
     *
     * @param registerPeerRequest 
     * @return [PeerInstanceResponse]
     */
    @POST("api/v1/peers")
    suspend fun registerPeer(@Body registerPeerRequest: RegisterPeerRequest): Response<PeerInstanceResponse>

    /**
     * POST api/v1/peers/{id}/transfer/{session_id}/chunk/{chunk_index}/retry
     * POST /api/v1/peers/:id/transfer/:session_id/chunk/:chunk_index/retry
     * 
     * Responses:
     *  - 200: Chunk queued for retry
     *
     * @param id Peer instance ID
     * @param sessionId Transfer session ID
     * @param chunkIndex Chunk index
     * @return [Unit]
     */
    @POST("api/v1/peers/{id}/transfer/{session_id}/chunk/{chunk_index}/retry")
    suspend fun retryChunk(@Path("id") id: java.util.UUID, @Path("session_id") sessionId: java.util.UUID, @Path("chunk_index") chunkIndex: kotlin.Int): Response<Unit>

    /**
     * POST api/v1/peers/{id}/sync
     * Trigger sync for peer instance
     * 
     * Responses:
     *  - 200: Sync triggered successfully
     *  - 404: Peer instance not found
     *  - 500: Internal server error
     *
     * @param id Peer instance ID
     * @return [Unit]
     */
    @POST("api/v1/peers/{id}/sync")
    suspend fun triggerSync(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * DELETE api/v1/peers/{id}/repositories/{repo_id}
     * Unassign repository from peer instance
     * 
     * Responses:
     *  - 200: Repository unassigned successfully
     *  - 404: Peer instance or repository not found
     *  - 500: Internal server error
     *
     * @param id Peer instance ID
     * @param repoId Repository ID
     * @return [Unit]
     */
    @DELETE("api/v1/peers/{id}/repositories/{repo_id}")
    suspend fun unassignRepo(@Path("id") id: java.util.UUID, @Path("repo_id") repoId: java.util.UUID): Response<Unit>

    /**
     * DELETE api/v1/peers/{id}
     * Unregister peer instance
     * 
     * Responses:
     *  - 200: Peer instance unregistered successfully
     *  - 404: Peer instance not found
     *  - 500: Internal server error
     *
     * @param id Peer instance ID
     * @return [Unit]
     */
    @DELETE("api/v1/peers/{id}")
    suspend fun unregisterPeer(@Path("id") id: java.util.UUID): Response<Unit>

    /**
     * PUT api/v1/peers/{id}/chunks/{artifact_id}
     * PUT /api/v1/peers/:id/chunks/:artifact_id
     * 
     * Responses:
     *  - 200: Chunk availability updated
     *
     * @param id Peer instance ID
     * @param artifactId Artifact ID
     * @param updateChunkAvailabilityBody 
     * @return [Unit]
     */
    @PUT("api/v1/peers/{id}/chunks/{artifact_id}")
    suspend fun updateChunkAvailability(@Path("id") id: java.util.UUID, @Path("artifact_id") artifactId: java.util.UUID, @Body updateChunkAvailabilityBody: UpdateChunkAvailabilityBody): Response<Unit>

    /**
     * PUT api/v1/peers/{id}/network-profile
     * PUT /api/v1/peers/:id/network-profile
     * 
     * Responses:
     *  - 200: Network profile updated
     *
     * @param id Peer instance ID
     * @param networkProfileBody 
     * @return [Unit]
     */
    @PUT("api/v1/peers/{id}/network-profile")
    suspend fun updateNetworkProfile(@Path("id") id: java.util.UUID, @Body networkProfileBody: NetworkProfileBody): Response<Unit>

}
