package es.mixmat.listener.data.repository

import es.mixmat.listener.data.api.ListenerApi
import es.mixmat.listener.data.api.dto.ResolveRequest
import es.mixmat.listener.data.api.toDomain
import es.mixmat.listener.data.local.dao.PendingRecognitionDao
import es.mixmat.listener.data.local.entity.PendingRecognition
import es.mixmat.listener.domain.model.RecognitionResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecognitionRepository @Inject constructor(
    private val api: ListenerApi,
    private val pendingDao: PendingRecognitionDao,
) {
    suspend fun resolve(url: String, groupId: String? = null): RecognitionResult =
        api.resolve(ResolveRequest(url = url, groupId = groupId)).data.toDomain()

    suspend fun recognize(audioFile: File, mimeType: String): RecognitionResult {
        val requestBody = audioFile.asRequestBody(mimeType.toMediaType())
        val part = MultipartBody.Part.createFormData("audio", audioFile.name, requestBody)
        return api.recognize(part).data.toDomain()
    }

    suspend fun queueForLater(audioPath: String, mimeType: String) {
        pendingDao.insert(
            PendingRecognition(audioPath = audioPath, mimeType = mimeType),
        )
    }

    suspend fun getPending(): List<PendingRecognition> = pendingDao.getAll()

    suspend fun removePending(id: Long) = pendingDao.delete(id)

    suspend fun cleanupOld(maxAgeMs: Long = 48 * 60 * 60 * 1000) {
        pendingDao.deleteOlderThan(System.currentTimeMillis() - maxAgeMs)
    }
}
