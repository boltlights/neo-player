#!/bin/bash
cat << 'INNER_EOF' > script.awk
/interface MusicDao {/ {
    print
    print "    @androidx.room.Query(\"SELECT * FROM smart_volume_zones\")"
    print "    fun getAllSmartVolumeZones(): kotlinx.coroutines.flow.Flow<List<SmartVolumeZoneEntity>>"
    print "    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)"
    print "    suspend fun insertSmartVolumeZone(zone: SmartVolumeZoneEntity): Long"
    print "    @androidx.room.Update"
    print "    suspend fun updateSmartVolumeZone(zone: SmartVolumeZoneEntity)"
    print "    @androidx.room.Delete"
    print "    suspend fun deleteSmartVolumeZone(zone: SmartVolumeZoneEntity)"
    print "    @androidx.room.Query(\"SELECT * FROM smart_volume_zones WHERE id = :id\")"
    print "    suspend fun getSmartVolumeZone(id: Int): SmartVolumeZoneEntity?"
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/data/MusicDao.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/data/MusicDao.kt
