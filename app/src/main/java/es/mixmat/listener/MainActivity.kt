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
import es.mixmat.listener.ui.MixMatesListenerApp
import es.mixmat.listener.ui.theme.MixMatesListenerTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authEvent: AuthEvent

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        publishShareShortcut()
        setContent {
            MixMatesListenerTheme {
                MixMatesListenerApp(authEvent)
            }
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
