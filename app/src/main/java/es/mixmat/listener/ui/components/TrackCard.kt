package es.mixmat.listener.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import es.mixmat.listener.domain.model.Platforms
import es.mixmat.listener.ui.theme.AppleMusicRed
import es.mixmat.listener.ui.theme.MixMatesListenerTheme
import es.mixmat.listener.ui.theme.SpotifyGreen
import es.mixmat.listener.ui.theme.TidalCyan

// The track card uses a fixed dark surface in BOTH light and dark themes so the
// brand-coloured platform buttons (Spotify green, Apple red, Tidal cyan) stay
// legible everywhere — without altering the brand colours themselves.
private val TrackCardSurface = Color(0xFF26242E)
private val TrackCardTitle = Color(0xFFF4F2F7)
private val TrackCardArtist = Color(0xFFB8B4C4)
private val TrackCardMeta = Color(0xFFCBBEF2)
private val TrackCardPositive = Color(0xFF7FE0A4)

@Composable
fun TrackCard(
    title: String,
    artist: String,
    thumbnail: String?,
    platforms: Platforms,
    status: String?,
    onPlatformClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    bpm: Double? = null,
    musicalKey: String? = null,
    keyScale: String? = null,
    shareUrl: String? = null,
    onShareClick: ((String) -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TrackCardSurface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (thumbnail != null) {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = "$title album art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialTheme.shapes.small),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TrackCardTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TrackCardArtist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    trackMetaLabel(bpm, musicalKey, keyScale)?.let { meta ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelMedium,
                            color = TrackCardMeta,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(TrackCardMeta.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            if (status == "duplicate") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Already in your queue!",
                    style = MaterialTheme.typography.labelSmall,
                    color = TrackCardPositive,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                platforms.spotify?.let { url ->
                    PlatformButton(
                        label = "Spotify",
                        color = SpotifyGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onPlatformClick(url) },
                    )
                }
                platforms.appleMusic?.let { url ->
                    PlatformButton(
                        label = "Apple Music",
                        color = AppleMusicRed,
                        modifier = Modifier.weight(1f),
                        onClick = { onPlatformClick(url) },
                    )
                }
                platforms.tidal?.let { url ->
                    PlatformButton(
                        label = "Tidal",
                        color = TidalCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onPlatformClick(url) },
                    )
                }
            }

            // Opt-in system-share (share sheet to any app). Passed only where there's
            // no persona-share below it — e.g. the Listen result card.
            // Brand gradient per the platform-branding spec (green→cyan, white text) —
            // reuses the "Open in MixMates" gradient vocabulary. Opt-in: only passed
            // where there's no persona-share below, e.g. the Listen result card.
            shareUrl?.let { url ->
                Spacer(modifier = Modifier.height(12.dp))
                GradientButton(
                    text = "Share link",
                    onClick = { onShareClick?.invoke(url) ?: onPlatformClick(url) },
                )
            }
        }
    }
}

/** Outlined pill tinted with a platform's brand colour, sized to fill its slot. */
@Composable
private fun PlatformButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = BorderStroke(2.dp, color.copy(alpha = 0.7f)),
    ) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackCardPreview() {
    MixMatesListenerTheme {
        TrackCard(
            title = "Evergreen",
            artist = "Richy Mitch & The Coal Miners",
            thumbnail = null,
            platforms = Platforms(
                spotify = "https://open.spotify.com/track/123",
                tidal = "https://tidal.com/track/456",
                appleMusic = null,
            ),
            status = null,
            onPlatformClick = {},
            bpm = 105.0,
            musicalKey = "FSharp",
            keyScale = "MINOR",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackCardDuplicatePreview() {
    MixMatesListenerTheme {
        TrackCard(
            title = "Evergreen",
            artist = "Richy Mitch & The Coal Miners",
            thumbnail = null,
            platforms = Platforms(spotify = "https://open.spotify.com/track/123", tidal = null, appleMusic = null),
            status = "duplicate",
            onPlatformClick = {},
        )
    }
}
