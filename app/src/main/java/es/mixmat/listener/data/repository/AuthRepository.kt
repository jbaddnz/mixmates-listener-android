package es.mixmat.listener.data.repository

import es.mixmat.listener.data.api.ListenerApi
import es.mixmat.listener.data.api.dto.AppleSignInRequest
import es.mixmat.listener.data.api.dto.AppleSignInUser
import es.mixmat.listener.data.api.dto.GoogleSignInRequest
import es.mixmat.listener.data.api.dto.ProviderSignInData
import es.mixmat.listener.data.api.toDomain
import es.mixmat.listener.data.auth.TokenManager
import es.mixmat.listener.data.prefs.ListenerPrefs
import es.mixmat.listener.domain.model.UserProfile
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

    fun clearToken() = tokenManager.clearToken()

    fun lastSignInMethod(): String? = listenerPrefs.getLastSignInMethod()

    fun recordSignInMethod(method: String) = listenerPrefs.setLastSignInMethod(method)

    suspend fun getProfile(): UserProfile =
        api.me().data.toDomain()

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
        tokenManager.clearToken()
        // The last-sign-in hint would now point at an account that no longer
        // exists. Deletion is the only thing that erases it — it survives
        // sign-out and 401/expiry by design.
        listenerPrefs.clearLastSignInMethod()
    }
}
