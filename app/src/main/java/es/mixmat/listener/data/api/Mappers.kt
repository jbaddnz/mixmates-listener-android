package es.mixmat.listener.data.api

import es.mixmat.listener.data.api.dto.*
import es.mixmat.listener.domain.model.*

fun TrackDto.toDomain() = Track(
    title = title,
    artist = artist,
    thumbnail = thumbnail,
    shortcode = shortcode,
    shareUrl = shareUrl,
    platforms = platforms.toDomain(),
)

fun PlatformsDto.toDomain() = Platforms(
    spotify = spotify,
    tidal = tidal,
    appleMusic = appleMusic,
)

fun HistoryItemDto.toDomain() = HistoryItem(
    id = id,
    title = title,
    artist = artist,
    thumbnail = thumbnail,
    shortcode = shortcode,
    shareUrl = shareUrl,
    platforms = platforms.toDomain(),
    createdAt = createdAt,
)

fun HistoryDetailData.toDomain() = HistoryDetail(
    id = id,
    title = title,
    artist = artist,
    thumbnail = thumbnail,
    shortcode = shortcode,
    shareUrl = shareUrl,
    platforms = platforms.toDomain(),
    createdAt = createdAt,
    sharedTo = sharedTo.map { it.toDomain() },
)

fun SharedGroupDto.toDomain() = SharedGroup(
    groupId = groupId,
    groupName = groupName,
)

fun GroupDto.toDomain() = Group(
    id = id,
    name = name,
    description = description,
)

fun RecognizeData.toDomain() = RecognitionResult(
    status = status,
    source = source,
    historyId = historyId,
    track = track?.toDomain(),
)

fun UserData.toDomain() = UserProfile(
    id = user.id,
    displayName = user.displayName,
    role = user.role,
    listenEnabled = user.listenEnabled,
    preferredPlatform = user.preferredPlatform,
    rateLimit = rateLimit?.let {
        RateLimit(
            limit = it.limit,
            remaining = it.remaining,
            resetAt = it.resetAt,
        )
    },
)

fun RecordingDto.toDomain() = Recording(
    recordingId = recordingId,
    createdAt = createdAt,
    outcome = outcome,
    title = title,
    artist = artist,
    mimeType = mimeType,
)
