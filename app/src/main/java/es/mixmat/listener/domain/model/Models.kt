package es.mixmat.listener.domain.model

data class Track(
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val shortcode: String?,
    val shareUrl: String?,
    val platforms: Platforms,
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
    val resetAt: String,
)

data class RecognitionResult(
    val status: String,
    val source: String?,
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
