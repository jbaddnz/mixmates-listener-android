package es.mixmat.listener.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import es.mixmat.listener.data.local.AppDatabase
import es.mixmat.listener.data.local.dao.PendingRecognitionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mixmates_listener.db",
        ).build()

    @Provides
    fun providePendingRecognitionDao(db: AppDatabase): PendingRecognitionDao =
        db.pendingRecognitionDao()
}
