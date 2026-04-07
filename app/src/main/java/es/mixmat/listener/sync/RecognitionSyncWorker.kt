package es.mixmat.listener.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import es.mixmat.listener.data.repository.RecognitionRepository
import java.io.File

@HiltWorker
class RecognitionSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val recognitionRepository: RecognitionRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        recognitionRepository.cleanupOld()

        val pending = recognitionRepository.getPending()
        if (pending.isEmpty()) return Result.success()

        var allSucceeded = true
        for (item in pending) {
            val file = File(item.audioPath)
            if (!file.exists()) {
                recognitionRepository.removePending(item.id)
                continue
            }

            try {
                recognitionRepository.recognize(file, item.mimeType)
                recognitionRepository.removePending(item.id)
                file.delete()
            } catch (e: Exception) {
                allSucceeded = false
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "recognition_sync"
    }
}
