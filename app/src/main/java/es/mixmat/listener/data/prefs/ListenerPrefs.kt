package es.mixmat.listener.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plain (unencrypted) preferences for non-credential state. The Listen Key
 * stays in the encrypted store; these are hints and settings, deliberately
 * kept out of it.
 */
@Singleton
class ListenerPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("listener_prefs", Context.MODE_PRIVATE)

    /** Provider of the last successful sign-in, e.g. [METHOD_GOOGLE]. */
    fun getLastSignInMethod(): String? = prefs.getString(KEY_LAST_SIGN_IN_METHOD, null)

    fun setLastSignInMethod(method: String) {
        prefs.edit().putString(KEY_LAST_SIGN_IN_METHOD, method).apply()
    }

    // Called only on account deletion — the hint must survive sign-out and
    // 401/expiry so the sign-in screen can say which method was used last.
    fun clearLastSignInMethod() {
        prefs.edit().remove(KEY_LAST_SIGN_IN_METHOD).apply()
    }

    /** Clamped on read so an out-of-range stored value can never leak out. */
    fun getRecordingLengthSeconds(): Int =
        prefs.getInt(KEY_RECORDING_LENGTH_SECONDS, RECORDING_LENGTH_DEFAULT_SECONDS)
            .coerceIn(RECORDING_LENGTH_MIN_SECONDS, RECORDING_LENGTH_MAX_SECONDS)

    fun setRecordingLengthSeconds(seconds: Int) {
        prefs.edit()
            .putInt(
                KEY_RECORDING_LENGTH_SECONDS,
                seconds.coerceIn(RECORDING_LENGTH_MIN_SECONDS, RECORDING_LENGTH_MAX_SECONDS),
            )
            .apply()
    }

    companion object {
        const val METHOD_GOOGLE = "google"
        const val METHOD_APPLE = "apple"
        const val METHOD_LISTEN_KEY = "listen_key"

        const val RECORDING_LENGTH_MIN_SECONDS = 6
        const val RECORDING_LENGTH_MAX_SECONDS = 12
        const val RECORDING_LENGTH_DEFAULT_SECONDS = 10

        private const val KEY_LAST_SIGN_IN_METHOD = "last_sign_in_method"
        private const val KEY_RECORDING_LENGTH_SECONDS = "recording_length_seconds"
    }
}
