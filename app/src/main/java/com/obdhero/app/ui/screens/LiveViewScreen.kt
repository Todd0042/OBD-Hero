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
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdhero.app.R
import com.obdhero.app.domain.LiveTelemetry
import com.obdhero.app.ui.theme.*

@Composable
fun LiveViewScreen(
    telemetry: LiveTelemetry,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                            text = if (isUnfolded) "Real-Time Powertrain & Chassis Matrix" else "Real-Time Cockpit",
                            color = TextPrimary,
                            fontSize = if (isUnfolded) 20.sp else 17.sp,
                            fontWeight = FontWeight.Bold
                        )
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

            // PRIMARY HERO TRIO: Engine Speed (RPM), Ground Speed (MPH), and Engine Torque (LB-FT)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // RPM (ECM - 0x7E0)
                    HeroMetricPod(
                        label = "ENGINE SPEED",
                        ecuBadge = "ECM 7E0",
                        value = "${telemetry.rpm}",
                        unit = "RPM",
                        progressPct = (telemetry.rpm.toFloat() / 7000f).coerceIn(0f, 1f),
                        accentColor = RaceRed,
                        valSize = heroValSize,
                        labelSize = heroLabelSize,
                        cardPadding = cardPadding,
                        modifier = Modifier.weight(1f)
                    )

                    // Ground Speed MPH (ECM / ABS Wheel Average)
                    HeroMetricPod(
                        label = "GROUND SPEED",
                        ecuBadge = "ECM 7E0",
                        value = "${telemetry.speedMph}",
                        unit = "MPH",
                        progressPct = (telemetry.speedMph.toFloat() / 140f).coerceIn(0f, 1f),
                        accentColor = RubyGlow,
                        valSize = heroValSize,
                        labelSize = heroLabelSize,
                        cardPadding = cardPadding,
                        modifier = Modifier.weight(1f)
                    )

                    // Instantaneous Torque (LB-FT & Nm) (ECM / TCM Computed)
                    HeroMetricPod(
                        label = "ENGINE TORQUE",
                        ecuBadge = "ECM 7E0",
                        value = "${telemetry.torqueLbFt}",
                        unit = "LB-FT (${telemetry.torqueNm} Nm)",
                        progressPct = (telemetry.torqueLbFt.toFloat() / 400f).coerceIn(0f, 1f),
                        accentColor = RaceRed,
                        valSize = heroValSize,
                        labelSize = heroLabelSize,
                        cardPadding = cardPadding,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // TRANSMISSION, FUEL & CHASSIS G-FORCE POD (MULTI-COMPUTER AGGREGATION)
            item {
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
                        // Gear & Transmission Fluid Temp (TCM - 0x7E1)
                        SubSystemTile(
                            category = "TRANSMISSION",
                            ecuSource = "TCM 7E1",
                            mainText = telemetry.transGear,
                            subText = "${telemetry.transTempF}°F Fluid",
                            accentColor = AlertAmber,
                            modifier = Modifier.weight(1f)
                        )

                        // Fuel Economy & Lambda (ECM - Mode 01)
                        SubSystemTile(
                            category = "FUEL / AFR",
                            ecuSource = "ECM 7E0",
                            mainText = "%.1f MPG".format(telemetry.instantMpg),
                            subText = "${telemetry.airFuelRatio}:1 AFR • ${telemetry.fuelLevelPct}%",
                            accentColor = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )

                        // Dynamic Steering & Lateral G (ABS/ESP - 0x7E2)
                        SubSystemTile(
                            category = "DYNAMICS",
                            ecuSource = "ABS 7E2",
                            mainText = "%.2f G".format(telemetry.lateralG),
                            subText = "%.0f° Steer • %.2f Gx".format(telemetry.steeringAngleDeg, telemetry.longitudinalG),
                            accentColor = RubyGlow,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // TPMS & ENTHUSIAST TELEMETRY SECTION
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
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                TpmsHeader("BCM 720 / MS-CAN")
                                Spacer(Modifier.height(10.dp))
                                Mx5TpmsVehicleLayout(telemetry = telemetry, isCompact = false)
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
                                    Text("THERMAL & COMBUSTION", color = RaceRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    EcuBadge("ECM 7E0")
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GeekMetricTile("Coolant", "${telemetry.coolantTempF}°F", Modifier.weight(1f))
                                    GeekMetricTile("Oil Temp", "${telemetry.oilTempF}°F", Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GeekMetricTile("Intake Air", "${telemetry.intakeTempF}°F", Modifier.weight(1f))
                                    GeekMetricTile("Boost", "%.1f PSI".format(telemetry.boostPsi), Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GeekMetricTile("Spark Timing", "${telemetry.sparkAdvanceDeg}°", Modifier.weight(1f))
                                    GeekMetricTile("Battery", "%.1fV".format(telemetry.batteryVolts), Modifier.weight(1f))
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
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            TpmsHeader("BCM 720 / MS-CAN")
                            Spacer(Modifier.height(10.dp))
                            Mx5TpmsVehicleLayout(telemetry = telemetry, isCompact = true)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("THERMAL & COMBUSTION TELEMETRY", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        EcuBadge("ECM 7E0")
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GeekMetricTile("Coolant", "${telemetry.coolantTempF}°F", Modifier.weight(1f))
                        GeekMetricTile("Oil Temp", "${telemetry.oilTempF}°F", Modifier.weight(1f))
                        GeekMetricTile("Intake", "${telemetry.intakeTempF}°F", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GeekMetricTile("Boost", "%.1f PSI".format(telemetry.boostPsi), Modifier.weight(1f))
                        GeekMetricTile("Timing", "${telemetry.sparkAdvanceDeg}°", Modifier.weight(1f))
                        GeekMetricTile("Battery", "%.1fV".format(telemetry.batteryVolts), Modifier.weight(1f))
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
                isCompact = isCompact
            )
            Mx5CornerTireCard(
                cornerLabel = "REAR LEFT",
                didCode = "DID 2A06",
                psi = telemetry.rlPsi,
                tempF = telemetry.rlTempF,
                isCompact = isCompact
            )
        }

        // Center Column: Mazda MX-5 RF Top-Down Silhouette
        Box(
            modifier = Modifier
                .weight(if (isCompact) 0.85f else 1f)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = SurfaceCardElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 140.dp else 170.dp)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
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
                isCompact = isCompact
            )
            Mx5CornerTireCard(
                cornerLabel = "REAR RIGHT",
                didCode = "DID 2A07",
                psi = telemetry.rrPsi,
                tempF = telemetry.rrTempF,
                isCompact = isCompact
            )
        }
    }
}

@Composable
private fun Mx5CornerTireCard(
    cornerLabel: String,
    didCode: String,
    psi: Float,
    tempF: Int,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val isPressureOk = psi in 29.0f..35.0f
    val statusDotColor = if (isPressureOk) EmeraldSuccess else AlertAmber

    Surface(
        color = SurfaceCardElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
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
                    color = TextMuted,
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
                    color = TextMuted,
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
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
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
                Text(label, color = TextMuted, fontSize = labelSize, fontWeight = FontWeight.Bold, maxLines = 1)
                EcuBadge(ecuBadge)
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
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCardElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                EcuBadge(ecuSource)
            }
            Spacer(Modifier.height(4.dp))
            Text(mainText, color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subText, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun EcuBadge(ecu: String) {
    Surface(
        color = Color(0xFF1E2129),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = ecu,
            color = TextMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TpmsHeader(badgeText: String = "BCM 720") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TireRepair, contentDescription = null, tint = RaceRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("TPMS TIRE PRESSURES & TEMPS", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TARGET: 29 PSI", color = TextMuted, fontSize = 10.sp)
            Spacer(Modifier.width(6.dp))
            EcuBadge(badgeText)
        }
    }
}

@Composable
private fun GeekMetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, color = TextMuted, fontSize = 9.sp, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        }
    }
}
