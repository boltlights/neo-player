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
        PlaybackHistoryEntity::class,
        SmartVolumeZoneEntity::class
    ],
    version = 15,
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
                val migrations = (1 until 15).map { version ->
                    object : Migration(version, version + 1) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            if (version == 10) {
                                db.execSQL("CREATE TABLE IF NOT EXISTS `smart_volume_zones` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `radiusMeters` REAL NOT NULL, `targetVolumePercent` REAL NOT NULL, `isGradualFade` INTEGER NOT NULL, `isActive` INTEGER NOT NULL DEFAULT 1)")
                            }
                                                        if (version == 11) {
                                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_title` ON `tracks` (`title`)")
                                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_artist` ON `tracks` (`artist`)")
                                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_album` ON `tracks` (`album`)")
                                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_genre` ON `tracks` (`genre`)")
                            }
                            if (version == 12) {
                                db.execSQL("ALTER TABLE `playback_state` ADD COLUMN `baseQueueTrackIds` TEXT NOT NULL DEFAULT ''")
                                db.execSQL("ALTER TABLE `playback_state` ADD COLUMN `isRepeatEnabled` INTEGER NOT NULL DEFAULT 0")
                            }
                            if (version == 13) {
                                db.execSQL("ALTER TABLE `playlists` ADD COLUMN `coverArtUri` TEXT DEFAULT NULL")
                            }
                            if (version == 14) {
                                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `year` INTEGER DEFAULT NULL")
                                db.execSQL("ALTER TABLE `tracks` ADD COLUMN `customTags` TEXT DEFAULT NULL")
                            }
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
