package com.artifactkeeper.client.apis

import com.artifactkeeper.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.artifactkeeper.client.models.ChunkResponse
import com.artifactkeeper.client.models.CompleteResponse
import com.artifactkeeper.client.models.CreateSessionRequest
import com.artifactkeeper.client.models.CreateSessionResponse
import com.artifactkeeper.client.models.ErrorResponse
import com.artifactkeeper.client.models.SessionStatusResponse

interface UploadsApi {
    /**
     * DELETE api/v1/uploads/{session_id}
     * 
     * 
     * Responses:
     *  - 204: Upload cancelled
     *  - 404: Session not found
     *
     * @param sessionId Upload session ID
     * @return [Unit]
     */
    @DELETE("api/v1/uploads/{session_id}")
    suspend fun cancel(@Path("session_id") sessionId: java.util.UUID): Response<Unit>

    /**
     * PUT api/v1/uploads/{session_id}/complete
     * 
     * 
     * Responses:
     *  - 200: Upload finalized, artifact created
     *  - 400: Incomplete chunks or invalid state
     *  - 404: Session not found
     *  - 409: Checksum mismatch
     *  - 410: Session expired
     *
     * @param sessionId Upload session ID
     * @return [CompleteResponse]
     */
    @PUT("api/v1/uploads/{session_id}/complete")
    suspend fun complete(@Path("session_id") sessionId: java.util.UUID): Response<CompleteResponse>

    /**
     * POST api/v1/uploads
     * 
     * 
     * Responses:
     *  - 201: Upload session created
     *  - 400: Invalid request
     *  - 401: Unauthorized
     *  - 403: Forbidden
     *  - 404: Repository not found
     *
     * @param createSessionRequest 
     * @return [CreateSessionResponse]
     */
    @POST("api/v1/uploads")
    suspend fun createSession(@Body createSessionRequest: CreateSessionRequest): Response<CreateSessionResponse>

    /**
     * GET api/v1/uploads/{session_id}
     * 
     * 
     * Responses:
     *  - 200: Session status
     *  - 404: Session not found
     *  - 410: Session expired
     *
     * @param sessionId Upload session ID
     * @return [SessionStatusResponse]
     */
    @GET("api/v1/uploads/{session_id}")
    suspend fun getSessionStatus(@Path("session_id") sessionId: java.util.UUID): Response<SessionStatusResponse>

    /**
     * PATCH api/v1/uploads/{session_id}
     * 
     * 
     * Responses:
     *  - 200: Chunk uploaded
     *  - 400: Invalid chunk or Content-Range
     *  - 401: Unauthorized
     *  - 404: Session not found
     *  - 410: Session expired
     *
     * @param sessionId Upload session ID
     * @return [ChunkResponse]
     */
    @PATCH("api/v1/uploads/{session_id}")
    suspend fun uploadChunk(@Path("session_id") sessionId: java.util.UUID): Response<ChunkResponse>

}
