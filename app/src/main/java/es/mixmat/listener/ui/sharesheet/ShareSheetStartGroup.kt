package es.mixmat.listener.ui.sharesheet

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import es.mixmat.listener.domain.model.Group
import es.mixmat.listener.ui.components.GradientButton

/** The server's limit, after trimming. Capped at input so `invalid_field` never fires. */
internal const val MAX_NAME_LENGTH = 100

/**
 * Replaces [ShareSheetEmptyState] when the account may start a group: with no
 * groups, starting one is the only thing this section can offer.
 */
@Composable
fun ShareSheetStartGroupPrompt(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = GroupFlowCopy.START_A_GROUP,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = GroupFlowCopy.START_HINT,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        GradientButton(text = GroupFlowCopy.START_A_GROUP, onClick = onStart)
    }
}

/**
 * Sits above a non-empty picker, first thing in the sheet. Most accounts have the
 * demo group or a friend's, so this is the usual way in. Gradient, Jamie's call:
 * starting a group is the point of the product, so it gets the brand treatment.
 */
@Composable
fun ShareSheetStartGroupButton(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GradientButton(
        text = GroupFlowCopy.START_A_GROUP,
        onClick = onStart,
        modifier = modifier,
        enabled = enabled,
    )
}

/** One field, then `POST /groups`. Errors stay here so the name can be edited. */
@Composable
fun ShareSheetStartGroupNaming(
    state: StartGroupState.Naming,
    onCreate: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf(state.draft) }
    val canSubmit = name.isNotBlank() && !state.isCreating
    val error = state.error

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = GroupFlowCopy.START_A_GROUP,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(MAX_NAME_LENGTH) },
            label = { Text(GroupFlowCopy.NAME_YOUR_GROUP) },
            singleLine = true,
            isError = error != null,
            supportingText = if (error != null) {
                { Text(error) }
            } else {
                null
            },
            enabled = !state.isCreating,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (canSubmit) onCreate(name) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        GradientButton(
            text = GroupFlowCopy.CREATE,
            onClick = { onCreate(name) },
            enabled = name.isNotBlank(),
            loading = state.isCreating,
        )
        TextButton(onClick = onCancel, enabled = !state.isCreating) {
            Text(GroupFlowCopy.CANCEL)
        }
    }
}

/**
 * After a create. Invite is the next tap rather than a share sheet that opens on
 * its own, for parity with iOS, whose ShareLink cannot be opened from code.
 */
@Composable
fun ShareSheetGroupCreated(
    group: Group,
    onShareToIt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = GroupFlowCopy.GROUP_READY,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = group.name,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(12.dp))
        group.inviteUrl?.let { url ->
            GradientButton(
                text = GroupFlowCopy.INVITE_A_FRIEND,
                onClick = { context.shareInvite(GroupFlowCopy.INVITE_MESSAGE_NEW, url) },
            )
        }
        TextButton(onClick = onShareToIt) {
            Text(GroupFlowCopy.SHARE_TO_IT)
        }
    }
}

/** `already_has_group`: the copy, nothing to tap, no link anywhere. */
@Composable
fun ShareSheetStartGroupRefused(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(bottom = 12.dp),
    )
}

/** The system share sheet with an invite link: the only URL in this flow, and the user sends it. */
internal fun Context.shareInvite(message: String, url: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, "$message $url")
        type = "text/plain"
    }
    startActivity(Intent.createChooser(sendIntent, null))
}
