package com.obdhero.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdhero.app.R
import com.obdhero.app.domain.BmwSleepLockManager
import com.obdhero.app.ui.theme.*

data class WalkthroughStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val warning: String? = null,
    val imageResId: Int? = null,
    val isTimerStep: Boolean = false
)

@Composable
fun BmwSleepWalkthroughDialog(
    lockManager: BmwSleepLockManager,
    vin: String,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var lockState by remember { mutableStateOf(lockManager.checkState()) }

    // Live tick for timer if active
    LaunchedEffect(Unit) {
        while (true) {
            lockState = lockManager.checkState()
            kotlinx.coroutines.delay(1000)
        }
    }

    val steps = remember {
        listOf(
            WalkthroughStep(
                stepNumber = 1,
                title = "Roll Down Driver Window",
                description = "Before turning off the ignition, roll down the driver's window completely. This prevents accidental lockout if the vehicle locks itself while the battery is disconnected.",
                warning = "Critical Safety Step",
                imageResId = R.drawable.img_bmw_window_down
            ),
            WalkthroughStep(
                stepNumber = 2,
                title = "Latch Trunk Lock Mechanism",
                description = "Open the trunk/tailgate to access the battery. Using a screwdriver or pen, manually push the rotary latch claw into its closed position. This tricks the Body Computer (CAS/FRM) into believing the trunk is shut so it can sleep.",
                warning = "Tricks vehicle into sleep while keeping trunk accessible",
                imageResId = R.drawable.img_bmw_trunk_latch
            ),
            WalkthroughStep(
                stepNumber = 3,
                title = "Move Key Fobs 25+ Feet Away",
                description = "Turn ignition completely OFF. Take all key fobs and Comfort Access transponders at least 25 feet (8 meters) away from the car so it stops pinging for keys and can power down.",
                warning = "Key fobs inside or near car will reset the sleep timer",
                imageResId = R.drawable.img_bmw_keys_away
            ),
            // STEP 4 IS THE 20-MINUTE TIMER
            WalkthroughStep(
                stepNumber = 4,
                title = "20-Minute Deep Sleep Countdown",
                description = "Vehicle modules are shutting down. Do not open doors, touch handles, or move keys. This timer persists in storage even if the app or phone restarts.",
                warning = "Deep Sleep Countdown Active",
                isTimerStep = true
            ),
            // STEP 5: VERIFY SHIFTER P OFF
            WalkthroughStep(
                stepNumber = 5,
                title = "Verify Gear Shifter 'P' Light OFF",
                description = "Inspect the automatic gear selector through the rolled-down window without opening doors or touching handles. The orange 'P' LED indicator next to Park must be completely dark.",
                warning = "If 'P' light is still lit, DO NOT touch battery terminals",
                imageResId = R.drawable.img_bmw_shifter_dark
            ),
            // STEP 6: VERIFY HAZARD OFF
            WalkthroughStep(
                stepNumber = 6,
                title = "Verify Hazard Triangle Light OFF",
                description = "Confirm the red triangle Hazard switch on the center dashboard is completely dark. When both 'P' and Hazard lights are extinguished, the CAN bus and FRM are safely asleep.",
                warning = "Guarantees zero active EEPROM writes to FRM3",
                imageResId = R.drawable.img_bmw_hazard_dark
            ),
            // STEP 7: BATTERY SWAP
            WalkthroughStep(
                stepNumber = 7,
                title = "Disconnect Negative Terminal 1st",
                description = "Disconnect the negative terminal (-) first using a 10mm socket. Install the new battery, connect the positive terminal (+) first, then connect the negative terminal firmly with zero bouncing/arcing.",
                warning = "Firm single-motion connection prevents voltage spike",
                imageResId = R.drawable.img_bmw_battery_swap
            )
        )
    }

    // If timer is already running, jump directly to timer step (index 3)
    var currentStepIndex by remember {
        mutableStateOf(if (lockState.isLocked) 3 else 0)
    }

    val step = steps[currentStepIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FRM3 Deep Sleep Walkthrough",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${currentStepIndex + 1} of ${steps.size}",
                    color = RaceRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (step.isTimerStep) {
                    // Timer Container Screen
                    Surface(
                        color = SurfaceCardElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(1.dp, RaceRed, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = RaceRed)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (lockState.isLocked) "Deep Sleep Timer Active" else "Ready to Begin Timer",
                                    color = RaceRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            val remaining = lockState.remainingSeconds
                            val minutes = remaining / 60
                            val seconds = remaining % 60
                            Text(
                                text = if (lockState.isLocked) "%02d:%02d".format(minutes, seconds) else "20:00",
                                color = TextPrimary,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.displayMedium
                            )

                            if (!lockState.isLocked) {
                                Button(
                                    onClick = {
                                        lockState = lockManager.startSleepSession(vin, durationSeconds = 1200L)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RaceRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("Start 20-Min Timer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                Text(
                                    "Vehicle is entering deep sleep...",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else if (step.imageResId != null) {
                    // Image container
                    Surface(
                        color = SurfaceCardElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = step.imageResId),
                            contentDescription = step.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Step Title & Tag
                Text(
                    text = step.title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (step.warning != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = RaceRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = step.warning,
                            color = RaceRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = step.description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            val isNextAllowed = if (step.isTimerStep) {
                // Cannot proceed past step 4 unless timer was started AND has finished
                !lockState.isLocked && (lockState.sessionStartEpochMs > 0L)
            } else {
                true
            }

            Button(
                onClick = {
                    if (currentStepIndex < steps.size - 1) {
                        currentStepIndex++
                    } else {
                        onComplete()
                    }
                },
                enabled = isNextAllowed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RaceRed,
                    disabledContainerColor = SurfaceCardElevated
                )
            ) {
                Text(
                    text = if (step.isTimerStep && lockState.isLocked) {
                        "Waiting for timer (${lockState.remainingSeconds / 60}m)"
                    } else if (currentStepIndex < steps.size - 1) {
                        "Next Step →"
                    } else {
                        "Complete Walkthrough"
                    },
                    color = if (isNextAllowed) Color.White else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (currentStepIndex > 0) {
                OutlinedButton(onClick = { currentStepIndex-- }) {
                    Text("← Back", color = TextPrimary)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextMuted)
                }
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(16.dp)
    )
}
