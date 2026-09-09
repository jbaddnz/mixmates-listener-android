package es.mixmat.listener.ui.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.data.api.dto.ProviderSignInData
import es.mixmat.listener.data.auth.AppleSignInHelper
import es.mixmat.listener.data.auth.AppleSignInReturn
import es.mixmat.listener.data.auth.GoogleSignInHelper
import es.mixmat.listener.data.prefs.ListenerPrefs
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
    val isAppleSigningIn: Boolean = false,
    val lastSignInMethod: String? = null,
    val showNewAccountDialog: Boolean = false,
)

@HiltViewModel
class TokenEntryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appleSignInHelper: AppleSignInHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TokenEntryUiState(lastSignInMethod = authRepository.lastSignInMethod()),
    )
    val uiState: StateFlow<TokenEntryUiState> = _uiState

    // Sign-in response held while the new-account dialog is up. The token in
    // here is never stored until the user explicitly keeps the account.
    private data class PendingSignIn(val data: ProviderSignInData, val method: String)

    private var pendingNewAccount: PendingSignIn? = null

    init {
        viewModelScope.launch {
            appleSignInHelper.returns.collect { ret ->
                if (ret != null) {
                    appleSignInHelper.consumeReturn()
                    handleAppleReturn(ret)
                }
            }
        }
    }

    fun isAppleSignInAvailable(): Boolean = appleSignInHelper.isConfigured()

    fun onTokenChange(token: String) {
        _uiState.value = _uiState.value.copy(token = token, error = null)
    }

    fun signInWithGoogle(helper: GoogleSignInHelper) {
        _uiState.value = _uiState.value.copy(isGoogleSigningIn = true, error = null)

        viewModelScope.launch {
            try {
                // Fresh nonce per attempt — the server replay-guards them
                // (single-use, 5-minute window), so a retry must never reuse one.
                val nonce = UUID.randomUUID().toString()
                val googleResult = helper.signIn(nonce)

                val response = authRepository.signInWithGoogle(
                    idToken = googleResult.idToken,
                    nonce = nonce,
                    name = googleResult.displayName,
                )

                onSignInResponse(response, ListenerPrefs.METHOD_GOOGLE)
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

    /** Opens the Apple web flow in a Custom Tab; the result arrives via the App Link. */
    fun signInWithApple(activity: Activity) {
        _uiState.value = _uiState.value.copy(error = null)
        appleSignInHelper.begin(activity)
    }

    private suspend fun handleAppleReturn(ret: AppleSignInReturn) {
        if (ret.isCancelled) {
            _uiState.value = _uiState.value.copy(isAppleSigningIn = false)
            return
        }
        if (ret.error != null || ret.idToken == null || ret.nonce == null) {
            Log.e("TokenEntry", "Apple sign-in return invalid: ${ret.error}")
            _uiState.value = _uiState.value.copy(
                isAppleSigningIn = false,
                error = "Apple sign-in failed. Try again or use a Listen Key.",
            )
            return
        }

        _uiState.value = _uiState.value.copy(isAppleSigningIn = true, error = null)
        try {
            val response = authRepository.signInWithApple(
                identityToken = ret.idToken,
                nonce = ret.nonce,
                name = ret.name,
            )
            onSignInResponse(response, ListenerPrefs.METHOD_APPLE)
        } catch (e: Exception) {
            Log.e("TokenEntry", "Apple sign-in failed", e)
            _uiState.value = _uiState.value.copy(
                isAppleSigningIn = false,
                error = "Apple sign-in failed. Try again or use a Listen Key.",
            )
        }
    }

    private fun onSignInResponse(response: ProviderSignInData, method: String) {
        if (response.isNewAccount) {
            // Tripwire: this sign-in had no MixMates account, so the server
            // just minted one. Never adopt it silently — hold the token behind
            // the dialog and let the user decide.
            pendingNewAccount = PendingSignIn(response, method)
            _uiState.value = _uiState.value.copy(
                isGoogleSigningIn = false,
                isAppleSigningIn = false,
                showNewAccountDialog = true,
            )
            return
        }

        adoptSession(response, method)
    }

    fun keepNewAccount() {
        val pending = pendingNewAccount ?: return
        pendingNewAccount = null
        _uiState.value = _uiState.value.copy(showNewAccountDialog = false)
        adoptSession(pending.data, pending.method)
    }

    fun declineNewAccount() {
        // Discard the held token and stay signed out. The server-side account
        // exists either way; cleanup is admin-side, not ours.
        pendingNewAccount = null
        _uiState.value = _uiState.value.copy(showNewAccountDialog = false)
    }

    private fun adoptSession(data: ProviderSignInData, method: String) {
        if (!data.listenEnabled) {
            _uiState.value = _uiState.value.copy(
                isGoogleSigningIn = false,
                isAppleSigningIn = false,
                error = "Listen isn't enabled on your account. Contact support if you think this is a mistake.",
            )
            return
        }

        authRepository.saveToken(data.token)
        authRepository.recordSignInMethod(method)
        _uiState.value = _uiState.value.copy(
            isGoogleSigningIn = false,
            isAppleSigningIn = false,
            isValid = true,
            lastSignInMethod = method,
        )
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
                // Token validated — keep it stored. Whether a Listen Key
                // connection counts as a "method" for the last-used hint is an
                // open product call — provider sign-ins only for now.
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
