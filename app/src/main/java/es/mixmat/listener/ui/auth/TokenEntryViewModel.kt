package es.mixmat.listener.ui.auth

import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.data.auth.GoogleSignInHelper
import es.mixmat.listener.data.repository.AuthRepository
import es.mixmat.listener.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TokenEntryUiState(
    val token: String = "",
    val isValidating: Boolean = false,
    val isValid: Boolean = false,
    val error: String? = null,
    val profile: UserProfile? = null,
    val isGoogleSigningIn: Boolean = false,
)

@HiltViewModel
class TokenEntryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TokenEntryUiState())
    val uiState: StateFlow<TokenEntryUiState> = _uiState

    fun hasToken(): Boolean = authRepository.hasToken()

    fun onTokenChange(token: String) {
        _uiState.value = _uiState.value.copy(token = token, error = null)
    }

    fun signInWithGoogle(helper: GoogleSignInHelper) {
        _uiState.value = _uiState.value.copy(isGoogleSigningIn = true, error = null)

        viewModelScope.launch {
            try {
                val nonce = UUID.randomUUID().toString()
                val googleResult = helper.signIn(nonce)

                val response = authRepository.signInWithGoogle(
                    idToken = googleResult.idToken,
                    nonce = nonce,
                    name = googleResult.displayName,
                )

                if (!response.listenEnabled) {
                    _uiState.value = _uiState.value.copy(
                        isGoogleSigningIn = false,
                        error = "Listen is not enabled on your account. Upgrade at mixmat.es to use Listener.",
                    )
                    return@launch
                }

                authRepository.saveToken(response.token)
                _uiState.value = _uiState.value.copy(
                    isGoogleSigningIn = false,
                    isValid = true,
                )
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = _uiState.value.copy(isGoogleSigningIn = false)
            } catch (e: Exception) {
                Log.e("TokenEntry", "Google sign-in failed", e)
                _uiState.value = _uiState.value.copy(
                    isGoogleSigningIn = false,
                    error = "Google sign-in failed. Try again or use a Listen Key.",
                )
            }
        }
    }

    fun validateAndSave() {
        val token = _uiState.value.token.trim()
        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Token cannot be empty")
            return
        }

        _uiState.value = _uiState.value.copy(isValidating = true, error = null)

        // Store token temporarily so the auth interceptor can use it for validation.
        // Cleared immediately if validation fails.
        authRepository.saveToken(token)

        viewModelScope.launch {
            try {
                val profile = authRepository.getProfile()
                if (!profile.listenEnabled) {
                    authRepository.clearToken()
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        error = "Listen is not enabled on your account. Enable it in MixMates Settings.",
                    )
                    return@launch
                }
                // Token validated — keep it stored
                _uiState.value = _uiState.value.copy(
                    isValidating = false,
                    isValid = true,
                    profile = profile,
                )
            } catch (e: Exception) {
                Log.e("TokenEntry", "Token validation failed", e)
                authRepository.clearToken()
                _uiState.value = _uiState.value.copy(
                    isValidating = false,
                    error = "Invalid token. Check your Listen Key in MixMates Settings.",
                )
            }
        }
    }
}
