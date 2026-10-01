package es.mixmat.listener.domain.model

data class Track(
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val shortcode: String?,
    val shareUrl: String?,
    val platforms: Platforms,
    val bpm: Double?,
    val musicalKey: String?,
    val keyScale: String?,
)

data class Platforms(
    val spotify: String?,
    val tidal: String?,
    val appleMusic: String?,
)

data class HistoryItem(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val shortcode: String?,
    val shareUrl: String?,
    val platforms: Platforms,
    val createdAt: String,
    val bpm: Double?,
    val musicalKey: String?,
    val keyScale: String?,
)

data class HistoryDetail(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val shortcode: String?,
    val shareUrl: String?,
    val platforms: Platforms,
    val createdAt: String,
    val bpm: Double?,
    val musicalKey: String?,
    val keyScale: String?,
    val sharedTo: List<SharedGroup>,
)

data class SharedGroup(
    val groupId: String,
    val groupName: String,
)

data class Group(
    val id: String,
    val name: String,
    val description: String?,
    /** The link that admits a friend. Null on the demo group, so no Invite there. */
    val inviteUrl: String? = null,
)

/**
 * One `GET /groups` response. [canCreate] is the server's word on whether this
 * account may start a group, and only ever comes from a fresh fetch.
 */
data class GroupList(
    val groups: List<Group>,
    val canCreate: Boolean,
)

data class UserProfile(
    val id: String,
    val displayName: String,
    val role: String,
    val listenEnabled: Boolean,
    val preferredPlatform: String?,
    val rateLimit: RateLimit?,
)

data class RateLimit(
    val limit: Int,
    val remaining: Int,
    val resetAt: Long,
)

data class RecognitionResult(
    val status: String,
    val source: String?,
    val historyId: String?,
    val track: Track?,
)

data class Recording(
    val recordingId: String,
    val createdAt: String?,
    val outcome: String?,
    val title: String?,
    val artist: String?,
    val mimeType: String?,
)
