package com.example.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.example.data.MusicDatabase
import com.example.data.SmartVolumeZoneEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import timber.log.Timber

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            Timber.tag("GeofenceReceiver").e("Geofencing error: %s", geofencingEvent.errorCode)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            
            val pendingResult = goAsync()
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val wakeLock = powerManager?.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "NeoPlayer:DriverModeGeofenceWakeLock"
            )?.apply {
                acquire(30000L) // 30 seconds max timeout to ensure fading completes while phone is locked
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = MusicDatabase.getDatabase(context)
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val sharedPrefs = context.getSharedPreferences("SmartVolumePrefs", Context.MODE_PRIVATE)

                    for (geofence in triggeringGeofences) {
                        val zoneId = geofence.requestId.toIntOrNull() ?: continue
                        val zone = db.musicDao().getSmartVolumeZone(zoneId) ?: continue

                        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                            Timber.tag("GeofenceReceiver").d("Entering zone: %s (radius: %sm, locked mode active)", zone.id, zone.radiusMeters)
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            sharedPrefs.edit().putInt("pre_zone_${zone.id}_volume", currentVol).apply()

                            val targetVol = (zone.targetVolumePercent / 100f * maxVolume).toInt()
                            applyVolume(audioManager, currentVol, targetVol, zone.isGradualFade)
                        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                            Timber.tag("GeofenceReceiver").d("Exiting zone: %s", zone.id)
                            val savedVol = sharedPrefs.getInt("pre_zone_${zone.id}_volume", -1)
                            if (savedVol != -1) {
                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                applyVolume(audioManager, currentVol, savedVol, zone.isGradualFade)
                                sharedPrefs.edit().remove("pre_zone_${zone.id}_volume").apply()
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Timber.tag("GeofenceReceiver").e(t, "Error executing geofence transition while device is locked or backgrounded")
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock.release()
                        }
                    } catch (_: Throwable) {}
                    pendingResult?.finish()
                }
            }
        }
    }

    private suspend fun applyVolume(audioManager: AudioManager, currentVol: Int, targetVol: Int, isGradualFade: Boolean) {
        if (!isGradualFade) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
        } else {
            var vol = currentVol
            val step = if (targetVol > currentVol) 1 else -1
            while (vol != targetVol) {
                vol += step
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                delay(500) // 500ms between steps for gradual fade
            }
        }
    }
}
