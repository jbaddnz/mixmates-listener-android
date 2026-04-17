package es.mixmat.listener.ui.share

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.data.api.RateLimitException
import es.mixmat.listener.data.repository.AuthRepository
import es.mixmat.listener.data.repository.GroupRepository
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.data.repository.RecognitionRepository
import es.mixmat.listener.domain.model.Group
import es.mixmat.listener.domain.model.RecognitionResult
import es.mixmat.listener.util.MusicUrlExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class ShareUiState(
    val isResolving: Boolean = true,
    val result: RecognitionResult? = null,
    val error: String? = null,
    val groups: List<Group> = emptyList(),
    val selectedGroupIds: Set<String> = emptySet(),
    val isSharing: Boolean = false,
    val shareResult: Map<String, String>? = null,
)

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val recognitionRepository: RecognitionRepository,
    private val authRepository: AuthRepository,
    private val historyRepository: HistoryRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState

    fun resolve(sharedText: String) {
        if (!authRepository.hasToken()) {
            _uiState.value = ShareUiState(
                isResolving = false,
                error = "Sign in to MixMates Listener first.",
            )
            return
        }

        val url = MusicUrlExtractor.extract(sharedText)
        if (url == null) {
            _uiState.value = ShareUiState(
                isResolving = false,
                error = "No supported music link found. Share a Spotify, Tidal, or Apple Music track link.",
            )
            return
        }

        viewModelScope.launch {
            try {
                val result = recognitionRepository.resolve(url)
                _uiState.value = _uiState.value.copy(
                    isResolving = false,
                    result = result,
                )
                loadGroups()
            } catch (e: RateLimitException) {
                _uiState.value = _uiState.value.copy(
                    isResolving = false,
                    error = "Rate limit reached. Try again in ${e.retryAfterSeconds} seconds.",
                )
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isResolving = false,
                    error = if (e.code() == 400) {
                        "This link type isn't supported. Share a direct track link."
                    } else {
                        "Something went wrong. Check your connection and try again."
                    },
                )
            } catch (e: Exception) {
                Log.e("Share", "Resolve failed", e)
                _uiState.value = _uiState.value.copy(
                    isResolving = false,
                    error = "Something went wrong. Check your connection and try again.",
                )
            }
        }
    }

    private suspend fun loadGroups() {
        try {
            val groups = groupRepository.getGroups()
            _uiState.value = _uiState.value.copy(groups = groups)
        } catch (e: Exception) {
            Log.e("Share", "Failed to load groups", e)
        }
    }

    fun toggleGroup(groupId: String) {
        val current = _uiState.value.selectedGroupIds
        _uiState.value = _uiState.value.copy(
            selectedGroupIds = if (groupId in current) current - groupId else current + groupId,
        )
    }

    fun share() {
        val historyId = _uiState.value.result?.historyId ?: return
        val groupIds = _uiState.value.selectedGroupIds.toList()
        if (groupIds.isEmpty()) return

        _uiState.value = _uiState.value.copy(isSharing = true)
        viewModelScope.launch {
            try {
                val results = historyRepository.share(historyId, groupIds)
                _uiState.value = _uiState.value.copy(
                    isSharing = false,
                    shareResult = results,
                )
            } catch (e: Exception) {
                Log.e("Share", "Failed to share", e)
                _uiState.value = _uiState.value.copy(
                    isSharing = false,
                    error = "Couldn't share — try again",
                )
            }
        }
    }
}
