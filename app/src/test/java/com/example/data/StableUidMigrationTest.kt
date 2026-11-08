package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Builds a real v15 database from the exported schema, fills it like an existing install, and lets
 * Room open it with [StableUidMigration]. Room validates the migrated schema against v16 on open and
 * throws if anything differs; the assertions then check the data survived.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StableUidMigrationTest {

    private val dbName = "migration-test.db"
    private lateinit var context: Context
    private var room: MusicDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        room?.close()
        context.deleteDatabase(dbName)
    }

    private fun createV15Database(): SQLiteDatabase {
        val schema = JSONObject(File("schemas/com.example.data.MusicDatabase/15.json").readText()).getJSONObject("database")
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(dbName).apply { parentFile?.mkdirs() }, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val e = entities.getJSONObject(i)
            val table = e.getString("tableName")
            db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
            e.optJSONArray("indices")?.let { idx ->
                for (j in 0 until idx.length()) db.execSQL(idx.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            e.optJSONArray("contentSyncTriggers")?.let { t -> for (j in 0 until t.length()) db.execSQL(t.getString(j)) }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        db.version = 15
        return db
    }

    private fun insertTrack(db: SQLiteDatabase, id: Int, title: String, addedAt: Long, album: String = "Album", path: String = "/storage/emulated/0/Music/$title.mp3") {
        db.execSQL(
            "INSERT INTO tracks (id, title, artist, album, durationMs, filePath, artPath, addedAt, genre, isMetadataPending, lastModified) " +
                "VALUES (?, ?, 'Artist', ?, 1000, ?, NULL, ?, 'Rock', 0, 0)",
            arrayOf<Any>(id, title, album, path, addedAt)
        )
    }

    @Test
    fun migratesPlaylistsHistoryAndQueueToStableUids() = runBlocking {
        createV15Database().apply {
            insertTrack(this, 1, "Old Song", addedAt = 100)
            insertTrack(this, 2, "New Song", addedAt = 300)
            insertTrack(this, 3, "Middle Song", addedAt = 200)
            execSQL("INSERT INTO playlists (id, name, coverArtUri, createdAt) VALUES (7, 'Mix', NULL, 0)")
            execSQL("INSERT INTO playlist_track_cross_ref (playlistId, trackId) VALUES (7, 1), (7, 2), (7, 3)")
            execSQL("INSERT INTO playback_history (id, trackId, timestamp) VALUES (1, 2, 10), (2, 2, 20), (3, 1, 30)")
            execSQL(
                "INSERT INTO playback_state (id, baseQueueTrackIds, queueTrackIds, currentTrackId, currentPositionMs, isShuffleEnabled, isRepeatEnabled) " +
                    "VALUES (0, '1,2,3', '3,1,2', 1, 4242, 1, 0)"
            )
            close()
        }

        // Opening runs the migration and validates the resulting schema against v16.
        room = Room.databaseBuilder(context, MusicDatabase::class.java, dbName)
            .addMigrations(StableUidMigration, DropDriverModeMigration)
            .allowMainThreadQueries()
            .build()
        val dao = room!!.musicDao()

        val tracks = dao.getAllTracksSync()
        assertEquals(3, tracks.size)
        tracks.forEach { assertEquals(TrackUid.of(it), it.uid) }

        // Playlist kept, in its previous display order (newest addedAt first).
        assertEquals(listOf("New Song", "Middle Song", "Old Song"), dao.getTracksForPlaylist(7).first().map { it.title })

        // History converted: "New Song" played twice is the most played.
        assertEquals("New Song", dao.getMostPlayed().first().first().title)

        // Saved queue converted to UIDs, order and position intact.
        val state = dao.getPlaybackState()
        assertNotNull(state)
        val byId = tracks.associateBy { it.id }
        assertEquals(listOf(byId[3]!!.uid, byId[1]!!.uid, byId[2]!!.uid).joinToString(","), state!!.queueTrackUids)
        assertEquals(byId[1]!!.uid, state.currentTrackUid)
        assertEquals(4242, state.currentPositionMs)
        assertTrue(state.isShuffleEnabled)
    }

    @Test
    fun emptyDatabaseMigratesCleanly() = runBlocking {
        createV15Database().close()
        room = Room.databaseBuilder(context, MusicDatabase::class.java, dbName)
            .addMigrations(StableUidMigration, DropDriverModeMigration)
            .allowMainThreadQueries()
            .build()
        assertTrue(room!!.musicDao().getAllTracksSync().isEmpty())
        assertEquals(null, room!!.musicDao().getPlaybackState())
    }
}
