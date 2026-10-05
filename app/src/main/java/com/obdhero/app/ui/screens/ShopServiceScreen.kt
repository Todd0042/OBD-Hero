package com.obdhero.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdhero.app.data.ShopMaintenanceRepository
import com.obdhero.app.domain.*
import com.obdhero.app.ui.theme.*

@Composable
fun ShopServiceScreen(
    vehicleProfile: VehicleProfile?,
    onExecuteRoutine: (ShopMaintenanceTool, (ServiceExecutionState) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val vehicleMake = vehicleProfile?.make ?: "Generic"
    val tools = remember(vehicleMake) { ShopMaintenanceRepository.getToolsForVehicle(vehicleMake) }
    var selectedCategory by remember { mutableStateOf<ServiceFunctionCategory?>(null) }
    var activeExecutingTool by remember { mutableStateOf<ShopMaintenanceTool?>(null) }
    var executionState by remember { mutableStateOf<ServiceExecutionState>(ServiceExecutionState.Idle) }
    var manualGuideTool by remember { mutableStateOf<ShopMaintenanceTool?>(null) }

    val filteredTools = remember(selectedCategory, tools) {
        if (selectedCategory == null) tools else tools.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SHOP MAINTENANCE SERVICE",
                    color = RaceRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Oil • EPB • Battery • TPMS",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                color = SurfaceCardElevated,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = vehicleProfile?.make?.uppercase() ?: "GENERIC OBD-II",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = (selectedCategory == null),
                onClick = { selectedCategory = null },
                label = { Text("All Tools (${tools.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RaceRed,
                    selectedLabelColor = Color.White
                )
            )
            ServiceFunctionCategory.entries.forEach { cat ->
                FilterChip(
                    selected = (selectedCategory == cat),
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = { Text(cat.badge, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RaceRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Tool Cards List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredTools) { tool ->
                ServiceToolCard(
                    tool = tool,
                    onExecute = {
                        activeExecutingTool = tool
                        executionState = ServiceExecutionState.Idle
                    },
                    onViewManual = {
                        manualGuideTool = tool
                    }
                )
            }
        }
    }

    // 1. Execution Modal Dialog
    if (activeExecutingTool != null) {
        val tool = activeExecutingTool!!
        AlertDialog(
            onDismissRequest = {
                if (executionState !is ServiceExecutionState.Executing) {
                    activeExecutingTool = null
                    executionState = ServiceExecutionState.Idle
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = tool.title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    when (executionState) {
                        is ServiceExecutionState.Idle -> {
                            Text(tool.description, color = TextSecondary, fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("PREREQUISITES:", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            tool.prerequisites.forEach { prereq ->
                                Text("• $prereq", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp))
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(tool.confirmationPrompt, color = AlertAmber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        is ServiceExecutionState.Executing -> {
                            val state = executionState as ServiceExecutionState.Executing
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = RaceRed, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(14.dp))
                                Text(state.stage, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        is ServiceExecutionState.Success -> {
                            val state = executionState as ServiceExecutionState.Success
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("SERVICE COMPLETE", color = EmeraldSuccess, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(state.message, color = TextPrimary, fontSize = 13.sp)
                            }
                        }
                        is ServiceExecutionState.FallbackManual -> {
                            val state = executionState as ServiceExecutionState.FallbackManual
                            Column {
                                Text(state.reason, color = AlertAmber, fontSize = 12.sp)
                                Spacer(Modifier.height(10.dp))
                                Text("OFFICIAL MANUAL PROCEDURE:", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                state.manualProcedure.forEachIndexed { i, step ->
                                    Text("${i + 1}. $step", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                        is ServiceExecutionState.Error -> {
                            val state = executionState as ServiceExecutionState.Error
                            Text(state.message, color = CriticalCrimson, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                when (executionState) {
                    is ServiceExecutionState.Idle -> {
                        Button(
                            onClick = {
                                onExecuteRoutine(tool) { newState ->
                                    executionState = newState
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaceRed)
                        ) {
                            Text("Confirm & Execute", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    is ServiceExecutionState.Success, is ServiceExecutionState.FallbackManual, is ServiceExecutionState.Error -> {
                        Button(
                            onClick = {
                                activeExecutingTool = null
                                executionState = ServiceExecutionState.Idle
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated)
                        ) {
                            Text("Done", color = TextPrimary)
                        }
                    }
                    is ServiceExecutionState.Executing -> {}
                }
            },
            dismissButton = {
                if (executionState is ServiceExecutionState.Idle) {
                    TextButton(onClick = { activeExecutingTool = null }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            }
        )
    }

    // 2. Manual Procedure Walkthrough Modal
    if (manualGuideTool != null) {
        val tool = manualGuideTool!!
        AlertDialog(
            onDismissRequest = { manualGuideTool = null },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "${tool.title} (Manual)",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Step-by-step physical pedal / button sequence:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    tool.manualProcedureSteps.forEachIndexed { i, step ->
                        Text("${i + 1}. $step", color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { manualGuideTool = null },
                    colors = ButtonDefaults.buttonColors(containerColor = RaceRed)
                ) {
                    Text("Understood", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun ServiceToolCard(
    tool: ShopMaintenanceTool,
    onExecute: () -> Unit,
    onViewManual: () -> Unit
) {
    val (badgeColor, badgeIcon) = when (tool.category) {
        ServiceFunctionCategory.OIL_MAINTENANCE -> Pair(Color(0xFFFFB300), Icons.Default.WaterDrop)
        ServiceFunctionCategory.BRAKES_EPB -> Pair(Color(0xFFFF5252), Icons.Default.DirectionsCar)
        ServiceFunctionCategory.BATTERY_BMS -> Pair(EmeraldSuccess, Icons.Default.BatteryChargingFull)
        ServiceFunctionCategory.TIRES_TPMS -> Pair(Color(0xFF00E5FF), Icons.Default.TireRepair)
    }

    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tool.category.badge, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = SurfaceCardElevated,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "ECU ${tool.targetEcuHeader}",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(text = tool.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(text = tool.description, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExecute,
                    colors = ButtonDefaults.buttonColors(containerColor = RaceRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Execute Routine", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onViewManual,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Manual Guide", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
