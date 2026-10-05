package com.obdhero.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.obdhero.app.domain.DtcInfo
import com.obdhero.app.domain.VinDecoder
import com.obdhero.app.ui.components.TopStatusBar
import com.obdhero.app.ui.screens.BatteryCalibrationScreen
import com.obdhero.app.ui.screens.DiagnosticsScreen
import com.obdhero.app.ui.screens.EmissionsScreen
import com.obdhero.app.ui.screens.VehicleProfileScreen
import com.obdhero.app.ui.theme.*
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.collectLatest

enum class AppTab(val title: String) {
    DIAGNOSTICS("Diagnostics"),
    BATTERY("Battery Reset"),
    EMISSIONS("Emissions I/M"),
    VEHICLE("Profile & Tools")
}

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Bluetooth permissions on Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
                )
            )
        }

        val app = application as ObdHeroApp
        val obdManager = app.obdConnectionManager
        val lockManager = app.bmwSleepLockManager

        setContent {

            MaterialTheme(colorScheme = MidnightColorScheme) {
                var isDualPane by remember { mutableStateOf(false) }

                // Window Info / Foldable Tracker for Google Pixel 9 Pro Fold
                LaunchedEffect(Unit) {
                    WindowInfoTracker.getOrCreate(this@MainActivity)
                        .windowLayoutInfo(this@MainActivity)
                        .collectLatest { layoutInfo ->
                            val isHalfFolded = layoutInfo.displayFeatures
                                .filterIsInstance<FoldingFeature>()
                                .any { it.state == FoldingFeature.State.HALF_OPENED }
                            val isExpanded = resources.configuration.smallestScreenWidthDp >= 600
                            isDualPane = isExpanded || isHalfFolded
                        }
                }

                val connectionState by obdManager.connectionState.collectAsState()
                val telemetry by obdManager.telemetry.collectAsState()
                val rawVin by obdManager.vin.collectAsState()
                val storedCodes by obdManager.storedDtcs.collectAsState()
                val pendingCodes by obdManager.pendingDtcs.collectAsState()
                val emissionsReadiness by obdManager.emissionsReadiness.collectAsState()

                val vehicleProfile = remember(rawVin) {
                    if (rawVin.isNotEmpty()) VinDecoder.decode(rawVin) else null
                }

                // Check persistent BMW sleep lock to auto-lock navigation to Battery screen if timer is active
                var activeTab by remember {
                    val lock = lockManager.checkState()
                    mutableStateOf(if (lock.isLocked) AppTab.BATTERY else AppTab.DIAGNOSTICS)
                }

                var selectedDtc by remember { mutableStateOf<DtcInfo?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = CanvasBg,
                    topBar = {
                        TopStatusBar(
                            connectionState = connectionState,
                            profile = vehicleProfile,
                            telemetry = telemetry
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = SurfaceCard,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = (activeTab == AppTab.DIAGNOSTICS),
                                onClick = { activeTab = AppTab.DIAGNOSTICS },
                                icon = { Icon(Icons.Default.Build, contentDescription = null) },
                                label = { Text(AppTab.DIAGNOSTICS.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                            NavigationBarItem(
                                selected = (activeTab == AppTab.BATTERY),
                                onClick = { activeTab = AppTab.BATTERY },
                                icon = { Icon(Icons.Default.BatteryChargingFull, contentDescription = null) },
                                label = { Text(AppTab.BATTERY.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = AlertAmber,
                                    indicatorColor = AlertAmber,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                            NavigationBarItem(
                                selected = (activeTab == AppTab.EMISSIONS),
                                onClick = { activeTab = AppTab.EMISSIONS },
                                icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                                label = { Text(AppTab.EMISSIONS.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = EmeraldSuccess,
                                    indicatorColor = EmeraldSuccess,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                            NavigationBarItem(
                                selected = (activeTab == AppTab.VEHICLE),
                                onClick = { activeTab = AppTab.VEHICLE },
                                icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                                label = { Text(AppTab.VEHICLE.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = ElectricTeal,
                                    indicatorColor = ElectricTeal,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(CanvasBg)
                    ) {
                        when (activeTab) {
                            AppTab.DIAGNOSTICS -> {
                                DiagnosticsScreen(
                                    storedCodes = storedCodes,
                                    pendingCodes = pendingCodes,
                                    selectedDtc = selectedDtc,
                                    onSelectDtc = { selectedDtc = it },
                                    onClearCodes = { obdManager.clearCodes() },
                                    isDualPane = isDualPane
                                )
                            }
                            AppTab.BATTERY -> {
                                BatteryCalibrationScreen(
                                    profile = vehicleProfile,
                                    lockManager = lockManager,
                                    onExecuteReset = {
                                        vehicleProfile?.let { obdManager.executeBatteryReset(it.vin) }
                                    },
                                    isDualPane = isDualPane
                                )
                            }
                            AppTab.EMISSIONS -> {
                                EmissionsScreen(
                                    readiness = emissionsReadiness
                                )
                            }
                            AppTab.VEHICLE -> {
                                VehicleProfileScreen(
                                    profile = vehicleProfile,
                                    pairedDevices = obdManager.getPairedScanners(),
                                    onConnectDevice = { obdManager.connectToDevice(it) },
                                    onStartSimulator = { obdManager.startSimulator() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
