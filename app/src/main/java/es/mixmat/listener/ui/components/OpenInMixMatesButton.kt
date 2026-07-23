package es.mixmat.listener.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Outlined "Open in MixMates" call-to-action with a leading open-in-new icon.
 * Opens [url] (the MixMates web app) in the browser. Reused on Track Details,
 * the shared-link screen and the Listen screen so the CTA stays identical.
 */
@Composable
fun OpenInMixMatesButton(
    modifier: Modifier = Modifier,
    url: String = "https://mixmat.es",
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Open in MixMates")
    }
}
