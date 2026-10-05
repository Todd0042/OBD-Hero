package com.obdhero.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdhero.app.domain.EmissionsReadiness
import com.obdhero.app.ui.theme.*

@Composable
fun EmissionsScreen(
    readiness: EmissionsReadiness?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "I/M Emissions Readiness Dashboard",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Verifies whether onboard emissions diagnostics have completed their drive cycle tests for state smog inspections.",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (readiness == null) {
            item {
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
                        Text("Connect scanner to query OBD-II Mode 01 PID 01 readiness monitors", color = TextMuted)
                    }
                }
            }
        } else {
            item {
                // Pass / Fail Overall Card
                val overallPass = readiness.isOverallPass
                Surface(
                    color = if (overallPass) EmeraldSuccess.copy(alpha = 0.15f) else CriticalCrimson.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (overallPass) EmeraldSuccess else CriticalCrimson, RoundedCornerShape(16.dp))
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (overallPass) Icons.Default.CheckCircle else Icons.Default.HighlightOff,
                            contentDescription = null,
                            tint = if (overallPass) EmeraldSuccess else CriticalCrimson,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (overallPass) "READY FOR STATE INSPECTION" else "NOT READY (SMOG CHECK WILL FAIL)",
                                color = if (overallPass) EmeraldSuccess else CriticalCrimson,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (overallPass) "All continuous and non-continuous emissions monitors completed." else "One or more emissions monitors have not completed a drive cycle.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("INDIVIDUAL MONITOR STATUS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                MonitorRow("Misfire Monitor (Continuous)", readiness.misfireReady)
                MonitorRow("Fuel System Monitor (Continuous)", readiness.fuelSystemReady)
                MonitorRow("Comprehensive Components (Continuous)", readiness.compComponentsReady)
                MonitorRow("Catalytic Converter Efficiency", readiness.catalystReady)
                MonitorRow("Evaporative Emission (EVAP) System", readiness.evapReady)
                MonitorRow("Oxygen / Air-Fuel Sensors", readiness.o2SensorReady)
                MonitorRow("EGR / VVT System", readiness.egrReady)
            }
        }
    }
}

@Composable
private fun MonitorRow(name: String, isReady: Boolean) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, color = TextPrimary, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.HighlightOff,
                    contentDescription = null,
                    tint = if (isReady) EmeraldSuccess else CriticalCrimson,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isReady) "COMPLETE" else "NOT READY",
                    color = if (isReady) EmeraldSuccess else CriticalCrimson,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
