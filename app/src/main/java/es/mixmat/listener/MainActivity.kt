package es.mixmat.listener

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import es.mixmat.listener.data.api.AuthEvent
import es.mixmat.listener.ui.MixMatesListenerApp
import es.mixmat.listener.ui.theme.MixMatesListenerTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authEvent: AuthEvent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MixMatesListenerTheme {
                MixMatesListenerApp(authEvent)
            }
        }
    }
}
