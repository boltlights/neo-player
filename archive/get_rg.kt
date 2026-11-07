package com.example.util

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

fun getReplayGain(path: String): Float? {
    try {
        val f = AudioFileIO.read(File(path))
        val tag = f.tag ?: return null
        // ReplayGain is usually in TXXX:REPLAYGAIN_TRACK_GAIN or similar
        return tag.getFirst(FieldKey.REPLAYGAIN_TRACK_GAIN)?.replace("dB", "")?.trim()?.toFloatOrNull()
    } catch(e: Exception) {}
    return null
}
