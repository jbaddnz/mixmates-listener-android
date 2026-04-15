package es.mixmat.listener.ui.listen

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.mixmat.listener.audio.RecorderState
import es.mixmat.listener.ui.components.TrackCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ListenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMicDisclosure by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listen") },
                actions = {
                    uiState.profile?.rateLimit?.let { rl ->
                        Text(
                            text = "${rl.remaining}/${rl.limit}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            uiState.profile?.let { profile ->
                Text(
                    text = "Hi, ${profile.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            when {
                uiState.isSubmitting -> {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Identifying...", style = MaterialTheme.typography.bodyLarge)
                }

                uiState.result != null -> {
                    val result = uiState.result!!
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
                                            putExtra(Intent.EXTRA_TEXT, "${track.artist} - ${track.title}\n$url")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, null))
                                    },
                                )
                            }
                        }
                        "no_match" -> {
                            Text(
                                text = "No match found",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try again with clearer audio",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        "no_links" -> {
                            Text(
                                text = "${result.track?.title ?: "Track"} identified but no streaming links available",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    if (result.historyId != null && result.status in listOf("saved", "duplicate")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.reported) {
                            Text(
                                text = "Reported — thanks!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        } else {
                            TextButton(
                                onClick = viewModel::reportWrongMatch,
                                enabled = !uiState.isReporting,
                            ) {
                                Text(if (uiState.isReporting) "Reporting…" else "Wrong match?")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://mixmat.es/?listen=1")),
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
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = viewModel::dismiss) {
                        Text("Listen again")
                    }
                }

                uiState.recorderState == RecorderState.RECORDING -> {
                    val animatedProgress by animateFloatAsState(
                        targetValue = uiState.recordingProgress,
                        label = "recording_progress",
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(120.dp),
                        strokeWidth = 8.dp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Listening...", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(onClick = viewModel::stopAndSubmit) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop early")
                    }
                }

                else -> {
                    uiState.error?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.queuedOffline) {
                        Text(
                            text = "Saved offline - will submit when connected",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.permissionDenied) {
                        Text(
                            text = "Microphone access is needed to identify music",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    },
                                )
                            },
                        ) {
                            Text("Open settings")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF1DB954),
                                        Color(0xFF2CCCD3),
                                    ),
                                ),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = {
                                if (uiState.hasAudioPermission) {
                                    viewModel.startListening()
                                } else {
                                    showMicDisclosure = true
                                }
                            },
                            modifier = Modifier.size(96.dp),
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Start listening",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap to listen",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
            Text(
                text = "mixmat.es",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }
    }

    if (showMicDisclosure) {
        AlertDialog(
            onDismissRequest = { showMicDisclosure = false },
            title = { Text("Microphone access") },
            text = {
                Text(
                    "MixMates Listener uses your microphone to capture a short audio clip " +
                        "of music playing nearby. The clip is sent to a recognition service to " +
                        "identify the song, then discarded. No audio is stored permanently.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMicDisclosure = false
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicDisclosure = false }) {
                    Text("Not now")
                }
            },
        )
    }
}
