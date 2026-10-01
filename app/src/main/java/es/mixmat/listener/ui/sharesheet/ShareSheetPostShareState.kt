package es.mixmat.listener.ui.sharesheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.mixmat.listener.R
import es.mixmat.listener.domain.model.Group

/**
 * Replaces the picker after a successful share — it does not append below it.
 *
 * No timer and no auto-dismiss: a timed revert would race someone still reading
 * a three-line result, and the sheet is modal so nothing is hidden behind it.
 *
 * Its own file so a notifications ask has somewhere to sit if push ever arrives.
 * Invite lives on the picker rows, not here.
 */
@Composable
fun ShareSheetPostShareState(
    results: Map<String, String>,
    groups: List<Group>,
    onShareSomewhereElse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        results.forEach { (groupId, status) ->
            // Falls back to the raw id when no name resolves. Reachable when the
            // user is still a member of a group the endpoint has stopped listing,
            // so it stays visible rather than being hidden behind something vague.
            val groupName = groups.find { it.id == groupId }?.name ?: groupId
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = when (status) {
                        "shared" -> stringResource(R.string.sharesheet_shared_to, groupName)
                        "duplicate" -> stringResource(R.string.sharesheet_already_in, groupName)
                        // Unreachable: the server's status is a closed set of the
                        // two above. Kept as the branch the `when` needs.
                        else -> "$groupName: $status"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = onShareSomewhereElse) {
            Text(stringResource(R.string.sharesheet_share_somewhere_else))
        }
    }
}
