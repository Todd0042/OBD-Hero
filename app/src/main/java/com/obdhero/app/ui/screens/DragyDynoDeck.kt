package com.obdhero.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdhero.app.data.PerformanceHighScoresRepository
import com.obdhero.app.domain.*
import com.obdhero.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DragyDynoDialog(
    dragySnapshot: DragyTelemetrySnapshot,
    highScores: VehiclePerformanceHighScores,
    vehicleVin: String,
    onResetRun: () -> Unit,
    onDismiss: () -> Unit
) {
    var showResetConfirmDialog by remember { mutableStateOf(false) }

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
                                .background(CyanPulse.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = CyanPulse, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("VIRTUAL ROAD DYNO & DRAGY", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("0-60 • 1/4 Mile • WHP • High Scores", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Launch Control & Run Status Banner
                RunStatusControlBanner(
                    runState = dragySnapshot.runState,
                    currentSpeedMph = dragySnapshot.currentSpeedMph,
                    onResetRun = onResetRun
                )

                Spacer(Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. 0-60 MPH Acceleration Dragy Card (With High Score Corner Badge)
                    item {
                        PerformanceDragyCard(
                            title = "0 - 60 MPH ACCELERATION",
                            primaryValue = if (dragySnapshot.elapsed0To60Seconds > 0f) "%.2fs".format(dragySnapshot.elapsed0To60Seconds) else "--.--s",
                            subValue = "Target: 60 MPH (${dragySnapshot.currentSpeedMph} MPH current)",
                            highScoreBadge = if (highScores.best0To60Seconds != null) "★ BEST: %.2fs".format(highScores.best0To60Seconds) else "★ BEST: --.--s",
                            isNewRecord = dragySnapshot.isNewRecord0To60,
                            accentColor = RaceRed,
                            progressPct = (dragySnapshot.currentSpeedMph / 60f).coerceIn(0f, 1f)
                        )
                    }

                    // 2. 1/4 Mile Acceleration & Trap Speed Card (With High Score Corner Badge)
                    item {
                        val trapText = if (dragySnapshot.currentTrapSpeedMph > 0) " @ ${dragySnapshot.currentTrapSpeedMph} MPH" else ""
                        val bestQuarterText = if (highScores.bestQuarterMileSeconds != null) {
                            val trap = if (highScores.quarterMileTrapMph != null) " @ ${highScores.quarterMileTrapMph} MPH" else ""
                            "★ BEST: %.2fs%s".format(highScores.bestQuarterMileSeconds, trap)
                        } else "★ BEST: --.--s"

                        PerformanceDragyCard(
                            title = "1/4 MILE DRAG STRIP (1320 FT)",
                            primaryValue = if (dragySnapshot.elapsedQuarterMileSeconds > 0f) "%.2fs%s".format(dragySnapshot.elapsedQuarterMileSeconds, trapText) else "--.--s",
                            subValue = "Distance: %.0f / 1320 ft".format(dragySnapshot.quarterMileDistanceFeet),
                            highScoreBadge = bestQuarterText,
                            isNewRecord = dragySnapshot.isNewRecordQuarter,
                            accentColor = AlertAmber,
                            progressPct = (dragySnapshot.quarterMileDistanceFeet / 1320f).coerceIn(0f, 1f)
                        )
                    }

                    // 3. Virtual Road Dyno: Peak Horsepower Card (With High Score Corner Badge)
                    item {
                        PerformanceDragyCard(
                            title = "ROAD DYNO: HORSEPOWER",
                            primaryValue = "${dragySnapshot.currentHorsepower} HP",
                            subValue = "(Torque x RPM) / 5252 • Current: ${dragySnapshot.currentRpm} RPM",
                            highScoreBadge = if (highScores.peakHorsepower > 0) "★ PEAK: ${highScores.peakHorsepower} HP" else "★ PEAK: -- HP",
                            isNewRecord = dragySnapshot.isNewRecordHp,
                            accentColor = CyanPulse,
                            progressPct = (dragySnapshot.currentHorsepower / 500f).coerceIn(0f, 1f)
                        )
                    }

                    // 4. Virtual Road Dyno: Peak Torque Card (With High Score Corner Badge)
                    item {
                        PerformanceDragyCard(
                            title = "ROAD DYNO: PEAK TORQUE",
                            primaryValue = "${dragySnapshot.currentTorqueLbFt} LB-FT",
                            subValue = "ECM Calculated Net Crankshaft Torque",
                            highScoreBadge = if (highScores.peakTorqueLbFt > 0) "★ PEAK: ${highScores.peakTorqueLbFt} LB-FT" else "★ PEAK: -- LB-FT",
                            isNewRecord = dragySnapshot.isNewRecordTorque,
                            accentColor = EmeraldSuccess,
                            progressPct = (dragySnapshot.currentTorqueLbFt / 500f).coerceIn(0f, 1f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Footer Actions: Reset High Scores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (vehicleVin.isNotBlank()) {
                        Text(
                            text = "VIN: $vehicleVin",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text("Default Vehicle Profile", color = TextMuted, fontSize = 11.sp)
                    }

                    TextButton(onClick = { showResetConfirmDialog = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset High Scores", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset High Scores?") },
            text = { Text("Are you sure you want to clear the personal best records (0-60, 1/4 mile, peak HP, peak torque) for this vehicle?") },
            confirmButton = {
                Button(
                    onClick = {
                        PerformanceHighScoresRepository.resetScores(vehicleVin)
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalCrimson)
                ) {
                    Text("Clear Records")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RunStatusControlBanner(
    runState: DragyRunState,
    currentSpeedMph: Int,
    onResetRun: () -> Unit
) {
    Surface(
        color = SurfaceCardElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (runState) {
                                DragyRunState.STAGED_AT_ZERO -> EmeraldSuccess
                                DragyRunState.ACCELERATING -> RaceRed
                                DragyRunState.FINISHED_60, DragyRunState.FINISHED_QUARTER -> CyanPulse
                                else -> TextMuted
                            }
                        )
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = runState.displayName,
                        color = when (runState) {
                            DragyRunState.STAGED_AT_ZERO -> EmeraldSuccess
                            DragyRunState.ACCELERATING -> RaceRed
                            DragyRunState.FINISHED_60, DragyRunState.FINISHED_QUARTER -> CyanPulse
                            else -> TextSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (runState == DragyRunState.STAGED_AT_ZERO) "Come to a complete stop, then hit accelerator pedal to start" else "Speed: $currentSpeedMph MPH",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Button(
                onClick = onResetRun,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Re-Stage", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Performance Dragy / Dyno card with the "Personal Best" High Score prominently displayed in the top-right corner.
 */
@Composable
fun PerformanceDragyCard(
    title: String,
    primaryValue: String,
    subValue: String,
    highScoreBadge: String,
    isNewRecord: Boolean,
    accentColor: Color,
    progressPct: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (isNewRecord) AlertAmber else BorderSubtle, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Card Title on Left, HIGH SCORE BADGE IN TOP-RIGHT CORNER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // High score badge in the corner of the card
                Surface(
                    color = AlertAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.border(1.dp, AlertAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = highScoreBadge,
                        color = AlertAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Primary Big Digital Reading
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = primaryValue,
                    color = accentColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                if (isNewRecord) {
                    Surface(
                        color = AlertAmber,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "★ NEW RECORD!",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(text = subValue, color = TextMuted, fontSize = 11.sp)

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progressPct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = SurfaceCardElevated
            )
        }
    }
}
