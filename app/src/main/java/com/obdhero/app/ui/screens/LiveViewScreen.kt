package com.obdhero.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        val isCompactHeight = screenHeight < 700.dp

        // Dynamic spacing & font sizing based on actual screen dimensions
        val contentPadding = if (isUnfolded) 20.dp else 12.dp
        val heroValSize = if (isUnfolded) 40.sp else if (isCompactHeight) 28.sp else 32.sp
        val heroLabelSize = if (isUnfolded) 11.sp else 10.sp
        val cardPadding = if (isUnfolded) 14.dp else 10.dp
        val sectionSpacing = if (isCompactHeight) 10.dp else 14.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LIVE POWERTRAIN & DYNAMICS",
                            color = RaceRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isUnfolded) "High-Performance Cockpit (Unfolded Dual-Grid)" else "Real-Time Cockpit",
                            color = TextPrimary,
                            fontSize = if (isUnfolded) 22.sp else 18.sp,
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
                    // RPM
                    HeroMetricPod(
                        label = "ENGINE SPEED",
                        value = "${telemetry.rpm}",
                        unit = "RPM",
                        progressPct = (telemetry.rpm.toFloat() / 7000f).coerceIn(0f, 1f),
                        accentColor = RaceRed,
                        valSize = heroValSize,
                        labelSize = heroLabelSize,
                        cardPadding = cardPadding,
                        modifier = Modifier.weight(1f)
                    )

                    // Ground Speed MPH
                    HeroMetricPod(
                        label = "GROUND SPEED",
                        value = "${telemetry.speedMph}",
                        unit = "MPH",
                        progressPct = (telemetry.speedMph.toFloat() / 140f).coerceIn(0f, 1f),
                        accentColor = RubyGlow,
                        valSize = heroValSize,
                        labelSize = heroLabelSize,
                        cardPadding = cardPadding,
                        modifier = Modifier.weight(1f)
                    )

                    // Instantaneous Torque (LB-FT & Nm)
                    HeroMetricPod(
                        label = "ENGINE TORQUE",
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

            // TPMS & ENTHUSIAST TELEMETRY SECTION
            if (isUnfolded) {
                // Wide Screen / Pixel 9 Pro Fold Unfolded: Side-by-Side 2-Column Layout
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Left Column: TPMS Pods
                        Surface(
                            color = SurfaceCard,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                TpmsHeader()
                                Spacer(Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TirePressurePod("FRONT LEFT", telemetry.flPsi, telemetry.flTempF, Modifier.weight(1f))
                                    TirePressurePod("FRONT RIGHT", telemetry.frPsi, telemetry.frTempF, Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TirePressurePod("REAR LEFT", telemetry.rlPsi, telemetry.rlTempF, Modifier.weight(1f))
                                    TirePressurePod("REAR RIGHT", telemetry.rrPsi, telemetry.rrTempF, Modifier.weight(1f))
                                }
                            }
                        }

                        // Right Column: Thermal & Combustion Telemetry Grid
                        Surface(
                            color = SurfaceCard,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("THERMAL & COMBUSTION TELEMETRY", color = RaceRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GeekMetricTile("Coolant Temp", "${telemetry.coolantTempF}°F", Modifier.weight(1f))
                                    GeekMetricTile("Engine Oil", "${telemetry.oilTempF}°F", Modifier.weight(1f))
                                    GeekMetricTile("Intake Temp", "${telemetry.intakeTempF}°F", Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GeekMetricTile("Manifold Boost", "%.1f PSI".format(telemetry.boostPsi), Modifier.weight(1f))
                                    GeekMetricTile("Spark Timing", "${telemetry.sparkAdvanceDeg}°", Modifier.weight(1f))
                                    GeekMetricTile("Battery Volts", "%.1fV".format(telemetry.batteryVolts), Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                // Folded / Single-Column: Stacked TPMS & Thermals
                item {
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            TpmsHeader()
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TirePressurePod("FL", telemetry.flPsi, telemetry.flTempF, Modifier.weight(1f))
                                TirePressurePod("FR", telemetry.frPsi, telemetry.frTempF, Modifier.weight(1f))
                                TirePressurePod("RL", telemetry.rlPsi, telemetry.rlTempF, Modifier.weight(1f))
                                TirePressurePod("RR", telemetry.rrPsi, telemetry.rrTempF, Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    Text("THERMAL & COMBUSTION TELEMETRY", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

            // Real-Time Oscilloscope Waveform Trace
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
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricPod(
    label: String,
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
            Text(label, color = TextMuted, fontSize = labelSize, fontWeight = FontWeight.Bold, maxLines = 1)
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
private fun TpmsHeader() {
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
        Text("TARGET: 32 PSI", color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun TirePressurePod(label: String, psi: Float, tempF: Int, modifier: Modifier = Modifier) {
    Surface(
        color = SurfaceCardElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "%.1f".format(psi),
                color = RaceRed,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text("PSI • ${tempF}°F", color = TextSecondary, fontSize = 9.sp)
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
