package com.obdhero.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdhero.app.data.DtcRepository
import com.obdhero.app.domain.DtcInfo
import com.obdhero.app.domain.FreezeFrameData
import com.obdhero.app.domain.ModuleDtcReport
import com.obdhero.app.ui.theme.*

@Composable
fun DiagnosticsScreen(
    storedCodes: List<String>,
    pendingCodes: List<String>,
    selectedDtc: DtcInfo?,
    onSelectDtc: (DtcInfo) -> Unit,
    onClearCodes: () -> Unit,
    onOpenLiveView: () -> Unit = {},
    vehicleMake: String = "",
    isDualPane: Boolean = false,
    moduleReports: List<ModuleDtcReport> = emptyList(),
    isScanning: Boolean = false,
    onScanAllModules: () -> Unit = {},
    freezeFrame: FreezeFrameData? = null,
    modifier: Modifier = Modifier
) {
    var showClearWarningDialog by remember { mutableStateOf(false) }

    if (showClearWarningDialog) {
        AlertDialog(
            onDismissRequest = { showClearWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AlertAmber)
                    Spacer(Modifier.width(8.dp))
                    Text("Emissions Monitor Warning", color = TextPrimary)
                }
            },
            text = {
                Text(
                    "Clearing diagnostic trouble codes will immediately reset all engine emissions readiness monitors (Catalyst, EVAP, O2, Misfire) to 'NOT READY'.\n\nYour vehicle will fail state inspection/smog checks until a full drive cycle is completed.\n\nDo you wish to proceed?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearWarningDialog = false
                        onClearCodes()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalCrimson)
                ) {
                    Text("Clear Codes & Reset Monitors", color = TextPrimary)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearWarningDialog = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (isDualPane) {
        // Dual-Pane Layout for Pixel 9 Pro Fold Unfolded
        Row(modifier = modifier.fillMaxSize().padding(16.dp)) {
            // Left pane: Live View Button & Code List
            Column(modifier = Modifier.weight(0.42f).fillMaxHeight()) {
                LiveViewLauncherButton(onClick = onOpenLiveView)
                Spacer(modifier = Modifier.height(14.dp))
                DtcListSection(
                    storedCodes = storedCodes,
                    pendingCodes = pendingCodes,
                    selectedCode = selectedDtc?.code,
                    moduleReports = moduleReports,
                    isScanning = isScanning,
                    vehicleMake = vehicleMake,
                    onScanAllModules = onScanAllModules,
                    onSelectCode = { code -> onSelectDtc(DtcRepository.get(code, vehicleMake)) },
                    onOpenClearDialog = { showClearWarningDialog = true }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Right pane: Detail Guide
            Column(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
                if (selectedDtc != null) {
                    DtcDetailGuideCard(dtcInfo = selectedDtc, freezeFrame = freezeFrame)
                } else {
                    EmptyDtcGuideCard()
                }
            }
        }
    } else {
        // Single-Column Layout for Folded Phone
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            if (selectedDtc != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onSelectDtc(DtcRepository.get("", vehicleMake)) }) {
                        Text("← Back to Fault List", color = RaceRed)
                    }
                }
                DtcDetailGuideCard(dtcInfo = selectedDtc, freezeFrame = freezeFrame)
            } else {
                LiveViewLauncherButton(onClick = onOpenLiveView)
                Spacer(modifier = Modifier.height(14.dp))
                DtcListSection(
                    storedCodes = storedCodes,
                    pendingCodes = pendingCodes,
                    selectedCode = null,
                    moduleReports = moduleReports,
                    isScanning = isScanning,
                    vehicleMake = vehicleMake,
                    onScanAllModules = onScanAllModules,
                    onSelectCode = { code -> onSelectDtc(DtcRepository.get(code, vehicleMake)) },
                    onOpenClearDialog = { showClearWarningDialog = true }
                )
            }
        }
    }
}

