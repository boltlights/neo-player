package com.example.geofence

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SmartVolumeZoneEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeofenceManagerTest {

    private lateinit var context: Context
    private lateinit var geofenceManager: GeofenceManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        geofenceManager = GeofenceManager(context)
    }

    @Test
    fun testGeofenceManagerInstantiation() {
        assertNotNull(geofenceManager)
    }

    @Test
    fun testZoneRadiusModification() {
        val initialZone = SmartVolumeZoneEntity(
            id = 1,
            latitude = 37.7749,
            longitude = -122.4194,
            radiusMeters = 100f,
            targetVolumePercent = 50f,
            isGradualFade = true,
            isActive = true
        )

        assertEquals(100f, initialZone.radiusMeters, 0.01f)

        // Simulate user adjusting the radius to 500 meters
        val updatedZone = initialZone.copy(radiusMeters = 500f)
        assertEquals(500f, updatedZone.radiusMeters, 0.01f)

        // Verify active status preserved
        assertTrue(updatedZone.isActive)
    }

    @Test
    fun testPermissionChecksDoNotThrow() {
        // Safe execution when location permission is not granted in test environment
        val hasFine = geofenceManager.hasLocationPermissions()
        val hasBg = geofenceManager.hasBackgroundLocationPermission()

        // Calling addGeofences with empty or missing permissions must be a safe no-op
        geofenceManager.addGeofences(emptyList())
        geofenceManager.removeGeofences()
    }
}
