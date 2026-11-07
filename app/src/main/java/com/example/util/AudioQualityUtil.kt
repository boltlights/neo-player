package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import androidx.media3.common.Metadata
import com.example.data.TrackEntity
import java.io.File
import java.util.Locale

object AudioQualityUtil {
    private val reflectionCache = mutableMapOf<String, java.lang.reflect.Method>()
    private var classCache: Class<*>? = null
    

    /**
     * Builds the badge from values actually read from the file. Unknown parts are omitted: this used to
     * fill gaps with invented defaults (FLAC -> "24-BIT / 48.0KHZ", no path -> "MP3 • 16-BIT / 44.1KHZ",
     * unreadable WAV -> "16-BIT / 22.1KHZ • MONO"), and the UI colours "24-BIT" FLACs as hi-res.
     */
    private fun badge(format: String, bitrateKbps: Long?, bits: Int?, sampleRateHz: Double?, extra: String? = null): String {
        val parts = mutableListOf(format.ifEmpty { "AUDIO" })
        if (bitrateKbps != null && bitrateKbps > 0) parts.add("${bitrateKbps}KBPS")
        val validBits = bits?.takeIf { it > 0 }
        val rate = sampleRateHz?.takeIf { it > 0 }?.let { String.format(Locale.US, "%.1fKHZ", it / 1000.0) }
        when {
            validBits != null && rate != null -> parts.add("$validBits-BIT / $rate")
            validBits != null -> parts.add("$validBits-BIT")
            rate != null -> parts.add(rate)
        }
        if (extra != null) parts.add(extra)
        return parts.joinToString(" • ")
    }

    suspend fun getQualityString(context: Context, track: TrackEntity): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        // Folder-imported tracks keep their location in sourceUri rather than filePath.
        val filePath = track.filePath.ifEmpty { track.sourceUri.orEmpty() }
        val file = File(filePath)
        val extension = if (file.exists()) file.extension.uppercase(Locale.US) else {
            // fallback to get from path string (decoded, so SAF document ids like "...%2Fsong.flac" work)
            Uri.decode(filePath).substringAfterLast('/').substringAfterLast('.', "").uppercase(Locale.US)
        }

        if (extension == "WAV" && file.exists()) {
            try {
                if (file.length() >= 44) {
                    val header = ByteArray(44)
                    file.inputStream().use { input ->
                        input.read(header)
                    }
                    // Check WAV signature: "RIFF" and "WAVE"
                    val isRiff = header[0].toInt() == 'R'.code && header[1].toInt() == 'I'.code && header[2].toInt() == 'F'.code && header[3].toInt() == 'F'.code
                    val isWave = header[8].toInt() == 'W'.code && header[9].toInt() == 'A'.code && header[10].toInt() == 'V'.code && header[11].toInt() == 'E'.code
                    if (isRiff && isWave) {
                        val channels = ((header[23].toInt() and 0xFF) shl 8) or (header[22].toInt() and 0xFF)
                        val sampleRate = ((header[27].toInt() and 0xFF) shl 24) or
                                        ((header[26].toInt() and 0xFF) shl 16) or
                                        ((header[25].toInt() and 0xFF) shl 8) or
                                        (header[24].toInt() and 0xFF)
                        val bitsPerSample = ((header[35].toInt() and 0xFF) shl 8) or (header[34].toInt() and 0xFF)
                        
                        val chLabel = if (channels == 1) "MONO" else "STEREO"
                        return@withContext badge("WAV", null, bitsPerSample, sampleRate.toDouble(), chLabel)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("AudioQualityUtil").w(e, "Failed to read WAV header for %s", track.title)
            }
            // Unreadable/unusual header: fall through to the generic readers below.
        }

        // Use Jaudiotagger for other tracks if file exists
        try {
            if (file.exists()) {
                val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                val header = audioFile.audioHeader
                
                val format = if (extension.isNotEmpty()) extension else header.encodingType.uppercase(Locale.US)
                return@withContext badge(
                    format,
                    header.bitRate.toLongOrNull(),
                    header.bitsPerSample,
                    header.sampleRate.toDoubleOrNull()
                )
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            timber.log.Timber.tag("AudioQualityUtil").w(e, "Jaudiotagger header read failed for %s", track.title)
        }
        
        // Fallback to MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()
        try {
            if (track.sourceUri != null && track.sourceUri.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(track.sourceUri))
            } else if (filePath.startsWith("file://")) {
                retriever.setDataSource(context, Uri.parse(filePath))
            } else {
                retriever.setDataSource(filePath)
            }
            
            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            
            val bitrateKbps = if (!bitrateStr.isNullOrEmpty()) {
                bitrateStr.toLongOrNull()?.let { it / 1000 }
            } else null

            val sampleRateStr = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
            } else null
            val bitsPerSampleStr = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
            } else null

            return@withContext badge(
                extension,
                bitrateKbps,
                bitsPerSampleStr?.toIntOrNull(),
                sampleRateStr?.toDoubleOrNull()
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            timber.log.Timber.tag("AudioQualityUtil").w(e, "MediaMetadataRetriever audio format read failed for %s", track.title)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }

        // Nothing readable: show only the container format we know (no invented bit depth / rate).
        return@withContext badge(extension, null, null, null)
    }

