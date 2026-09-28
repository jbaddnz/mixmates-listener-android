package es.mixmat.listener.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import es.mixmat.listener.R
import es.mixmat.listener.ui.theme.BrandCyan
import es.mixmat.listener.ui.theme.BrandGreen

// Rendered live from the bundled MuseoModerno variable font (OFL, licence in
// assets/OFL.txt) rather than a baked image — same recipe as the wordmark
// package: weight 700, brand gradient #1DB954 → #2CCCD3 through the glyphs.
@OptIn(ExperimentalTextApi::class)
private val MuseoModerno = FontFamily(
    Font(
        R.font.museomoderno_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

/**
 * The mixmat.es wordmark. Unlike iOS's (deliberately inert for App Review reasons
 * Android doesn't have), this one is a link to the MixMates web app — on the idle
 * Listen state it replaces the old bottom "Open in MixMates" button.
 *
 * @param fontSize hero size on the idle screen; pass something smaller where it
 * sits in a footer or the end of a list.
 */
@Composable
fun MixmatesWordmarkLink(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
) {
    val context = LocalContext.current
    Text(
        text = "mixmat.es",
        fontFamily = MuseoModerno,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        style = TextStyle(
            brush = Brush.horizontalGradient(listOf(BrandGreen, BrandCyan)),
        ),
        modifier = modifier.clickable(role = Role.Button) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://mixmat.es")),
            )
        },
    )
}
