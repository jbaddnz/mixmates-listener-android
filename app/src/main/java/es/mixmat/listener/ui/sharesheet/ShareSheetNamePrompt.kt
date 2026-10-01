package es.mixmat.listener.ui.sharesheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import es.mixmat.listener.ui.components.GradientButton

/**
 * Shown after `name_required`, in place of the groups section rather than as a
 * sheet on a sheet. Saving retries the refused share or create once, so the
 * user never starts again and is never sent anywhere else to do this.
 */
@Composable
fun ShareSheetNamePrompt(
    prompt: NamePrompt,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    val canSubmit = name.isNotBlank() && !prompt.isSaving
    val error = prompt.error

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = GroupFlowCopy.CHOOSE_A_NAME,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = GroupFlowCopy.NICKNAME_FINE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(MAX_NAME_LENGTH) },
            label = { Text(GroupFlowCopy.YOUR_NAME) },
            singleLine = true,
            isError = error != null,
            supportingText = if (error != null) {
                { Text(error) }
            } else {
                null
            },
            enabled = !prompt.isSaving,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSave(name) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        GradientButton(
            text = if (prompt.forCreate) GroupFlowCopy.SAVE else GroupFlowCopy.SAVE_AND_SHARE,
            onClick = { onSave(name) },
            enabled = name.isNotBlank(),
            loading = prompt.isSaving,
        )
        TextButton(onClick = onCancel, enabled = !prompt.isSaving) {
            Text(GroupFlowCopy.NOT_NOW)
        }
    }
}
