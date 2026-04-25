package es.mixmat.listener.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val data: T,
    val meta: Meta? = null,
)

@Serializable
data class ApiErrorResponse(
    val error: ApiError? = null,
    val meta: Meta? = null,
)

@Serializable
data class ApiError(
    val code: String = "",
    val message: String = "",
)

@Serializable
data class Meta(
    @SerialName("request_id") val requestId: String = "",
    val timestamp: String = "",
)

// -- Health --

@Serializable
data class HealthData(
    val status: String = "",
    val version: String = "",
)

// -- Auth --

@Serializable
data class UserData(
    val user: UserInfo,
    @SerialName("rate_limit") val rateLimit: RateLimitInfo? = null,
)

@Serializable
data class UserInfo(
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val role: String = "",
    @SerialName("listen_enabled") val listenEnabled: Boolean = false,
    @SerialName("preferred_platform") val preferredPlatform: String? = null,
)

@Serializable
data class RateLimitInfo(
    val limit: Int = 0,
    val remaining: Int = 0,
    @SerialName("reset_at") val resetAt: Long = 0,
)

// -- Recognition --

@Serializable
data class RecognizeData(
    val status: String = "",
    val source: String? = null,
    @SerialName("history_id") val historyId: String? = null,
    val track: TrackDto? = null,
)

@Serializable
data class TrackDto(
    val title: String = "",
    val artist: String = "",
    val thumbnail: String? = null,
    val shortcode: String? = null,
    @SerialName("share_url") val shareUrl: String? = null,
    val platforms: PlatformsDto = PlatformsDto(),
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
    val items: List<HistoryItemDto> = emptyList(),
    val cursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class HistoryItemDto(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val thumbnail: String? = null,
    val shortcode: String? = null,
    @SerialName("share_url") val shareUrl: String? = null,
    val platforms: PlatformsDto = PlatformsDto(),
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class HistoryDetailData(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val thumbnail: String? = null,
    val shortcode: String? = null,
    @SerialName("share_url") val shareUrl: String? = null,
    val platforms: PlatformsDto = PlatformsDto(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("shared_to") val sharedTo: List<SharedGroupDto> = emptyList(),
)

@Serializable
data class SharedGroupDto(
    @SerialName("group_id") val groupId: String = "",
    @SerialName("group_name") val groupName: String = "",
)

@Serializable
data class DeletedData(
    val deleted: Boolean = false,
)

// -- Share --

@Serializable
data class ShareRequest(
    @SerialName("group_ids") val groupIds: List<String>,
)

@Serializable
data class ShareResultItem(
    @SerialName("group_id") val groupId: String = "",
    val status: String = "",
)

@Serializable
data class ShareData(
    val results: List<ShareResultItem> = emptyList(),
)

// -- Groups --

@Serializable
data class GroupListData(
    val items: List<GroupDto> = emptyList(),
)

@Serializable
data class GroupDto(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
)

// -- Recordings --

@Serializable
data class RecordingListData(
    val items: List<RecordingDto> = emptyList(),
)

@Serializable
data class RecordingDto(
    @SerialName("recording_id") val recordingId: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    val outcome: String? = null,
    val title: String? = null,
    val artist: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
)

@Serializable
data class DeletedCountData(
    val deleted: Int = 0,
)

// -- Resolve --

@Serializable
data class ResolveRequest(
    val url: String,
    @SerialName("group_id") val groupId: String? = null,
)

// -- Report --

@Serializable
data class ReportRequest(
    val reason: String? = null,
)

@Serializable
data class ReportData(
    val reported: Boolean = false,
)

// -- Google Sign-In --

@Serializable
data class GoogleSignInRequest(
    @SerialName("id_token") val idToken: String,
    val nonce: String,
    val name: String? = null,
)

@Serializable
data class GoogleSignInData(
    val token: String,
    @SerialName("is_new_account") val isNewAccount: Boolean,
    @SerialName("listen_enabled") val listenEnabled: Boolean,
)
