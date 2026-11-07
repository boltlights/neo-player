#!/bin/bash
cat << 'INNER_EOF' > script.awk
/locationPermissionLauncher =/ {
    print "    var hasBackgroundLocation by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) }"
    print
    next
}
/fusedLocationClient.lastLocation.addOnSuccessListener { location ->/ {
    print
    print "            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && !hasBackgroundLocation) {"
    print "                // Needs background location for Geofences to work properly in background"
    print "            }"
    next
}
/Manifest.permission.ACCESS_BACKGROUND_LOCATION/ {
    # Skip requesting background location in the array to avoid failure on Android 11+
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/ui/screens/SmartVolumeScreen.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/ui/screens/SmartVolumeScreen.kt
