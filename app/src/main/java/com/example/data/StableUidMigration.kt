package com.example.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v15 -> v16: switch persisted song references from row IDs to stable UIDs (see [TrackUid]).
 *
 *  - tracks: add `uid`, computed per row with the same [TrackUid] code the app uses at runtime.
 *  - playlist_track_cross_ref(playlistId, trackId, FK cascade to tracks)
 *      -> playlist_entries(playlistId, trackUid, position), no FK to tracks. Positions reproduce the
 *         order playlists were shown in before (track addedAt, newest first).
 *  - playback_history.trackId -> trackUid.
 *  - playback_state queue/current track IDs -> UIDs.
 *
 * CREATE statements mirror Room's exported schema (schemas/.../16.json) exactly; the migration test
 * opens the migrated database with Room, which fails if anything differs.
 */
object StableUidMigration : Migration(15, 16) {

    override fun migrate(db: SupportSQLiteDatabase) {
        val uidById = migrateTracks(db)
        migratePlaylists(db)
        migrateHistory(db)
        migratePlaybackState(db, uidById)
    }

    private fun migrateTracks(db: SupportSQLiteDatabase): Map<Int, String> {
        db.execSQL("ALTER TABLE `tracks` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''")
        val uidById = HashMap<Int, String>()
        db.query(
            "SELECT id, title, artist, album, albumArtist, year, trackNumber, discNumber, musicBrainzId, filePath, sourceUri FROM tracks"
        ).use { c ->
            while (c.moveToNext()) {
                val id = c.getInt(0)
                uidById[id] = TrackUid.compute(
                    title = c.getStringOrNull(1),
                    artist = c.getStringOrNull(2),
                    album = c.getStringOrNull(3),
                    albumArtist = c.getStringOrNull(4),
                    year = c.getIntOrNull(5),
                    trackNumber = c.getIntOrNull(6),
                    discNumber = c.getIntOrNull(7),
                    musicBrainzId = c.getStringOrNull(8),
                    filePath = c.getStringOrNull(9),
                    sourceUri = c.getStringOrNull(10)
                )
            }
        }
        for ((id, uid) in uidById) {
            db.execSQL("UPDATE tracks SET uid = ? WHERE id = ?", arrayOf<Any>(uid, id))
        }
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_uid` ON `tracks` (`uid`)")
        return uidById
    }

    private fun migratePlaylists(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_entries` (`playlistId` INTEGER NOT NULL, `trackUid` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, PRIMARY KEY(`playlistId`, `trackUid`), FOREIGN KEY(`playlistId`) " +
                "REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_entries_playlistId` ON `playlist_entries` (`playlistId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_entries_trackUid` ON `playlist_entries` (`trackUid`)")

        var currentPlaylist = Int.MIN_VALUE
        var position = 0
        db.query(
            "SELECT ref.playlistId, t.uid FROM playlist_track_cross_ref ref " +
                "INNER JOIN tracks t ON t.id = ref.trackId " +
                "ORDER BY ref.playlistId, t.addedAt DESC, t.id"
        ).use { c ->
            while (c.moveToNext()) {
                val playlistId = c.getInt(0)
                if (playlistId != currentPlaylist) {
                    currentPlaylist = playlistId
                    position = 0
                }
                // OR IGNORE: two old rows can map to one UID (identical copies); keep the first.
                db.execSQL(
                    "INSERT OR IGNORE INTO playlist_entries (playlistId, trackUid, position) VALUES (?, ?, ?)",
                    arrayOf<Any>(playlistId, c.getString(1), position++)
                )
            }
        }
        db.execSQL("DROP TABLE IF EXISTS `playlist_track_cross_ref`")
    }

    private fun migrateHistory(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playback_history_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`trackUid` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
        )
        // History of tracks no longer in the library was already invisible (it joined on tracks); drop it.
        db.execSQL(
            "INSERT INTO playback_history_new (id, trackUid, timestamp) " +
                "SELECT h.id, t.uid, h.timestamp FROM playback_history h INNER JOIN tracks t ON t.id = h.trackId"
        )
        db.execSQL("DROP TABLE `playback_history`")
        db.execSQL("ALTER TABLE `playback_history_new` RENAME TO `playback_history`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playback_history_trackUid` ON `playback_history` (`trackUid`)")
    }

    private fun migratePlaybackState(db: SupportSQLiteDatabase, uidById: Map<Int, String>) {
        var converted: Array<Any?>? = null
        db.query(
            "SELECT baseQueueTrackIds, queueTrackIds, currentTrackId, currentPositionMs, isShuffleEnabled, isRepeatEnabled " +
                "FROM playback_state WHERE id = 0"
        ).use { c ->
            if (c.moveToFirst()) {
                fun idsToUids(csv: String?): String = csv.orEmpty().split(",")
                    .mapNotNull { it.trim().toIntOrNull()?.let(uidById::get) }
                    .joinToString(",")
                converted = arrayOf(
                    idsToUids(c.getStringOrNull(0)),
                    idsToUids(c.getStringOrNull(1)),
                    uidById[c.getInt(2)],
                    c.getInt(3),
                    c.getInt(4),
                    c.getInt(5)
                )
            }
        }
        db.execSQL("DROP TABLE IF EXISTS `playback_state`")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playback_state` (`id` INTEGER NOT NULL, `baseQueueTrackUids` TEXT NOT NULL, " +
                "`queueTrackUids` TEXT NOT NULL, `currentTrackUid` TEXT, `currentPositionMs` INTEGER NOT NULL, " +
                "`isShuffleEnabled` INTEGER NOT NULL, `isRepeatEnabled` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        converted?.let { values ->
            db.execSQL(
                "INSERT INTO playback_state (id, baseQueueTrackUids, queueTrackUids, currentTrackUid, currentPositionMs, " +
                    "isShuffleEnabled, isRepeatEnabled) VALUES (0, ?, ?, ?, ?, ?, ?)",
                values
            )
        }
    }

    private fun android.database.Cursor.getStringOrNull(i: Int): String? = if (isNull(i)) null else getString(i)
    private fun android.database.Cursor.getIntOrNull(i: Int): Int? = if (isNull(i)) null else getInt(i)
}
