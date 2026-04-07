package es.mixmat.listener.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.mixmat.listener.data.local.entity.PendingRecognition
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingRecognitionDao {

    @Insert
    suspend fun insert(recognition: PendingRecognition): Long

    @Query("SELECT * FROM pending_recognitions ORDER BY created_at ASC")
    suspend fun getAll(): List<PendingRecognition>

    @Query("SELECT COUNT(*) FROM pending_recognitions")
    fun countFlow(): Flow<Int>

    @Query("DELETE FROM pending_recognitions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_recognitions WHERE created_at < :before")
    suspend fun deleteOlderThan(before: Long)
}
