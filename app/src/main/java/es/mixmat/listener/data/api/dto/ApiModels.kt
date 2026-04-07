package es.mixmat.listener.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val data: T,
    val meta: Meta,
)

@Serializable
data class ApiErrorResponse(
    val error: ApiError,
    val meta: Meta,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
)

@Serializable
data class Meta(
    @SerialName("request_id") val requestId: String,
    val timestamp: String,
)

// -- Health --

@Serializable
data class HealthData(
    val status: String,
    val version: String,
)

// -- Auth --

@Serializable
data class UserData(
    val user: UserInfo,
    @SerialName("rate_limit") val rateLimit: RateLimitInfo? = null,
)

@Serializable
data class UserInfo(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val role: String,
    @SerialName("listen_enabled") val listenEnabled: Boolean,
    @SerialName("preferred_platform") val preferredPlatform: String? = null,
)

@Serializable
data class RateLimitInfo(
    val limit: Int,
    val remaining: Int,
    @SerialName("reset_at") val resetAt: String,
)

// -- Recognition --

@Serializable
data class RecognizeData(
    val status: String,
    val source: String? = null,
    val track: TrackDto? = null,
)

@Serializable
data class TrackDto(
    val title: String,
    val artist: String,
    val thumbnail: String? = null,
    val shortcode: String? = null,
    @SerialName("share_url") val shareUrl: String? = null,
    val platforms: PlatformsDto,
)

@Serializable
data class PlatformsDto(
    val spotify: String? = null,
    val tidal: String? = null,
    val appleMusic: String? = null,
)

// -- History --

@Serializable
data class HistoryListData(
    val items: List<HistoryItemDto>,
    val cursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean,
)

@Serializable
data class HistoryItemDto(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnail: String? = null,
    val shortcode: String? = null,
    @SerialName("share_url") val shareUrl: String? = null,
    val platforms: PlatformsDto,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class HistoryDetailData(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnail: String? = null,
    val shortcode: String? = null,
    @SerialName("share_url") val shareUrl: String? = null,
    val platforms: PlatformsDto,
    @SerialName("created_at") val createdAt: String,
    @SerialName("shared_to") val sharedTo: List<SharedGroupDto>,
)

@Serializable
data class SharedGroupDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
)

@Serializable
data class DeletedData(
    val deleted: Boolean,
)

// -- Share --

@Serializable
data class ShareRequest(
    @SerialName("group_ids") val groupIds: List<String>,
)

@Serializable
data class ShareData(
    val results: Map<String, String>,
)

// -- Groups --

@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val description: String? = null,
)

// -- Recordings --

@Serializable
data class RecordingDto(
    @SerialName("recording_id") val recordingId: String,
    @SerialName("created_at") val createdAt: String? = null,
    val outcome: String? = null,
    val title: String? = null,
    val artist: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
)

@Serializable
data class DeletedCountData(
    val deleted: Int,
)
