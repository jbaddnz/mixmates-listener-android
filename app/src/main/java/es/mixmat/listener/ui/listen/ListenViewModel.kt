package es.mixmat.listener.ui.listen

import android.util.Log
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.R
import es.mixmat.listener.audio.AudioRecorder
import es.mixmat.listener.audio.RecorderState
import es.mixmat.listener.data.api.RateLimitException
import es.mixmat.listener.data.prefs.ListenerPrefs
import es.mixmat.listener.data.repository.AuthRepository
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.data.repository.RecognitionRepository
import es.mixmat.listener.data.session.UnseenTracks
import es.mixmat.listener.domain.model.RecognitionResult
import es.mixmat.listener.domain.model.UserProfile
import es.mixmat.listener.ui.text.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ListenUiState(
    val profile: UserProfile? = null,
    val recorderState: RecorderState = RecorderState.IDLE,
    val recordingProgress: Float = 0f,
    val isSubmitting: Boolean = false,
    val result: RecognitionResult? = null,
    val error: UiText? = null,
    val queuedOffline: Boolean = false,
    val hasAudioPermission: Boolean = false,
    val permissionDenied: Boolean = false,
    val reported: Boolean = false,
    val isReporting: Boolean = false,
    val hasUnseenTracks: Boolean = false,
)

@HiltViewModel
class ListenViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val recognitionRepository: RecognitionRepository,
    private val authRepository: AuthRepository,
    private val historyRepository: HistoryRepository,
    private val connectivityManager: ConnectivityManager,
    private val listenerPrefs: ListenerPrefs,
    private val unseenTracks: UnseenTracks,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListenUiState())
    val uiState: StateFlow<ListenUiState> = _uiState

    init {
        loadProfile()
        // Follows the repository rather than keeping its own copy, so a name set
        // in the share sheet shows in the greeting straight away.
        viewModelScope.launch {
            authRepository.profile.collect { profile ->
                if (profile != null) _uiState.value = _uiState.value.copy(profile = profile)
            }
        }
        viewModelScope.launch {
            audioRecorder.state.collect { state ->
                _uiState.value = _uiState.value.copy(recorderState = state)
            }
        }
        viewModelScope.launch {
            unseenTracks.hasUnseen.collect { hasUnseen ->
                _uiState.value = _uiState.value.copy(hasUnseenTracks = hasUnseen)
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                // Lands in uiState through the profile flow collected in init.
                authRepository.getProfile()
            } catch (e: Exception) {
                Log.e("Listen", "Failed to load profile", e)
                _uiState.value = _uiState.value.copy(
                    error = UiText(R.string.listen_error_verify),
                )
            }
        }
    }

    fun startListening() {
        _uiState.value = _uiState.value.copy(
            result = null,
            error = null,
            queuedOffline = false,
            recordingProgress = 0f,
        )

        viewModelScope.launch {
            // Read at record start, not at construction, so a settings change
            // applies to the very next capture with no restart.
            val durationMs = listenerPrefs.getRecordingLengthSeconds() * 1000L

            try {
                withContext(Dispatchers.IO) {
                    audioRecorder.start()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = UiText(R.string.listen_error_mic))
                return@launch
            }

            val startTime = System.currentTimeMillis()
            while (audioRecorder.state.value == RecorderState.RECORDING) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                _uiState.value = _uiState.value.copy(recordingProgress = progress)

                if (elapsed >= durationMs) {
                    stopAndSubmit()
                    return@launch
                }
                delay(50)
            }
        }
    }

    fun stopAndSubmit() {
        val file = audioRecorder.stop() ?: run {
            _uiState.value = _uiState.value.copy(error = UiText(R.string.listen_error_recording))
            return
        }

        if (!isOnline()) {
            viewModelScope.launch {
                recognitionRepository.queueForLater(file.absolutePath, AudioRecorder.MIME_TYPE)
                _uiState.value = _uiState.value.copy(queuedOffline = true)
            }
            return
        }

        _uiState.value = _uiState.value.copy(isSubmitting = true)

        viewModelScope.launch {
            try {
                val result = recognitionRepository.recognize(file, AudioRecorder.MIME_TYPE)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    result = result,
                )
                // A history id means it landed somewhere, so mark History unseen.
                // A duplicate counts; a no-match does not.
                if (result.historyId != null) unseenTracks.markUnseen()
                file.delete()
            } catch (e: RateLimitException) {
                Log.w("Listen", "Rate limited, retry after ${e.retryAfterSeconds}s")
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = UiText(R.string.listen_error_rate_limit, e.retryAfterSeconds),
                )
                file.delete()
            } catch (e: Exception) {
                Log.e("Listen", "Recognition failed, queuing for retry", e)
                recognitionRepository.queueForLater(file.absolutePath, AudioRecorder.MIME_TYPE)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = UiText(R.string.listen_error_recognition),
                    queuedOffline = true,
                )
            }
        }
    }

    fun reportWrongMatch() {
        val historyId = _uiState.value.result?.historyId ?: return
        _uiState.value = _uiState.value.copy(isReporting = true)
        viewModelScope.launch {
            try {
                historyRepository.report(historyId)
                _uiState.value = _uiState.value.copy(
                    isReporting = false,
                    reported = true,
                )
            } catch (e: Exception) {
                Log.e("Listen", "Report failed", e)
                _uiState.value = _uiState.value.copy(
                    isReporting = false,
                    error = UiText(R.string.listen_error_report),
                )
            }
        }
    }

    fun dismiss() {
        audioRecorder.reset()
        _uiState.value = ListenUiState(
            profile = _uiState.value.profile,
            hasAudioPermission = _uiState.value.hasAudioPermission,
            // Carried over deliberately: listening again does not make the earlier
            // track any more seen. The StateFlow would not re-emit to restore it,
            // since its value hasn't changed.
            hasUnseenTracks = _uiState.value.hasUnseenTracks,
        )
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasAudioPermission = granted,
            permissionDenied = !granted,
        )
        if (granted) startListening()
    }

    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
