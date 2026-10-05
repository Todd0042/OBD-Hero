package com.obdhero.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.obdhero.app.R
import com.obdhero.app.data.AdapterDatabase
import com.obdhero.app.domain.AdapterProfile
import com.obdhero.app.domain.BlackBoxIncidentReport
import com.obdhero.app.domain.FlightRecorderState
import com.obdhero.app.domain.FocusedMetric
import com.obdhero.app.domain.LiveTelemetry
import com.obdhero.app.ui.theme.*

enum class PodSlot(val title: String) {
    HERO_1("Hero Pod 1 (Top Left)"),
    HERO_2("Hero Pod 2 (Top Center)"),
    HERO_3("Hero Pod 3 (Top Right)"),
    SUB_1("System Tile 1 (Bottom Left)"),
    SUB_2("System Tile 2 (Bottom Center)"),
    SUB_3("System Tile 3 (Bottom Right)")
}

data class MetricDisplayData(
    val label: String,
    val ecuBadge: String,
    val value: String,
    val unit: String,
    val progressPct: Float,
    val accentColor: Color,
    val subText: String = ""
)

fun getMetricDisplayData(metric: FocusedMetric, telemetry: LiveTelemetry): MetricDisplayData {
    return when (metric) {
        FocusedMetric.RPM -> MetricDisplayData("ENGINE SPEED", "ECM 7E0", "${telemetry.rpm}", "RPM", (telemetry.rpm.toFloat() / 7000f).coerceIn(0f, 1f), RaceRed, "Crankshaft Tach")
        FocusedMetric.SPEED -> MetricDisplayData("GROUND SPEED", "ECM 7E0", "${telemetry.speedMph}", "MPH", (telemetry.speedMph.toFloat() / 140f).coerceIn(0f, 1f), RubyGlow, "${telemetry.speedKmh} km/h")
        FocusedMetric.TORQUE -> MetricDisplayData("ENGINE TORQUE", "ECM 7E0", "${telemetry.torqueLbFt}", "LB-FT", (telemetry.torqueLbFt.toFloat() / 400f).coerceIn(0f, 1f), RaceRed, "${telemetry.torqueNm} Nm")
        FocusedMetric.BOOST -> MetricDisplayData("BOOST / VACUUM", "ECM 7E0", "%.1f".format(telemetry.boostPsi), "PSI", (telemetry.boostPsi / 25f).coerceIn(0f, 1f), AlertAmber, "MAP / Boost Pressure")
        FocusedMetric.OIL_PRESSURE -> MetricDisplayData("OIL PRESSURE", "ECM 7E0", "${telemetry.oilPressurePsi}", "PSI", (telemetry.oilPressurePsi / 80f).coerceIn(0f, 1f), EmeraldSuccess, "Main Oil Gallery")
        FocusedMetric.OIL_TEMP -> MetricDisplayData("ENGINE OIL TEMP", "ECM 7E0", "${telemetry.oilTempF}", "°F", ((telemetry.oilTempF - 100f) / 180f).coerceIn(0f, 1f), AlertAmber, "Sump / Gallery")
        FocusedMetric.COOLANT -> MetricDisplayData("COOLANT TEMP", "ECM 7E0", "${telemetry.coolantTempF}", "°F", ((telemetry.coolantTempF - 100f) / 150f).coerceIn(0f, 1f), RubyGlow, "${telemetry.coolantTempC}°C")
        FocusedMetric.INTAKE_AIR -> MetricDisplayData("INTAKE AIR TEMP", "ECM 7E0", "${telemetry.intakeTempF}", "°F", ((telemetry.intakeTempF - 40f) / 120f).coerceIn(0f, 1f), EmeraldSuccess, "IAT Sensor")
        FocusedMetric.BATTERY -> MetricDisplayData("12V BATTERY", "ECM 7E0", "%.1f".format(telemetry.batteryVolts), "VOLTS", ((telemetry.batteryVolts - 11f) / 4f).coerceIn(0f, 1f), EmeraldSuccess, "Alternator Charge")
        FocusedMetric.SPARK -> MetricDisplayData("SPARK TIMING", "ECM 7E0", "%.1f".format(telemetry.sparkAdvanceDeg), "DEG", (telemetry.sparkAdvanceDeg / 40f).coerceIn(0f, 1f), RubyGlow, "Cyl 1 Timing")
        FocusedMetric.TRANSMISSION_GEAR -> MetricDisplayData("TRANSMISSION", "TCM 7E1", telemetry.transGear, "GEAR", ((telemetry.transTempF - 100f) / 160f).coerceIn(0f, 1f), AlertAmber, "${telemetry.transTempF}°F Fluid")
        FocusedMetric.TRANSMISSION_TEMP -> MetricDisplayData("ATF TEMP", "TCM 7E1", "${telemetry.transTempF}", "°F", ((telemetry.transTempF - 100f) / 160f).coerceIn(0f, 1f), AlertAmber, "Valve Body Temp")
        FocusedMetric.FUEL_ECONOMY -> MetricDisplayData("FUEL / AFR", "ECM 7E0", "%.1f".format(telemetry.instantMpg), "MPG", (telemetry.instantMpg / 50f).coerceIn(0f, 1f), EmeraldSuccess, "${telemetry.airFuelRatio}:1 AFR • ${telemetry.fuelLevelPct}%")
        FocusedMetric.DYNAMICS_G -> MetricDisplayData("DYNAMICS G", "ABS 7E2", "%.2f".format(telemetry.lateralG), "G LAT", (Math.abs(telemetry.lateralG.toDouble()).toFloat() / 1.2f).coerceIn(0f, 1f), RubyGlow, "%.0f° Steer • %.2f Gx".format(telemetry.steeringAngleDeg, telemetry.longitudinalG))
        FocusedMetric.TPMS -> MetricDisplayData("TIRE SYSTEM", "BCM 720", "%.1f".format(telemetry.flPsi), "PSI FL", 0.5f, RaceRed, "4-Wheel Monitoring")
    }
}

