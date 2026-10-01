package es.mixmat.listener.ui.sharesheet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.R
import es.mixmat.listener.data.api.ApiException
import es.mixmat.listener.data.api.RateLimitException
import es.mixmat.listener.data.repository.AuthRepository
import es.mixmat.listener.data.repository.GroupRepository
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.domain.model.Group
import es.mixmat.listener.ui.text.UiText
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
 * wrong answers, and it would offer to start another on top of the ones they
 * already have.
 */
sealed interface GroupsState {
    data object Loading : GroupsState

    /**
     * [canCreate] lives here and nowhere else: Start a group renders only from a
     * freshly loaded response that says so. Loading and Failed carry no answer,
     * which is the same as no.
     */
    data class Loaded(val groups: List<Group>, val canCreate: Boolean) : GroupsState

    /** [retryable] is false for a permanent failure, where a Try again button would be a lie. */
    data class Failed(val message: UiText, val retryable: Boolean) : GroupsState
}

/** The start-a-group part of the sheet. */
sealed interface StartGroupState {
    /** Shows the Start a group row, if the loaded response allows it. */
    data object Closed : StartGroupState
    /**
     * [draft] refills the field when it comes back after the name prompt, which
     * took its place and so dropped what was typed.
     */
    data class Naming(
        val isCreating: Boolean = false,
        val error: UiText? = null,
        val draft: String = "",
    ) : StartGroupState
    data class Created(val group: Group) : StartGroupState

    /**
     * `already_has_group`. Shown whatever the refreshed list says, because that
     * refresh will normally come back with `can_create: false`.
     */
    data class Refused(val message: UiText) : StartGroupState
}

/** Asking for a display name after `name_required`, in place of the rest of the sheet. */
data class NamePrompt(
    val isSaving: Boolean = false,
    val error: UiText? = null,
    /** Refused a create rather than a share, which changes the button's wording. */
    val forCreate: Boolean = false,
)

data class TrackShareUiState(
    val groups: GroupsState = GroupsState.Loading,
    val selectedGroupIds: Set<String> = emptySet(),
    val isSharing: Boolean = false,
    val shareResult: Map<String, String>? = null,
    val shareError: UiText? = null,
    val startGroup: StartGroupState = StartGroupState.Closed,
    val namePrompt: NamePrompt? = null,
)

/**
 * The single owner of group sharing, for the result screen, history detail and
 * the inbound-share screen alike. Replaces the two inline pickers that had
 * started to drift apart. Also where a group is started, since this is the one
 * place the app loads `GET /groups` fresh.
 */
