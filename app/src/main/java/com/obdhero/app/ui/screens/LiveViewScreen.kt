package com.obdhero.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBg)
            .padding(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("LIVE POWERTRAIN & TELEMETRY", color = RaceRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Real-Time Cockpit", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("← Exit Live", color = TextPrimary, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Primary Speed & RPM Hero Cluster
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                // RPM Card
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ENGINE SPEED", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${telemetry.rpm}",
                            color = RaceRed,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("RPM", color = TextSecondary, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        // RPM Bar
                        val rpmPct = (telemetry.rpm.toFloat() / 7000f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { rpmPct },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (rpmPct > 0.85f) RaceRed else RubyGlow,
                            trackColor = SurfaceCardElevated
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Speed MPH Card
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GROUND SPEED", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${telemetry.speedMph}",
                            color = TextPrimary,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("MPH", color = TextSecondary, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        val speedPct = (telemetry.speedMph.toFloat() / 140f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { speedPct },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = RubyGlow,
                            trackColor = SurfaceCardElevated
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Live TPMS Visual Tire Card
        item {
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TireRepair, contentDescription = null, tint = RaceRed, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("TPMS TIRE PRESSURES & TEMPS", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("TARGET: 32.0 PSI", color = TextMuted, fontSize = 11.sp)
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        TirePressurePod("FRONT LEFT", telemetry.flPsi, telemetry.flTempF)
                        TirePressurePod("FRONT RIGHT", telemetry.frPsi, telemetry.frTempF)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        TirePressurePod("REAR LEFT", telemetry.rlPsi, telemetry.rlTempF)
                        TirePressurePod("REAR RIGHT", telemetry.rrPsi, telemetry.rrTempF)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Geeky Engine Temperatures & Telemetry Grid
        item {
            Text("THERMAL & COMBUSTION TELEMETRY", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                GeekMetricTile("Coolant Temp", "${telemetry.coolantTempF}°F", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                GeekMetricTile("Engine Oil Temp", "${telemetry.oilTempF}°F", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                GeekMetricTile("Intake Air Temp", "${telemetry.intakeTempF}°F", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                GeekMetricTile("Manifold Boost", "%.1f PSI".format(telemetry.boostPsi), Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                GeekMetricTile("Ignition Timing", "${telemetry.sparkAdvanceDeg}° BTDC", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                GeekMetricTile("Battery Alternator", "%.1fV".format(telemetry.batteryVolts), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Live Oscilloscope Simulation / Spark Chart
        item {
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LIVE RPM OSCILLOSCOPE TRACE", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("250ms REFRESH", color = RaceRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))

                    // Animated Oscilloscope Canvas
                    Canvas(modifier = Modifier.fillMaxWidth().height(90.dp).background(SurfaceCardElevated, RoundedCornerShape(8.dp))) {
                        val w = size.width
                        val h = size.height

                        // Grid lines
                        drawLine(Color(0xFF261D21), Offset(0f, h * 0.25f), Offset(w, h * 0.25f), 1f)
                        drawLine(Color(0xFF261D21), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 1f)
                        drawLine(Color(0xFF261D21), Offset(0f, h * 0.75f), Offset(w, h * 0.75f), 1f)

                        val path = Path()
                        val points = 24
                        val stepX = w / (points - 1)

                        for (i in 0 until points) {
                            val wave = Math.sin((i * 0.6) + (telemetry.rpm * 0.005)).toFloat()
                            val y = (h * 0.5f) + (wave * (h * 0.3f))
                            if (i == 0) path.moveTo(0f, y) else path.lineTo(i * stepX, y)
                        }

                        drawPath(path, color = RaceRed, style = Stroke(width = 3.dp.toPx()))
                    }
                }
            }
        }
    }
}

@Composable
private fun TirePressurePod(label: String, psi: Float, tempF: Int) {
    Surface(
        color = SurfaceCardElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(150.dp)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "%.1f PSI".format(psi),
                color = RaceRed,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${tempF}°F",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun GeekMetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = TextMuted, fontSize = 10.sp, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
