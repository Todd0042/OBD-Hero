package com.obdhero.app.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.obdhero.app.data.AdapterDatabase
import com.obdhero.app.domain.AdapterHardwareTier
import com.obdhero.app.domain.AdapterProfile
import com.obdhero.app.domain.BlackBoxIncidentReport
import com.obdhero.app.domain.FlightRecorderState
import com.obdhero.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackBoxDialog(
    adapterProfile: AdapterProfile,
    recorderState: FlightRecorderState,
    incidentHistory: List<BlackBoxIncidentReport>,
    currentVin: String = "",
    onTriggerManual: () -> Unit,
    onRearm: () -> Unit,
    onDeleteIncident: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showDecodedJoke by remember { mutableStateOf(false) }
    var filterCurrentVinOnly by remember { mutableStateOf(false) }

    val displayedIncidents = remember(incidentHistory, currentVin, filterCurrentVinOnly) {
        if (filterCurrentVinOnly && currentVin.isNotBlank()) {
            incidentHistory.filter { it.vehicleVin.equals(currentVin, ignoreCase = true) }
        } else {
            incidentHistory
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CanvasBg,
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RaceRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = RaceRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("BLACK BOX FLIGHT RECORDER", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Pre/Post Fault Telemetry Capture", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Hardware Tier Identity Card
                    item {
                        AdapterTierCard(adapterProfile = adapterProfile)
                    }

                    // 2. Flight Recorder Control Deck
                    item {
                        FlightRecorderControlCard(
                            recorderState = recorderState,
                            adapterTier = adapterProfile.tier,
                            onTriggerManual = onTriggerManual,
                            onRearm = onRearm
                        )
                    }

                    // 3. Incident History Header with VIN Filter
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Captured Incidents (${displayedIncidents.size})",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (recorderState == FlightRecorderState.ARMED_LISTENING) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldSuccess)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("5s Pre-Buffer Active", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (currentVin.isNotBlank() && incidentHistory.any { !it.vehicleVin.equals(currentVin, ignoreCase = true) }) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = !filterCurrentVinOnly,
                                        onClick = { filterCurrentVinOnly = false },
                                        label = { Text("All Vehicles (${incidentHistory.size})", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = filterCurrentVinOnly,
                                        onClick = { filterCurrentVinOnly = true },
                                        label = { Text("Current VIN (${incidentHistory.count { it.vehicleVin.equals(currentVin, ignoreCase = true) }})", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // 4. Incident Reports
                    if (displayedIncidents.isEmpty()) {
                        item {
                            Surface(
                                color = SurfaceCard,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = if (filterCurrentVinOnly) "No incidents recorded for VIN: $currentVin." else "No incidents logged yet.\nWhen an engine fault code triggers or you tap 'Mark Incident', a 35-second pre/post dump will be saved here.",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(displayedIncidents) { incident ->
                            IncidentCard(
                                incident = incident,
                                showDecodedJoke = showDecodedJoke,
                                onToggleJoke = { showDecodedJoke = !showDecodedJoke },
                                onDelete = { onDeleteIncident(incident.incidentId) },
                                onShare = {
                                    val summary = generateIncidentExport(incident)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, summary)
                                        putExtra(Intent.EXTRA_SUBJECT, "OBD-Hero Flight Recorder - ${incident.incidentId}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Export Black Box Incident"))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdapterTierCard(adapterProfile: AdapterProfile) {
    val (badgeColor, badgeText) = when (adapterProfile.tier) {
        AdapterHardwareTier.OPTIMAL -> Pair(Color(0xFF00E5FF), "OPTIMAL TIER")
        AdapterHardwareTier.EXCEPTIONAL -> Pair(Color(0xFFFFD700), "ENTERPRISE PRO")
        AdapterHardwareTier.BLE_FILTERED -> Pair(EmeraldSuccess, "BLE FILTERED")
        AdapterHardwareTier.CHEAP_GARBAGE -> Pair(CriticalCrimson, "BUDGET CLONE")
    }

    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = adapterProfile.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(text = adapterProfile.tier.description, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HardwareStatPill("Buffer", "${adapterProfile.bufferBytes} Bytes", Modifier.weight(1f))
                HardwareStatPill("Baud Max", "${adapterProfile.maxBaudRateBps / 1000}k bps", Modifier.weight(1f))
                HardwareStatPill("Multi-Bus", if (adapterProfile.multiBusCapable) "HS/MS Dual" else "HS-CAN", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HardwareStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = SurfaceCardElevated,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(text = label, color = TextMuted, fontSize = 9.sp)
            Text(text = value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FlightRecorderControlCard(
    recorderState: FlightRecorderState,
    adapterTier: AdapterHardwareTier,
    onTriggerManual: () -> Unit,
    onRearm: () -> Unit
) {
    Surface(
        color = SurfaceCardElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (recorderState) {
                                    FlightRecorderState.ARMED_LISTENING -> EmeraldSuccess
                                    FlightRecorderState.TRIGGERED_RECORDING -> CriticalCrimson
                                    FlightRecorderState.INCIDENT_SAVED -> AlertAmber
                                    else -> TextMuted
                                }
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (recorderState) {
                            FlightRecorderState.ARMED_LISTENING -> "RECORDER ARMED (Listening)"
                            FlightRecorderState.TRIGGERED_RECORDING -> "RECORDING POST-TRIGGER INCIDENT..."
                            FlightRecorderState.INCIDENT_SAVED -> "INCIDENT CAPTURED & SAVED"
                            FlightRecorderState.STANDBY -> "STANDBY"
                        },
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (recorderState == FlightRecorderState.INCIDENT_SAVED) {
                    TextButton(onClick = onRearm, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = RaceRed, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Re-Arm", color = RaceRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onTriggerManual,
                enabled = (recorderState != FlightRecorderState.TRIGGERED_RECORDING),
                colors = ButtonDefaults.buttonColors(containerColor = CriticalCrimson),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("MARK INCIDENT (TRIGGER BLACK BOX)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun IncidentCard(
    incident: BlackBoxIncidentReport,
    showDecodedJoke: Boolean,
    onToggleJoke: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(incident.recordedAt))

    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = incident.incidentId, color = RaceRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = dateStr, color = TextMuted, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Incident", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trigger: ${incident.triggerReason}",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = if (incident.vehicleVin.isNotBlank()) SurfaceCardElevated else CriticalCrimson.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = CyanPulse, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (incident.vehicleVin.isNotBlank()) incident.vehicleVin else "NO VIN",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Easter egg joke view for cheap clone dongles
            if (incident.booleanAsciiJoke != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CriticalCrimson.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "[CLONE UART BUFFER OVERFLOW DUMP]",
                            color = CriticalCrimson,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = incident.booleanAsciiJoke,
                            color = EmeraldSuccess,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 13.sp
                        )

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onToggleJoke,
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (showDecodedJoke) "Hide Binary Translation" else "Translate Binary Payload →",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AnimatedVisibility(visible = showDecodedJoke) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = "Translation: \"${AdapterDatabase.DECODED_JOKE}\"",
                                    color = AlertAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HardwareStatPill("Pre-Trigger", "${incident.preTriggerSamples.size} pts (-5.0s)", Modifier.weight(1f))
                    HardwareStatPill("Post-Trigger", "${incident.postTriggerSamples.size} pts (+30.0s)", Modifier.weight(1f))
                    HardwareStatPill("Freeze Frame", if (incident.freezeFrame != null) "Mode 02 OK" else "N/A", Modifier.weight(1f))
                }
            }
        }
    }
}

fun generateIncidentExport(incident: BlackBoxIncidentReport): String {
    val sb = StringBuilder()
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(incident.recordedAt))
    sb.appendLine("==================================================")
    sb.appendLine("       OBD-HERO BLACK BOX FLIGHT RECORDER DUMP    ")
    sb.appendLine("==================================================")
    sb.appendLine("Incident ID: ${incident.incidentId}")
    sb.appendLine("Timestamp: $dateStr")
    sb.appendLine("Trigger: ${incident.triggerReason}")
    sb.appendLine("Vehicle VIN: ${if (incident.vehicleVin.isNotEmpty()) incident.vehicleVin else "N/A"}")
    sb.appendLine("Adapter: ${incident.adapterProfile.name} (${incident.adapterProfile.tier.badge})")
    sb.appendLine("Hardware Architecture: ${incident.adapterProfile.chipArchitecture}")
    sb.appendLine("Buffer: ${incident.adapterProfile.bufferBytes} Bytes")
    sb.appendLine("--------------------------------------------------")

    if (incident.booleanAsciiJoke != null) {
        sb.appendLine("\n[RAW UART MEMORY INTERCEPT]:")
        sb.appendLine(incident.booleanAsciiJoke)
        sb.appendLine("\nDecoded ASCII: \"${AdapterDatabase.DECODED_JOKE}\"")
    } else {
        sb.appendLine("\n[TELEMETRY FLIGHT DATA (TIME OFFSET, RPM, SPEED, BOOST, OIL PSI, VOLTS, STFT, LTFT)]")
        val allSamples = incident.preTriggerSamples + incident.postTriggerSamples
        sb.appendLine("Offset_ms,RPM,Speed_mph,Boost_psi,Oil_psi,Volts,STFT,LTFT")
        for (s in allSamples) {
            sb.appendLine("${s.timestampOffsetMs},${s.rpm},${s.speedMph},${s.boostPsi},${s.oilPressurePsi},${s.batteryVolts},${s.shortTermFuelTrim},${s.longTermFuelTrim}")
        }

        if (incident.freezeFrame != null) {
            sb.appendLine("\n[ECU MODE 02 HARDWARE FREEZE FRAME SNAPSHOT]")
            sb.appendLine("Trigger DTC: ${incident.freezeFrame.dtc}")
            incident.freezeFrame.rpm?.let { sb.appendLine("RPM at fault: $it") }
            incident.freezeFrame.speedMph?.let { sb.appendLine("Speed at fault: $it mph") }
            incident.freezeFrame.coolantTempF?.let { sb.appendLine("Coolant Temp: $it °F") }
            incident.freezeFrame.engineLoadPct?.let { sb.appendLine("Load: $it %") }
        }
    }

    sb.appendLine("\n==================================================")
    sb.appendLine("Exported by OBD-Hero Workshop Edition")
    sb.appendLine("==================================================")
    return sb.toString()
}
