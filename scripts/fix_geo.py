with open('app/src/main/java/com/example/geofence/GeofenceBroadcastReceiver.kt', 'r') as f:
    geofence_content = f.read()

geofence_content = geofence_content.replace(
    '                pendingResult?.finish() else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {',
    '                    } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {'
)
geofence_content = geofence_content.replace(
    '                        }\n                    }\n                }\n            }\n        }\n    }\n\n    private suspend fun applyVolume',
    '                        }\n                    }\n                }\n                pendingResult?.finish()\n            }\n        }\n    }\n\n    private suspend fun applyVolume'
)

with open('app/src/main/java/com/example/geofence/GeofenceBroadcastReceiver.kt', 'w') as f:
    f.write(geofence_content)
