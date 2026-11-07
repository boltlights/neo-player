package com.example

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

enum class ReplayGainMode {
    OFF,
    TRACK,
    ALBUM,
    DYNAMIC
}

class ReplayGainAudioProcessor : BaseAudioProcessor() {

    @Volatile private var targetGainMultiplier: Float = 1.0f
    val currentMultiplier: Float get() = targetGainMultiplier

    @Volatile var mode: ReplayGainMode = ReplayGainMode.DYNAMIC
        set(value) {
            field = value
            updateMultiplier()
        }

    @Volatile var preAmpDb: Float = 0f
        set(value) {
            field = value
            updateMultiplier()
        }
        
    @Volatile var trackGainDb: Float? = null
        set(value) {
            field = value
            updateMultiplier()
        }

    @Volatile var trackPeak: Float? = null
        set(value) {
            field = value
            updateMultiplier()
        }

    @Volatile var albumGainDb: Float? = null
        set(value) {
            field = value
            updateMultiplier()
        }

    @Volatile var albumPeak: Float? = null
        set(value) {
            field = value
            updateMultiplier()
        }

    @Volatile var isShuffleActive: Boolean = false
        set(value) {
            field = value
            updateMultiplier()
        }

    @Volatile var isAlbumPlayback: Boolean = false
        set(value) {
            field = value
            updateMultiplier()
        }

    fun clearGain() {
        trackGainDb = null
        trackPeak = null
        albumGainDb = null
        albumPeak = null
        updateMultiplier()
    }

    fun setGains(trackGain: Float?, albumGain: Float?, trackPeak: Float?, albumPeak: Float?) {
        this.trackGainDb = trackGain
        this.albumGainDb = albumGain
        this.trackPeak = trackPeak
        this.albumPeak = albumPeak
        updateMultiplier()
    }

    private fun updateMultiplier() {
        if (mode == ReplayGainMode.OFF) {
            targetGainMultiplier = 1.0f
            return
        }

        val effectiveGainDb: Float? = when (mode) {
            ReplayGainMode.OFF -> null
            ReplayGainMode.TRACK -> trackGainDb ?: albumGainDb
            ReplayGainMode.ALBUM -> albumGainDb ?: trackGainDb
            ReplayGainMode.DYNAMIC -> {
                if (isShuffleActive || !isAlbumPlayback) {
                    trackGainDb ?: albumGainDb
                } else {
                    albumGainDb ?: trackGainDb
                }
            }
        }

        val effectivePeak: Float = when (mode) {
            ReplayGainMode.OFF -> 1.0f
            ReplayGainMode.TRACK -> trackPeak ?: albumPeak ?: 1.0f
            ReplayGainMode.ALBUM -> albumPeak ?: trackPeak ?: 1.0f
            ReplayGainMode.DYNAMIC -> {
                if (isShuffleActive || !isAlbumPlayback) {
                    trackPeak ?: albumPeak ?: 1.0f
                } else {
                    albumPeak ?: trackPeak ?: 1.0f
                }
            }
        }

        if (effectiveGainDb == null && preAmpDb == 0f) {
            targetGainMultiplier = 1.0f
            return
        }

        val totalGainDb = (effectiveGainDb ?: 0f) + preAmpDb
        var multiplier = Math.pow(10.0, totalGainDb / 20.0).toFloat()
        val peakVal = if (effectivePeak > 0f) effectivePeak else 1.0f
        if (peakVal * multiplier > 1.0f) {
            multiplier = 1.0f / peakVal
        }
        targetGainMultiplier = multiplier
        timber.log.Timber.tag("ReplayGainProcessor").d(
            "ReplayGain updated: mode=%s, gain=%sdB, peak=%s, preAmp=%sdB, multiplier=%s",
            mode, effectiveGainDb, effectivePeak, preAmpDb, multiplier
        )
    }

    private var currentEncoding: Int = androidx.media3.common.C.ENCODING_PCM_16BIT

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_FLOAT) {
            currentEncoding = androidx.media3.common.C.ENCODING_INVALID
            return AudioProcessor.AudioFormat.NOT_SET
        }
        currentEncoding = inputAudioFormat.encoding
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (targetGainMultiplier == 1.0f || (currentEncoding != androidx.media3.common.C.ENCODING_PCM_16BIT && currentEncoding != androidx.media3.common.C.ENCODING_PCM_FLOAT)) {
            replaceOutputBuffer(remaining).put(inputBuffer).flip()
            return
        }

        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val size = limit - position
        
        val buffer = replaceOutputBuffer(size)

        val bytesPerSample = if (currentEncoding == androidx.media3.common.C.ENCODING_PCM_FLOAT) 4 else 2
        val processLimit = position + (size / bytesPerSample) * bytesPerSample

        if (currentEncoding == androidx.media3.common.C.ENCODING_PCM_16BIT) {
            for (i in position until processLimit step 2) {
                val sample = (inputBuffer.get(i).toInt() and 0xFF) or (inputBuffer.get(i + 1).toInt() shl 8)
                val shortSample = sample.toShort()
                var newSample = shortSample * targetGainMultiplier
                
                if (newSample > Short.MAX_VALUE) {
                    newSample = Short.MAX_VALUE.toFloat()
                } else if (newSample < Short.MIN_VALUE) {
                    newSample = Short.MIN_VALUE.toFloat()
                }
                
                val finalSample = newSample.toInt().toShort()
                buffer.put((finalSample.toInt() and 0xFF).toByte())
                buffer.put(((finalSample.toInt() shr 8) and 0xFF).toByte())
            }
            if (processLimit < limit) {
                for(i in processLimit until limit) {
                    buffer.put(inputBuffer.get(i))
                }
            }
        } else if (currentEncoding == androidx.media3.common.C.ENCODING_PCM_FLOAT) {
            for (i in position until processLimit step 4) {
                val bits = (inputBuffer.get(i).toInt() and 0xFF) or
                           ((inputBuffer.get(i + 1).toInt() and 0xFF) shl 8) or
                           ((inputBuffer.get(i + 2).toInt() and 0xFF) shl 16) or
                           ((inputBuffer.get(i + 3).toInt() and 0xFF) shl 24)
                var floatSample = java.lang.Float.intBitsToFloat(bits)
                floatSample *= targetGainMultiplier
                
                if (floatSample > 1.0f) floatSample = 1.0f
                else if (floatSample < -1.0f) floatSample = -1.0f
                
                val finalBits = java.lang.Float.floatToIntBits(floatSample)
                buffer.put((finalBits and 0xFF).toByte())
                buffer.put(((finalBits shr 8) and 0xFF).toByte())
                buffer.put(((finalBits shr 16) and 0xFF).toByte())
                buffer.put(((finalBits shr 24) and 0xFF).toByte())
            }
            if (processLimit < limit) {
                for (i in processLimit until limit) {
                    buffer.put(inputBuffer.get(i))
                }
            }
        }

        inputBuffer.position(limit)
        buffer.flip()
    }
}
