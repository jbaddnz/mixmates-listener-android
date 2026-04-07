package es.mixmat.listener.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.data.preferences.ThemePreferences
import es.mixmat.listener.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val themePreferences: ThemePreferences,
) : ViewModel() {

    val isDarkMode = themePreferences.isDarkMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true,
    )

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setDarkMode(enabled) }
    }

    fun clearToken() {
        authRepository.clearToken()
    }
}
