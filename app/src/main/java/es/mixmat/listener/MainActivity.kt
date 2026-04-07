package es.mixmat.listener

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import es.mixmat.listener.data.api.AuthEvent
import es.mixmat.listener.data.preferences.ThemePreferences
import es.mixmat.listener.ui.MixMatesListenerApp
import es.mixmat.listener.ui.theme.MixMatesListenerTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authEvent: AuthEvent

    @Inject
    lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by themePreferences.isDarkMode.collectAsStateWithLifecycle(initialValue = true)
            MixMatesListenerTheme(darkTheme = isDarkMode) {
                MixMatesListenerApp(authEvent)
            }
        }
    }
}
