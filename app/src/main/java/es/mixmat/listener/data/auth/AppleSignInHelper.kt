package es.mixmat.listener.data.auth

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import es.mixmat.listener.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLDecoder
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of an Apple web sign-in round trip. [idToken] and [nonce] are set on
 * success; [error] carries Apple's error code (or [ERROR_STATE_MISMATCH]) when
 * the attempt failed. [name] is only present on the very first authorization
 * for this Apple ID — capture it then or it's gone.
 */
data class AppleSignInReturn(
    val idToken: String? = null,
    val nonce: String? = null,
    val name: String? = null,
    val error: String? = null,
) {
    val isCancelled: Boolean get() = error == "user_cancelled_authorize"
}

/**
 * Sign in with Apple on Android is inherently a web OAuth flow (Apple ships no
 * Android SDK): a Custom Tab to appleid.apple.com — never a handoff into the
 * mixmat.es web app or its sign-in UI. Apple form-POSTs the response to the
 * server's stateless return endpoint, which bounces it into the app via the
 * claimed App Link with the parameters in the URL fragment.
 */
@Singleton
class AppleSignInHelper @Inject constructor(
    @ApplicationContext context: Context,
) {
    // Plain prefs: nonce and state are public flow parameters, not credentials.
    // Persisted (not held in memory) so the flow survives process death while
    // the Custom Tab is in the foreground.
    private val prefs = context.getSharedPreferences("apple_signin", Context.MODE_PRIVATE)

    private val _returns = MutableStateFlow<AppleSignInReturn?>(null)

    /** Completed round trips; consume with [consumeReturn]. */
    val returns: StateFlow<AppleSignInReturn?> = _returns

    fun isConfigured(): Boolean = BuildConfig.APPLE_SERVICES_ID.isNotBlank()

    /**
     * Starts a sign-in attempt in a Custom Tab. A fresh nonce and state are
     * generated per attempt — the server replay-guards nonces (single-use,
     * 5-minute window), so a retry must never reuse one.
     */
    fun begin(activity: Activity) {
        val nonce = randomToken()
        val state = randomToken()
        prefs.edit()
            .putString(KEY_PENDING_NONCE, nonce)
            .putString(KEY_PENDING_STATE, state)
            .putLong(KEY_STARTED_AT, System.currentTimeMillis())
            .apply()

        val authorizeUri = Uri.parse("https://appleid.apple.com/auth/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", BuildConfig.APPLE_SERVICES_ID)
            .appendQueryParameter("redirect_uri", RETURN_ENDPOINT)
            // The server consumes a raw identity_token JWT and offers no code
            // exchange, so the id_token must come back in the redirect itself.
            .appendQueryParameter("response_type", "code id_token")
            // scope=name (first-auth only) forces response_mode=form_post,
            // which is why the server return endpoint exists at all.
            .appendQueryParameter("scope", "name")
            .appendQueryParameter("response_mode", "form_post")
            .appendQueryParameter("nonce", nonce)
            .appendQueryParameter("state", state)
            .build()

        CustomTabsIntent.Builder().build().launchUrl(activity, authorizeUri)
    }

    /** True when [uri] is the claimed App Link the return endpoint bounces to. */
    fun isReturnUri(uri: Uri): Boolean =
        uri.host == "mixmat.es" && uri.path == APP_LINK_PATH

    /**
     * Handles the App Link intent. The return endpoint guarantees each
     * parameter appears URL-encoded in the fragment exactly once.
     */
    fun handleReturnUri(uri: Uri) {
        val params = parseFragment(uri)

        val pendingState = prefs.getString(KEY_PENDING_STATE, null)
        val pendingNonce = prefs.getString(KEY_PENDING_NONCE, null)
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        // Single-use: whatever comes of this return, the pending attempt is spent.
        prefs.edit()
            .remove(KEY_PENDING_STATE)
            .remove(KEY_PENDING_NONCE)
            .remove(KEY_STARTED_AT)
            .apply()

        params["error"]?.let {
            _returns.value = AppleSignInReturn(error = it)
            return
        }

        val idToken = params["id_token"]
        val stateValid = pendingState != null &&
            params["state"] == pendingState &&
            System.currentTimeMillis() - startedAt <= ATTEMPT_VALIDITY_MS
        if (idToken == null || !stateValid) {
            _returns.value = AppleSignInReturn(error = ERROR_STATE_MISMATCH)
            return
        }

        _returns.value = AppleSignInReturn(
            idToken = idToken,
            nonce = pendingNonce,
            name = parseName(params["user"]),
        )
    }

    fun consumeReturn() {
        _returns.value = null
    }

    // Split on the ENCODED fragment first — decoding whole would corrupt
    // values containing encoded '&' or '='.
    private fun parseFragment(uri: Uri): Map<String, String> =
        (uri.encodedFragment ?: "")
            .split("&")
            .mapNotNull { pair ->
                val eq = pair.indexOf('=')
                if (eq <= 0) return@mapNotNull null
                val key = pair.substring(0, eq)
                val value = URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                key to value
            }
            .toMap()

    // Apple's user payload, first authorization only:
    // {"name":{"firstName":"...","lastName":"..."}}
    private fun parseName(userJson: String?): String? {
        if (userJson.isNullOrBlank()) return null
        return try {
            val name = Json.parseToJsonElement(userJson).jsonObject["name"]?.jsonObject
                ?: return null
            listOf("firstName", "lastName")
                .mapNotNull { name[it]?.jsonPrimitive?.content }
                .joinToString(" ")
                .trim()
                .ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    private fun randomToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val ERROR_STATE_MISMATCH = "state_mismatch"

        // Registered on the Apple Services ID; Apple form-POSTs here.
        private const val RETURN_ENDPOINT = "https://mixmat.es/api/v1/listener/auth/apple/return"

        // The App Link the return endpoint 303s to, claimed via assetlinks.json.
        private const val APP_LINK_PATH = "/app/listener/signin-apple"

        private const val ATTEMPT_VALIDITY_MS = 10 * 60 * 1000L

        private const val KEY_PENDING_NONCE = "pending_nonce"
        private const val KEY_PENDING_STATE = "pending_state"
        private const val KEY_STARTED_AT = "started_at"
    }
}
