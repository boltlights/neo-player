#!/bin/bash
cat << 'INNER_EOF' > script.awk
/private val mediaStoreManager/ {
    print
    print "    private val geofenceManager = com.example.geofence.GeofenceManager(application)"
    print "    val allSmartVolumeZones: kotlinx.coroutines.flow.StateFlow<List<com.example.data.SmartVolumeZoneEntity>> = repository.allSmartVolumeZones.stateIn("
    print "        viewModelScope,"
    print "        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),"
    print "        emptyList()"
    print "    )"
    print ""
    print "    init {"
    print "        viewModelScope.launch {"
    print "            allSmartVolumeZones.collect { zones ->"
    print "                try {"
    print "                    geofenceManager.addGeofences(zones)"
    print "                } catch (e: Exception) {"
    print "                    // Needs permission first time"
    print "                }"
    print "            }"
    print "        }"
    print "    }"
    print ""
    print "    fun insertSmartVolumeZone(zone: com.example.data.SmartVolumeZoneEntity) = viewModelScope.launch { repository.insertSmartVolumeZone(zone) }"
    print "    fun updateSmartVolumeZone(zone: com.example.data.SmartVolumeZoneEntity) = viewModelScope.launch { repository.updateSmartVolumeZone(zone) }"
    print "    fun deleteSmartVolumeZone(zone: com.example.data.SmartVolumeZoneEntity) = viewModelScope.launch { repository.deleteSmartVolumeZone(zone) }"
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MusicViewModel.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MusicViewModel.kt
