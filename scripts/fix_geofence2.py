with open('app/src/main/java/com/example/geofence/GeofenceBroadcastReceiver.kt', 'r') as f:
    content = f.read()

correct_onreceive = """    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                val db = MusicDatabase.getDatabase(context)
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                
                val sharedPrefs = context.getSharedPreferences("SmartVolumePrefs", Context.MODE_PRIVATE)

                for (geofence in triggeringGeofences) {
                    val zoneId = geofence.requestId.toIntOrNull() ?: continue
                    val zone = db.musicDao().getSmartVolumeZone(zoneId) ?: continue

                    if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                        Log.d("GeofenceReceiver", "Entering zone: ${zone.id}")
                        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        sharedPrefs.edit().putInt("pre_zone_${zone.id}_volume", currentVol).apply()

                        val targetVol = (zone.targetVolumePercent / 100f * maxVolume).toInt()
                        applyVolume(audioManager, currentVol, targetVol, zone.isGradualFade)
                    } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                        Log.d("GeofenceReceiver", "Exiting zone: ${zone.id}")
                        val savedVol = sharedPrefs.getInt("pre_zone_${zone.id}_volume", -1)
                        if (savedVol != -1) {
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            applyVolume(audioManager, currentVol, savedVol, zone.isGradualFade)
                            sharedPrefs.edit().remove("pre_zone_${zone.id}_volume").apply()
                        }
                    }
                }
                pendingResult?.finish()
            }
        }
    }"""

import re
content = re.sub(r'    override fun onReceive\(context: Context, intent: Intent\) \{.*?(?=    private suspend fun applyVolume)', correct_onreceive + '\n\n', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/geofence/GeofenceBroadcastReceiver.kt', 'w') as f:
    f.write(content)

