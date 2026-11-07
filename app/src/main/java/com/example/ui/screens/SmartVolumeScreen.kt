package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MusicViewModel
import com.example.data.SmartVolumeZoneEntity
import com.google.android.gms.location.LocationServices

@SuppressLint("MissingPermission")
@Composable
fun SmartVolumeScreen(
    onBgColor: Color,
    viewModel: MusicViewModel,
    accentColor: Color,
    surfaceColor: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val zones by viewModel.allSmartVolumeZones.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var zoneToEdit by remember { mutableStateOf<SmartVolumeZoneEntity?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }

    var hasBackgroundLocation by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val bgLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundLocation = granted
        if (granted) {
            viewModel.refreshGeofences()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                currentLocation = location
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasBg = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                hasBackgroundLocation = hasBg
                if (!hasBg) {
                    bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    viewModel.refreshGeofences()
                }
            } else {
                viewModel.refreshGeofences()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocation) {
            hasBackgroundLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    if (showAddDialog) {
        AddSmartVolumeZoneDialog(
            onBgColor = onBgColor,
            viewModel = viewModel,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onDismiss = { showAddDialog = false },
            fusedLocationClient = fusedLocationClient
        )
    }

    if (zoneToEdit != null) {
        EditSmartVolumeZoneDialog(
            zone = zoneToEdit!!,
            onBgColor = onBgColor,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onDismiss = { zoneToEdit = null },
            onSave = { updatedZone ->
                viewModel.updateSmartVolumeZone(updatedZone)
                viewModel.refreshGeofences()
                zoneToEdit = null
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = accentColor)
                }
                Column(modifier = Modifier.offset(x = (-12).dp)) {
                    Text(
                        "DRIVER MODE",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        "Location-based Smart Volume",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "ADD ZONE",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            // Lock Screen & Background Status Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (hasBackgroundLocation) Color(0xFF1B5E20).copy(alpha = 0.25f) else Color(0xFFE65100).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, if (hasBackgroundLocation) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color(0xFFFF9800).copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable {
                        if (!hasBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            } catch (_: Throwable) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (hasBackgroundLocation) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (hasBackgroundLocation) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (hasBackgroundLocation) "ACTIVE WHEN PHONE IS LOCKED" else "LOCK SCREEN PERMISSION NEEDED",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (hasBackgroundLocation) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (hasBackgroundLocation)
                                "Background geofences & wake locks are active. Driver Mode activates as soon as you cross zone radii even with the screen locked or phone in pocket."
                            else
                                "Tap here to allow 'All the time' location access so Driver Mode works reliably while your phone is locked.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.75f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Zones Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Configured Zones (${zones.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Text(
                    "Tap zone to adjust radius",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = onBgColor.copy(alpha = 0.5f)
                )
            }

            if (zones.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Adjust,
                            contentDescription = null,
                            tint = onBgColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No driver zones configured yet.\nTap 'ADD ZONE' to set locations & activation radii.",
                            color = onBgColor.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(zones, key = { it.id }) { zone ->
                        ZoneItem(
                            onBgColor = onBgColor,
                            zone = zone,
                            accentColor = accentColor,
                            surfaceColor = surfaceColor,
                            onToggle = { 
                                viewModel.updateSmartVolumeZone(zone.copy(isActive = it))
                                viewModel.refreshGeofences()
                            },
                            onEdit = { zoneToEdit = zone },
                            onDelete = { 
                                viewModel.deleteSmartVolumeZone(zone)
                                viewModel.refreshGeofences()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZoneItem(
    onBgColor: Color,
    zone: SmartVolumeZoneEntity,
    accentColor: Color,
    surfaceColor: Color,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (zone.isActive) accentColor else onBgColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Lat: ${"%.4f".format(zone.latitude)}, Lng: ${"%.4f".format(zone.longitude)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = onBgColor
                    )
                    Text(
                        if (zone.isGradualFade) "Gradual Volume Fade" else "Immediate Volume Transition",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = onBgColor.copy(alpha = 0.6f)
                    )
                }
                
                Switch(
                    checked = zone.isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Radius & Volume Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Radius Badge with Quick Adjust Indication
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { onEdit() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Adjust, contentDescription = null, tint = accentColor, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Radius: ${zone.radiusMeters.toInt()}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = accentColor
                            )
                        }
                    }

                    // Target Volume Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Vol: ${zone.targetVolumePercent.toInt()}%",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Edit Button
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adjust", color = accentColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

/**
 * Edit dialog to customize radius, target volume, and transition fade for an existing zone.
 */
@Composable
fun EditSmartVolumeZoneDialog(
    zone: SmartVolumeZoneEntity,
    onBgColor: Color,
    accentColor: Color,
    surfaceColor: Color,
    onDismiss: () -> Unit,
    onSave: (SmartVolumeZoneEntity) -> Unit
) {
    var radius by remember { mutableStateOf(zone.radiusMeters) }
    var targetVol by remember { mutableStateOf(zone.targetVolumePercent) }
    var isGradual by remember { mutableStateOf(zone.isGradualFade) }

    val radiusPresets = listOf(50f, 100f, 250f, 500f, 1000f, 2000f)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Adjust Zone Radius",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "ID #${zone.id}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Location: ${"%.4f".format(zone.latitude)}, ${"%.4f".format(zone.longitude)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = onBgColor.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Radius Section Header with Current Value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Activation Radius", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(
                        "${radius.toInt()} meters",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Radius Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    radiusPresets.take(3).forEach { preset ->
                        RadiusPresetChip(
                            radiusMeters = preset,
                            isSelected = (radius.toInt() == preset.toInt()),
                            accentColor = accentColor,
                            onClick = { radius = preset },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    radiusPresets.drop(3).forEach { preset ->
                        RadiusPresetChip(
                            radiusMeters = preset,
                            isSelected = (radius.toInt() == preset.toInt()),
                            accentColor = accentColor,
                            onClick = { radius = preset },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Continuous Radius Slider
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..2500f,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("50m (Close)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = onBgColor.copy(alpha = 0.4f))
                    Text("2.5 km (Wide)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = onBgColor.copy(alpha = 0.4f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target Volume Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Target Volume", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("${targetVol.toInt()}%", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = targetVol,
                    onValueChange = { targetVol = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Gradual Fade Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gradual Volume Fade", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Smoothly steps volume over time", color = onBgColor.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Switch(
                        checked = isGradual,
                        onCheckedChange = { isGradual = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lock-screen operational note
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Activates automatically when phone is locked or screen is off.",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = onBgColor.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                zone.copy(
                                    radiusMeters = radius,
                                    targetVolumePercent = targetVol,
                                    isGradualFade = isGradual
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Changes", color = MaterialTheme.colorScheme.onPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun AddSmartVolumeZoneDialog(
    onBgColor: Color,
    viewModel: MusicViewModel,
    accentColor: Color,
    surfaceColor: Color,
    onDismiss: () -> Unit,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
) {
    val context = LocalContext.current
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(100f) }
    var targetVol by remember { mutableStateOf(50f) }
    var isGradual by remember { mutableStateOf(true) }

    val radiusPresets = listOf(50f, 100f, 250f, 500f, 1000f, 2000f)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Add Driver Zone",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                latitude = loc.latitude.toString()
                                longitude = loc.longitude.toString()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Current Location", color = MaterialTheme.colorScheme.onPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        focusedLabelColor = accentColor
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        focusedLabelColor = accentColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                // Radius Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Activation Radius", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("${radius.toInt()}m", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Radius Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    radiusPresets.take(3).forEach { preset ->
                        RadiusPresetChip(
                            radiusMeters = preset,
                            isSelected = (radius.toInt() == preset.toInt()),
                            accentColor = accentColor,
                            onClick = { radius = preset },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    radiusPresets.drop(3).forEach { preset ->
                        RadiusPresetChip(
                            radiusMeters = preset,
                            isSelected = (radius.toInt() == preset.toInt()),
                            accentColor = accentColor,
                            onClick = { radius = preset },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..2500f,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Target Volume", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("${targetVol.toInt()}%", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = targetVol,
                    onValueChange = { targetVol = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gradual Fade", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Steps volume smoothly on arrival", color = onBgColor.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Switch(
                        checked = isGradual,
                        onCheckedChange = { isGradual = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Operates even when device is locked & screen is off.",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = onBgColor.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val lat = latitude.toDoubleOrNull()
                            val lng = longitude.toDoubleOrNull()
                            if (lat != null && lng != null) {
                                viewModel.insertSmartVolumeZone(
                                    SmartVolumeZoneEntity(
                                        latitude = lat,
                                        longitude = lng,
                                        radiusMeters = radius,
                                        targetVolumePercent = targetVol,
                                        isGradualFade = isGradual
                                    )
                                )
                                viewModel.refreshGeofences()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RadiusPresetChip(
    radiusMeters: Float,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when {
        radiusMeters >= 1000f -> "${(radiusMeters / 1000f).toInt()} km"
        else -> "${radiusMeters.toInt()}m"
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.08f),
        border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) Color.Black else Color.White
            )
        }
    }
}
