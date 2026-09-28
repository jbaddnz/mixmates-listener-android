package es.mixmat.listener.ui.sharesheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shown when the groups fetch succeeded and came back empty — which is not the
 * same as the fetch failing, and must not look the same.
 *
 * Its own file because group creation replaces it wholesale: the title becomes
 * "Start a group" and this gains a button that creates one and hands over an
 * invite link. Keeping it out of the picker means that change touches this file
 * and nothing else.
 */
@Composable
fun ShareSheetEmptyState(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "No groups yet",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Once you're in a group, your finds can go straight to it from here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
