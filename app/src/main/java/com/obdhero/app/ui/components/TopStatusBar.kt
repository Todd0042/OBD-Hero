package com.obdhero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obdhero.app.domain.LiveTelemetry
import com.obdhero.app.domain.VehicleProfile
import com.obdhero.app.obd.ConnectionState
import com.obdhero.app.ui.theme.*

@Composable
fun TopStatusBar(
    connectionState: ConnectionState,
    profile: VehicleProfile?,
    telemetry: LiveTelemetry,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        color = SurfaceCardElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Connection status
            Row(verticalAlignment = Alignment.CenterVertically) {
                val iconColor = when (connectionState) {
                    is ConnectionState.Connected -> RaceRed
                    is ConnectionState.Connecting -> AlertAmber
                    is ConnectionState.Error -> CriticalCrimson
                    ConnectionState.Disconnected -> TextMuted
                }
                Icon(
                    imageVector = Icons.Default.BluetoothConnected,
                    contentDescription = "Status",
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val title = when (connectionState) {
                    is ConnectionState.Connected -> connectionState.deviceName
                    is ConnectionState.Connecting -> "Connecting…"
                    is ConnectionState.Error -> "Connection Error"
                    ConnectionState.Disconnected -> "Disconnected"
                }
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Center/Right: Vehicle & Voltage
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profile != null && profile.make != "Unknown") {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Car",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${profile.year ?: ""} ${profile.make} ${profile.model}".trim(),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Voltage indicator
                val voltColor = when {
                    telemetry.batteryVolts >= 13.5f -> EmeraldSuccess
                    telemetry.batteryVolts >= 11.9f -> NeonCyan
                    telemetry.batteryVolts > 0.0f -> CriticalCrimson
                    else -> TextMuted
                }
                val voltText = if (telemetry.batteryVolts > 0.0f) "%.1fV".format(telemetry.batteryVolts) else "--.-V"
                Text(
                    text = voltText,
                    color = voltColor,
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}
