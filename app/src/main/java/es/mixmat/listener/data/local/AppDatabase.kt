package es.mixmat.listener.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import es.mixmat.listener.data.local.dao.PendingRecognitionDao
import es.mixmat.listener.data.local.entity.PendingRecognition

@Database(
    entities = [PendingRecognition::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingRecognitionDao(): PendingRecognitionDao
}
