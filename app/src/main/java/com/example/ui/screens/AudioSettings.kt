package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MusicViewModel
import com.example.R

@Composable
fun PlaybackAudioSettingsScreen(
    viewModel: MusicViewModel,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = accentColor)
                }
                Text(
                    "PLAYBACK & AUDIO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            Text(
                "VOLUME NORMALIZATION & SYSTEM AUDIO",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = onBgColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }


        Text(
            "Volume Normalization",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        // ReplayGain Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "ReplayGain",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            "Adjusts volume levels using embedded track and album peak/gain tags without clipping.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = onBgColor.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    "Mode",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val replayGainMode by viewModel.replayGainMode.collectAsStateWithLifecycle()
                val modes = listOf(
                    com.example.ReplayGainMode.DYNAMIC to "Dynamic",
                    com.example.ReplayGainMode.TRACK to "Track",
                    com.example.ReplayGainMode.ALBUM to "Album",
                    com.example.ReplayGainMode.OFF to "Off"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modes.forEach { (mode, label) ->
                        val isSelected = replayGainMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accentColor else surfaceColor.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) accentColor else onBgColor.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setReplayGainMode(mode) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color.Black else Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                val modeDesc = when (replayGainMode) {
                    com.example.ReplayGainMode.DYNAMIC -> "Dynamic: Automatically applies Track Gain when shuffling or playing mixed lists, and Album Gain when playing continuous albums."
                    com.example.ReplayGainMode.TRACK -> "Track: Normalizes each track independently to a uniform perceptual loudness."
                    com.example.ReplayGainMode.ALBUM -> "Album: Preserves the original dynamic range and volume differences between tracks in the same album."
                    com.example.ReplayGainMode.OFF -> "Off: ReplayGain tags are ignored. Audio plays at native encoded gain."
                }
                Text(
                    modeDesc,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = onBgColor.copy(alpha = 0.6f),
                    lineHeight = 15.sp
                )

                if (replayGainMode != com.example.ReplayGainMode.OFF) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = onBgColor.copy(alpha = 0.1f)
                    )

                    val replayGainPreAmpDb by viewModel.replayGainPreAmpDb.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pre-Amplifier",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            "${if (replayGainPreAmpDb >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", replayGainPreAmpDb)} dB",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = replayGainPreAmpDb,
                        onValueChange = { viewModel.setReplayGainPreAmpDb(Math.round(it * 2f) / 2f) },
                        valueRange = -12f..12f,
                        steps = 47,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = onBgColor.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "System & Hardware Integration",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        // Audio Focus & Headphone Disconnect Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Headphones,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Audio Focus & Headphone Auto-Pause",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "â€¢ Automatically pauses playback when headphones or Bluetooth audio disconnects (Becoming Noisy).\nâ€¢ Manages system audio focus: pauses during incoming phone calls and ducks volume during incoming notifications.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    color = onBgColor.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Equalizer launcher
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        val sessionId = viewModel.playbackController.exoPlayer?.audioSessionId ?: 0
                        val intent = android.content.Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                            putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                            putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, 0)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, context.getString(R.string.toast_equalizer_not_supported), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "System Equalizer",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "System Equalizer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            "Launch DSP & sound effects panel",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = onBgColor.copy(alpha = 0.5f)
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
