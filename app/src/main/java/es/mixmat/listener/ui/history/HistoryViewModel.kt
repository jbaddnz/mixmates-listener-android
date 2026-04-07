package es.mixmat.listener.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.domain.model.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val items: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val cursor: String? = null,
    val hasMore: Boolean = false,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    init {
        loadHistory()
    }

    fun loadHistory() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val page = historyRepository.getHistory()
                _uiState.value = HistoryUiState(
                    items = page.items,
                    cursor = page.cursor,
                    hasMore = page.hasMore,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load history",
                )
            }
        }
    }

    fun loadMore() {
        val cursor = _uiState.value.cursor ?: return
        if (_uiState.value.isLoadingMore) return

        _uiState.value = _uiState.value.copy(isLoadingMore = true)
        viewModelScope.launch {
            try {
                val page = historyRepository.getHistory(cursor = cursor)
                _uiState.value = _uiState.value.copy(
                    items = _uiState.value.items + page.items,
                    cursor = page.cursor,
                    hasMore = page.hasMore,
                    isLoadingMore = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            try {
                historyRepository.delete(id)
                _uiState.value = _uiState.value.copy(
                    items = _uiState.value.items.filter { it.id != id },
                )
            } catch (_: Exception) { }
        }
    }
}
