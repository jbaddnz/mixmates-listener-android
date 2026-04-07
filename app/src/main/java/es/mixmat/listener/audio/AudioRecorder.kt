package es.mixmat.listener.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class RecorderState {
    IDLE, RECORDING, DONE, ERROR
}

@Singleton
class AudioRecorder @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    private val _state = MutableStateFlow(RecorderState.IDLE)
    val state: StateFlow<RecorderState> = _state

    fun start(): File {
        val file = File(context.cacheDir, "listen_${System.currentTimeMillis()}.mp4")
        outputFile = file

        recorder = createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128_000)
            setAudioChannels(1)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        _state.value = RecorderState.RECORDING
        return file
    }

    fun stop(): File? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            _state.value = RecorderState.DONE
            outputFile
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            _state.value = RecorderState.ERROR
            null
        }
    }

    fun reset() {
        recorder?.release()
        recorder = null
        _state.value = RecorderState.IDLE
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

    companion object {
        const val MIME_TYPE = "audio/mp4"
        const val RECORD_DURATION_MS = 11_000L
    }
}