@Composable
private fun LiveViewLauncherButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = RaceRed),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, RubyGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("LIVE POWERTRAIN COCKPIT", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Speed • RPM • TPMS • Temperatures • Oscilloscope", color = TextPrimary.copy(alpha = 0.8f), fontSize = 10.sp)
                }
            }
            Text("OPEN →", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DtcListSection(
    storedCodes: List<String>,
    pendingCodes: List<String>,
    selectedCode: String?,
    moduleReports: List<ModuleDtcReport> = emptyList(),
    isScanning: Boolean = false,
    vehicleMake: String? = null,
    onScanAllModules: () -> Unit = {},
    onSelectCode: (String) -> Unit,
    onOpenClearDialog: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Diagnostic Codes (${storedCodes.size + pendingCodes.size})",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Scan All ECUs Button
                Button(
                    onClick = onScanAllModules,
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(color = RaceRed, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scanning...", color = RaceRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Scan All ECUs", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (storedCodes.isNotEmpty() || pendingCodes.isNotEmpty()) {
                    Button(
                        onClick = onOpenClearDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = CriticalCrimson.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CriticalCrimson, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear", color = CriticalCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Multi-Computer CAN Health Bar
        if (moduleReports.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            MultiModuleHealthChips(moduleReports = moduleReports)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (storedCodes.isEmpty() && pendingCodes.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                color = SurfaceCard
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No Fault Codes Detected", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Powertrain and body computers reporting healthy.", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (storedCodes.isNotEmpty()) {
                    item {
                        Text("CONFIRMED / STORED", color = CriticalCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(storedCodes) { code ->
                        val originModule = moduleReports.find { it.storedCodes.contains(code) }?.module?.badge
                        DtcItemRow(
                            code = code,
                            vehicleMake = vehicleMake,
                            moduleBadge = originModule,
                            isPending = false,
                            isSelected = (selectedCode == code),
                            onClick = { onSelectCode(code) }
                        )
                    }
                }
                if (pendingCodes.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("PENDING", color = AlertAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(pendingCodes) { code ->
                        val originModule = moduleReports.find { it.pendingCodes.contains(code) }?.module?.badge
                        DtcItemRow(
                            code = code,
                            vehicleMake = vehicleMake,
                            moduleBadge = originModule,
                            isPending = true,
                            isSelected = (selectedCode == code),
                            onClick = { onSelectCode(code) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiModuleHealthChips(
    moduleReports: List<ModuleDtcReport>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        moduleReports.forEach { report ->
            val hasFaults = report.totalFaults > 0
            val chipBg = if (!report.isResponding) SurfaceCardElevated else if (hasFaults) CriticalCrimson.copy(alpha = 0.15f) else EmeraldSuccess.copy(alpha = 0.15f)
            val chipBorder = if (!report.isResponding) BorderSubtle else if (hasFaults) CriticalCrimson.copy(alpha = 0.5f) else EmeraldSuccess.copy(alpha = 0.5f)
            val textColor = if (!report.isResponding) TextMuted else if (hasFaults) CriticalCrimson else EmeraldSuccess

            Surface(
                color = chipBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(1.dp, chipBorder, RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (!report.isResponding) Icons.Default.Hub else if (hasFaults) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = report.module.badge,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (!report.isResponding) "Offline" else if (hasFaults) "${report.totalFaults} Faults" else "Healthy",
                            color = textColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DtcItemRow(
    code: String,
    vehicleMake: String? = null,
    moduleBadge: String? = null,
    isPending: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dtcInfo = remember(code, vehicleMake) { DtcRepository.get(code, vehicleMake ?: "") }
    val borderColor = if (isSelected) RaceRed else BorderSubtle

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = if (isSelected) SurfaceCardElevated else SurfaceCard
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isPending) AlertAmber else CriticalCrimson)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = code, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (moduleBadge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = SurfaceCardElevated,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = moduleBadge,
                                color = RaceRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (dtcInfo.isCommunitySourced) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = AlertAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(1.dp, AlertAmber.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = "COMMUNITY",
                                color = AlertAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = dtcInfo.category, color = TextMuted, fontSize = 11.sp, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = dtcInfo.title, color = TextSecondary, fontSize = 13.sp, maxLines = 1)
            }
            Text("Guide →", color = RaceRed, fontSize = 12.sp)
        }
    }
}

@Composable
fun DtcDetailGuideCard(
    dtcInfo: DtcInfo,
    freezeFrame: FreezeFrameData? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        color = SurfaceCard
    ) {
        LazyColumn(modifier = Modifier.padding(20.dp)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(text = dtcInfo.code, color = RaceRed, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(text = dtcInfo.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(text = dtcInfo.category, color = TextSecondary, fontSize = 12.sp)
                    }
                    Surface(
                        color = SurfaceCardElevated,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = dtcInfo.difficulty,
                            color = AlertAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(16.dp))

                // Prominent Orange/Amber Community Sourced Disclaimer Banner
                if (dtcInfo.isCommunitySourced) {
                    Surface(
                        color = AlertAmber.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.5.dp, AlertAmber, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "COMMUNITY-SOURCED FIELD REPAIR DATA",
                                    color = AlertAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "This repair procedure is compiled from technician forum consensus, mechanic field logs, and enthusiast community teardowns. Accurate results may vary depending on vehicle trim, engine, and operating conditions.\n\nFor official factory specifications, wiring schematics, and manufacturer service bulletins, consult an authorized OEM repair facility.",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                // Mode 02 Freeze Frame Engine Snapshot (if recorded for this DTC)
                if (freezeFrame != null) {
                    Surface(
                        color = SurfaceCardElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.dp, RaceRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = RaceRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("MODE 02 FREEZE FRAME SNAPSHOT", color = RaceRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("AT DTC TRIP", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FreezeFrameStat("Engine Speed", "${freezeFrame.rpm ?: "--"} RPM", Modifier.weight(1f))
                                FreezeFrameStat("Vehicle Speed", "${freezeFrame.speedMph ?: "--"} MPH", Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FreezeFrameStat("Coolant Temp", "${freezeFrame.coolantTempF ?: "--"}°F", Modifier.weight(1f))
                                FreezeFrameStat("Engine Load", "${freezeFrame.engineLoadPct ?: "--"}%", Modifier.weight(1f))
                            }
                            if (freezeFrame.shortTermFuelTrim != null || freezeFrame.longTermFuelTrim != null) {
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FreezeFrameStat("STFT Bank 1", "${freezeFrame.shortTermFuelTrim?.let { if (it > 0) "+%.1f".format(it) else "%.1f".format(it) } ?: "--"}%", Modifier.weight(1f))
                                    FreezeFrameStat("LTFT Bank 1", "${freezeFrame.longTermFuelTrim?.let { if (it > 0) "+%.1f".format(it) else "%.1f".format(it) } ?: "--"}%", Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Text("WHAT THE ECU DETECTED", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = dtcInfo.meaning, color = TextPrimary, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(16.dp))
                Text("SYMPTOMS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                dtcInfo.symptoms.forEach { symptom ->
                    Text("• $symptom", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("STEP-BY-STEP INSPECTION", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                dtcInfo.inspectionSteps.forEachIndexed { i, step ->
                    Text("${i + 1}. $step", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 3.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("VERIFIED FIXES", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                dtcInfo.verifiedFixes.forEach { fix ->
                    Text("✔ $fix", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun FreezeFrameStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyDtcGuideCard() {
    Surface(
        modifier = Modifier.fillMaxSize().border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        color = SurfaceCard
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
            Text("Select a Diagnostic Code to view actionable repair guide", color = TextMuted, fontSize = 14.sp)
        }
    }
}
