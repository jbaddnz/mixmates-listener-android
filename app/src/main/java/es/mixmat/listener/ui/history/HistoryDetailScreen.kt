package es.mixmat.listener.ui.history

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.mixmat.listener.ui.components.TrackCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    historyId: String,
    onBack: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                        shareUrl = detail.shareUrl,
                        status = null,
                        onPlatformClick = { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        onShareClick = { url ->
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, "${detail.artist} - ${detail.title}\n$url")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, null))
                        },
                    )

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
                                val groupName = uiState.groups.find { it.id == groupId }?.name ?: groupId
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
                }
            }
        }
    }
}
