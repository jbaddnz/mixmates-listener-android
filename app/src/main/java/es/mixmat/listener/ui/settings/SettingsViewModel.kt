package es.mixmat.listener.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDeleting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun clearToken() {
        authRepository.clearToken()
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        if (_uiState.value.isDeleting) return
        _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
        viewModelScope.launch {
            try {
                authRepository.deleteAccount()
                _uiState.value = _uiState.value.copy(isDeleting = false)
                onDeleted()
            } catch (e: Exception) {
                Log.e("Settings", "Account deletion failed", e)
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = "Couldn't delete your account. Try again.",
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
