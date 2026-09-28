package es.mixmat.listener.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether a recognition has landed that History hasn't been opened since.
 *
 * Answers "where did my track go?" at the moment the question forms, which is the
 * first-run gap this release set out to close.
 *
 * Deliberately not persisted. Anything surviving a relaunch needs a rule for when
 * it expires, and the question only arises in the session that caught the track —
 * so a `@Singleton`, which dies with the process, is exactly the right lifetime.
 * It survives Activity recreation, which a composable's `remember` would not.
 */
@Singleton
class UnseenTracks @Inject constructor() {

    private val _hasUnseen = MutableStateFlow(false)
    val hasUnseen: StateFlow<Boolean> = _hasUnseen

    /**
     * Called for any result carrying a history id, so a duplicate counts (it is
     * still a track that went somewhere) and a no-match does not. Surviving
     * "Listen again" is intentional: listening again does not make the earlier
     * track any more seen.
     */
    fun markUnseen() {
        _hasUnseen.value = true
    }

    fun clear() {
        _hasUnseen.value = false
    }
}
