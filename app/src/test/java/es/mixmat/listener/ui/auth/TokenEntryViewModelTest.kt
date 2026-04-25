package es.mixmat.listener.ui.auth

import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import es.mixmat.listener.data.api.dto.GoogleSignInData
import es.mixmat.listener.data.auth.GoogleSignInHelper
import es.mixmat.listener.data.auth.GoogleSignInResult
import es.mixmat.listener.data.repository.AuthRepository
import es.mixmat.listener.domain.model.RateLimit
import es.mixmat.listener.domain.model.UserProfile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val googleSignInDataEnabled = GoogleSignInData(
        token = "bearer-token",
        isNewAccount = false,
        listenEnabled = true,
    )

    private val googleSignInDataDisabled = GoogleSignInData(
        token = "bearer-token",
        isNewAccount = true,
        listenEnabled = false,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        authRepository = mockk(relaxed = true)
        googleHelper = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -- Paste flow tests --

    @Test
    fun `onTokenChange updates token`() {
        val viewModel = TokenEntryViewModel(authRepository)
        viewModel.onTokenChange("abc123")
        assertEquals("abc123", viewModel.uiState.value.token)
    }

    @Test
    fun `onTokenChange clears error`() {
        val viewModel = TokenEntryViewModel(authRepository)
        viewModel.validateAndSave() // triggers empty error
        assertNotNull(viewModel.uiState.value.error)

        viewModel.onTokenChange("abc")
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `validateAndSave rejects blank token`() {
        val viewModel = TokenEntryViewModel(authRepository)
        viewModel.onTokenChange("   ")
        viewModel.validateAndSave()

        assertEquals("Token cannot be empty", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isValidating)
    }

    @Test
    fun `validateAndSave saves and validates token`() = runTest {
        coEvery { authRepository.getProfile() } returns validProfile

        val viewModel = TokenEntryViewModel(authRepository)
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

        val viewModel = TokenEntryViewModel(authRepository)
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

        val viewModel = TokenEntryViewModel(authRepository)
        viewModel.onTokenChange("valid-token")
        viewModel.validateAndSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isValid)
        assertTrue(state.error!!.contains("not enabled"))
        verify { authRepository.clearToken() }
    }

    // -- Google sign-in tests --

    @Test
    fun `signInWithGoogle success`() = runTest {
        coEvery { googleHelper.signIn(any()) } returns googleSignInResult
        coEvery { authRepository.signInWithGoogle(any(), any(), any()) } returns googleSignInDataEnabled

        val viewModel = TokenEntryViewModel(authRepository)
        viewModel.signInWithGoogle(googleHelper)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isValid)
        assertFalse(state.isGoogleSigningIn)
        assertNull(state.error)
        verify { authRepository.saveToken("bearer-token") }
    }

    @Test
    fun `signInWithGoogle listen not enabled`() = runTest {
        coEvery { googleHelper.signIn(any()) } returns googleSignInResult
        coEvery { authRepository.signInWithGoogle(any(), any(), any()) } returns googleSignInDataDisabled

        val viewModel = TokenEntryViewModel(authRepository)
        viewModel.signInWithGoogle(googleHelper)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isValid)
        assertFalse(state.isGoogleSigningIn)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("not enabled"))
        verify(exactly = 0) { authRepository.saveToken(any()) }
    }

    @Test
    fun `signInWithGoogle server error`() = runTest {
        coEvery { googleHelper.signIn(any()) } returns googleSignInResult
        coEvery { authRepository.signInWithGoogle(any(), any(), any()) } throws RuntimeException("Server error")

        val viewModel = TokenEntryViewModel(authRepository)
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

        val viewModel = TokenEntryViewModel(authRepository)
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
        coEvery { authRepository.signInWithGoogle(any(), any(), any()) } returns googleSignInDataEnabled

        val viewModel = TokenEntryViewModel(authRepository)
        viewModel.signInWithGoogle(googleHelper)

        assertTrue(viewModel.uiState.value.isGoogleSigningIn)
    }
}
