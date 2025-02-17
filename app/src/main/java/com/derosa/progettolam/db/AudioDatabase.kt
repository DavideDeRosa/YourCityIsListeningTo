package com.derosa.progettolam.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AudioDataEntity::class, UploadDataEntity::class, AllAudioDataEntity::class],
    version = 3
)
abstract class AudioDatabase : RoomDatabase() {

    abstract fun audioDataDao(): AudioDataDao
    abstract fun uploadDataDao(): UploadDataDao
    abstract fun allAudioDataDao(): AllAudioDataDao

    companion object {
        @Volatile
        var INSTANCE: AudioDatabase? = null

        @Synchronized
        fun getInstance(context: Context): AudioDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context,
                    AudioDatabase::class.java,
                    "audiodata.db"
                ).fallbackToDestructiveMigration()
                    .build()
            }
            return INSTANCE as AudioDatabase
        }
    }
}