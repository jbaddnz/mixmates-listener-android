package es.mixmat.listener.ui.history

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.mixmat.listener.ui.components.GradientButton
import es.mixmat.listener.ui.components.OpenInMixMatesButton
import es.mixmat.listener.ui.components.TrackCard
import es.mixmat.listener.ui.sharesheet.TrackShareSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    historyId: String,
    onBack: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showShareSheet by remember { mutableStateOf(false) }

    LaunchedEffect(historyId) {
        viewModel.load(historyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            uiState.error != null && uiState.detail == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            uiState.detail != null -> {
                val detail = uiState.detail!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    TrackCard(
                        title = detail.title,
                        artist = detail.artist,
                        thumbnail = detail.thumbnail,
                        platforms = detail.platforms,
                        status = null,
                        bpm = detail.bpm,
                        musicalKey = detail.musicalKey,
                        keyScale = detail.keyScale,
                        onPlatformClick = { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                    )

                    if (detail.sharedTo.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Shared to",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        detail.sharedTo.forEach { group ->
                            Text(
                                text = group.groupName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }

                    // One picker for the whole app, in the sheet. The inline copy
                    // that used to live here has gone; it had already started to
                    // drift from the one on the result screen.
                    Spacer(modifier = Modifier.height(24.dp))
                    GradientButton(
                        text = "Share",
                        onClick = { showShareSheet = true },
                    )

                    uiState.error?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    OpenInMixMatesButton(url = "https://mixmat.es/?listen=1")
                }
            }
        }
    }

    if (showShareSheet) {
        val detail = uiState.detail
        TrackShareSheet(
            historyId = detail?.id,
            shareUrl = detail?.shareUrl,
            artist = detail?.artist,
            title = detail?.title,
            onDismiss = { showShareSheet = false },
            // Already-shared groups start ticked; the sheet intersects them
            // against what the endpoint actually returns.
            preselecting = detail?.sharedTo?.map { it.groupId }?.toSet() ?: emptySet(),
        )
    }
}
