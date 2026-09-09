package es.mixmat.listener.ui.settings

import es.mixmat.listener.data.prefs.ListenerPrefs
import es.mixmat.listener.data.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val listenerPrefs: ListenerPrefs = mockk(relaxed = true)

    @Test
    fun `clearToken delegates to repository`() {
        val viewModel = SettingsViewModel(authRepository, listenerPrefs)
        viewModel.clearToken()
        verify { authRepository.clearToken() }
    }

    @Test
    fun `recording length is read at construction`() {
        every { listenerPrefs.getRecordingLengthSeconds() } returns 8
        val viewModel = SettingsViewModel(authRepository, listenerPrefs)
        assertEquals(8, viewModel.uiState.value.recordingLengthSeconds)
    }

    @Test
    fun `setRecordingLength persists and reflects the stored value`() {
        every { listenerPrefs.getRecordingLengthSeconds() } returns 10 andThen 7
        val viewModel = SettingsViewModel(authRepository, listenerPrefs)

        viewModel.setRecordingLength(7)

        verify { listenerPrefs.setRecordingLengthSeconds(7) }
        assertEquals(7, viewModel.uiState.value.recordingLengthSeconds)
    }
}
