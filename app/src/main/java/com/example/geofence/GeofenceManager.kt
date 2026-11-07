package com.example.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.SmartVolumeZoneEntity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import timber.log.Timber

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Checks if fine location permission is granted.
     */
    fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if background location is granted on Android 10+ (API 29+).
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Checks if device-level location providers (GPS or Network) are enabled.
     */
    fun isLocationServiceEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    @SuppressLint("MissingPermission")
    fun addGeofences(zones: List<SmartVolumeZoneEntity>) {
        if (zones.isEmpty()) {
            removeGeofences()
            return
        }

        val activeZones = zones.filter { it.isActive }
        if (activeZones.isEmpty()) {
            removeGeofences()
            return
        }

        // Verify required permissions before calling Google Play Services to prevent ApiException 1004
        if (!hasLocationPermissions()) {
            Timber.tag("GeofenceManager").d(
                "Fine location permission not granted yet; skipping geofence registration until user grants location in Driver Mode."
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission()) {
            Timber.tag("GeofenceManager").d(
                "Background location permission not granted yet; waiting for background permission before adding geofences."
            )
            return
        }

        // Verify device location service is enabled
        if (!isLocationServiceEnabled()) {
            Timber.tag("GeofenceManager").d(
                "Location services (GPS) disabled on device; skipping geofence registration."
            )
            return
        }

        val geofences = activeZones.map { zone ->
            Geofence.Builder()
                .setRequestId(zone.id.toString())
                .setCircularRegion(
                    zone.latitude,
                    zone.longitude,
                    zone.radiusMeters
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setNotificationResponsiveness(0) // Zero latency hint: trigger immediately even when phone is locked
                .build()
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Timber.tag("GeofenceManager").d("Geofences added successfully for ${activeZones.size} zones")
                }
                addOnFailureListener { exception ->
                    if (exception is ApiException) {
                        when (exception.statusCode) {
                            GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION -> {
                                Timber.tag("GeofenceManager").w(
                                    "Location permissions insufficient for background geofences (code 1004). Driver Mode will activate once background location is enabled."
                                )
                                return@addOnFailureListener
                            }
                            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> {
                                Timber.tag("GeofenceManager").w(
                                    "Location service is unavailable or disabled on device (code 1000). Geofencing paused."
                                )
                                return@addOnFailureListener
                            }
                            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> {
                                Timber.tag("GeofenceManager").w("Too many geofences registered (code 1001).")
                                return@addOnFailureListener
                            }
                            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> {
                                Timber.tag("GeofenceManager").w("Too many pending intents for geofences (code 1002).")
                                return@addOnFailureListener
                            }
                        }
                    }
                    Timber.tag("GeofenceManager").w(exception, "Failed to add geofences: ${exception.message}")
                }
            }
        } catch (t: Throwable) {
            Timber.tag("GeofenceManager").w(t, "Exception while submitting geofences request")
        }
    }

    fun removeGeofences() {
        if (!hasLocationPermissions()) {
            return
        }

        try {
            geofencingClient.removeGeofences(geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Timber.tag("GeofenceManager").d("Geofences removed successfully")
                }
                addOnFailureListener { exception ->
                    Timber.tag("GeofenceManager").w("Failed to remove geofences: ${exception.message}")
                }
            }
        } catch (t: Throwable) {
            Timber.tag("GeofenceManager").w(t, "Exception while removing geofences")
        }
    }
}
