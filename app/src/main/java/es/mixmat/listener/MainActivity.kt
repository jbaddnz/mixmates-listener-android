package es.mixmat.listener

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import es.mixmat.listener.data.api.AuthEvent
import es.mixmat.listener.data.auth.AppleSignInHelper
import es.mixmat.listener.ui.MixMatesListenerApp
import es.mixmat.listener.ui.theme.MixMatesListenerTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authEvent: AuthEvent

    @Inject
    lateinit var appleSignInHelper: AppleSignInHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        publishShareShortcut()
        handleAppleReturn(intent)
        setContent {
            MixMatesListenerTheme {
                MixMatesListenerApp(authEvent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAppleReturn(intent)
    }

    // The Sign in with Apple App Link lands here (onNewIntent while the task
    // is alive, onCreate after process death behind the Custom Tab).
    private fun handleAppleReturn(intent: Intent?) {
        val uri = intent?.data ?: return
        if (appleSignInHelper.isReturnUri(uri)) {
            appleSignInHelper.handleReturnUri(uri)
        }
    }

    private fun publishShareShortcut() {
        val shortcut = ShortcutInfoCompat.Builder(this, "share_resolve")
            .setShortLabel("MixMates")
            .setLongLabel("Resolve in MixMates")
            .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
            .setIntent(
                Intent(Intent.ACTION_SEND).apply {
                    setClass(this@MainActivity, ShareActivity::class.java)
                    type = "text/plain"
                },
            )
            .setCategories(setOf("es.mixmat.listener.category.SHARE"))
            .build()

        ShortcutManagerCompat.addDynamicShortcuts(this, listOf(shortcut))
    }
}
