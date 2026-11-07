#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/data/MusicDatabase.kt
package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TrackEntity::class,
        TrackFtsEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlaybackStateEntity::class,
        PlaybackHistoryEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return it }

                val migrations = (1 until 10).map { version ->
                    object : Migration(version, version + 1) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            // Empty migration for safety
                        }
                    }
                }.toTypedArray()

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_database"
                )
                .addMigrations(*migrations)
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
INNER_EOF