@Composable
fun LiveViewScreen(
    telemetry: LiveTelemetry,
    focusedMetric: FocusedMetric? = null,
    onSelectMetric: (FocusedMetric?) -> Unit = {},
    vehicleMake: String = "",
    vehicleModel: String = "",
    vehicleVin: String = "",
    adapterProfile: AdapterProfile? = null,
    recorderState: FlightRecorderState = FlightRecorderState.ARMED_LISTENING,
    incidentHistory: List<BlackBoxIncidentReport> = emptyList(),
    onTriggerBlackBox: () -> Unit = {},
    onRearmBlackBox: () -> Unit = {},
    onDeleteBlackBoxIncident: (String) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMiata = vehicleMake.equals("Mazda", ignoreCase = true) && (vehicleModel.contains("MX-5", ignoreCase = true) || vehicleModel.contains("Miata", ignoreCase = true))

    var hero1Metric by remember { mutableStateOf(FocusedMetric.RPM) }
    var hero2Metric by remember { mutableStateOf(FocusedMetric.SPEED) }
    var hero3Metric by remember {
        mutableStateOf(
            if (vehicleMake.contains("Ford", ignoreCase = true)) FocusedMetric.BOOST
            else if (vehicleMake.contains("Chevy", ignoreCase = true) || vehicleMake.contains("GMC", ignoreCase = true)) FocusedMetric.OIL_PRESSURE
            else FocusedMetric.TORQUE
        )
    }
    var sub1Metric by remember { mutableStateOf(FocusedMetric.TRANSMISSION_GEAR) }
    var sub2Metric by remember { mutableStateOf(FocusedMetric.FUEL_ECONOMY) }
    var sub3Metric by remember { mutableStateOf(FocusedMetric.DYNAMICS_G) }

    var configuringSlot by remember { mutableStateOf<PodSlot?>(null) }
    var showBlackBoxDialog by remember { mutableStateOf(false) }

    // Keep the screen awake while Live Telemetry cockpit is displayed
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBg)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isUnfolded = screenWidth >= 600.dp
        val isCompactHeight = screenHeight < 720.dp

        // Dynamic metrics & typography adapted to device display
        val contentPadding = if (isUnfolded) 18.dp else 12.dp
        val heroValSize = if (isUnfolded) 38.sp else if (isCompactHeight) 26.sp else 30.sp
        val heroLabelSize = if (isUnfolded) 11.sp else 10.sp
        val cardPadding = if (isUnfolded) 14.dp else 10.dp
        val sectionSpacing = if (isCompactHeight) 10.dp else 12.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            // Header Bar & ECU Network Status
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MULTI-ECU LIVE TELEMETRY",
                                color = RaceRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            // CAN Bus Heartbeat Pill
                            Surface(
                                color = SurfaceCardElevated,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldSuccess)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("CAN 500k", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Text(
                            text = if (isUnfolded)
                                "Real-Time Powertrain & Chassis Matrix"
                            else
                                "Real-Time Cockpit",
                            color = TextPrimary,
                            fontSize = if (isUnfolded) 20.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { showBlackBoxDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (recorderState == FlightRecorderState.TRIGGERED_RECORDING) CriticalCrimson else SurfaceCardElevated
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.border(
                                1.dp,
                                if (recorderState == FlightRecorderState.TRIGGERED_RECORDING) CriticalCrimson else BorderSubtle,
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Icon(
                                Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = if (recorderState == FlightRecorderState.TRIGGERED_RECORDING) Color.White else RaceRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (recorderState == FlightRecorderState.TRIGGERED_RECORDING) "REC..." else "Black Box",
                                color = if (recorderState == FlightRecorderState.TRIGGERED_RECORDING) Color.White else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { configuringSlot = PodSlot.HERO_3 },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = RaceRed, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Gauges", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("← Exit Live", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // PRIMARY HERO TRIO: Fully Customizable Pods
            item {
                val hero1Data = getMetricDisplayData(hero1Metric, telemetry)
                val hero2Data = getMetricDisplayData(hero2Metric, telemetry)
                val hero3Data = getMetricDisplayData(hero3Metric, telemetry)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroMetricPod(
                        label = hero1Data.label,
                        ecuBadge = hero1Data.ecuBadge,
                        value = hero1Data.value,
                        unit = hero1Data.unit,
                        progressPct = hero1Data.progressPct,
                        accentColor = hero1Data.accentColor,
                        valSize = heroValSize,
                        labelSize = heroLabelSize,
                        cardPadding = cardPadding,
                        isFocused = (focusedMetric == hero1Metric),
                        onConfigure = { configuringSlot = PodSlot.HERO_1 },
                        onClick = { onSelectMetric(if (focusedMetric == hero1Metric) null else hero1Metric) },
                        modifier = Modifier.weight(1f)
                    )

                    HeroMetricPod(
                        label = hero2Data.label,
                        ecuBadge = hero2Data.ecuBadge,
                        value = hero2Data.value,
                        unit = hero2Data.unit,
                        progressPct = hero2Data.progressPct,
                        accentColor = hero2Data.accentColor,
                        valSize = heroValSize,
                        labelSize = heroLabelSize,
                        cardPadding = cardPadding,
                        isFocused = (focusedMetric == hero2Metric),
                        onConfigure = { configuringSlot = PodSlot.HERO_2 },
                        onClick = { onSelectMetric(if (focusedMetric == hero2Metric) null else hero2Metric) },
                        modifier = Modifier.weight(1f)
                    )

                    HeroMetricPod(
                        label = hero3Data.label,
                        ecuBadge = hero3Data.ecuBadge,
                        value = hero3Data.value,
                        unit = hero3Data.unit,
                        progressPct = hero3Data.progressPct,
                        accentColor = hero3Data.accentColor,
                        valSize = heroValSize,
                        labelSize = heroLabelSize,
                        cardPadding = cardPadding,
                        isFocused = (focusedMetric == hero3Metric),
                        onConfigure = { configuringSlot = PodSlot.HERO_3 },
                        onClick = { onSelectMetric(if (focusedMetric == hero3Metric) null else hero3Metric) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // TRANSMISSION, FUEL & CHASSIS G-FORCE POD (MULTI-COMPUTER AGGREGATION - FULLY CUSTOMIZABLE)
            item {
                val sub1Data = getMetricDisplayData(sub1Metric, telemetry)
                val sub2Data = getMetricDisplayData(sub2Metric, telemetry)
                val sub3Data = getMetricDisplayData(sub3Metric, telemetry)

                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardPadding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SubSystemTile(
                            category = sub1Data.label,
                            ecuSource = sub1Data.ecuBadge,
                            mainText = sub1Data.value,
                            subText = sub1Data.subText.ifEmpty { sub1Data.unit },
                            accentColor = sub1Data.accentColor,
                            isFocused = (focusedMetric == sub1Metric),
                            onConfigure = { configuringSlot = PodSlot.SUB_1 },
                            onClick = { onSelectMetric(if (focusedMetric == sub1Metric) null else sub1Metric) },
                            modifier = Modifier.weight(1f)
                        )

                        SubSystemTile(
                            category = sub2Data.label,
                            ecuSource = sub2Data.ecuBadge,
                            mainText = sub2Data.value,
                            subText = sub2Data.subText.ifEmpty { sub2Data.unit },
                            accentColor = sub2Data.accentColor,
                            isFocused = (focusedMetric == sub2Metric),
                            onConfigure = { configuringSlot = PodSlot.SUB_2 },
                            onClick = { onSelectMetric(if (focusedMetric == sub2Metric) null else sub2Metric) },
                            modifier = Modifier.weight(1f)
                        )

                        SubSystemTile(
                            category = sub3Data.label,
                            ecuSource = sub3Data.ecuBadge,
                            mainText = sub3Data.value,
                            subText = sub3Data.subText.ifEmpty { sub3Data.unit },
                            accentColor = sub3Data.accentColor,
                            isFocused = (focusedMetric == sub3Metric),
                            onConfigure = { configuringSlot = PodSlot.SUB_3 },
                            onClick = { onSelectMetric(if (focusedMetric == sub3Metric) null else sub3Metric) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // TPMS & ENTHUSIAST TELEMETRY SECTION
            val isTpmsFocused = (focusedMetric == FocusedMetric.TPMS)
            if (isUnfolded) {
                // Wide Screen / Pixel 9 Pro Fold Unfolded: Side-by-Side 2-Column Layout
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Left Column: TPMS Pods with center MX-5 Silhouette
                        Surface(
                            color = SurfaceCard,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .border(
                                    if (isTpmsFocused) 2.dp else 1.dp,
                                    if (isTpmsFocused) RaceRed else BorderSubtle,
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                TpmsHeader("BCM 720 / MS-CAN", isFocused = isTpmsFocused)
                                Spacer(Modifier.height(10.dp))
                                if (isMiata) {
                                    Mx5TpmsVehicleLayout(
                                        telemetry = telemetry,
                                        isCompact = false,
                                        isFocused = isTpmsFocused,
                                        onSelectTpms = { onSelectMetric(if (focusedMetric == FocusedMetric.TPMS) null else FocusedMetric.TPMS) }
                                    )
                                } else {
                                    UniversalProceduralChassisLayout(
                                        telemetry = telemetry,
                                        isCompact = false,
                                        isFocused = isTpmsFocused,
                                        vehicleMake = vehicleMake,
                                        onSelectTpms = { onSelectMetric(if (focusedMetric == FocusedMetric.TPMS) null else FocusedMetric.TPMS) }
                                    )
                                }
                            }
                        }

                        // Right Column: Thermal & Combustion Telemetry Grid (ECM 0x7E0)
                        Surface(
                            color = SurfaceCard,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(0.85f)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "THERMAL & COMBUSTION",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    EcuBadge("ECM 7E0")
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GeekMetricTile("Coolant", "${telemetry.coolantTempF}°F", isFocused = (focusedMetric == FocusedMetric.COOLANT), onClick = { onSelectMetric(if (focusedMetric == FocusedMetric.COOLANT) null else FocusedMetric.COOLANT) }, modifier = Modifier.weight(1f))
                                    GeekMetricTile("Oil Temp", "${telemetry.oilTempF}°F", isFocused = (focusedMetric == FocusedMetric.OIL_TEMP), onClick = { onSelectMetric(if (focusedMetric == FocusedMetric.OIL_TEMP) null else FocusedMetric.OIL_TEMP) }, modifier = Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GeekMetricTile("Intake Air", "${telemetry.intakeTempF}°F", isFocused = (focusedMetric == FocusedMetric.INTAKE_AIR), onClick = { onSelectMetric(if (focusedMetric == FocusedMetric.INTAKE_AIR) null else FocusedMetric.INTAKE_AIR) }, modifier = Modifier.weight(1f))
                                    GeekMetricTile("Boost", "%.1f PSI".format(telemetry.boostPsi), isFocused = (focusedMetric == FocusedMetric.BOOST), onClick = { onSelectMetric(if (focusedMetric == FocusedMetric.BOOST) null else FocusedMetric.BOOST) }, modifier = Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GeekMetricTile("Spark Timing", "${telemetry.sparkAdvanceDeg}°", isFocused = (focusedMetric == FocusedMetric.SPARK), onClick = { onSelectMetric(if (focusedMetric == FocusedMetric.SPARK) null else FocusedMetric.SPARK) }, modifier = Modifier.weight(1f))
                                    GeekMetricTile("Battery", "%.1fV".format(telemetry.batteryVolts), isFocused = (focusedMetric == FocusedMetric.BATTERY), onClick = { onSelectMetric(if (focusedMetric == FocusedMetric.BATTERY) null else FocusedMetric.BATTERY) }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                // Folded / Single-Column: Vehicle Silhouette with Per-Tire Corner Cards
                item {
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                if (isTpmsFocused) 2.dp else 1.dp,
                                if (isTpmsFocused) RaceRed else BorderSubtle,
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            TpmsHeader("BCM 720 / MS-CAN", isFocused = isTpmsFocused)
                            Spacer(Modifier.height(10.dp))
                            if (isMiata) {
                                Mx5TpmsVehicleLayout(
                                    telemetry = telemetry,
                                    isCompact = true,
                                    isFocused = isTpmsFocused,
                                    onSelectTpms = { onSelectMetric(if (focusedMetric == FocusedMetric.TPMS) null else FocusedMetric.TPMS) }
                                )
                            } else {
                                UniversalProceduralChassisLayout(
                                    telemetry = telemetry,
                                    isCompact = true,
                                    isFocused = isTpmsFocused,
                                    vehicleMake = vehicleMake,
                                    onSelectTpms = { onSelectMetric(if (focusedMetric == FocusedMetric.TPMS) null else FocusedMetric.TPMS) }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "THERMAL & COMBUSTION TELEMETRY",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        EcuBadge("ECM 7E0")
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GeekMetricTile("Coolant", "${telemetry.coolantTempF}°F", isFocused = (focusedMetric == FocusedMetric.COOLANT), onClick = { onSelectMetric(if (focusedMetric == FocusedMetric.COOLANT) null else FocusedMetric.COOLANT) }, modifier = Modifier.weight(1f))
                        GeekMetricTile("Oil Temp", "${telemetry.oilTempF}°F", isFocused = (focusedMetric == FocusedMetric.OIL_TEMP), onClick = { onSelectMetric(if (focusedMetric == FocusedMetric.OIL_TEMP) null else FocusedMetric.OIL_TEMP) }, modifier = Modifier.weight(1f))
                        GeekMetricTile("Intake", "${telemetry.intakeTempF}°F", isFocused = (focusedMetric == FocusedMetric.INTAKE_AIR), onClick = { onSelectMetric(if (focusedMetric == FocusedMetric.INTAKE_AIR) null else FocusedMetric.INTAKE_AIR) }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GeekMetricTile("Boost", "%.1f PSI".format(telemetry.boostPsi), isFocused = (focusedMetric == FocusedMetric.BOOST), onClick = { onSelectMetric(if (focusedMetric == FocusedMetric.BOOST) null else FocusedMetric.BOOST) }, modifier = Modifier.weight(1f))
                        GeekMetricTile("Timing", "${telemetry.sparkAdvanceDeg}°", isFocused = (focusedMetric == FocusedMetric.SPARK), onClick = { onSelectMetric(if (focusedMetric == FocusedMetric.SPARK) null else FocusedMetric.SPARK) }, modifier = Modifier.weight(1f))
                        GeekMetricTile("Battery", "%.1fV".format(telemetry.batteryVolts), isFocused = (focusedMetric == FocusedMetric.BATTERY), onClick = { onSelectMetric(if (focusedMetric == FocusedMetric.BATTERY) null else FocusedMetric.BATTERY) }, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Real-Time Oscilloscope Waveform Trace & Multi-ECU Bus Polling Latency Footer
            item {
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(cardPadding)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = RaceRed, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("LIVE DYNAMICS OSCILLOSCOPE TRACE", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("200ms SYNC", color = RaceRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))

                        val canvasHeight = if (isCompactHeight) 64.dp else 80.dp
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(canvasHeight)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceCardElevated)
                        ) {
                            val w = size.width
                            val h = size.height

                            // Oscilloscope Background Grid Lines
                            drawLine(Color(0xFF261D21), Offset(0f, h * 0.25f), Offset(w, h * 0.25f), 1f)
                            drawLine(Color(0xFF261D21), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 1f)
                            drawLine(Color(0xFF261D21), Offset(0f, h * 0.75f), Offset(w, h * 0.75f), 1f)

                            val path = Path()
                            val points = 32
                            val stepX = w / (points - 1)

                            // Waveform dynamic frequency based on engine RPM and Torque load
                            for (i in 0 until points) {
                                val freq = 0.5 + (telemetry.rpm * 0.0003)
                                val wave = Math.sin((i * freq) + (telemetry.torqueLbFt * 0.02)).toFloat()
                                val y = (h * 0.5f) + (wave * (h * 0.35f))
                                if (i == 0) path.moveTo(0f, y) else path.lineTo(i * stepX, y)
                            }

                            drawPath(path, color = RaceRed, style = Stroke(width = 2.5.dp.toPx()))
                        }

                        Spacer(Modifier.height(10.dp))
                        // Bus & Multi-ECU Provenance Health Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Hub, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("BUS LATENCY:", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "ECM: ${telemetry.ecmLatencyMs}ms  •  TCM: ${telemetry.tcmLatencyMs}ms  •  BCM: ${telemetry.bcmLatencyMs}ms",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // CUSTOMIZE GAUGE POD MODAL DIALOG
        if (configuringSlot != null) {
            val slot = configuringSlot!!
            AlertDialog(
                onDismissRequest = { configuringSlot = null },
                containerColor = SurfaceCard,
                title = {
                    Column {
                        Text(
                            text = "CUSTOMIZE COCKPIT POD",
                            color = RaceRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = slot.title,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    val availableMetrics = listOf(
                        FocusedMetric.RPM,
                        FocusedMetric.SPEED,
                        FocusedMetric.TORQUE,
                        FocusedMetric.BOOST,
                        FocusedMetric.OIL_PRESSURE,
                        FocusedMetric.OIL_TEMP,
                        FocusedMetric.COOLANT,
                        FocusedMetric.INTAKE_AIR,
                        FocusedMetric.BATTERY,
                        FocusedMetric.SPARK,
                        FocusedMetric.TRANSMISSION_GEAR,
                        FocusedMetric.TRANSMISSION_TEMP,
                        FocusedMetric.FUEL_ECONOMY,
                        FocusedMetric.DYNAMICS_G,
                        FocusedMetric.TPMS
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableMetrics.size) { index ->
                            val metric = availableMetrics[index]
                            val data = getMetricDisplayData(metric, telemetry)
                            val isCurrentlySelected = when (slot) {
                                PodSlot.HERO_1 -> hero1Metric == metric
                                PodSlot.HERO_2 -> hero2Metric == metric
                                PodSlot.HERO_3 -> hero3Metric == metric
                                PodSlot.SUB_1 -> sub1Metric == metric
                                PodSlot.SUB_2 -> sub2Metric == metric
                                PodSlot.SUB_3 -> sub3Metric == metric
                            }

                            Surface(
                                color = if (isCurrentlySelected) SurfaceCardElevated else SurfaceCard,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isCurrentlySelected) RaceRed else BorderSubtle,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        when (slot) {
                                            PodSlot.HERO_1 -> hero1Metric = metric
                                            PodSlot.HERO_2 -> hero2Metric = metric
                                            PodSlot.HERO_3 -> hero3Metric = metric
                                            PodSlot.SUB_1 -> sub1Metric = metric
                                            PodSlot.SUB_2 -> sub2Metric = metric
                                            PodSlot.SUB_3 -> sub3Metric = metric
                                        }
                                        configuringSlot = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = data.label,
                                                color = if (isCurrentlySelected) RaceRed else TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            EcuBadge(data.ecuBadge, isFocused = isCurrentlySelected)
                                        }
                                        if (data.subText.isNotEmpty()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(text = data.subText, color = TextMuted, fontSize = 10.sp)
                                        }
                                    }
                                    Text(
                                        text = "${data.value} ${data.unit}",
                                        color = data.accentColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { configuringSlot = null }) {
                        Text("Close", color = TextMuted)
                    }
                }
            )
        }

        // BLACK BOX FLIGHT RECORDER MODAL DIALOG
        if (showBlackBoxDialog) {
            BlackBoxDialog(
                adapterProfile = adapterProfile ?: AdapterDatabase.PROFILES.first { it.name == "Vgate vLinker MS" },
                recorderState = recorderState,
                incidentHistory = incidentHistory,
                currentVin = vehicleVin,
                onTriggerManual = onTriggerBlackBox,
                onRearm = onRearmBlackBox,
                onDeleteIncident = onDeleteBlackBoxIncident,
                onDismiss = { showBlackBoxDialog = false }
            )
        }
    }
}

/**
 * Mazda MX-5 RF Top-Down Silhouette with 4 Corner TPMS Cards.
 * Layout mirrors mx5-dash architecture:
 *   Left Column:  Front-Left (DID 2A05), Rear-Left (DID 2A06)
 *   Center:       Mazda MX-5 RF Top-Down Silhouette
 *   Right Column: Front-Right (DID 2A08), Rear-Right (DID 2A07)
 */
@Composable
private fun Mx5TpmsVehicleLayout(
    telemetry: LiveTelemetry,
    isCompact: Boolean,
    isFocused: Boolean = false,
    onSelectTpms: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column (Driver Side): Front-Left (2A05) & Rear-Left (2A06)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 14.dp)
        ) {
            Mx5CornerTireCard(
                cornerLabel = "FRONT LEFT",
                didCode = "DID 2A05",
                psi = telemetry.flPsi,
                tempF = telemetry.flTempF,
                isCompact = isCompact,
                isFocused = isFocused,
                onClick = onSelectTpms
            )
            Mx5CornerTireCard(
                cornerLabel = "REAR LEFT",
                didCode = "DID 2A06",
                psi = telemetry.rlPsi,
                tempF = telemetry.rlTempF,
                isCompact = isCompact,
                isFocused = isFocused,
                onClick = onSelectTpms
            )
        }

        // Center Column: Mazda MX-5 RF Top-Down Silhouette
        Box(
            modifier = Modifier
                .weight(if (isCompact) 0.85f else 1f)
                .padding(horizontal = 6.dp)
                .clickable { onSelectTpms() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = SurfaceCardElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 140.dp else 170.dp)
                    .border(
                        if (isFocused) 2.dp else 1.dp,
                        if (isFocused) RaceRed else BorderSubtle,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mx5_rf_tpms),
                        contentDescription = "Mazda MX-5 RF Top-Down Silhouette",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Right Column (Passenger Side): Front-Right (2A08) & Rear-Right (2A07)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 14.dp)
        ) {
            Mx5CornerTireCard(
                cornerLabel = "FRONT RIGHT",
                didCode = "DID 2A08",
                psi = telemetry.frPsi,
                tempF = telemetry.frTempF,
                isCompact = isCompact,
                isFocused = isFocused,
                onClick = onSelectTpms
            )
            Mx5CornerTireCard(
                cornerLabel = "REAR RIGHT",
                didCode = "DID 2A07",
                psi = telemetry.rrPsi,
                tempF = telemetry.rrTempF,
                isCompact = isCompact,
                isFocused = isFocused,
                onClick = onSelectTpms
            )
        }
    }
}

/**
 * Universal Procedural Powertrain & Chassis Layout with 4 Corner Tires.
 * Used for all non-MX-5 vehicles (Ford, GM, Chrysler, BMW, Toyota, Honda, etc.),
 * rendering a minimalist procedural spine, axles, differential, powertrain block, and 4 tire health indicators.
 */
@Composable
private fun UniversalProceduralChassisLayout(
    telemetry: LiveTelemetry,
    isCompact: Boolean,
    isFocused: Boolean = false,
    vehicleMake: String = "",
    onSelectTpms: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Front-Left & Rear-Left
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 14.dp)
        ) {
            Mx5CornerTireCard(
                cornerLabel = "FRONT LEFT",
                didCode = "FL TIRE",
                psi = telemetry.flPsi,
                tempF = telemetry.flTempF,
                isCompact = isCompact,
                isFocused = isFocused,
                onClick = onSelectTpms
            )
            Mx5CornerTireCard(
                cornerLabel = "REAR LEFT",
                didCode = "RL TIRE",
                psi = telemetry.rlPsi,
                tempF = telemetry.rlTempF,
                isCompact = isCompact,
                isFocused = isFocused,
                onClick = onSelectTpms
            )
        }

        // Center Column: Minimalist Procedural Chassis Canvas
        Box(
            modifier = Modifier
                .weight(if (isCompact) 0.9f else 1.05f)
                .padding(horizontal = 6.dp)
                .clickable { onSelectTpms() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = SurfaceCardElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 140.dp else 170.dp)
                    .border(
                        if (isFocused) 2.dp else 1.dp,
                        if (isFocused) RaceRed else BorderSubtle,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    ProceduralChassisCanvas(
                        flPsi = telemetry.flPsi,
                        frPsi = telemetry.frPsi,
                        rlPsi = telemetry.rlPsi,
                        rrPsi = telemetry.rrPsi,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = if (vehicleMake.isNotEmpty()) vehicleMake.uppercase() else "CHASSIS",
                        color = TextSecondary.copy(alpha = 0.45f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // Right Column: Front-Right & Rear-Right
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 14.dp)
        ) {
            Mx5CornerTireCard(
                cornerLabel = "FRONT RIGHT",
                didCode = "FR TIRE",
                psi = telemetry.frPsi,
                tempF = telemetry.frTempF,
                isCompact = isCompact,
                isFocused = isFocused,
                onClick = onSelectTpms
            )
            Mx5CornerTireCard(
                cornerLabel = "REAR RIGHT",
                didCode = "RR TIRE",
                psi = telemetry.rrPsi,
                tempF = telemetry.rrTempF,
                isCompact = isCompact,
                isFocused = isFocused,
                onClick = onSelectTpms
            )
        }
    }
}

@Composable
private fun ProceduralChassisCanvas(
    flPsi: Float,
    frPsi: Float,
    rlPsi: Float,
    rrPsi: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val frontAxleY = h * 0.22f
        val rearAxleY = h * 0.78f
        val axleLeft = w * 0.22f
        val axleRight = w * 0.78f

        val spineColor = Color(0xFF334155)
        val axleColor = Color(0xFF475569)
        val componentColor = Color(0xFF1E293B)

        // 1. Central Longitudinal Spine / Driveshaft
        drawLine(
            color = spineColor,
            start = Offset(centerX, frontAxleY - 10f),
            end = Offset(centerX, rearAxleY + 10f),
            strokeWidth = 6f
        )

        // 2. Front & Rear Axles
        drawLine(
            color = axleColor,
            start = Offset(axleLeft, frontAxleY),
            end = Offset(axleRight, frontAxleY),
            strokeWidth = 5f
        )
        drawLine(
            color = axleColor,
            start = Offset(axleLeft, rearAxleY),
            end = Offset(axleRight, rearAxleY),
            strokeWidth = 5f
        )

        // 3. Powertrain Block (Front Engine / Motor)
        drawRoundRect(
            color = componentColor,
            topLeft = Offset(centerX - 16f, frontAxleY - 20f),
            size = androidx.compose.ui.geometry.Size(32f, 40f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = axleColor,
            topLeft = Offset(centerX - 16f, frontAxleY - 20f),
            size = androidx.compose.ui.geometry.Size(32f, 40f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
            style = Stroke(width = 1.5f)
        )

        // 4. Rear Differential
        drawCircle(
            color = componentColor,
            radius = 12f,
            center = Offset(centerX, rearAxleY)
        )
        drawCircle(
            color = axleColor,
            radius = 12f,
            center = Offset(centerX, rearAxleY),
            style = Stroke(width = 1.5f)
        )

        // 5. 4 Wheels / Tires with real-time pressure indicators
        val tireW = 14f
        val tireH = 32f

        fun drawTire(centerXPos: Float, centerYPos: Float, psi: Float) {
            val tireColor = if (psi in 28f..38f) EmeraldSuccess.copy(alpha = 0.9f) else AlertAmber
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(centerXPos - (tireW / 2f), centerYPos - (tireH / 2f)),
                size = androidx.compose.ui.geometry.Size(tireW, tireH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = tireColor,
                topLeft = Offset(centerXPos - (tireW / 2f), centerYPos - (tireH / 2f)),
                size = androidx.compose.ui.geometry.Size(tireW, tireH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                style = Stroke(width = 2f)
            )
            drawLine(
                color = AccentSilver,
                start = Offset(centerXPos, centerYPos - 5f),
                end = Offset(centerXPos, centerYPos + 5f),
                strokeWidth = 2f
            )
        }

        drawTire(axleLeft, frontAxleY, flPsi)
        drawTire(axleRight, frontAxleY, frPsi)
        drawTire(axleLeft, rearAxleY, rlPsi)
        drawTire(axleRight, rearAxleY, rrPsi)
    }
}

@Composable
private fun Mx5CornerTireCard(
    cornerLabel: String,
    didCode: String,
    psi: Float,
    tempF: Int,
    isCompact: Boolean,
    isFocused: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPressureOk = psi in 29.0f..35.0f
    val statusDotColor = if (isPressureOk) EmeraldSuccess else AlertAmber
    val borderColor = if (isFocused) RaceRed else BorderSubtle
    val borderWidth = if (isFocused) 2.dp else 1.dp

    Surface(
        color = if (isFocused) SurfaceCard.copy(alpha = 0.95f) else SurfaceCardElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(if (isCompact) 8.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cornerLabel,
                    color = if (isFocused) RaceRed else TextMuted,
                    fontSize = if (isCompact) 8.sp else 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "%.1f".format(psi),
                    color = RaceRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCompact) 17.sp else 20.sp,
                    maxLines = 1
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "PSI",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Spacer(Modifier.height(1.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tempF}°F",
                    color = TextSecondary,
                    fontSize = if (isCompact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = didCode,
                    color = if (isFocused) RaceRed else TextMuted,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HeroMetricPod(
    label: String,
    ecuBadge: String,
    value: String,
    unit: String,
    progressPct: Float,
    accentColor: Color,
    valSize: TextUnit,
    labelSize: TextUnit,
    cardPadding: Dp,
    isFocused: Boolean = false,
    onConfigure: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = if (isFocused) RaceRed else BorderSubtle
    val borderWidth = if (isFocused) 2.dp else 1.dp

    Surface(
        color = if (isFocused) SurfaceCardElevated else SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = if (isFocused) RaceRed else TextMuted,
                    fontSize = labelSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EcuBadge(ecuBadge, isFocused = isFocused)
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onConfigure,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Customize Gauge",
                            tint = if (isFocused) RaceRed else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = valSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(unit, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progressPct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progressPct > 0.85f) RaceRed else RubyGlow,
                trackColor = SurfaceCardElevated
            )
        }
    }
}

@Composable
private fun SubSystemTile(
    category: String,
    ecuSource: String,
    mainText: String,
    subText: String,
    accentColor: Color,
    isFocused: Boolean = false,
    onConfigure: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = if (isFocused) RaceRed else BorderSubtle
    val borderWidth = if (isFocused) 2.dp else 1.dp

    Surface(
        color = SurfaceCardElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    color = if (isFocused) RaceRed else TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EcuBadge(ecuSource, isFocused = isFocused)
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onConfigure,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Customize Tile",
                            tint = if (isFocused) RaceRed else TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(mainText, color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subText, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun EcuBadge(ecu: String, isFocused: Boolean = false) {
    Surface(
        color = if (isFocused) RaceRed.copy(alpha = 0.25f) else Color(0xFF1E2129),
        shape = RoundedCornerShape(4.dp),
        modifier = if (isFocused) Modifier.border(1.dp, RaceRed, RoundedCornerShape(4.dp)) else Modifier
    ) {
        Text(
            text = ecu,
            color = if (isFocused) RaceRed else TextMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TpmsHeader(badgeText: String = "BCM 720", isFocused: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.TireRepair,
                contentDescription = null,
                tint = if (isFocused) RaceRed else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "TPMS TIRE PRESSURES & TEMPS",
                color = if (isFocused) RaceRed else TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TARGET: 29 PSI", color = TextMuted, fontSize = 10.sp)
            Spacer(Modifier.width(6.dp))
            EcuBadge(badgeText, isFocused = isFocused)
        }
    }
}

@Composable
private fun GeekMetricTile(
    label: String,
    value: String,
    isFocused: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = if (isFocused) RaceRed else BorderSubtle
    val borderWidth = if (isFocused) 2.dp else 1.dp

    Surface(
        color = if (isFocused) SurfaceCardElevated else SurfaceCard,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, color = if (isFocused) RaceRed else TextMuted, fontSize = 9.sp, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        }
    }
}
