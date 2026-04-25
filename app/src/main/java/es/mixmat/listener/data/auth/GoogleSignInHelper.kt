package es.mixmat.listener.data.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import es.mixmat.listener.BuildConfig

data class GoogleSignInResult(
    val idToken: String,
    val displayName: String?,
)

class GoogleSignInHelper(private val activity: Activity) {

    private val credentialManager = CredentialManager.create(activity)

    suspend fun signIn(nonce: String): GoogleSignInResult {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(activity, request)
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)

        return GoogleSignInResult(
            idToken = googleIdTokenCredential.idToken,
            displayName = googleIdTokenCredential.displayName,
        )
    }
}
