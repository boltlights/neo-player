package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** v16 -> v17: the Driver Mode table is dropped and everything else survives. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DropDriverModeMigrationTest {

    private val dbName = "drop-driver-mode-test.db"
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

    private fun createV16Database(): SQLiteDatabase {
        val schema = JSONObject(File("schemas/com.example.data.MusicDatabase/16.json").readText()).getJSONObject("database")
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
        db.version = 16
        return db
    }

    @Test
    fun dropsDriverModeTableAndKeepsLibrary() = runBlocking {
        createV16Database().apply {
            execSQL(
                "INSERT INTO tracks (id, title, artist, album, durationMs, filePath, artPath, addedAt, genre, isMetadataPending, lastModified, uid) " +
                    "VALUES (1, 'Keep Me', 'Artist', 'Album', 1000, '/music/keep.mp3', NULL, 1, 'Rock', 0, 0, 's1:abc')"
            )
            execSQL("INSERT INTO playlists (id, name, coverArtUri, createdAt) VALUES (3, 'Mix', NULL, 0)")
            execSQL("INSERT INTO playlist_entries (playlistId, trackUid, position) VALUES (3, 's1:abc', 0)")
            execSQL(
                "INSERT INTO smart_volume_zones (latitude, longitude, radiusMeters, targetVolumePercent, isGradualFade, isActive) " +
                    "VALUES (9.0, 38.7, 200.0, 40.0, 1, 1)"
            )
            close()
        }

        // Opening validates the migrated schema against v17 and fails if anything differs.
        room = Room.databaseBuilder(context, MusicDatabase::class.java, dbName)
            .addMigrations(DropDriverModeMigration)
            .allowMainThreadQueries()
            .build()
        val db = room!!.openHelper.writableDatabase

        val tableExists = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='smart_volume_zones'").use { it.count > 0 }
        assertFalse(tableExists)
        assertEquals(listOf("Keep Me"), room!!.musicDao().getAllTracksSync().map { it.title })
        assertEquals(1, db.query("SELECT COUNT(*) FROM playlist_entries").use { it.moveToFirst(); it.getInt(0) })
    }
}
