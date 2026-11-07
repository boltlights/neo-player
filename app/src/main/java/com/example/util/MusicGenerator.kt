package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.example.data.MusicDao
import com.example.data.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MusicGenerator {

    suspend fun generateDefaultTracksIfEmpty(context: Context, musicDao: MusicDao) = withContext(Dispatchers.IO) {
        // Only generate default tracks if the track list is completely empty
        val existingTracks = musicDao.getAllTracks().first()
        if (existingTracks.isNotEmpty()) {
            return@withContext
        }

        val songsDir = File(context.filesDir, "ambient_songs")
        songsDir.mkdirs()
        val artDir = File(context.filesDir, "ambient_art")
        artDir.mkdirs()

        // Generate 5 default high-quality ambient synth tracks
        createAndInsertTrack(
            context = context,
            musicDao = musicDao,
            songsDir = songsDir,
            artDir = artDir,
            title = "Celestial Hum",
            artist = "Aether Ambient",
            album = "Cosmic Spheres",
            frequencyHz = 120.0,
            durationSeconds = 30,
            color1 = Color.parseColor("#1B1F38"),
            color2 = Color.parseColor("#4A0E4E"),
            filename = "celestial_hum.wav",
            artFilename = "celestial_hum.png"
        )

        createAndInsertTrack(
            context = context,
            musicDao = musicDao,
            songsDir = songsDir,
            artDir = artDir,
            title = "Ethereal Chords",
            artist = "Soma Drone Project",
            album = "Soma Harmonics",
            frequencyHz = 220.0,
            durationSeconds = 35,
            color1 = Color.parseColor("#0F2A30"),
            color2 = Color.parseColor("#1E824C"),
            filename = "ethereal_chords.wav",
            artFilename = "ethereal_chords.png"
        )

        createAndInsertTrack(
            context = context,
            musicDao = musicDao,
            songsDir = songsDir,
            artDir = artDir,
            title = "Deep Space",
            artist = "Cosminaut",
            album = "Unbounded Voids",
            frequencyHz = 80.0,
            durationSeconds = 40,
            color1 = Color.parseColor("#2C001E"),
            color2 = Color.parseColor("#0F172A"),
            filename = "deep_space.wav",
            artFilename = "deep_space.png"
        )

        createAndInsertTrack(
            context = context,
            musicDao = musicDao,
            songsDir = songsDir,
            artDir = artDir,
            title = "Solar Wind",
            artist = "Solaric",
            album = "Heliosphere",
            frequencyHz = 150.0,
            durationSeconds = 45,
            color1 = Color.parseColor("#141A29"),
            color2 = Color.parseColor("#D97706"),
            filename = "solar_wind.wav",
            artFilename = "solar_wind.png"
        )

        createAndInsertTrack(
            context = context,
            musicDao = musicDao,
            songsDir = songsDir,
            artDir = artDir,
            title = "Nebula Dream",
            artist = "Nebula Drift",
            album = "Stardust Lullabies",
            frequencyHz = 180.0,
            durationSeconds = 50,
            color1 = Color.parseColor("#2E1065"),
            color2 = Color.parseColor("#EC4899"),
            filename = "nebula_dream.wav",
            artFilename = "nebula_dream.png"
        )
    }

    private suspend fun createAndInsertTrack(
        context: Context,
        musicDao: MusicDao,
        songsDir: File,
        artDir: File,
        title: String,
        artist: String,
        album: String,
        frequencyHz: Double,
        durationSeconds: Int,
        color1: Int,
        color2: Int,
        filename: String,
        artFilename: String
    ) {
        val songFile = File(songsDir, filename)
        if (!songFile.exists()) {
            generateAmbientTone(songFile, frequencyHz, durationSeconds)
        }

        val artFile = File(artDir, artFilename)
        if (!artFile.exists()) {
            generateGeometricArtwork(artFile, title, color1, color2)
        }

        val track = TrackEntity(
            title = title,
            artist = artist,
            album = album,
            durationMs = (durationSeconds * 1000).toLong(),
            filePath = songFile.absolutePath,
            artPath = artFile.absolutePath
        )
        musicDao.insertTrack(track)
    }

    suspend fun readdSampleTracks(context: Context, musicDao: MusicDao) = withContext(Dispatchers.IO) {
        val songsDir = File(context.filesDir, "ambient_songs")
        songsDir.mkdirs()
        val artDir = File(context.filesDir, "ambient_art")
        artDir.mkdirs()

        // Force generate them (or if files exist, just reuse them) and insert them!
        val songSpecs = listOf(
            Spec("Celestial Hum", "Aether Ambient", "Cosmic Spheres", 120.0, 30, "#1B1F38", "#4A0E4E", "celestial_hum.wav", "celestial_hum.png"),
            Spec("Ethereal Chords", "Soma Drone Project", "Soma Harmonics", 220.0, 35, "#0F2A30", "#1E824C", "ethereal_chords.wav", "ethereal_chords.png"),
            Spec("Deep Space", "Cosminaut", "Unbounded Voids", 80.0, 40, "#2C001E", "#0F172A", "deep_space.wav", "deep_space.png"),
            Spec("Solar Wind", "Solaric", "Heliosphere", 150.0, 45, "#141A29", "#D97706", "solar_wind.wav", "solar_wind.png"),
            Spec("Nebula Dream", "Nebula Drift", "Stardust Lullabies", 180.0, 50, "#2E1065", "#EC4899", "nebula_dream.wav", "nebula_dream.png")
        )

        for (spec in songSpecs) {
            val songFile = File(songsDir, spec.filename)
            if (!songFile.exists()) {
                generateAmbientTone(songFile, spec.frequencyHz, spec.durationSeconds)
            }
            val artFile = File(artDir, spec.artFilename)
            if (!artFile.exists()) {
                generateGeometricArtwork(artFile, spec.title, Color.parseColor(spec.color1), Color.parseColor(spec.color2))
            }

            val track = TrackEntity(
                title = spec.title,
                artist = spec.artist,
                album = spec.album,
                durationMs = (spec.durationSeconds * 1000).toLong(),
                filePath = songFile.absolutePath,
                artPath = artFile.absolutePath
            )
            musicDao.insertTrack(track)
        }
    }

    private data class Spec(
        val title: String,
        val artist: String,
        val album: String,
        val frequencyHz: Double,
        val durationSeconds: Int,
        val color1: String,
        val color2: String,
        val filename: String,
        val artFilename: String
    )

    private fun generateAmbientTone(file: File, frequencyHz: Double, durationSeconds: Int) {
        val sampleRate = 22050
        val numSamples = sampleRate * durationSeconds
        val dataSize = numSamples * 2 // 16-bit Mono = 2 bytes per sample
        val totalSize = 36 + dataSize

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalSize and 0xff).toByte()
        header[5] = ((totalSize shr 8) and 0xff).toByte()
        header[6] = ((totalSize shr 16) and 0xff).toByte()
        header[7] = ((totalSize shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Header chunk size
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // Format: PCM Uncompressed
        header[21] = 0
        header[22] = 1 // Mono (1 channel)
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        val byteRate = sampleRate * 1 * 2
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2 // BlockAlign
        header[33] = 0
        header[34] = 16 // BitsPerSample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (dataSize and 0xff).toByte()
        header[41] = ((dataSize shr 8) and 0xff).toByte()
        header[42] = ((dataSize shr 16) and 0xff).toByte()
        header[43] = ((dataSize shr 24) and 0xff).toByte()

        file.outputStream().buffered().use { out ->
            out.write(header)
            val buffer = ByteBuffer.allocate(2)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Harmonized layered synthesis for a pleasing analog-like modular sound
                val wave = 0.5 * Math.sin(2.0 * Math.PI * frequencyHz * t) +
                           0.3 * Math.sin(2.0 * Math.PI * (frequencyHz * 1.5) * t) * (0.6 + 0.4 * Math.sin(2.0 * Math.PI * 0.4 * t)) +
                           0.2 * Math.sin(2.0 * Math.PI * (frequencyHz * 0.5) * t) * (0.7 + 0.3 * Math.cos(2.0 * Math.PI * 0.15 * t))
                val amplitude = 26000.0 // keep headroom to prevent digital clipping
                val shortVal = (wave * amplitude).toInt().coerceIn(-32768, 32767).toShort()
                buffer.putShort(0, shortVal)
                out.write(buffer.array())
            }
        }
    }

    private fun generateGeometricArtwork(file: File, name: String, color1: Int, color2: Int) {
        val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Gradient Background
        val shader = LinearGradient(0f, 0f, 400f, 400f, color1, color2, Shader.TileMode.CLAMP)
        paint.shader = shader
        canvas.drawRect(0f, 0f, 400f, 400f, paint)

        // Draw Ambient Minimalist Accents
        paint.shader = null
        paint.color = Color.WHITE
        paint.strokeWidth = 3f

        for (i in 1..3) {
            paint.alpha = 20 * i
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(200f, 200f, 60f * i, paint)
        }

        paint.alpha = 180
        paint.style = Paint.Style.FILL
        if (name.contains("Celestial")) {
            // Draw central orb and ring
            canvas.drawCircle(200f, 200f, 30f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            canvas.drawCircle(200f, 200f, 45f, paint)
        } else if (name.contains("Ethereal")) {
            // Draw elegant minimalist diamond
            val path = Path()
            path.moveTo(200f, 160f)
            path.lineTo(240f, 200f)
            path.lineTo(200f, 240f)
            path.lineTo(160f, 200f)
            path.close()
            canvas.drawPath(path, paint)
        } else if (name.contains("Solar")) {
            // Draw a neat modern burst / star
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            for (angle in 0 until 360 step 45) {
                val rad = Math.toRadians(angle.toDouble())
                val startX = 200f + (30f * Math.cos(rad)).toFloat()
                val startY = 200f + (30f * Math.sin(rad)).toFloat()
                val endX = 200f + (60f * Math.cos(rad)).toFloat()
                val endY = 200f + (60f * Math.sin(rad)).toFloat()
                canvas.drawLine(startX, startY, endX, endY, paint)
            }
            paint.style = Paint.Style.FILL
            canvas.drawCircle(200f, 200f, 20f, paint)
        } else if (name.contains("Nebula")) {
            // Draw overlapping soft cloud-like shapes
            paint.style = Paint.Style.FILL
            paint.alpha = 140
            canvas.drawCircle(180f, 200f, 25f, paint)
            canvas.drawCircle(220f, 200f, 25f, paint)
            canvas.drawCircle(200f, 180f, 25f, paint)
            canvas.drawCircle(200f, 220f, 25f, paint)
        } else {
            // Draw neat nested organic squares
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            canvas.drawRect(170f, 170f, 230f, 230f, paint)
            paint.style = Paint.Style.FILL
            canvas.drawRect(185f, 185f, 215f, 215f, paint)
        }

        file.outputStream().buffered().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bitmap.recycle()
    }
}
