package es.mixmat.listener.ui.auth

import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import es.mixmat.listener.data.api.dto.ProviderSignInData
import es.mixmat.listener.data.auth.AppleSignInHelper
import es.mixmat.listener.data.auth.AppleSignInReturn
import es.mixmat.listener.data.auth.GoogleSignInHelper
import es.mixmat.listener.data.auth.GoogleSignInResult
import es.mixmat.listener.data.prefs.ListenerPrefs
import es.mixmat.listener.data.repository.AuthRepository
import es.mixmat.listener.domain.model.RateLimit
import es.mixmat.listener.domain.model.UserProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TokenEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var googleHelper: GoogleSignInHelper
    private lateinit var appleHelper: AppleSignInHelper
    private lateinit var appleReturns: MutableStateFlow<AppleSignInReturn?>

    private val validProfile = UserProfile(
        id = "u1",
        displayName = "Jamie",
        role = "user",
        listenEnabled = true,
        preferredPlatform = "spotify",
        rateLimit = RateLimit(limit = 20, remaining = 15, resetAt = 0),
    )

    private val googleSignInResult = GoogleSignInResult(
        idToken = "google-id-token",
        displayName = "Jamie",
    )

    private val signInDataEnabled = ProviderSignInData(
        token = "bearer-token",
        isNewAccount = false,
        listenEnabled = true,
    )

    private val signInDataDisabled = ProviderSignInData(
        token = "bearer-token",
        isNewAccount = false,
        listenEnabled = false,
    )

    private val signInDataNewAccount = ProviderSignInData(
        token = "bearer-token",
        isNewAccount = true,
        listenEnabled = true,
    )

    private val appleReturnSuccess = AppleSignInReturn(
        idToken = "apple-identity-token",
        nonce = "apple-nonce",
        name = "Jamie B",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        authRepository = mockk(relaxed = true)
        every { authRepository.lastSignInMethod() } returns null
        googleHelper = mockk()
        appleReturns = MutableStateFlow(null)
        appleHelper = mockk {
            every { returns } returns appleReturns
            justRun { consumeReturn() }
            every { isConfigured() } returns true
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TokenEntryViewModel(authRepository, appleHelper)

    // -- Paste flow tests --

    @Test
    fun `onTokenChange updates token`() {
        val viewModel = viewModel()
        viewModel.onTokenChange("abc123")
        assertEquals("abc123", viewModel.uiState.value.token)
    }

    @Test
    fun `onTokenChange clears error`() {
        val viewModel = viewModel()
        viewModel.validateAndSave() // triggers empty error
        assertNotNull(viewModel.uiState.value.error)

        viewModel.onTokenChange("abc")
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `validateAndSave rejects blank token`() {
        val viewModel = viewModel()
        viewModel.onTokenChange("   ")
        viewModel.validateAndSave()

        assertEquals("Token cannot be empty", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isValidating)
    }

    @Test
    fun `validateAndSave saves and validates token`() = runTest {
        coEvery { authRepository.getProfile() } returns validProfile

        val viewModel = viewModel()
        viewModel.onTokenChange("valid-token")
        viewModel.validateAndSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isValid)
        assertNull(state.error)
        assertEquals("Jamie", state.profile?.displayName)
        verify { authRepository.saveToken("valid-token") }
    }

    @Test
    fun `validateAndSave clears token on failure`() = runTest {
        coEvery { authRepository.getProfile() } throws RuntimeException("401")

        val viewModel = viewModel()
        viewModel.onTokenChange("bad-token")
        viewModel.validateAndSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isValid)
        assertNotNull(state.error)
        verify { authRepository.clearToken() }
    }

    @Test
    fun `validateAndSave rejects listen-not-enabled`() = runTest {
        val noListen = validProfile.copy(listenEnabled = false)
        coEvery { authRepository.getProfile() } returns noListen

        val viewModel = viewModel()
        viewModel.onTokenChange("valid-token")
        viewModel.validateAndSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isValid)
        assertTrue(state.error!!.contains("enabled"))
        verify { authRepository.clearToken() }
    }

    @Test
    fun `validateAndSave does not record a sign-in method`() = runTest {
        // Whether a Listen Key connection counts as a "method" for the
        // last-used hint is an open product call — provider sign-ins only.
        coEvery { authRepository.getProfile() } returns validProfile

        val viewModel = viewModel()
        viewModel.onTokenChange("valid-token")
        viewModel.validateAndSave()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { authRepository.recordSignInMethod(any()) }
    }

    // -- Google sign-in tests --

    @Test
    fun `signInWithGoogle success`() = runTest {
        coEvery { googleHelper.signIn(any()) } returns googleSignInResult
        coEvery { authRepository.signInWithGoogle(any(), any(), any()) } returns signInDataEnabled

        val viewModel = viewModel()
        viewModel.signInWithGoogle(googleHelper)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isValid)
        assertFalse(state.isGoogleSigningIn)
        assertFalse(state.showNewAccountDialog)
        assertNull(state.error)
        verify { authRepository.saveToken("bearer-token") }
        verify { authRepository.recordSignInMethod(ListenerPrefs.METHOD_GOOGLE) }
    }

    @Test
    fun `signInWithGoogle listen not enabled`() = runTest {
        coEvery { googleHelper.signIn(any()) } returns googleSignInResult
        coEvery { authRepository.signInWithGoogle(any(), any(), any()) } returns signInDataDisabled

        val viewModel = viewModel()
        viewModel.signInWithGoogle(googleHelper)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isValid)
        assertFalse(state.isGoogleSigningIn)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("enabled"))
        verify(exactly = 0) { authRepository.saveToken(any()) }
    }

    @Test
    fun `signInWithGoogle server error`() = runTest {
        coEvery { googleHelper.signIn(any()) } returns googleSignInResult
        coEvery { authRepository.signInWithGoogle(any(), any(), any()) } throws RuntimeException("Server error")

        val viewModel = viewModel()
        viewModel.signInWithGoogle(googleHelper)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isValid)
        assertFalse(state.isGoogleSigningIn)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("failed"))
    }

    @Test
    fun `signInWithGoogle user cancels`() = runTest {
        coEvery { googleHelper.signIn(any()) } throws GetCredentialCancellationException()

        val viewModel = viewModel()
        viewModel.signInWithGoogle(googleHelper)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isValid)
        assertFalse(state.isGoogleSigningIn)
        assertNull(state.error)
    }

    @Test
    fun `signInWithGoogle sets isGoogleSigningIn during processing`() {
        coEvery { googleHelper.signIn(any()) } returns googleSignInResult
        coEvery { authRepository.signInWithGoogle(any(), any(), any()) } returns signInDataEnabled

        val viewModel = viewModel()
        viewModel.signInWithGoogle(googleHelper)

        assertTrue(viewModel.uiState.value.isGoogleSigningIn)
    }

    // -- Apple sign-in tests --

    @Test
    fun `apple return success posts token and adopts session`() = runTest {
        coEvery { authRepository.signInWithApple(any(), any(), any()) } returns signInDataEnabled

        val viewModel = viewModel()
        appleReturns.value = appleReturnSuccess
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isValid)
        assertFalse(state.isAppleSigningIn)
        assertNull(state.error)
        coVerify { authRepository.signInWithApple("apple-identity-token", "apple-nonce", "Jamie B") }
        verify { authRepository.saveToken("bearer-token") }
        verify { authRepository.recordSignInMethod(ListenerPrefs.METHOD_APPLE) }
    }

    @Test
    fun `apple return new account shows dialog and holds the token`() = runTest {
        coEvery { authRepository.signInWithApple(any(), any(), any()) } returns signInDataNewAccount

        val viewModel = viewModel()
        appleReturns.value = appleReturnSuccess
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showNewAccountDialog)
        assertFalse(state.isValid)
        verify(exactly = 0) { authRepository.saveToken(any()) }
    }

    @Test
    fun `apple return cancelled is quiet`() = runTest {
        val viewModel = viewModel()
        appleReturns.value = AppleSignInReturn(error = "user_cancelled_authorize")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertFalse(state.isValid)
        coVerify(exactly = 0) { authRepository.signInWithApple(any(), any(), any()) }
    }

    @Test
    fun `apple return state mismatch shows error without posting`() = runTest {
        val viewModel = viewModel()
        appleReturns.value = AppleSignInReturn(error = AppleSignInHelper.ERROR_STATE_MISMATCH)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.error!!.contains("failed"))
        coVerify(exactly = 0) { authRepository.signInWithApple(any(), any(), any()) }
    }

    // -- New-account tripwire tests (Google path) --

    @Test
    fun `new account shows dialog and holds the token`() = runTest {
        coEvery { googleHelper.signIn(any()) } returns googleSignInResult
        coEvery { authRepository.signInWithGoogle(any(), any(), any()) } returns signInDataNewAccount

        val viewModel = viewModel()
        viewModel.signInWithGoogle(googleHelper)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showNewAccountDialog)
        assertFalse(state.isValid)
        assertFalse(state.isGoogleSigningIn)
        verify(exactly = 0) { authRepository.saveToken(any()) }
    }

    @Test
    fun `keepNewAccount adopts the held session`() = runTest {
        coEvery { googleHelper.signIn(any()) } returns googleSignInResult
        coEvery { authRepository.signInWithGoogle(any(), any(), any()) } returns signInDataNewAccount

        val viewModel = viewModel()
        viewModel.signInWithGoogle(googleHelper)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.keepNewAccount()

        val state = viewModel.uiState.value
        assertFalse(state.showNewAccountDialog)
        assertTrue(state.isValid)
        verify { authRepository.saveToken("bearer-token") }
        verify { authRepository.recordSignInMethod(ListenerPrefs.METHOD_GOOGLE) }
    }

    @Test
    fun `keepNewAccount with listen disabled shows error`() = runTest {
        val newButDisabled = signInDataNewAccount.copy(listenEnabled = false)
        coEvery { googleHelper.signIn(any()) } returns googleSignInResult
        coEvery { authRepository.signInWithGoogle(any(), any(), any()) } returns newButDisabled

        val viewModel = viewModel()
        viewModel.signInWithGoogle(googleHelper)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.keepNewAccount()

        val state = viewModel.uiState.value
        assertFalse(state.showNewAccountDialog)
        assertFalse(state.isValid)
        assertTrue(state.error!!.contains("enabled"))
        verify(exactly = 0) { authRepository.saveToken(any()) }
    }

    @Test
    fun `declineNewAccount discards the held token`() = runTest {
        coEvery { googleHelper.signIn(any()) } returns googleSignInResult
        coEvery { authRepository.signInWithGoogle(any(), any(), any()) } returns signInDataNewAccount

        val viewModel = viewModel()
        viewModel.signInWithGoogle(googleHelper)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.declineNewAccount()

        val state = viewModel.uiState.value
        assertFalse(state.showNewAccountDialog)
        assertFalse(state.isValid)
        assertNull(state.error)
        verify(exactly = 0) { authRepository.saveToken(any()) }

        // A later keep must be a no-op — the pending session is gone.
        viewModel.keepNewAccount()
        assertFalse(viewModel.uiState.value.isValid)
        verify(exactly = 0) { authRepository.saveToken(any()) }
    }
}
