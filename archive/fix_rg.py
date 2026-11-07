with open('app/src/main/java/com/example/ReplayGainAudioProcessor.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    @Volatile var preAmpDb: Float = 0f
        set(value) {
            field = value
            updateMultiplier()
        }
    
    // The track/album gain parsed from the metadata
    @Volatile var trackGainDb: Float = 0f
        set(value) {
            field = value
            updateMultiplier()
        }

    private fun updateMultiplier() {
        val totalGainDb = trackGainDb + preAmpDb
        // Convert dB to linear multiplier
        targetGainMultiplier = Math.pow(10.0, totalGainDb / 20.0).toFloat()
    }''',
'''    @Volatile var preAmpDb: Float = 0f
        set(value) {
            field = value
            updateMultiplier()
        }
    
    // The track/album gain parsed from the metadata
    @Volatile var trackGainDb: Float = 0f
        set(value) {
            field = value
            updateMultiplier()
        }

    // Peak amplitude of the track (1.0 = 0 dBFS)
    @Volatile var trackPeak: Float = 1.0f
        set(value) {
            field = if (value > 0f) value else 1.0f
            updateMultiplier()
        }

    private fun updateMultiplier() {
        val totalGainDb = trackGainDb + preAmpDb
        // Convert dB to linear multiplier
        var multiplier = Math.pow(10.0, totalGainDb / 20.0).toFloat()
        // Prevent clipping based on track peak
        if (trackPeak * multiplier > 1.0f) {
            multiplier = 1.0f / trackPeak
        }
        targetGainMultiplier = multiplier
    }'''
)

# Handle float encoding by NOT skipping it if it's float
content = content.replace(
'''    override fun queueInput(inputBuffer: ByteBuffer) {
        if (targetGainMultiplier == 1.0f || currentEncoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {''',
'''    override fun queueInput(inputBuffer: ByteBuffer) {
        if (targetGainMultiplier == 1.0f || (currentEncoding != androidx.media3.common.C.ENCODING_PCM_16BIT && currentEncoding != androidx.media3.common.C.ENCODING_PCM_FLOAT)) {'''
)

# Add float processing loop
old_process_loop = '''        // Process 16-bit audio (2 bytes per sample)
        for (i in position until processLimit step 2) {
            // Read 16-bit little-endian sample
            val sample = (inputBuffer.get(i).toInt() and 0xFF) or (inputBuffer.get(i + 1).toInt() shl 8)
            val shortSample = sample.toShort()
            // Apply ReplayGain multiplier
            var newSample = shortSample * targetGainMultiplier
            // Hard Limit to prevent digital clipping
            if (newSample > Short.MAX_VALUE) {
                newSample = Short.MAX_VALUE.toFloat()
            } else if (newSample < Short.MIN_VALUE) {
                newSample = Short.MIN_VALUE.toFloat()
            }
            // Write back to buffer
            val finalSample = newSample.toInt().toShort()
            buffer.put((finalSample.toInt() and 0xFF).toByte())
            buffer.put(((finalSample.toInt() shr 8) and 0xFF).toByte())
        }
        if (processLimit < limit) {
            buffer.put(inputBuffer.get(processLimit))
        }'''

new_process_loop = '''        if (currentEncoding == androidx.media3.common.C.ENCODING_PCM_16BIT) {
            // Process 16-bit audio (2 bytes per sample)
            for (i in position until processLimit step 2) {
                val sample = (inputBuffer.get(i).toInt() and 0xFF) or (inputBuffer.get(i + 1).toInt() shl 8)
                val shortSample = sample.toShort()
                var newSample = shortSample * targetGainMultiplier
                if (newSample > Short.MAX_VALUE) newSample = Short.MAX_VALUE.toFloat()
                else if (newSample < Short.MIN_VALUE) newSample = Short.MIN_VALUE.toFloat()
                
                val finalSample = newSample.toInt().toShort()
                buffer.put((finalSample.toInt() and 0xFF).toByte())
                buffer.put(((finalSample.toInt() shr 8) and 0xFF).toByte())
            }
            if (processLimit < limit) buffer.put(inputBuffer.get(processLimit))
        } else if (currentEncoding == androidx.media3.common.C.ENCODING_PCM_FLOAT) {
            // Process 32-bit float audio (4 bytes per sample)
            val floatLimit = position + (size / 4) * 4
            for (i in position until floatLimit step 4) {
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
            if (floatLimit < limit) {
                for (i in floatLimit until limit) {
                    buffer.put(inputBuffer.get(i))
                }
            }
        }'''

content = content.replace(old_process_loop, new_process_loop)

# Fix processLimit calculation to be dynamic based on encoding
content = content.replace(
'''        // Ensure we only process pairs of bytes
        val processLimit = position + (size / 2) * 2''',
'''        // Ensure we only process complete samples
        val bytesPerSample = if (currentEncoding == androidx.media3.common.C.ENCODING_PCM_FLOAT) 4 else 2
        val processLimit = position + (size / bytesPerSample) * bytesPerSample'''
)

with open('app/src/main/java/com/example/ReplayGainAudioProcessor.kt', 'w') as f:
    f.write(content)
