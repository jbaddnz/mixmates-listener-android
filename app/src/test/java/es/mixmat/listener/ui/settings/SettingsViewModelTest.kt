package es.mixmat.listener.ui.settings

import es.mixmat.listener.data.preferences.ThemePreferences
import es.mixmat.listener.data.repository.AuthRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var themePreferences: ThemePreferences

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        themePreferences = mockk(relaxed = true)
        every { themePreferences.isDarkMode } returns flowOf(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isDarkMode reflects preference`() = runTest {
        val viewModel = SettingsViewModel(authRepository, themePreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isDarkMode.value)
    }

    @Test
    fun `setDarkMode updates preference`() = runTest {
        val viewModel = SettingsViewModel(authRepository, themePreferences)

        viewModel.setDarkMode(false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { themePreferences.setDarkMode(false) }
    }

    @Test
    fun `clearToken delegates to repository`() {
        val viewModel = SettingsViewModel(authRepository, themePreferences)
        viewModel.clearToken()
        verify { authRepository.clearToken() }
    }
}
