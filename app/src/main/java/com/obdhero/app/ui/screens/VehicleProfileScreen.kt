package com.obdhero.app.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdhero.app.domain.VehicleProfile
import com.obdhero.app.obd.ConnectionState
import com.obdhero.app.ui.theme.*

@Composable
fun VehicleProfileScreen(
    profile: VehicleProfile?,
    connectionState: ConnectionState,
    pairedDevices: List<BluetoothDevice>,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onStartSimulator: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Vehicle Profile & Hardware",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Decoded VIN profile, gateway parameters, and system-bonded diagnostic tools.",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Active Vehicle Identification Card
        item {
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = RaceRed)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (profile != null && profile.make != "Unknown")
                                "${profile.year ?: ""} ${profile.make} ${profile.model}"
                            else
                                "No Vehicle Connected",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    if (profile != null && profile.vin.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileDetailRow("VIN", profile.vin)
                        ProfileDetailRow("Origin / Make", profile.make)
                        ProfileDetailRow("Model Classification", profile.model)
                        ProfileDetailRow("Decoded Year", profile.year?.toString() ?: "Undetermined")

                        if (profile.isSgwLocked) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = AlertAmber.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AlertAmber, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Security Gateway (SGW) Active: Direct OBD write requires SGW bypass harness.", color = AlertAmber, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("CONNECT TO OBD-II ADAPTER", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Virtual ECU Simulator Button
        item {
            Surface(
                color = SurfaceCardElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, RaceRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { onStartSimulator() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = RaceRed)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Launch Virtual ECU Simulator", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Test telemetry, BMW FRM3 gating, & DTCs at your desk", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Text("START →", color = RaceRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // List of System-Paired OBD Scanners
        if (pairedDevices.isEmpty()) {
            item {
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("No Paired OBD Adapters Found", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Pair your vLinker MS, OBDLink CX, or ELM327 in Android System Settings (Settings > Bluetooth), then return here to connect.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(pairedDevices) { device ->
                val isThisConnected = connectionState is ConnectionState.Connected &&
                        ((connectionState.deviceName == device.name) || (device.name != null && connectionState.deviceName.contains(device.name!!)))
                val isThisConnecting = connectionState is ConnectionState.Connecting &&
                        ((connectionState.deviceName == device.name) || (device.name != null && connectionState.deviceName.contains(device.name!!)))

                Surface(
                    color = if (isThisConnected) SurfaceCardElevated else SurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            1.dp,
                            if (isThisConnected) EmeraldSuccess.copy(alpha = 0.6f) else BorderSubtle,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (isThisConnected) {
                                onDisconnect()
                            } else {
                                onConnectDevice(device)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isThisConnected) EmeraldSuccess else RaceRed
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(device.name ?: "Unknown OBD Tool", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(device.address, color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        when {
                            isThisConnected -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = EmeraldSuccess.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "CONNECTED",
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("DISCONNECT", color = CriticalCrimson, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            isThisConnecting -> {
                                Text("CONNECTING…", color = AlertAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            else -> {
                                Text("CONNECT", color = RaceRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
