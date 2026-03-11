package com.portablebrain.client.network

import com.portablebrain.client.data.ExecutionStartRequest
import com.portablebrain.client.data.ExecutionStepRequest
import com.portablebrain.client.data.ExecutionResponse
import com.portablebrain.client.data.UIStateSnapshot
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // --- Background monitoring ---

    @POST("monitoring/background-tasks/session/start")
    suspend fun startTracking(): Response<Unit>

    @POST("monitoring/background-tasks/session/stop")
    suspend fun stopTracking(): Response<Unit>

    @POST("monitoring/background-tasks/process-ui-state")
    suspend fun postState(@Body snapshot: UIStateSnapshot): Response<Unit>

    // --- Execution loop ---

    @POST("execution/start")
    suspend fun startExecution(@Body request: ExecutionStartRequest): Response<ExecutionResponse>

    @POST("execution/{session_id}/step")
    suspend fun executionStep(
        @Path("session_id") sessionId: String,
        @Body request: ExecutionStepRequest
    ): Response<ExecutionResponse>
}
