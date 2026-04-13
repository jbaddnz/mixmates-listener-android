package es.mixmat.listener.ui.auth

import es.mixmat.listener.data.repository.AuthRepository
import es.mixmat.listener.domain.model.RateLimit
import es.mixmat.listener.domain.model.UserProfile
import android.util.Log
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

    private val validProfile = UserProfile(
        id = "u1",
        displayName = "Jamie",
        role = "user",
        listenEnabled = true,
        preferredPlatform = "spotify",
        rateLimit = RateLimit(limit = 20, remaining = 15, resetAt = 0),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        authRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
}
