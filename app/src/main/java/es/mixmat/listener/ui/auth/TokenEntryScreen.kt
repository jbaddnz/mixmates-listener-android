package es.mixmat.listener.ui.auth

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.mixmat.listener.R
import es.mixmat.listener.data.auth.GoogleSignInHelper
import es.mixmat.listener.data.prefs.ListenerPrefs
import es.mixmat.listener.ui.text.asString
import es.mixmat.listener.ui.theme.MixMatesListenerTheme

// The sign-in screen commits to the fixed dark brand surface in both themes,
// matching iOS (its LaunchBackground navy) and the track card's approach.
private val SignInBackground = Color(0xFF1A1A2E)
private val SignInError = Color(0xFFFFB4AB)

@Composable
fun TokenEntryScreen(
    onTokenSaved: () -> Unit,
    viewModel: TokenEntryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val googleSignInHelper = remember { GoogleSignInHelper(context as Activity) }

    LaunchedEffect(uiState.isValid) {
        if (uiState.isValid) onTokenSaved()
    }

    // Dark surface needs light status-bar icons regardless of theme; restore
    // whatever the theme had set when this screen leaves.
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(Unit) {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            val previous = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = false
            onDispose { controller.isAppearanceLightStatusBars = previous }
        }
    }

    SignInContent(
        uiState = uiState,
        appleAvailable = viewModel.isAppleSignInAvailable(),
        onGoogleClick = { viewModel.signInWithGoogle(googleSignInHelper) },
        onAppleClick = { viewModel.signInWithApple(context as Activity) },
        onTokenChange = viewModel::onTokenChange,
        onConnectClick = viewModel::validateAndSave,
    )

    // Fork tripwire: the sign-in minted a brand-new account. The token is held
    // by the view model and only stored if the user keeps the account;
    // dismissing any other way counts as declining.
    if (uiState.showNewAccountDialog) {
        AlertDialog(
            onDismissRequest = viewModel::declineNewAccount,
            title = { Text(stringResource(R.string.auth_new_account_title)) },
            text = { Text(stringResource(R.string.auth_new_account_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::keepNewAccount) {
                    Text(stringResource(R.string.auth_new_account_keep))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::declineNewAccount) {
                    Text(stringResource(R.string.auth_new_account_sign_out))
                }
            },
        )
    }
}

@Composable
private fun SignInContent(
    uiState: TokenEntryUiState,
    appleAvailable: Boolean,
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onTokenChange: (String) -> Unit,
    onConnectClick: () -> Unit,
) {
    val isBusy = uiState.isValidating || uiState.isGoogleSigningIn || uiState.isAppleSigningIn

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SignInBackground)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.mml_wordmark),
            contentDescription = stringResource(R.string.auth_wordmark_description),
            modifier = Modifier.width(280.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            // Product name, the same in every language.
            text = "Listener",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White.copy(alpha = 0.8f),
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.auth_prompt),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onGoogleClick,
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (uiState.isGoogleSigningIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.auth_sign_in_google), style = MaterialTheme.typography.titleSmall)
        }

        // White like the iOS treatment; sits below Google on purpose — Google
        // stays at least as prominent. Hidden until the Services ID is set.
        if (appleAvailable) {
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAppleClick,
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (uiState.isAppleSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_apple),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.auth_sign_in_apple), style = MaterialTheme.typography.titleSmall)
            }
        }

        uiState.lastSignInMethod?.let { method ->
            // The whole sentence is one resource so each language can place the
            // name where it belongs; the name is then found in it and bolded.
            val provider = providerDisplayName(method)
            val sentence = stringResource(
                if (method == ListenerPrefs.METHOD_LISTEN_KEY) {
                    R.string.auth_last_signed_in_with_key
                } else {
                    R.string.auth_last_signed_in_with
                },
                provider,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = buildAnnotatedString {
                    append(sentence)
                    val start = sentence.indexOf(provider)
                    if (start >= 0) {
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + provider.length)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.auth_free_no_purchases),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
        )

        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error.asString(),
                color = SignInError,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color.White.copy(alpha = 0.2f),
            )
            Text(
                text = stringResource(R.string.auth_or),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color.White.copy(alpha = 0.2f),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.auth_use_listen_key),
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.7f),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.auth_listen_key_where),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.token,
            onValueChange = onTokenChange,
            label = { Text(stringResource(R.string.auth_listen_key_label)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.7f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = Color.White.copy(alpha = 0.7f),
                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onConnectClick,
            enabled = !isBusy,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isValidating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.auth_connect))
        }
    }
}

/** Brand names, the same in every language, so plain strings rather than resources. */
private fun providerDisplayName(method: String): String = when (method) {
    ListenerPrefs.METHOD_GOOGLE -> "Google"
    ListenerPrefs.METHOD_APPLE -> "Apple"
    ListenerPrefs.METHOD_LISTEN_KEY -> "Listen Key"
    else -> method.replaceFirstChar { it.uppercase() }
}

@Preview(showBackground = true)
@Composable
private fun SignInContentPreview() {
    MixMatesListenerTheme {
        SignInContent(
            uiState = TokenEntryUiState(lastSignInMethod = ListenerPrefs.METHOD_GOOGLE),
            appleAvailable = true,
            onGoogleClick = {},
            onAppleClick = {},
            onTokenChange = {},
            onConnectClick = {},
        )
    }
}
