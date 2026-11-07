package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smart_volume_zones")
data class SmartVolumeZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val targetVolumePercent: Float,
    val isGradualFade: Boolean,
    val isActive: Boolean = true
)