    /**
     * Extracts ReplayGain Track Gain tag in dB from Media3 metadata.
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getReplayGainFromMetadata(metadata: Metadata): Float? {
        return getReplayGainTrackGainFromMetadata(metadata)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getReplayGainTrackGainFromMetadata(metadata: Metadata): Float? {
        return extractReplayGainValue(metadata, "REPLAYGAIN_TRACK_GAIN")
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getReplayGainAlbumGainFromMetadata(metadata: Metadata): Float? {
        return extractReplayGainValue(metadata, "REPLAYGAIN_ALBUM_GAIN")
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getReplayGainTrackPeakFromMetadata(metadata: Metadata): Float? {
        return extractReplayGainValue(metadata, "REPLAYGAIN_TRACK_PEAK")
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getReplayGainAlbumPeakFromMetadata(metadata: Metadata): Float? {
        return extractReplayGainValue(metadata, "REPLAYGAIN_ALBUM_PEAK")
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun extractReplayGainValue(metadata: Metadata, targetKey: String): Float? {
        for (i in 0 until metadata.length()) {
            val entry = metadata.get(i) ?: continue
            val className = entry.javaClass.name

            if (className.endsWith("TxxxFrame") || className.endsWith("TextInformationFrame")) {
                try {
                    val descField = entry.javaClass.getField("description")
                    val description = descField.get(entry) as? String
                    if (targetKey.equals(description, ignoreCase = true)) {
                        var valueStr: String? = null
                        try {
                            val valuesField = entry.javaClass.getField("values")
                            val values = valuesField.get(entry) as? List<*>
                            valueStr = values?.firstOrNull() as? String
                        } catch (e: Exception) {
                            try {
                                val valueField = entry.javaClass.getField("value")
                                valueStr = valueField.get(entry) as? String
                            } catch (ex: Exception) {}
                        }

                        if (!valueStr.isNullOrBlank()) {
                            val cleanVal = valueStr.replace("dB", "", ignoreCase = true).trim()
                            return cleanVal.toFloatOrNull()
                        }
                    }
                } catch (e: Exception) {
                    // ignore reflection error
                }
            } else if (className.endsWith("VorbisComment")) {
                try {
                    val keyField = entry.javaClass.getField("key")
                    val key = keyField.get(entry) as? String
                    if (targetKey.equals(key, ignoreCase = true)) {
                        val valueField = entry.javaClass.getField("value")
                        val valueStr = valueField.get(entry) as? String
                        if (!valueStr.isNullOrBlank()) {
                            val cleanVal = valueStr.replace("dB", "", ignoreCase = true).trim()
                            return cleanVal.toFloatOrNull()
                        }
                    }
                } catch (e: Exception) {
                    // ignore reflection error
                }
            }
        }
        return null
    }

    /**
     * Bit-Perfect/Mixer Attributes (Android 14+):
     * If a USB DAC is connected, requests direct, unprocessed output for high-res FLAC files.
     * Implemented using reflection to ensure clean compilation across all build tools.
     */
    fun configureBitPerfectIfSupported(context: Context, track: TrackEntity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                val usbDevice = devices.firstOrNull { 
                    it.type == android.media.AudioDeviceInfo.TYPE_USB_DEVICE || 
                    it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET 
                }
                
                if (usbDevice != null) {
                    val isHighRes = track.filePath.endsWith(".flac", ignoreCase = true) || 
                                    track.sourceUri?.endsWith(".flac", ignoreCase = true) == true
                    
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                        
                    if (isHighRes) {
                        val getSupportedMixerAttributesMethod = reflectionCache.getOrPut("getSupportedMixerAttributes") {
                            audioManager.javaClass.getMethod("getSupportedMixerAttributes", android.media.AudioDeviceInfo::class.java)
                        }
                        val supportedMixers = getSupportedMixerAttributesMethod.invoke(audioManager, usbDevice) as? List<*>
                        
                        var directMixer: Any? = null
                        if (supportedMixers != null) {
                            for (mixer in supportedMixers) {
                                if (mixer != null) {
                                    val getMixerBehaviorMethod = reflectionCache.getOrPut("getMixerBehavior") {
                                        mixer.javaClass.getMethod("getMixerBehavior")
                                    }
                                    val behavior = getMixerBehaviorMethod.invoke(mixer) as? Int
                                    // MIXER_BEHAVIOR_DIRECT is 1
                                    if (behavior == 1) {
                                        directMixer = mixer
                                        break
                                    }
                                }
                            }
                        }
                        
                        if (directMixer == null) {
                            val audioFormat = android.media.AudioFormat.Builder()
                                .setEncoding(android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED)
                                .setSampleRate(192000)
                                .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_STEREO)
                                .build()
                                
                            val builderClass = classCache ?: Class.forName("android.media.AudioMixerAttributes\$Builder").also { classCache = it }
                            val constructor = builderClass.getConstructor(android.media.AudioFormat::class.java)
                            val builderInstance = constructor.newInstance(audioFormat)
                            
                            val setMixerBehaviorMethod = reflectionCache.getOrPut("setMixerBehavior") {
                                builderClass.getMethod("setMixerBehavior", Int::class.javaPrimitiveType)
                            }
                            setMixerBehaviorMethod.invoke(builderInstance, 1)
                            
                            val buildMethod = reflectionCache.getOrPut("build") {
                                builderClass.getMethod("build")
                            }
                            directMixer = buildMethod.invoke(builderInstance)
                        }
                        
                        if (directMixer != null) {
                            val setPreferredMixerAttributesMethod = reflectionCache.getOrPut("setPreferredMixerAttributes") {
                                audioManager.javaClass.getMethod(
                                    "setPreferredMixerAttributes",
                                    android.media.AudioAttributes::class.java,
                                    android.media.AudioDeviceInfo::class.java,
                                    Class.forName("android.media.AudioMixerAttributes")
                                )
                            }
                            setPreferredMixerAttributesMethod.invoke(audioManager, audioAttributes, usbDevice, directMixer)
                            timber.log.Timber.tag("AudioQualityUtil").d("Bit-perfect direct mode configured via reflection for USB DAC.")
                        }
                    } else {
                        try {
                            val clearPreferredMixerAttributesMethod = reflectionCache.getOrPut("clearPreferredMixerAttributes") {
                                audioManager.javaClass.getMethod(
                                    "clearPreferredMixerAttributes",
                                    android.media.AudioAttributes::class.java,
                                    android.media.AudioDeviceInfo::class.java
                                )
                            }
                            clearPreferredMixerAttributesMethod.invoke(audioManager, audioAttributes, usbDevice)
                            timber.log.Timber.tag("AudioQualityUtil").d("Standard mixer path restored via reflection.")
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
            } catch (e: Exception) {
                timber.log.Timber.tag("AudioQualityUtil").e(e, "Reflection error in configureBitPerfectIfSupported")
            }
        }
    }
}
