package es.mixmat.listener.data.repository

import es.mixmat.listener.data.api.ListenerApi
import es.mixmat.listener.data.api.asApiException
import es.mixmat.listener.data.api.dto.AppleSignInRequest
import es.mixmat.listener.data.api.dto.AppleSignInUser
import es.mixmat.listener.data.api.dto.GoogleSignInRequest
import es.mixmat.listener.data.api.dto.ProviderSignInData
import es.mixmat.listener.data.api.dto.UpdateMeRequest
import es.mixmat.listener.data.api.toDomain
import es.mixmat.listener.data.auth.TokenManager
import es.mixmat.listener.data.prefs.ListenerPrefs
import es.mixmat.listener.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ListenerApi,
    private val tokenManager: TokenManager,
    private val listenerPrefs: ListenerPrefs,
) {
    fun hasToken(): Boolean = tokenManager.hasToken()

    fun saveToken(token: String) = tokenManager.saveToken(token)

    /**
     * The last profile the server returned, so a name set in the share sheet
     * reaches the Listen screen's greeting without a relaunch.
     */
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    fun clearToken() {
        tokenManager.clearToken()
        _profile.value = null
    }

    fun lastSignInMethod(): String? = listenerPrefs.getLastSignInMethod()

    fun recordSignInMethod(method: String) = listenerPrefs.setLastSignInMethod(method)

    suspend fun getProfile(): UserProfile =
        api.me().data.toDomain().also { _profile.value = it }

    /**
     * `PATCH /auth/me`. Throws ApiException for `invalid_field` and
     * `private_relay_name`, and RateLimitException above 10 an hour.
     */
    suspend fun setDisplayName(name: String): UserProfile =
        try {
            api.updateMe(UpdateMeRequest(displayName = name)).data.toDomain()
                .also { _profile.value = it }
        } catch (e: HttpException) {
            throw e.asApiException() ?: e
        }

    suspend fun signInWithGoogle(idToken: String, nonce: String, name: String?): ProviderSignInData =
        api.signInWithGoogle(GoogleSignInRequest(idToken = idToken, nonce = nonce, name = name)).data

    suspend fun signInWithApple(identityToken: String, nonce: String, name: String?): ProviderSignInData =
        api.signInWithApple(
            AppleSignInRequest(
                identityToken = identityToken,
                nonce = nonce,
                user = name?.let { AppleSignInUser(name = it) },
            ),
        ).data

    /** Deletes the account server-side, then clears the local token. */
    suspend fun deleteAccount() {
        api.deleteAccount()
        clearToken()
        // The last-sign-in hint would now point at an account that no longer
        // exists. Deletion is the only thing that erases it — it survives
        // sign-out and 401/expiry by design.
        listenerPrefs.clearLastSignInMethod()
    }
}