@HiltViewModel
class TrackShareViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val historyRepository: HistoryRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackShareUiState())
    val uiState: StateFlow<TrackShareUiState> = _uiState

    private var preselecting: Set<String> = emptySet()

    /** What `name_required` refused, held so it can be retried once a name is set. */
    private sealed interface RefusedAction {
        data class Share(val historyId: String, val groupIds: List<String>) : RefusedAction
        data class Create(val name: String) : RefusedAction
    }

    private var refused: RefusedAction? = null

    /**
     * Resets to loading and fetches the groups. Called every time the sheet opens,
     * which is what returns a reopened sheet to the picker rather than whatever it
     * was last showing.
     */
    fun load(preselecting: Set<String> = emptySet()) {
        this.preselecting = preselecting
        refused = null
        _uiState.value = TrackShareUiState(groups = GroupsState.Loading)

        viewModelScope.launch {
            try {
                val list = groupRepository.getGroups()
                // Intersect: a group the track was shared into but which the
                // endpoint no longer returns must not stay selected. Left in it
                // ticks nothing on screen yet enables Share, then either fails the
                // whole post with a 403 or comes back with an id we cannot name.
                val available = list.groups.map { it.id }.toSet()
                _uiState.value = _uiState.value.copy(
                    groups = GroupsState.Loaded(list.groups, list.canCreate),
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
        share(historyId, groupIds, afterName = false)
    }

    /** [afterName] marks the one retry after a name was set, so it cannot loop. */
    private fun share(historyId: String, groupIds: List<String>, afterName: Boolean) {
        _uiState.value = _uiState.value.copy(isSharing = true, shareError = null)
        viewModelScope.launch {
            try {
                val results = historyRepository.share(historyId, groupIds)
                _uiState.value = _uiState.value.copy(isSharing = false, shareResult = results)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to share", e)
                if (!afterName && e.isNameRequired()) {
                    askForName(RefusedAction.Share(historyId, groupIds))
                    _uiState.value = _uiState.value.copy(isSharing = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSharing = false,
                        shareError = e.toShareError(),
                    )
                    // Left the group: re-read so it drops out of the picker and
                    // the selection, as iOS does.
                    if (e is ApiException && e.code == CODE_NOT_FOUND) refreshGroups(select = null)
                }
            }
        }
    }

    /** Backs "Share somewhere else": drops the result and shows the picker again. */
    fun backToPicker() {
        _uiState.value = _uiState.value.copy(shareResult = null, shareError = null)
    }

    // -- Start a group --

    fun openStartGroup() {
        val groups = _uiState.value.groups
        if (groups is GroupsState.Loaded && groups.canCreate) {
            _uiState.value = _uiState.value.copy(startGroup = StartGroupState.Naming())
        }
    }

    /** Also backs "Share this track to it": the new group is already ticked in the picker. */
    fun closeStartGroup() {
        _uiState.value = _uiState.value.copy(startGroup = StartGroupState.Closed)
    }

    fun createGroup(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        createGroup(trimmed, afterName = false)
    }

    private fun createGroup(name: String, afterName: Boolean) {
        _uiState.value = _uiState.value.copy(
            startGroup = StartGroupState.Naming(isCreating = true, draft = name),
        )
        viewModelScope.launch {
            try {
                val group = groupRepository.createGroup(name)
                _uiState.value = _uiState.value.copy(startGroup = StartGroupState.Created(group))
                refreshGroups(select = group)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create group", e)
                when {
                    !afterName && e.isNameRequired() -> {
                        askForName(RefusedAction.Create(name))
                        _uiState.value = _uiState.value.copy(
                            startGroup = StartGroupState.Naming(draft = name),
                        )
                    }
                    e is ApiException && e.code == CODE_ALREADY_HAS_GROUP -> {
                        _uiState.value = _uiState.value.copy(
                            startGroup = StartGroupState.Refused(UiText(R.string.sharesheet_already_has_group)),
                        )
                        refreshGroups(select = null)
                    }
                    else -> _uiState.value = _uiState.value.copy(
                        startGroup = StartGroupState.Naming(error = e.toCreateError(), draft = name),
                    )
                }
            }
        }
    }

    /**
     * Re-reads the list in place, without going back through Loading, so the
     * picker keeps its selection. [select] ticks a just-created group, ready to
     * share into.
     *
     * If the re-read fails after a create, the new group is added locally with
     * `canCreate` false: the server has just said yes to this account's one
     * group, so that is the one answer it cannot have changed to allow.
     */
    private suspend fun refreshGroups(select: Group?) {
        val current = _uiState.value
        try {
            val list = groupRepository.getGroups()
            val available = list.groups.map { it.id }.toSet()
            val selected = current.selectedGroupIds + listOfNotNull(select?.id)
            _uiState.value = _uiState.value.copy(
                groups = GroupsState.Loaded(list.groups, list.canCreate),
                selectedGroupIds = selected intersect available,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh groups", e)
            val loaded = current.groups as? GroupsState.Loaded ?: return
            if (select == null) return
            _uiState.value = _uiState.value.copy(
                groups = GroupsState.Loaded(loaded.groups + select, canCreate = false),
                selectedGroupIds = current.selectedGroupIds + select.id,
            )
        }
    }

    // -- Names --

    private fun askForName(action: RefusedAction) {
        refused = action
        _uiState.value = _uiState.value.copy(
            namePrompt = NamePrompt(forCreate = action is RefusedAction.Create),
        )
    }

    /** Sets the display name, then retries whatever was refused, once. */
    fun submitName(name: String) {
        val trimmed = name.trim()
        val prompt = _uiState.value.namePrompt ?: return
        if (trimmed.isEmpty()) return
        _uiState.value = _uiState.value.copy(namePrompt = prompt.copy(isSaving = true, error = null))

        viewModelScope.launch {
            try {
                authRepository.setDisplayName(trimmed)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set display name", e)
                _uiState.value = _uiState.value.copy(
                    namePrompt = prompt.copy(isSaving = false, error = e.toNameError()),
                )
                return@launch
            }

            val action = refused
            refused = null
            _uiState.value = _uiState.value.copy(namePrompt = null)
            when (action) {
                is RefusedAction.Share -> share(action.historyId, action.groupIds, afterName = true)
                is RefusedAction.Create -> createGroup(action.name, afterName = true)
                null -> Unit
            }
        }
    }

    /** Back to wherever the refused action left off, with nothing retried. */
    fun cancelName() {
        refused = null
        _uiState.value = _uiState.value.copy(namePrompt = null)
    }

    private fun Throwable.isNameRequired() =
        this is ApiException && code == CODE_NAME_REQUIRED

    /**
     * `auth_listen_disabled` is an admin kill-flag that arrives from the shared
     * auth preamble, so it can land on this endpoint and it is permanent. Anything
     * else is worth another go.
     */
    private fun Throwable.toGroupsFailure(): GroupsState.Failed =
        if (this is ApiException && code == CODE_LISTEN_DISABLED) {
            GroupsState.Failed(LISTEN_DISABLED, retryable = false)
        } else {
            GroupsState.Failed(UiText(R.string.sharesheet_load_failed), retryable = true)
        }

    /**
     * Keyed on the server's error code, never the HTTP status: 403 here carries
     * four different codes and they need different sentences. Keying on 403
     * would tell someone who has simply left a group that it has stopped
     * accepting tracks. `name_required` only lands here on the retry after a
     * name was set, where asking again would loop.
     */
    private fun Throwable.toShareError(): UiText {
        if (this !is ApiException) return GENERIC_SHARE_FAILURE
        return when (code) {
            CODE_GROUP_LOCKED -> UiText(R.string.sharesheet_group_locked)
            CODE_NOT_FOUND -> UiText(R.string.sharesheet_not_a_member)
            CODE_LISTEN_DISABLED -> LISTEN_DISABLED
            else -> GENERIC_SHARE_FAILURE
        }
    }

    private fun Throwable.toCreateError(): UiText {
        if (this is RateLimitException) return TOO_MANY_TRIES
        if (this !is ApiException) return CREATE_FAILED
        return when (code) {
            CODE_NAME_TAKEN -> UiText(R.string.sharesheet_name_taken)
            CODE_LISTEN_DISABLED -> LISTEN_DISABLED
            else -> CREATE_FAILED
        }
    }

    private fun Throwable.toNameError(): UiText = when {
        this is RateLimitException -> TOO_MANY_TRIES
        this is ApiException && code == CODE_PRIVATE_RELAY -> UiText(R.string.sharesheet_private_relay)
        else -> UiText(R.string.sharesheet_name_save_failed)
    }

    private companion object {
        const val TAG = "TrackShare"

        const val CODE_GROUP_LOCKED = "group_locked"

        /** 403 with this code means "not a member of the group", not a missing route. */
        const val CODE_NOT_FOUND = "not_found"
        const val CODE_LISTEN_DISABLED = "auth_listen_disabled"
        const val CODE_NAME_REQUIRED = "name_required"
        const val CODE_NAME_TAKEN = "name_taken"
        const val CODE_ALREADY_HAS_GROUP = "already_has_group"
        const val CODE_PRIVATE_RELAY = "private_relay_name"

        val GENERIC_SHARE_FAILURE = UiText(R.string.sharesheet_share_failed)
        val LISTEN_DISABLED = UiText(R.string.sharesheet_listen_disabled)
        val CREATE_FAILED = UiText(R.string.sharesheet_create_failed)
        val TOO_MANY_TRIES = UiText(R.string.sharesheet_too_many_tries)
    }
}
