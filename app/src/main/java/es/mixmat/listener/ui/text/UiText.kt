package es.mixmat.listener.ui.text

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Copy a view model hands to a screen, without the view model knowing the
 * language. The screen resolves it, so the phone's language applies and the
 * view model stays a plain JVM class its tests can run without Android.
 *
 * Tests compare these by value: `UiText(R.string.x)` equals itself, so an
 * assertion pins which sentence was chosen rather than its English wording.
 */
data class UiText(
    @StringRes val id: Int,
    /** A list, not varargs, so two equal messages compare equal. */
    val args: List<Any> = emptyList(),
) {
    constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList())

    fun asString(context: Context): String = context.getString(id, *args.toTypedArray())
}

@Composable
fun UiText.asString(): String = stringResource(id, *args.toTypedArray())
