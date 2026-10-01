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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import es.mixmat.listener.R
import es.mixmat.listener.ui.components.GradientButton
import es.mixmat.listener.ui.text.asString

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
    val error = prompt.error?.asString()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.sharesheet_choose_a_name),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.sharesheet_nickname_fine),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(MAX_NAME_LENGTH) },
            label = { Text(stringResource(R.string.sharesheet_your_name)) },
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
            text = stringResource(
                if (prompt.forCreate) R.string.sharesheet_save else R.string.sharesheet_save_and_share,
            ),
            onClick = { onSave(name) },
            enabled = name.isNotBlank(),
            loading = prompt.isSaving,
        )
        TextButton(onClick = onCancel, enabled = !prompt.isSaving) {
            Text(stringResource(R.string.sharesheet_not_now))
        }
    }
}
