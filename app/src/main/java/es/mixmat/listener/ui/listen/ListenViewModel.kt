package es.mixmat.listener.ui.listen

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.mixmat.listener.audio.AudioRecorder
import es.mixmat.listener.audio.RecorderState
import es.mixmat.listener.data.repository.AuthRepository
import es.mixmat.listener.data.repository.RecognitionRepository
import es.mixmat.listener.domain.model.RecognitionResult
import es.mixmat.listener.domain.model.UserProfile
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
    val error: String? = null,
    val queuedOffline: Boolean = false,
)

@HiltViewModel
class ListenViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val recognitionRepository: RecognitionRepository,
    private val authRepository: AuthRepository,
    private val connectivityManager: ConnectivityManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListenUiState())
    val uiState: StateFlow<ListenUiState> = _uiState

    init {
        loadProfile()
        viewModelScope.launch {
            audioRecorder.state.collect { state ->
                _uiState.value = _uiState.value.copy(recorderState = state)
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val profile = authRepository.getProfile()
                _uiState.value = _uiState.value.copy(profile = profile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Could not verify your account. Check your connection.",
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
            try {
                withContext(Dispatchers.IO) {
                    audioRecorder.start()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Could not access microphone")
                return@launch
            }

            val startTime = System.currentTimeMillis()
            while (audioRecorder.state.value == RecorderState.RECORDING) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / AudioRecorder.RECORD_DURATION_MS).coerceIn(0f, 1f)
                _uiState.value = _uiState.value.copy(recordingProgress = progress)

                if (elapsed >= AudioRecorder.RECORD_DURATION_MS) {
                    stopAndSubmit()
                    return@launch
                }
                delay(50)
            }
        }
    }

    fun stopAndSubmit() {
        val file = audioRecorder.stop() ?: run {
            _uiState.value = _uiState.value.copy(error = "Recording failed")
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
                file.delete()
            } catch (e: Exception) {
                recognitionRepository.queueForLater(file.absolutePath, AudioRecorder.MIME_TYPE)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Recognition failed. Queued for retry.",
                    queuedOffline = true,
                )
            }
        }
    }

    fun dismiss() {
        audioRecorder.reset()
        _uiState.value = ListenUiState(profile = _uiState.value.profile)
    }

    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
