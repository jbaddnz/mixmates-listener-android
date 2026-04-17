package es.mixmat.listener

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import es.mixmat.listener.ui.share.ShareScreen
import es.mixmat.listener.ui.share.ShareViewModel
import es.mixmat.listener.ui.theme.MixMatesListenerTheme

@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)

        enableEdgeToEdge()
        setContent {
            MixMatesListenerTheme {
                val viewModel: ShareViewModel = hiltViewModel()

                LaunchedEffect(Unit) {
                    if (sharedText != null) {
                        viewModel.resolve(sharedText)
                    } else {
                        viewModel.resolve("")
                    }
                }

                ShareScreen(
                    viewModel = viewModel,
                    onDismiss = { finish() },
                )
            }
        }
    }
}
