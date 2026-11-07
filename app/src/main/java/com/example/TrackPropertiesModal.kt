package com.example

import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun TrackPropertiesDialog(
    track: TrackEntity,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var fileProperties by remember { mutableStateOf<Map<String, String>?>(null) }
    LaunchedEffect(track) {
        val props = withContext(Dispatchers.IO) {
            extractTrackProperties(context, track)
        }
        fileProperties = props
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Track Properties",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (fileProperties == null) {
                    CircularProgressIndicator(
                        color = accentColor,
                        modifier = Modifier
                            .padding(vertical = 32.dp)
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                    )
                } else {
                    fileProperties?.forEach { (key, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = key,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = onBgColor.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = value,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = onBgColor,
                                modifier = Modifier.weight(1.5f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                ) {
                    Text(
                        "Close",
                        color = MaterialTheme.colorScheme.background,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private suspend fun extractTrackProperties(context: android.content.Context, track: TrackEntity): Map<String, String> {
    val props = mutableMapOf<String, String>()
    
    if (track.musicBrainzId != null) {
        props["MusicBrainz ID"] = track.musicBrainzId
    }
    if (track.trackNumber != null) {
        val discStr = if (track.discNumber != null && track.discNumber > 0) "Disc ${track.discNumber}, " else ""
        props["Track Position"] = "${discStr}#${track.trackNumber}"
    } else if (track.discNumber != null && track.discNumber > 0) {
        props["Disc Position"] = "Disc ${track.discNumber}"
    }
    if (!track.albumArtist.isNullOrBlank() && !track.albumArtist.equals(track.artist, ignoreCase = true)) {
        props["Album Artist"] = track.albumArtist
    }
    if (!track.genre.isNullOrBlank() && track.genre != "Unknown") {
        props["Genre"] = track.genre
    }
    if (track.lyrics != null) {
        props["Lyrics"] = if (track.lyrics.length > 50) track.lyrics.substring(0, 50) + "..." else track.lyrics
    }
    val durationSec = (track.durationMs / 1000f).roundToInt()
    val mins = durationSec / 60
    val secs = durationSec % 60
    props["Duration"] = String.format("%d:%02d", mins, secs)
    
    if (track.filePath.startsWith("content://")) {
        try {
            val uri = android.net.Uri.parse(track.filePath)
            val cursor = context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.SIZE), null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val sizeIndex = c.getColumnIndex(android.provider.MediaStore.MediaColumns.SIZE)
                    if (sizeIndex != -1 && !c.isNull(sizeIndex)) {
                        val size = c.getLong(sizeIndex)
                        val mb = size / (1024.0 * 1024.0)
                        props["File Size"] = String.format("%.2f MB", mb)
                    }
                }
            }
            if (!props.containsKey("File Size")) {
                val df = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)
                if (df != null && df.exists()) {
                    val mb = df.length() / (1024.0 * 1024.0)
                    props["File Size"] = String.format("%.2f MB", mb)
                } else {
                    props["File Size"] = "Unknown"
                }
            }
        } catch (e: Exception) {
            props["File Size"] = "Unknown"
        }
    } else {
        val file = File(track.filePath)
        if (file.exists()) {
            val length = file.length()
            val mb = length / (1024.0 * 1024.0)
            props["File Size"] = String.format("%.2f MB", mb)
        } else {
            props["File Size"] = "Unknown"
        }
    }
    
    try {
        val app = context.applicationContext as com.example.MusicApplication
        val count = app.repository.getPlayCountForTrack(track.id)
        props["Play Count"] = count.toString()
    } catch (e: Exception) {
        props["Play Count"] = "Unknown" 
    }

    // Attempt to read ReplayGain tags
    try {
        var rgGain: String? = null
        var rgPeak: String? = null
        val f = File(track.filePath)
        if (f.exists() && f.canRead()) {
            val audioFile = org.jaudiotagger.audio.AudioFileIO.read(f)
            val tag = audioFile.tag
            if (tag != null) {
                val trackGain = tag.getFirst("REPLAYGAIN_TRACK_GAIN").ifBlank { null }
                val trackPeak = tag.getFirst("REPLAYGAIN_TRACK_PEAK").ifBlank { null }
                val albumGain = tag.getFirst("REPLAYGAIN_ALBUM_GAIN").ifBlank { null }
                if (trackGain != null) rgGain = trackGain
                if (trackPeak != null) rgPeak = trackPeak
                if (albumGain != null) {
                    props["ReplayGain Album"] = albumGain
                }
            }
        }
        if (rgGain != null) {
            props["ReplayGain Track"] = rgGain
            if (rgPeak != null) props["ReplayGain Peak"] = rgPeak
            props["Volume Leveling"] = "Supported"
        } else {
            props["Volume Leveling"] = "Standard (Not Tagged)"
        }
    } catch (e: Exception) {
        props["Volume Leveling"] = "Standard"
    }
    val extractor = MediaExtractor()
    try {
        if (track.filePath.startsWith("content://")) {
            extractor.setDataSource(context, android.net.Uri.parse(track.filePath), null)
        } else {
            extractor.setDataSource(track.filePath)
        }
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                props["Encoded With"] = mime.substringAfter("audio/").uppercase()
                
                if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    props["Channels"] = if (channels == 1) "Mono ($channels)" else "Stereo ($channels)"
                }
                
                if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    val sr = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    props["Sample Rate"] = "${sr} Hz"
                }
                
                if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                    val br = format.getInteger(MediaFormat.KEY_BIT_RATE)
                    props["Bitrate"] = "${br / 1000} kbps"
                } else {
                    // Estimate bitrate from size and duration if not provided
                    if (durationSec > 0) {
                        var fsize = 0L
                        if (track.filePath.startsWith("content://")) {
                             val uri = android.net.Uri.parse(track.filePath)
                             val df = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)
                             if (df != null && df.exists()) fsize = df.length()
                        } else {
                             val f = File(track.filePath)
                             if (f.exists()) fsize = f.length()
                        }
                        if (fsize > 0) {
                            val br = (fsize * 8) / durationSec
                            props["Bitrate"] = "${br / 1000} kbps (est)"
                        }
                    }
                }
                
                props["Audio Quality"] = "Standard"
                
                val sr = format.getInteger(MediaFormat.KEY_SAMPLE_RATE, 0)
                val br = format.getInteger(MediaFormat.KEY_BIT_RATE, 0)
                
                if (props["Encoded With"]?.contains("FLAC") == true || props["Encoded With"]?.contains("ALAC") == true) {
                    props["Audio Quality"] = "Lossless"
                } else if (sr > 48000 || br >= 320000) {
                    props["Audio Quality"] = "High Quality"
                }
                break
            }
        }
    } catch (e: Exception) {
        timber.log.Timber.tag("TrackPropertiesModal").w(e, "Failed to read track properties")
    } finally {
        extractor.release()
    }
    
    return props
}
