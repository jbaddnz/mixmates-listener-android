package es.mixmat.listener.ui.legal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Legal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            TextButton(onClick = { openUrl("https://mixmat.es/privacy") }) {
                Text("Privacy Policy")
            }

            TextButton(onClick = { openUrl("https://mixmat.es/terms") }) {
                Text("Terms of Service")
            }

            TextButton(onClick = { openUrl("https://github.com/jbaddnz/mixmates-listener-android") }) {
                Text("Source Code (GitHub)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Our marks line — keep above any third-party credits. Never "upgrade"
            // this with "registered" or ®: these are common-law marks, and falsely
            // representing a mark as registered is an offence under the NZ Trade
            // Marks Act. "A New Zealand company" attaches to MixMat Ltd (which is
            // a registered company), not to the marks.
            Text(
                text = "MixMates, MixMates Listener, mixmat.es, and the mmL mark are " +
                    "trademarks of MixMat Ltd, a New Zealand company.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Third-party credit — stays below our own marks line. Licence
            // text ships in assets/OFL.txt.
            Text(
                text = "Includes the MuseoModerno font, © its authors, " +
                    "used under the SIL Open Font License 1.1.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "MixMates Listener",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "v${context.packageManager.getPackageInfo(context.packageName, 0).versionName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\u00A9 MixMat Ltd",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
