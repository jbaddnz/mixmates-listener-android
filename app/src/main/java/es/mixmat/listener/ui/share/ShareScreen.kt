package es.mixmat.listener.ui.share

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.mixmat.listener.ui.components.GradientButton
import es.mixmat.listener.ui.components.OpenInMixMatesButton
import es.mixmat.listener.ui.components.TrackCard
import es.mixmat.listener.ui.sharesheet.TrackShareSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    viewModel: ShareViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showShareSheet by remember { mutableStateOf(false) }

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
                                        status = result.status,
                                        bpm = track.bpm,
                                        musicalKey = track.musicalKey,
                                        keyScale = track.keyScale,
                                        onPlatformClick = { url ->
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url)),
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

                        // Same sheet as the result screen and history detail. The
                        // inline picker that was here hid itself entirely when the
                        // fetch came back empty — and also when it failed, which
                        // is not the same thing.
                        if (uiState.result?.historyId != null) {
                            Spacer(modifier = Modifier.height(24.dp))
                            GradientButton(
                                text = "Share",
                                onClick = { showShareSheet = true },
                            )
                        }

                        uiState.error?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        OpenInMixMatesButton(url = "https://mixmat.es/?listen=1")

                        Spacer(modifier = Modifier.height(12.dp))
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

    if (showShareSheet) {
        val track = uiState.result?.track
        TrackShareSheet(
            historyId = uiState.result?.historyId,
            shareUrl = track?.shareUrl,
            artist = track?.artist,
            title = track?.title,
            onDismiss = { showShareSheet = false },
        )
    }
}
