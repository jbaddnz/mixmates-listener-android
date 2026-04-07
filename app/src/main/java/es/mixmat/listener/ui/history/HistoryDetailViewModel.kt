package es.mixmat.listener.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.data.repository.GroupRepository
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.domain.model.Group
import es.mixmat.listener.domain.model.HistoryDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryDetailUiState(
    val detail: HistoryDetail? = null,
    val groups: List<Group> = emptyList(),
    val selectedGroupIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isSharing: Boolean = false,
    val shareResult: Map<String, String>? = null,
    val error: String? = null,
)

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState: StateFlow<HistoryDetailUiState> = _uiState

    fun load(id: String) {
        viewModelScope.launch {
            try {
                val detail = historyRepository.getDetail(id)
                val groups = groupRepository.getGroups()
                val alreadyShared = detail.sharedTo.map { it.groupId }.toSet()
                _uiState.value = HistoryDetailUiState(
                    detail = detail,
                    groups = groups,
                    selectedGroupIds = alreadyShared,
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

    fun toggleGroup(groupId: String) {
        val current = _uiState.value.selectedGroupIds
        _uiState.value = _uiState.value.copy(
            selectedGroupIds = if (groupId in current) current - groupId else current + groupId,
        )
    }

    fun share() {
        val detail = _uiState.value.detail ?: return
        val groupIds = _uiState.value.selectedGroupIds.toList()
        if (groupIds.isEmpty()) return

        _uiState.value = _uiState.value.copy(isSharing = true)
        viewModelScope.launch {
            try {
                val results = historyRepository.share(detail.id, groupIds)
                _uiState.value = _uiState.value.copy(
                    isSharing = false,
                    shareResult = results,
                )
            } catch (e: Exception) {
                Log.e("HistoryDetail", "Failed to share", e)
                _uiState.value = _uiState.value.copy(
                    isSharing = false,
                    error = "Couldn't share — try again",
                )
            }
        }
    }
}
