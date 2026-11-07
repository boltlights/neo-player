#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/TrackPropertiesModal.kt
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

private fun extractTrackProperties(context: android.content.Context, track: TrackEntity): Map<String, String> {
    val props = mutableMapOf<String, String>()
    
    if (track.musicBrainzId != null) {
        props["MusicBrainz ID"] = track.musicBrainzId
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
                    if (sizeIndex != -1) {
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
    
    // Default mock data for fields we can't reliably get instantly
    props["Play Count"] = "Unknown" 
    props["Volume Leveling"] = "N/A"
    
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
                if (props["Sample Rate"]?.contains("48000") == true || props["Bitrate"]?.contains("320") == true) {
                    props["Audio Quality"] = "High Quality"
                } else if (props["Encoded With"]?.contains("FLAC") == true || props["Encoded With"]?.contains("ALAC") == true) {
                    props["Audio Quality"] = "Lossless"
                }
                break
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        extractor.release()
    }
    
    return props
}
INNER_EOF
