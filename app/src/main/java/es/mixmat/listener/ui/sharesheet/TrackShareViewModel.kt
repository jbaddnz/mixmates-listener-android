package es.mixmat.listener.ui.sharesheet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.data.api.ApiException
import es.mixmat.listener.data.repository.GroupRepository
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.domain.model.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Loading, loaded and failed, kept as three separate things on purpose.
 *
 * Empty is [Loaded] with an empty list, so there is no way to render "no groups
 * yet" for a fetch that failed without writing a new branch deliberately.
 * Telling someone with ten groups that they have none is the worse of the two
 * wrong answers, and once group creation lands it would offer to create another
 * on top of the ones they already have.
 */
sealed interface GroupsState {
    data object Loading : GroupsState
    data class Loaded(val groups: List<Group>) : GroupsState

    /** [retryable] is false for a permanent failure, where a Try again button would be a lie. */
    data class Failed(val message: String, val retryable: Boolean) : GroupsState
}

data class TrackShareUiState(
    val groups: GroupsState = GroupsState.Loading,
    val selectedGroupIds: Set<String> = emptySet(),
    val isSharing: Boolean = false,
    val shareResult: Map<String, String>? = null,
    val shareError: String? = null,
)

/**
 * The single owner of group sharing, for the result screen, history detail and
 * the inbound-share screen alike. Replaces the two inline pickers that had
 * started to drift apart.
 */
@HiltViewModel
class TrackShareViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackShareUiState())
    val uiState: StateFlow<TrackShareUiState> = _uiState

    private var preselecting: Set<String> = emptySet()

    /**
     * Resets to loading and fetches the groups. Called every time the sheet opens,
     * which is what returns a reopened sheet to the picker rather than whatever it
     * was last showing.
     */
    fun load(preselecting: Set<String> = emptySet()) {
        this.preselecting = preselecting
        _uiState.value = TrackShareUiState(groups = GroupsState.Loading)

        viewModelScope.launch {
            try {
                val groups = groupRepository.getGroups()
                // Intersect: a group the track was shared into but which the
                // endpoint no longer returns must not stay selected. Left in it
                // ticks nothing on screen yet enables Share, then either fails the
                // whole post with a 403 or comes back with an id we cannot name.
                val available = groups.map { it.id }.toSet()
                _uiState.value = _uiState.value.copy(
                    groups = GroupsState.Loaded(groups),
                    selectedGroupIds = preselecting intersect available,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load groups", e)
                _uiState.value = _uiState.value.copy(groups = e.toGroupsFailure())
            }
        }
    }

    fun retry() = load(preselecting)

    fun toggleGroup(groupId: String) {
        val current = _uiState.value.selectedGroupIds
        _uiState.value = _uiState.value.copy(
            selectedGroupIds = if (groupId in current) current - groupId else current + groupId,
            // Changing the selection makes the previous failure stale.
            shareError = null,
        )
    }

    fun share(historyId: String) {
        val groupIds = _uiState.value.selectedGroupIds.toList()
        if (groupIds.isEmpty()) return

        _uiState.value = _uiState.value.copy(isSharing = true, shareError = null)
        viewModelScope.launch {
            try {
                val results = historyRepository.share(historyId, groupIds)
                _uiState.value = _uiState.value.copy(isSharing = false, shareResult = results)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to share", e)
                _uiState.value = _uiState.value.copy(
                    isSharing = false,
                    shareError = e.toShareError(),
                )
            }
        }
    }

    /** Backs "Share somewhere else": drops the result and shows the picker again. */
    fun backToPicker() {
        _uiState.value = _uiState.value.copy(shareResult = null, shareError = null)
    }

    /**
     * `auth_listen_disabled` is an admin kill-flag that arrives from the shared
     * auth preamble, so it can land on this endpoint and it is permanent. Anything
     * else is worth another go.
     */
    private fun Throwable.toGroupsFailure(): GroupsState.Failed =
        if (this is ApiException && code == CODE_LISTEN_DISABLED) {
            GroupsState.Failed(LISTEN_DISABLED, retryable = false)
        } else {
            GroupsState.Failed("Couldn't load your groups", retryable = true)
        }

    /**
     * Keyed on the server's error code, never the HTTP status: 403 here carries
     * three different codes and they need three different sentences. Keying on 403
     * would tell someone who has simply left a group that it has stopped accepting
     * tracks.
     */
    private fun Throwable.toShareError(): String {
        if (this !is ApiException) return GENERIC_SHARE_FAILURE
        return when (code) {
            CODE_GROUP_LOCKED -> "This group is no longer accepting new tracks"
            CODE_NOT_FOUND -> "You're no longer in that group"
            CODE_LISTEN_DISABLED -> LISTEN_DISABLED
            else -> GENERIC_SHARE_FAILURE
        }
    }

    private companion object {
        const val TAG = "TrackShare"

        const val CODE_GROUP_LOCKED = "group_locked"

        /** 403 with this code means "not a member of the group", not a missing route. */
        const val CODE_NOT_FOUND = "not_found"
        const val CODE_LISTEN_DISABLED = "auth_listen_disabled"

        /** Two sentences, no dash: em and en dashes are out in this flow. */
        const val GENERIC_SHARE_FAILURE = "Couldn't share. Try again."
        const val LISTEN_DISABLED = "Listening isn't enabled on this account"
    }
}
