package com.example

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ReplayGainAudioProcessorTest {

    private lateinit var processor: ReplayGainAudioProcessor

    @Before
    fun setUp() {
        processor = ReplayGainAudioProcessor()
    }

    @Test
    fun defaultState_hasUnityGain() {
        assertEquals(ReplayGainMode.DYNAMIC, processor.mode)
        assertEquals(1.0f, processor.currentMultiplier, 0.0001f)
    }

    @Test
    fun modeOff_alwaysProducesUnityMultiplier() {
        processor.mode = ReplayGainMode.OFF
        processor.trackGainDb = -10.0f
        processor.albumGainDb = -12.0f
        processor.preAmpDb = 5.0f

        assertEquals(1.0f, processor.currentMultiplier, 0.0001f)
    }

    @Test
    fun trackMode_calculatesGainAccuratelyWithoutClipping() {
        processor.mode = ReplayGainMode.TRACK
        // -6 dB reduction -> 10^(-6/20) ≈ 0.501187f
        processor.trackGainDb = -6.0f
        processor.trackPeak = 0.9f

        val expectedMultiplier = Math.pow(10.0, -6.0 / 20.0).toFloat()
        assertEquals(expectedMultiplier, processor.currentMultiplier, 0.001f)
        assertTrue(processor.currentMultiplier < 1.0f)
    }

    @Test
    fun trackMode_positiveGainWithoutPeakClip_appliesFullGain() {
        processor.mode = ReplayGainMode.TRACK
        // +6 dB boost -> ~1.99526f
        processor.trackGainDb = 6.0f
        // Peak is low enough that peak * multiplier <= 1.0 (0.5 * 1.995 < 1.0)
        processor.trackPeak = 0.5f

        val expectedMultiplier = Math.pow(10.0, 6.0 / 20.0).toFloat()
        assertEquals(expectedMultiplier, processor.currentMultiplier, 0.001f)
    }

    @Test
    fun clippingPrevention_clampsMultiplierWhenPeakExceedsUnity() {
        processor.mode = ReplayGainMode.TRACK
        // +6 dB boost -> ~1.99526f
        processor.trackGainDb = 6.0f
        // Peak of 0.8: 0.8 * 1.99526 = 1.596 > 1.0, so it must clamp to 1.0 / 0.8 = 1.25f
        processor.trackPeak = 0.8f

        val expectedClamped = 1.0f / 0.8f
        assertEquals(expectedClamped, processor.currentMultiplier, 0.001f)
    }

    @Test
    fun preAmpDb_isAddedToGain() {
        processor.mode = ReplayGainMode.TRACK
        processor.trackGainDb = -3.0f
        processor.preAmpDb = 3.0f // Neutralizes to 0 dB
        processor.trackPeak = 0.5f

        assertEquals(1.0f, processor.currentMultiplier, 0.001f)
    }

    @Test
    fun dynamicMode_respectsAlbumPlaybackAndShuffleToggle() {
        processor.mode = ReplayGainMode.DYNAMIC
        processor.trackGainDb = -4.0f
        processor.albumGainDb = -8.0f
        processor.trackPeak = 0.9f
        processor.albumPeak = 0.9f

        val expectedTrackMultiplier = Math.pow(10.0, -4.0 / 20.0).toFloat()
        val expectedAlbumMultiplier = Math.pow(10.0, -8.0 / 20.0).toFloat()

        // By default (not album playback, no shuffle): uses track gain
        processor.isAlbumPlayback = false
        processor.isShuffleActive = false
        assertEquals(expectedTrackMultiplier, processor.currentMultiplier, 0.001f)

        // When album playback is active without shuffle: uses album gain
        processor.isAlbumPlayback = true
        processor.isShuffleActive = false
        assertEquals(expectedAlbumMultiplier, processor.currentMultiplier, 0.001f)

        // When shuffle is activated during album playback: falls back to track gain
        processor.isAlbumPlayback = true
        processor.isShuffleActive = true
        assertEquals(expectedTrackMultiplier, processor.currentMultiplier, 0.001f)
    }

    @Test
    fun clearGain_resetsAllValuesToDefault() {
        processor.mode = ReplayGainMode.TRACK
        processor.trackGainDb = -5.0f
        processor.trackPeak = 0.8f
        processor.albumGainDb = -7.0f
        processor.albumPeak = 0.7f

        assertTrue(processor.currentMultiplier != 1.0f)

        processor.clearGain()
        assertEquals(1.0f, processor.currentMultiplier, 0.0001f)
    }

    @Test
    fun queueInput_pcm16Bit_preventsSampleClipping() {
        // Configure processor for 16-bit PCM, 2 channels, 44100Hz
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        // Set a 2x boost multiplier without peak limitation
        processor.mode = ReplayGainMode.TRACK
        processor.trackGainDb = 6.02f // ~2.0x
        processor.trackPeak = 0.0f // Defaults to 1.0 peak clamp -> 1.0x, so let's set preAmp
        processor.clearGain()
        // Use preAmp: +6.0206 dB -> 2.0x multiplier, peak 0.5
        processor.mode = ReplayGainMode.TRACK
        processor.preAmpDb = 6.0206f
        processor.trackPeak = 0.5f

        val multiplier = processor.currentMultiplier
        assertEquals(2.0f, multiplier, 0.05f)

        // Allocate input buffer with a sample near Short.MAX_VALUE (e.g. 20000 -> 40000 which exceeds 32767)
        val sampleValue: Short = 25000
        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN)
        input.putShort(sampleValue)
        input.putShort((-sampleValue).toShort())
        input.flip()

        processor.queueInput(input)
        val output = processor.output
        output.order(ByteOrder.LITTLE_ENDIAN)

        assertTrue(output.remaining() >= 4)
        val boostedPositive = output.short
        val boostedNegative = output.short

        // Must be clamped to Short.MAX_VALUE and Short.MIN_VALUE without integer overflow distortion
        assertEquals(Short.MAX_VALUE, boostedPositive)
        assertEquals(Short.MIN_VALUE, boostedNegative)
    }
}
