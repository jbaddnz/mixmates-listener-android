package es.mixmat.listener.data.repository

import es.mixmat.listener.data.api.ListenerApi
import es.mixmat.listener.data.api.dto.GoogleSignInData
import es.mixmat.listener.data.api.dto.GoogleSignInRequest
import es.mixmat.listener.data.api.toDomain
import es.mixmat.listener.data.auth.TokenManager
import es.mixmat.listener.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ListenerApi,
    private val tokenManager: TokenManager,
) {
    fun hasToken(): Boolean = tokenManager.hasToken()

    fun saveToken(token: String) = tokenManager.saveToken(token)

    fun clearToken() = tokenManager.clearToken()

    suspend fun getProfile(): UserProfile =
        api.me().data.toDomain()

    suspend fun signInWithGoogle(idToken: String, nonce: String, name: String?): GoogleSignInData =
        api.signInWithGoogle(GoogleSignInRequest(idToken = idToken, nonce = nonce, name = name)).data
}
