package es.mixmat.listener.data.api

import es.mixmat.listener.data.api.dto.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface ListenerApi {

    @GET("health")
    suspend fun health(): ApiResponse<HealthData>

    @GET("auth/me")
    suspend fun me(): ApiResponse<UserData>

    @Multipart
    @POST("recognize")
    suspend fun recognize(
        @Part audio: MultipartBody.Part,
    ): ApiResponse<RecognizeData>

    @GET("history")
    suspend fun history(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null,
    ): ApiResponse<HistoryListData>

    @GET("history/{id}")
    suspend fun historyDetail(
        @Path("id") id: String,
    ): ApiResponse<HistoryDetailData>

    @DELETE("history/{id}")
    suspend fun historyDelete(
        @Path("id") id: String,
    ): ApiResponse<DeletedData>

    @POST("history/{id}/share")
    suspend fun historyShare(
        @Path("id") id: String,
        @Body request: ShareRequest,
    ): ApiResponse<ShareData>

    @GET("groups")
    suspend fun groups(): ApiResponse<List<GroupDto>>

    @GET("recordings")
    suspend fun recordings(): ApiResponse<List<RecordingDto>>

    @DELETE("recordings")
    suspend fun deleteRecordings(): ApiResponse<DeletedCountData>
}
