package es.mixmat.listener.ui.sharesheet

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.mixmat.listener.domain.model.Group
import es.mixmat.listener.ui.components.GradientButton

/**
 * The app's own share sheet: groups first, then a way out to the system share.
 *
 * Recognition is the moment of peak motivation, and sharing into a MixMates group
 * is the point of the product — so the group picker is what the Share hero opens,
 * and the public link costs one extra tap rather than the other way round.
 *
 * Section visibility is conditional, so a result with no saved row shows the
 * system share alone and a result with no public link shows groups alone:
 * @param historyId non-null only when there is a saved entry to share; gates the groups section.
 * @param shareUrl non-null only when there is a public link; gates the system-share row.
 * @param preselecting groups the track is already in, intersected against what the endpoint returns.
 *
 * No close button by design — Android sheets dismiss by drag handle, scrim tap or
 * back. iOS needs an explicit one because it has none of those.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackShareSheet(
    historyId: String?,
    shareUrl: String?,
    artist: String?,
    title: String?,
    onDismiss: () -> Unit,
    preselecting: Set<String> = emptySet(),
    viewModel: TrackShareViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    // Runs on every open, because the sheet leaves composition when it closes.
    // That is what returns a reopened sheet to the picker instead of the result
    // it was last showing.
    LaunchedEffect(historyId) {
        if (historyId != null) viewModel.load(preselecting)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Share",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))

            val namePrompt = uiState.namePrompt
            if (historyId != null && namePrompt != null) {
                ShareSheetNamePrompt(
                    prompt = namePrompt,
                    onSave = viewModel::submitName,
                    onCancel = viewModel::cancelName,
                )
            } else if (historyId != null) {
                GroupsSection(
                    uiState = uiState,
                    onToggleGroup = viewModel::toggleGroup,
                    onShare = { viewModel.share(historyId) },
                    onRetry = viewModel::retry,
                    onShareSomewhereElse = viewModel::backToPicker,
                    onStartGroup = viewModel::openStartGroup,
                    onCreateGroup = viewModel::createGroup,
                    onCloseStartGroup = viewModel::closeStartGroup,
                )
            }

            // Only when both halves are present — otherwise it would be a rule
            // separating a section from nothing.
            if (historyId != null && shareUrl != null) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
            }

            if (shareUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                SystemShareRow(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, shareText(artist, title, shareUrl))
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    },
                )
            }
        }
    }
}

/**
 * Four mutually exclusive states. Empty and failed are deliberately different
 * things — see [GroupsState]. Start a group only ever appears from Loaded with
 * `canCreate`, never from Loading or Failed.
 */
@Composable
private fun GroupsSection(
    uiState: TrackShareUiState,
    onToggleGroup: (String) -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onShareSomewhereElse: () -> Unit,
    onStartGroup: () -> Unit,
    onCreateGroup: (String) -> Unit,
    onCloseStartGroup: () -> Unit,
) {
    when (val groups = uiState.groups) {
        GroupsState.Loading -> Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        // Replaces this section only, so the system-share row below still works
        // through a groups outage.
        is GroupsState.Failed -> Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = groups.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (groups.retryable) {
                TextButton(onClick = onRetry) { Text("Try again") }
            }
        }

        is GroupsState.Loaded -> when (val start = uiState.startGroup) {
            is StartGroupState.Naming -> ShareSheetStartGroupNaming(
                state = start,
                onCreate = onCreateGroup,
                onCancel = onCloseStartGroup,
            )
            is StartGroupState.Created -> ShareSheetGroupCreated(
                group = start.group,
                onShareToIt = onCloseStartGroup,
            )
            // The refusal sits above whatever the refreshed list now shows.
            is StartGroupState.Refused -> Column(modifier = Modifier.fillMaxWidth()) {
                ShareSheetStartGroupRefused(message = start.message)
                LoadedGroups(uiState, groups, onToggleGroup, onShare, onShareSomewhereElse, onStartGroup)
            }
            StartGroupState.Closed ->
                LoadedGroups(uiState, groups, onToggleGroup, onShare, onShareSomewhereElse, onStartGroup)
        }
    }
}

@Composable
private fun LoadedGroups(
    uiState: TrackShareUiState,
    groups: GroupsState.Loaded,
    onToggleGroup: (String) -> Unit,
    onShare: () -> Unit,
    onShareSomewhereElse: () -> Unit,
    onStartGroup: () -> Unit,
) {
    when {
        groups.groups.isEmpty() && groups.canCreate -> ShareSheetStartGroupPrompt(onStart = onStartGroup)
        groups.groups.isEmpty() -> ShareSheetEmptyState()
        uiState.shareResult != null -> ShareSheetPostShareState(
            results = uiState.shareResult,
            groups = groups.groups,
            onShareSomewhereElse = onShareSomewhereElse,
        )
        else -> Column(modifier = Modifier.fillMaxWidth()) {
            GroupPicker(
                groups = groups.groups,
                selectedGroupIds = uiState.selectedGroupIds,
                isSharing = uiState.isSharing,
                shareError = uiState.shareError,
                onToggleGroup = onToggleGroup,
                onShare = onShare,
            )
            if (groups.canCreate) {
                Spacer(modifier = Modifier.height(8.dp))
                ShareSheetStartGroupRow(onStart = onStartGroup)
            }
        }
    }
}

@Composable
private fun GroupPicker(
    groups: List<Group>,
    selectedGroupIds: Set<String>,
    isSharing: Boolean,
    shareError: String?,
    onToggleGroup: (String) -> Unit,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Share to groups",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // No cap, no search, no select-all: the multi-select is the point, and the
        // list just scrolls inside the sheet.
        groups.forEach { group ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Toggling is this inner row only, so Invite is its own target
                // and never ticks or unticks the group by accident.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleGroup(group.id) },
                ) {
                    Checkbox(
                        checked = group.id in selectedGroupIds,
                        onCheckedChange = { onToggleGroup(group.id) },
                    )
                    Text(group.name, modifier = Modifier.weight(1f))
                }
                // Hidden when null, which the demo group always is.
                group.inviteUrl?.let { url ->
                    IconButton(onClick = { context.shareInvite(GroupFlowCopy.INVITE_MESSAGE, url) }) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = GroupFlowCopy.INVITE_A_FRIEND,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        GradientButton(
            text = "Share",
            onClick = onShare,
            enabled = selectedGroupIds.isNotEmpty(),
            loading = isSharing,
        )

        shareError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SystemShareRow(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            // One horizontal ellipsis, not three full stops.
            text = "More ways to share…",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/**
 * "Artist - Title" then the link, built here so all three hosting screens share
 * one format rather than each rolling its own.
 */
private fun shareText(artist: String?, title: String?, url: String): String =
    if (artist != null && title != null) "$artist - $title\n$url" else url
