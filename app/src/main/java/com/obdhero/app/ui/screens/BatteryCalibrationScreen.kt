package com.obdhero.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdhero.app.domain.BmwSleepLockManager
import com.obdhero.app.domain.VehicleProfile
import com.obdhero.app.ui.theme.*

@Composable
fun BatteryCalibrationScreen(
    profile: VehicleProfile?,
    lockManager: BmwSleepLockManager,
    onExecuteReset: () -> Unit,
    isDualPane: Boolean = false,
    modifier: Modifier = Modifier
) {
    var lockState by remember { mutableStateOf(lockManager.checkState()) }
    var checkedGearShifterOff by remember { mutableStateOf(false) }
    var checkedHazardOff by remember { mutableStateOf(false) }
    var resetSuccess by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            lockState = lockManager.checkState()
            kotlinx.coroutines.delay(1000)
        }
    }

    val isBmwFrm = profile?.isBmwFrmVulnerable == true

    var showWalkthroughDialog by remember { mutableStateOf(false) }

    if (showWalkthroughDialog && profile != null) {
        BmwSleepWalkthroughDialog(
            lockManager = lockManager,
            vin = profile.vin,
            onDismiss = { showWalkthroughDialog = false },
            onComplete = {
                showWalkthroughDialog = false
                lockState = lockManager.checkState()
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "12V Battery Calibration & BMS Reset",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Informs the alternator and power management computers of a fresh battery to recalibrate voltage regulation.",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // BMW / Mini FRM3 Hard Safety Gate
        if (isBmwFrm) {
            item {
                BmwFrmSafetyBanner()
                Spacer(modifier = Modifier.height(16.dp))

                if (lockState.isLocked) {
                    BmwCountdownLockCard(
                        remainingSeconds = lockState.remainingSeconds,
                        checkedGearShifter = checkedGearShifterOff,
                        onCheckGearShifter = { checkedGearShifterOff = it },
                        checkedHazard = checkedHazardOff,
                        onCheckHazard = { checkedHazardOff = it }
                    )
                } else {
                    BmwSleepPreparationCard(
                        onStartTimer = {
                            showWalkthroughDialog = true
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }


        // Standard Calibration Action Card
        item {
            val canExecute = if (isBmwFrm) (!lockState.isLocked && checkedGearShifterOff && checkedHazardOff) else true

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                color = SurfaceCard
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ECU Calibration Execution",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val procedureText = when (profile?.make?.uppercase()) {
                        "FORD" -> "Ford UDS BCM Routine: Mode 31 01 22 40 28 resets battery age counter to 0 days."
                        "BMW", "MINI" -> "BMW CBS Battery Replacement: DME job registers new battery mileage and wipes charge histogram."
                        "CHEVROLET", "GMC", "CADILLAC" -> "GM BCM Adaptation: Resets battery state-of-charge calculation model."
                        "VOLKSWAGEN", "AUDI" -> "VAG Gateway 19: Triggers battery adaptation sequence."
                        else -> "Universal OBD2 Powertrain Reset: Calibrates battery adaptation curves."
                    }
                    Text(text = procedureText, color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (resetSuccess == true) {
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                                Spacer(Modifier.width(8.dp))
                                Text("Battery Calibration Confirmed Complete!", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onExecuteReset()
                            resetSuccess = true
                        },
                        enabled = canExecute,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            disabledContainerColor = SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isBmwFrm && lockState.isLocked) "Gated: Vehicle Sleeping (${lockState.remainingSeconds / 60}m remaining)" else "Execute BMS Recalibration",
                            color = if (canExecute) Color.Black else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BmwFrmSafetyBanner() {
    Surface(
        color = CriticalCrimson.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, CriticalCrimson.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = CriticalCrimson, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("FOOTWELL MODULE (FRM3) BRICK RISK DETECTED", color = CriticalCrimson, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Disconnecting the battery on this BMW/Mini while the vehicle is awake corrupts the Footwell Module's processor, permanently breaking windows, lighting, and OBD communication. The vehicle must be in deep sleep before terminals are touched.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun BmwSleepPreparationCard(onStartTimer: () -> Unit) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Step 1: Deep Sleep Preparation", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Text("1. Roll down driver window (prevents lockout).\n2. Open trunk and trip the mechanical latch closed using a screwdriver.\n3. Turn ignition off and place key fobs 25+ feet away.", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStartTimer,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlertAmber),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Begin battery installation procedure", color = Color.Black, fontWeight = FontWeight.Bold)
            }

        }
    }
}

@Composable
private fun BmwCountdownLockCard(
    remainingSeconds: Long,
    checkedGearShifter: Boolean,
    onCheckGearShifter: (Boolean) -> Unit,
    checkedHazard: Boolean,
    onCheckHazard: (Boolean) -> Unit
) {
    Surface(
        color = SurfaceCardElevated,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, AlertAmber, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = AlertAmber)
                Spacer(Modifier.width(8.dp))
                Text("Anti-Bypass Sleep Timer Active", color = AlertAmber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))

            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            Text(
                text = "%02d:%02d".format(minutes, seconds),
                color = NeonCyan,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                "Do not open doors, touch handles, or move keys. This timer persists even if the app or phone restarts.",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(Modifier.height(12.dp))

            Text("VISUAL SLEEP CONFIRMATION CHECKLIST", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checkedGearShifter,
                    onCheckedChange = onCheckGearShifter,
                    colors = CheckboxDefaults.colors(checkedColor = EmeraldSuccess)
                )
                Spacer(Modifier.width(6.dp))
                Text("Orange 'P' light on gear shifter is completely OFF", color = TextPrimary, fontSize = 12.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checkedHazard,
                    onCheckedChange = onCheckHazard,
                    colors = CheckboxDefaults.colors(checkedColor = EmeraldSuccess)
                )
                Spacer(Modifier.width(6.dp))
                Text("Red triangle Hazard button backlight is completely OFF", color = TextPrimary, fontSize = 12.sp)
            }
        }
    }
}
