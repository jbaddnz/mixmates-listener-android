package es.mixmat.listener.ui.listen

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.mixmat.listener.audio.RecorderState
import es.mixmat.listener.ui.components.Equalizer
import es.mixmat.listener.ui.components.MixmatesWordmarkLink
import es.mixmat.listener.ui.components.SuccessWave
import es.mixmat.listener.ui.components.TrackCard
import es.mixmat.listener.ui.sharesheet.TrackShareSheet
import es.mixmat.listener.ui.theme.BrandCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ListenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    var showMicDisclosure by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    // Success haptic when a recognition lands (Android's confirm idiom;
    // CONFIRM needs API 30, minSdk is 26).
    val isSuccess = uiState.result?.status in listOf("saved", "duplicate")
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.VIRTUAL_KEY
                },
            )
        }
    }

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
                        // Cyan, not red: this is news, not an error.
                        BadgedBox(
                            badge = {
                                if (uiState.hasUnseenTracks) {
                                    Badge(containerColor = BrandCyan)
                                }
                            },
                        ) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
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
                    Equalizer()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Identifying...", style = MaterialTheme.typography.bodyLarge)
                }

                uiState.result != null -> {
                    val result = uiState.result!!
                    when (result.status) {
                        "saved", "duplicate" -> {
                            SuccessWave()
                            Spacer(modifier = Modifier.height(16.dp))
                            result.track?.let { track ->
                                TrackCard(
                                    title = track.title,
                                    artist = track.artist,
                                    thumbnail = track.thumbnail,
                                    platforms = track.platforms,
                                    status = result.status,
                                    bpm = track.bpm,
                                    musicalKey = track.musicalKey,
                                    keyScale = track.keyScale,
                                    shareUrl = track.shareUrl,
                                    onPlatformClick = { url ->
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                        )
                                    },
                                    // Opens our own sheet, not the system chooser:
                                    // the group picker is the point of the moment.
                                    onShareClick = { showShareSheet = true },
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
                    // Quiet secondary by design — Share on the card is the hero.
                    TextButton(onClick = viewModel::dismiss) {
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
            // Idle state only, like iOS's §7 wordmark — but ours links to the
            // web app, doing the job of the old bottom "Open in MixMates"
            // button it replaced.
            val isIdle = uiState.result == null &&
                !uiState.isSubmitting &&
                uiState.recorderState != RecorderState.RECORDING
            if (isIdle) {
                MixmatesWordmarkLink(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                )
            }
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

    if (showShareSheet) {
        val result = uiState.result
        val track = result?.track
        TrackShareSheet(
            // Only a saved entry can be shared to a group, so a no_match or a
            // no_links result opens the sheet with the system share alone.
            historyId = result?.historyId?.takeIf {
                result.status in listOf("saved", "duplicate")
            },
            shareUrl = track?.shareUrl,
            artist = track?.artist,
            title = track?.title,
            onDismiss = { showShareSheet = false },
        )
    }
}
