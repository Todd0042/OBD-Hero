package com.obdhero.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BuildCircle
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
import com.obdhero.app.data.VehicleProfileRepository
import com.obdhero.app.domain.DtcInfo
import com.obdhero.app.domain.VinDecoder
import com.obdhero.app.ui.components.TopStatusBar
import com.obdhero.app.ui.screens.BatteryCalibrationScreen
import com.obdhero.app.ui.screens.DiagnosticsScreen
import com.obdhero.app.ui.screens.EmissionsScreen
import com.obdhero.app.ui.screens.LiveViewScreen
import com.obdhero.app.ui.screens.ShopServiceScreen
import com.obdhero.app.ui.screens.VehicleProfileScreen
import com.obdhero.app.ui.theme.*
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.collectLatest

enum class AppTab(val title: String) {
    DIAGNOSTICS("Diagnostics"),
    SERVICE("Shop Service"),
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
        enableEdgeToEdge()

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
                val moduleReports by obdManager.moduleReports.collectAsState()
                val isScanning by obdManager.isScanning.collectAsState()
                val freezeFrameData by obdManager.freezeFrameData.collectAsState()
                val emissionsReadiness by obdManager.emissionsReadiness.collectAsState()
                val focusedMetric by obdManager.focusedMetric.collectAsState()
                val adapterProfile by obdManager.adapterProfile.collectAsState()
                val recorderState by obdManager.flightRecorder.recorderState.collectAsState()
                val incidentHistory by com.obdhero.app.data.BlackBoxRepository.incidents.collectAsState()

                val vehicleProfile = remember(rawVin) {
                    if (rawVin.isNotEmpty()) VinDecoder.decode(rawVin) else null
                }

                val resolvedProfile = remember(rawVin, vehicleProfile) {
                    if (vehicleProfile != null) {
                        VehicleProfileRepository.resolveProfile(
                            vin = vehicleProfile.vin,
                            make = vehicleProfile.make,
                            model = vehicleProfile.model,
                            year = vehicleProfile.year
                        )
                    } else null
                }

                // Check persistent BMW sleep lock to auto-lock navigation to Battery screen if timer is active
                var activeTab by remember {
                    val lock = lockManager.checkState()
                    mutableStateOf(if (lock.isLocked) AppTab.BATTERY else AppTab.DIAGNOSTICS)
                }

                var selectedDtc by remember { mutableStateOf<DtcInfo?>(null) }
                var showLiveView by remember { mutableStateOf(false) }

                if (showLiveView) {
                    LiveViewScreen(
                        telemetry = telemetry,
                        focusedMetric = focusedMetric,
                        vehicleMake = vehicleProfile?.make ?: "",
                        vehicleModel = vehicleProfile?.model ?: "",
                        vehicleVin = vehicleProfile?.vin ?: "",
                        adapterProfile = adapterProfile,
                        recorderState = recorderState,
                        incidentHistory = incidentHistory,
                        onTriggerBlackBox = { obdManager.triggerBlackBoxManual() },
                        onRearmBlackBox = { obdManager.rearmBlackBox() },
                        onDeleteBlackBoxIncident = { com.obdhero.app.data.BlackBoxRepository.deleteIncident(it) },
                        onSelectMetric = { obdManager.setFocusedMetric(it) },
                        onBack = { showLiveView = false },
                        modifier = Modifier.statusBarsPadding()
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = CanvasBg,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        topBar = {
                            TopStatusBar(
                                connectionState = connectionState,
                                profile = vehicleProfile,
                                telemetry = telemetry,
                                modifier = Modifier.statusBarsPadding()
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
                                        selectedIconColor = Color.White,
                                        selectedTextColor = RaceRed,
                                        indicatorColor = RaceRed,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = (activeTab == AppTab.SERVICE),
                                    onClick = { activeTab = AppTab.SERVICE },
                                    icon = { Icon(Icons.Default.BuildCircle, contentDescription = null) },
                                    label = { Text(AppTab.SERVICE.title, fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = CyanPulse,
                                        indicatorColor = CyanPulse,
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
                                        selectedIconColor = Color.White,
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
                                        selectedIconColor = Color.White,
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
                                        selectedIconColor = Color.White,
                                        selectedTextColor = RubyGlow,
                                        indicatorColor = RubyGlow,
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
                                        onOpenLiveView = { showLiveView = true },
                                        vehicleMake = vehicleProfile?.make ?: "",
                                        isDualPane = isDualPane,
                                        moduleReports = moduleReports,
                                        isScanning = isScanning,
                                        onScanAllModules = { obdManager.scanAllModules(resolvedProfile) },
                                        freezeFrame = selectedDtc?.let { freezeFrameData[it.code] }
                                    )
                                }
                                AppTab.SERVICE -> {
                                    ShopServiceScreen(
                                        vehicleProfile = vehicleProfile,
                                        onExecuteRoutine = { tool, callback ->
                                            obdManager.executeShopServiceRoutine(tool, callback)
                                        }
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
                                        connectionState = connectionState,
                                        pairedDevices = obdManager.getPairedScanners(),
                                        onConnectDevice = { obdManager.connectToDevice(it) },
                                        onDisconnect = { obdManager.disconnect() },
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
}
