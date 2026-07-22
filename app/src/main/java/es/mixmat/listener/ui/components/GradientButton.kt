package es.mixmat.listener.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

/**
 * The app's primary call-to-action: a green→cyan gradient pill with white text.
 * Falls back to a muted flat fill when disabled. Reserve it for the single most
 * important action on a screen — the share-to-a-persona action.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = if (enabled) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        ),
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = if (enabled) {
                    Brush.horizontalGradient(listOf(Color(0xFF1DB954), Color(0xFF00D4FF)))
                } else {
                    SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                },
                shape = MaterialTheme.shapes.extraLarge,
            ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text)
    }
}
