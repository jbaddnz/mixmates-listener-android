package es.mixmat.listener.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.domain.model.HistoryDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryDetailUiState(
    val detail: HistoryDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Group sharing lives in `TrackShareViewModel` now, not here — one picker for the
 * result screen, this screen and the inbound-share screen alike.
 */
@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState: StateFlow<HistoryDetailUiState> = _uiState

    fun load(id: String) {
        viewModelScope.launch {
            try {
                val detail = historyRepository.getDetail(id)
                _uiState.value = HistoryDetailUiState(
                    detail = detail,
                    isLoading = false,
                )
            } catch (e: Exception) {
                Log.e("HistoryDetail", "Failed to load details", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load details: ${e.message}",
                )
            }
        }
    }
}
