package es.mixmat.listener.ui.share

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.mixmat.listener.ui.components.TrackCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    viewModel: ShareViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shared link") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isResolving -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Resolving...", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                uiState.error != null && uiState.result == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }

                uiState.result != null -> {
                    val result = uiState.result!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        when (result.status) {
                            "saved", "duplicate" -> {
                                result.track?.let { track ->
                                    TrackCard(
                                        title = track.title,
                                        artist = track.artist,
                                        thumbnail = track.thumbnail,
                                        platforms = track.platforms,
                                        shareUrl = track.shareUrl,
                                        status = result.status,
                                        onPlatformClick = { url ->
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                            )
                                        },
                                        onShareClick = { url ->
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "${track.artist} - ${track.title}\n$url",
                                                )
                                                type = "text/plain"
                                            }
                                            context.startActivity(
                                                Intent.createChooser(sendIntent, null),
                                            )
                                        },
                                    )
                                }
                            }
                            "no_links" -> {
                                Text(
                                    text = "${result.track?.title ?: "Track"} identified but no streaming links available",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://mixmat.es/?listen=1"),
                                    ),
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF1DB954),
                                            Color(0xFF2CCCD3),
                                        ),
                                    ),
                                    shape = MaterialTheme.shapes.extraLarge,
                                ),
                        ) {
                            Text("Open in MixMates")
                        }

                        if (uiState.groups.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Share to groups",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            uiState.groups.forEach { group ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Checkbox(
                                        checked = group.id in uiState.selectedGroupIds,
                                        onCheckedChange = { viewModel.toggleGroup(group.id) },
                                    )
                                    Text(group.name, modifier = Modifier.weight(1f))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = viewModel::share,
                                enabled = !uiState.isSharing && uiState.selectedGroupIds.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (uiState.isSharing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Share")
                            }

                            uiState.shareResult?.let { results ->
                                Spacer(modifier = Modifier.height(8.dp))
                                results.forEach { (groupId, status) ->
                                    val groupName =
                                        uiState.groups.find { it.id == groupId }?.name ?: groupId
                                    val displayStatus = when (status) {
                                        "duplicate" -> "Already in $groupName!"
                                        "shared" -> "Shared to $groupName"
                                        else -> "$groupName: $status"
                                    }
                                    Text(
                                        text = displayStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }

                        uiState.error?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}
