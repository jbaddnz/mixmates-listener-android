package es.mixmat.listener.ui.auth

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.mixmat.listener.R
import es.mixmat.listener.data.auth.GoogleSignInHelper
import es.mixmat.listener.data.prefs.ListenerPrefs
import es.mixmat.listener.ui.theme.MixMatesListenerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenEntryScreen(
    onTokenSaved: () -> Unit,
    viewModel: TokenEntryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val googleSignInHelper = remember { GoogleSignInHelper(context as Activity) }

    val isBusy = uiState.isValidating || uiState.isGoogleSigningIn || uiState.isAppleSigningIn

    LaunchedEffect(uiState.isValid) {
        if (uiState.isValid) onTokenSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("MixMates Listener") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Sign in to start listening",
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.signInWithGoogle(googleSignInHelper) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
                modifier = Modifier.fillMaxWidth(),
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
                Text("Sign in with Google")
            }

            // Hidden until the Apple Services ID is configured. Sits below
            // Google on purpose — Google stays at least as prominent.
            if (viewModel.isAppleSignInAvailable()) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.signInWithApple(context as Activity) },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.isAppleSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Sign in with Apple")
                }
            }

            uiState.lastSignInMethod?.let { method ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Last time you signed in with ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(providerDisplayName(method))
                        }
                        append(".")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "or",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Use a Listen Key",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Find it in MixMates > Settings > Listening",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.token,
                onValueChange = viewModel::onTokenChange,
                label = { Text("Listen Key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = viewModel::validateAndSave,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Connect")
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    // Fork tripwire: the sign-in minted a brand-new account. The token is held
    // by the view model and only stored if the user keeps the account;
    // dismissing any other way counts as declining.
    if (uiState.showNewAccountDialog) {
        AlertDialog(
            onDismissRequest = viewModel::declineNewAccount,
            title = { Text("New MixMates account created") },
            text = {
                Text(
                    "There was no MixMates account for this sign-in, so we made a new one. " +
                        "Already have an account — maybe with Apple, or another Google account? " +
                        "Sign out and use that instead.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::keepNewAccount) {
                    Text("Keep this account")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::declineNewAccount) {
                    Text("Sign out")
                }
            },
        )
    }
}

private fun providerDisplayName(method: String): String = when (method) {
    ListenerPrefs.METHOD_GOOGLE -> "Google"
    ListenerPrefs.METHOD_APPLE -> "Apple"
    else -> method.replaceFirstChar { it.uppercase() }
}

@Preview(showBackground = true)
@Composable
private fun TokenEntryScreenPreview() {
    MixMatesListenerTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(title = { Text("MixMates Listener") })
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Sign in to start listening", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Google")
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "or",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Use a Listen Key",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Find it in MixMates > Settings > Listening",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Listen Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("Connect")
                }
            }
        }
    }
}
