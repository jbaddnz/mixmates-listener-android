package es.mixmat.listener.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import es.mixmat.listener.domain.model.Platforms

@Composable
fun TrackCard(
    title: String,
    artist: String,
    thumbnail: String?,
    platforms: Platforms,
    shareUrl: String?,
    status: String?,
    onPlatformClick: (String) -> Unit,
    onShareClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (status == "duplicate") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Already in your queue",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                platforms.spotify?.let { url ->
                    FilledTonalButton(onClick = { onPlatformClick(url) }) {
                        Text("Spotify")
                    }
                }
                platforms.appleMusic?.let { url ->
                    FilledTonalButton(onClick = { onPlatformClick(url) }) {
                        Text("Apple Music")
                    }
                }
                platforms.tidal?.let { url ->
                    FilledTonalButton(onClick = { onPlatformClick(url) }) {
                        Text("Tidal")
                    }
                }
            }

            shareUrl?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onShareClick?.invoke(url) ?: onPlatformClick(url) }) {
                    Text("Share link")
                }
            }
        }
    }
}
