package es.mixmat.listener.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The five bar colours ARE the brand green→cyan gradient — the signature for a
// song's journey between people. Colours and order are exact per the shared
// equalizer spec (web, iOS and Android render the same wave); do not reorder.
private val BarColors = listOf(
    Color(0xFF1DB954),
    Color(0xFF1ED760),
    Color(0xFF33CC99),
    Color(0xFF00D4FF),
    Color(0xFF00B4D8),
)

// Web-scale geometry ×1.5, ratios preserved (bar : gap : max height = 4 : 3 : 28,
// minimum 6/28 of maximum, fully rounded ends). Time is absolute, never scaled.
private val BarWidth = 6.dp
private val BarGap = 4.5.dp
private val BarMaxHeight = 42.dp
private val BarMinHeight = 9.dp
private val ContainerHeight = 48.dp

// The static wave (10/18/28/18/10 at web scale): the reduced-motion fallback
// and the shape the success wave settles into.
private val StaticHeights = listOf(15.dp, 27.dp, 42.dp, 27.dp, 15.dp)

/**
 * The brand equalizer: plays only during the identifying window — after
 * capture ends, before the result arrives. A working indicator, not a success
 * state. Decorative for accessibility; the "Identifying…" text nearby carries
 * the meaning.
 */
@Composable
fun Equalizer(modifier: Modifier = Modifier) {
    if (rememberReducedMotion()) {
        BarRow(heights = StaticHeights, modifier = modifier)
        return
    }

    val transition = rememberInfiniteTransition(label = "equalizer")
    val heights = List(BarColors.size) { index ->
        // 6 → 28 → 6 over 1.0 s, ease-in-out each way, 0.15 s stagger per bar —
        // the stagger is what makes the wave roll.
        transition.animateFloat(
            initialValue = BarMinHeight.value,
            targetValue = BarMaxHeight.value,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 150),
            ),
            label = "equalizer_bar_$index",
        )
    }
    BarRow(heights = heights.map { it.value.dp }, modifier = modifier)
}

/**
 * Recognition-success celebration: the five gradient bars pulse briefly
 * (~1.2 s) and settle into the static wave. Static from the start under the
 * system's reduced-motion setting. Decorative for accessibility.
 */
@Composable
fun SuccessWave(modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val bars = remember(reducedMotion) {
        StaticHeights.map { static ->
            Animatable(if (reducedMotion) static.value else BarMinHeight.value)
        }
    }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) return@LaunchedEffect
        bars.forEachIndexed { index, bar ->
            launch {
                delay(index * 100L)
                bar.animateTo(BarMaxHeight.value, tween(250, easing = EaseInOut))
                bar.animateTo(BarMinHeight.value, tween(250, easing = EaseInOut))
                bar.animateTo(StaticHeights[index].value, tween(300, easing = EaseInOut))
            }
        }
    }

    BarRow(heights = bars.map { it.value.dp }, modifier = modifier)
}

@Composable
private fun BarRow(heights: List<Dp>, modifier: Modifier = Modifier) {
    Row(
        // Decorative: no semantics — the accompanying text carries the meaning.
        modifier = modifier
            .height(ContainerHeight)
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(BarGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(BarWidth)
                    .height(height)
                    .background(BarColors[index], RoundedCornerShape(BarWidth / 2)),
            )
        }
    }
}

@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    // Animator scale 0 is what the system's "Remove animations" setting writes.
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
