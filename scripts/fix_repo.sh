#!/bin/bash
cat << 'INNER_EOF' > script.awk
/class MusicRepository/ {
    print
    print "    val allSmartVolumeZones: kotlinx.coroutines.flow.Flow<List<SmartVolumeZoneEntity>> = musicDao.getAllSmartVolumeZones()"
    print "    suspend fun insertSmartVolumeZone(zone: SmartVolumeZoneEntity) = musicDao.insertSmartVolumeZone(zone)"
    print "    suspend fun updateSmartVolumeZone(zone: SmartVolumeZoneEntity) = musicDao.updateSmartVolumeZone(zone)"
    print "    suspend fun deleteSmartVolumeZone(zone: SmartVolumeZoneEntity) = musicDao.deleteSmartVolumeZone(zone)"
    print "    suspend fun getSmartVolumeZone(id: Int) = musicDao.getSmartVolumeZone(id)"
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/data/MusicRepository.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/data/MusicRepository.kt
