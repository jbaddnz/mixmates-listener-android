package es.mixmat.listener.ui.settings

import es.mixmat.listener.data.repository.AuthRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SettingsViewModelTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)

    @Test
    fun `clearToken delegates to repository`() {
        val viewModel = SettingsViewModel(authRepository)
        viewModel.clearToken()
        verify { authRepository.clearToken() }
    }
}
