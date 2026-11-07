package com.example.viewmodel

import android.app.Application
import com.example.data.MusicRepository
import com.example.data.SmartVolumeZoneEntity
import com.example.geofence.GeofenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Domain-specific ViewModel handling geofenced Smart Volume zones.
 */
class SmartVolumeViewModel(
    private val application: Application,
    private val repository: MusicRepository,
    private val geofenceManager: GeofenceManager,
    private val scope: CoroutineScope
) {
    val allSmartVolumeZones: StateFlow<List<SmartVolumeZoneEntity>> = repository.allSmartVolumeZones.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        scope.launch {
            allSmartVolumeZones.collect { zones ->
                try {
                    geofenceManager.addGeofences(zones)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    // Needs permission first time or background location not yet granted
                }
            }
        }
    }

    fun insertSmartVolumeZone(zone: SmartVolumeZoneEntity) = scope.launch {
        repository.insertSmartVolumeZone(zone)
    }

    fun updateSmartVolumeZone(zone: SmartVolumeZoneEntity) = scope.launch {
        repository.updateSmartVolumeZone(zone)
    }

    fun deleteSmartVolumeZone(zone: SmartVolumeZoneEntity) = scope.launch {
        repository.deleteSmartVolumeZone(zone)
    }

    fun hasLocationPermissions(): Boolean = geofenceManager.hasLocationPermissions()

    fun hasBackgroundLocationPermission(): Boolean = geofenceManager.hasBackgroundLocationPermission()

    fun refreshGeofences() {
        val currentZones = allSmartVolumeZones.value
        if (currentZones.isNotEmpty()) {
            geofenceManager.addGeofences(currentZones)
        }
    }
}
