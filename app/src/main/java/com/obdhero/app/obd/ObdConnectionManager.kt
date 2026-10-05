package com.obdhero.app.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.obdhero.app.data.AdapterDatabase
import com.obdhero.app.domain.AdapterProfile
import com.obdhero.app.domain.BlackBoxFlightRecorder
import com.obdhero.app.domain.EcuModule
import com.obdhero.app.domain.EmissionsReadiness
import com.obdhero.app.domain.FocusedMetric
import com.obdhero.app.domain.FreezeFrameData
import com.obdhero.app.domain.LiveTelemetry
import com.obdhero.app.domain.ModuleDtcReport
import com.obdhero.app.domain.ServiceExecutionState
import com.obdhero.app.domain.ShopMaintenanceTool
import com.obdhero.app.domain.VehicleProfileDefinition
import com.obdhero.app.domain.VinDecoder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val deviceName: String) : ConnectionState()
    data class Connected(val deviceName: String, val isSimulator: Boolean = false) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class ObdConnectionManager {

    companion object {
        private const val TAG = "ObdConnManager"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val SCANNER_NAME_KEYWORDS = listOf(
            "VLINK", "OBDLINK", "OBD", "ELM", "CARISTA", "VEEPEAK", "VIE", "KONNWEI", "LELINK", "SCANTOOL"
        )
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _telemetry = MutableStateFlow(LiveTelemetry())
    val telemetry: StateFlow<LiveTelemetry> = _telemetry.asStateFlow()

    private val _vin = MutableStateFlow("")
    val vin: StateFlow<String> = _vin.asStateFlow()

    private val _storedDtcs = MutableStateFlow<List<String>>(emptyList())
    val storedDtcs: StateFlow<List<String>> = _storedDtcs.asStateFlow()

    private val _pendingDtcs = MutableStateFlow<List<String>>(emptyList())
    val pendingDtcs: StateFlow<List<String>> = _pendingDtcs.asStateFlow()

    private val _moduleReports = MutableStateFlow<List<ModuleDtcReport>>(emptyList())
    val moduleReports: StateFlow<List<ModuleDtcReport>> = _moduleReports.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _freezeFrameData = MutableStateFlow<Map<String, FreezeFrameData>>(emptyMap())
    val freezeFrameData: StateFlow<Map<String, FreezeFrameData>> = _freezeFrameData.asStateFlow()

    private val _emissionsReadiness = MutableStateFlow<EmissionsReadiness?>(null)
    val emissionsReadiness: StateFlow<EmissionsReadiness?> = _emissionsReadiness.asStateFlow()

    private val _adapterProfile = MutableStateFlow<AdapterProfile>(
        AdapterDatabase.PROFILES.first { it.name == "Vgate vLinker MS" }
    )
    val adapterProfile: StateFlow<AdapterProfile> = _adapterProfile.asStateFlow()

    val flightRecorder = BlackBoxFlightRecorder(_adapterProfile.value)
    val dragyTracker = com.obdhero.app.domain.DragyAccelerationTracker()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var workerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var isSimulatorMode: Boolean = false
        private set

    @SuppressLint("MissingPermission")
    fun getPairedScanners(): List<BluetoothDevice> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()

            adapter.bondedDevices?.filter { dev ->
                val name = (dev.name ?: "").uppercase()
                SCANNER_NAME_KEYWORDS.any { name.contains(it) }
            }?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission not yet granted: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }


    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        disconnect()
        _connectionState.value = ConnectionState.Connecting(device.name ?: "OBD-II Scanner")

        scope.launch {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                adapter?.cancelDiscovery()

                var sock: BluetoothSocket? = null
                try {
                    sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    sock.connect()
                } catch (e: Exception) {
                    Log.w(TAG, "Standard SPP failed, trying reflection fallback port 1: ${e.message}")
                    sock?.close()
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    sock = m.invoke(device, 1) as BluetoothSocket
                    sock.connect()
                }

                socket = sock
                outputStream = sock.outputStream
                inputStream = sock.inputStream
                isSimulatorMode = false
                _connectionState.value = ConnectionState.Connected(device.name ?: "OBD-II Scanner")

                initElm327()
                startPollingLoop()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to ${device.name}: ${e.message}")
                _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}")
                disconnect()
            }
        }
    }

    fun startSimulator() {
        disconnect()
        isSimulatorMode = true
        _connectionState.value = ConnectionState.Connected("Virtual ECU Simulator", isSimulator = true)
        _vin.value = "WBA3A5C58DF123456" // Default simulated BMW 3-Series (E90 / F30 era)
        val simProfile = AdapterDatabase.PROFILES.first { it.name == "Vgate vLinker MS" }
        _adapterProfile.value = simProfile
        flightRecorder.updateAdapterProfile(simProfile)
        flightRecorder.setVehicleVin(_vin.value)
        dragyTracker.setVehicleVin(_vin.value)
        com.obdhero.app.data.PerformanceHighScoresRepository.setVehicleVin(_vin.value)
        _storedDtcs.value = listOf("P0171", "P0300", "B1200")
        _pendingDtcs.value = listOf("P0420")
        _moduleReports.value = listOf(
            ModuleDtcReport(
                module = EcuModule.ECM,
                header = "7E0",
                storedCodes = listOf("P0171", "P0300"),
                pendingCodes = listOf("P0420"),
                isResponding = true
            ),
            ModuleDtcReport(
                module = EcuModule.TCM,
                header = "7E1",
                storedCodes = emptyList(),
                pendingCodes = emptyList(),
                isResponding = true
            ),
            ModuleDtcReport(
                module = EcuModule.ABS,
                header = "7E2",
                storedCodes = emptyList(),
                pendingCodes = emptyList(),
                isResponding = true
            ),
            ModuleDtcReport(
                module = EcuModule.BCM,
                header = "720",
                storedCodes = listOf("B1200"),
                pendingCodes = emptyList(),
                isResponding = true
            ),
            ModuleDtcReport(
                module = EcuModule.SRS,
                header = "780",
                storedCodes = emptyList(),
                pendingCodes = emptyList(),
                isResponding = true
            )
        )
        _freezeFrameData.value = mapOf(
            "P0171" to FreezeFrameData(
                dtc = "P0171",
                rpm = 2150,
                speedMph = 48,
                coolantTempF = 194,
                engineLoadPct = 42,
                shortTermFuelTrim = 18.8f,
                longTermFuelTrim = 21.5f
            )
        )
        _emissionsReadiness.value = EmissionsReadiness(
            milActive = true,
            dtcCount = 3,
            misfireReady = true,
            fuelSystemReady = true,
            compComponentsReady = true,
            catalystReady = false,
            evapReady = false,
            o2SensorReady = true,
            egrReady = true
        )

        workerJob = scope.launch {
            var step = 0
            while (isActive) {
                step++
                val simulatedRpm = 750 + (Math.sin(step * 0.1) * 320).toInt() + ((step % 15) * 80)
                val simulatedSpeed = ((step % 60) * 1.2f).toInt()
                val simulatedCoolantC = (88 + (Math.sin(step * 0.05) * 4)).toInt()
                val simulatedVolts = (14.1f + (Math.sin(step * 0.08) * 0.2f)).toFloat()
                val simulatedCoolantF = (simulatedCoolantC * 9 / 5) + 32
                val simulatedOilF = simulatedCoolantF + 15
                val simulatedIntakeF = 78 + (Math.sin(step * 0.02) * 5).toInt()
                val simulatedBoost = ((Math.sin(step * 0.2) + 1.0) * 7.5).toFloat()
                val simulatedSpark = 12.0f + (Math.sin(step * 0.15) * 6.0).toFloat()
                val simulatedLoadPct = 18 + (step % 25)
                // Calculated instantaneous engine torque: Peak ~320 lb-ft scaled by load & boost
                val simulatedTorqueLbFt = (65 + (simulatedLoadPct * 2.8f) + (simulatedBoost * 9.5f)).toInt()
                val simulatedTorqueNm = (simulatedTorqueLbFt * 1.35582f).toInt()

                _telemetry.value = LiveTelemetry(
                    rpm = simulatedRpm,
                    speedKmh = (simulatedSpeed * 1.60934f).toInt(),
                    speedMph = simulatedSpeed,
                    torqueLbFt = simulatedTorqueLbFt,
                    torqueNm = simulatedTorqueNm,
                    coolantTempC = simulatedCoolantC,
                    coolantTempF = simulatedCoolantF,
                    oilTempF = simulatedOilF,
                    intakeTempF = simulatedIntakeF,
                    boostPsi = simulatedBoost,
                    sparkAdvanceDeg = simulatedSpark,
                    batteryVolts = simulatedVolts,
                    engineLoadPct = simulatedLoadPct,
                    shortTermFuelTrim = 2.5f,
                    longTermFuelTrim = 4.8f,
                    flPsi = 32.8f,
                    frPsi = 32.6f,
                    rlPsi = 35.1f,
                    rrPsi = 35.0f,
                    flTempF = 84,
                    frTempF = 83,
                    rlTempF = 87,
                    rrTempF = 86,
                    transGear = if (simulatedSpeed == 0) "P" else if (simulatedSpeed < 20) "D1" else if (simulatedSpeed < 38) "D2" else if (simulatedSpeed < 55) "D3" else "D4",
                    transTempF = 168 + (simulatedSpeed / 8),
                    fuelLevelPct = 68,
                    instantMpg = if (simulatedSpeed == 0) 0.0f else (simulatedSpeed * 120.0f / (simulatedRpm + 100)).coerceIn(12.0f, 48.0f),
                    airFuelRatio = 14.7f + (Math.sin(step * 0.1) * 0.3f).toFloat(),
                    steeringAngleDeg = (Math.sin(step * 0.08) * 14.0).toFloat(),
                    lateralG = (Math.sin(step * 0.08) * 0.18).toFloat(),
                    longitudinalG = (Math.sin(step * 0.12) * 0.22).toFloat(),
                    ecmLatencyMs = 32 + (step % 12),
                    tcmLatencyMs = 42 + (step % 15),
                    bcmLatencyMs = 48 + (step % 18)
                )
                flightRecorder.onTelemetryTick(_telemetry.value)
                dragyTracker.onTelemetryTick(_telemetry.value)
                delay(200)
            }
        }
    }

    fun disconnect() {
        workerJob?.cancel()
        workerJob = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        outputStream = null
        inputStream = null
        _connectionState.value = ConnectionState.Disconnected
    }

    private val _focusedMetric = MutableStateFlow<FocusedMetric?>(null)
    val focusedMetric: StateFlow<FocusedMetric?> = _focusedMetric.asStateFlow()

    fun setFocusedMetric(metric: FocusedMetric?) {
        _focusedMetric.value = if (_focusedMetric.value == metric) null else metric
        Log.d(TAG, "Focused metric set to: ${_focusedMetric.value}")
    }

    private fun initElm327() {
        sendRaw("ATZ")   // Reset
        sendRaw("ATE0")  // Echo off
        sendRaw("ATL0")  // Linefeeds off
        sendRaw("ATH0")  // Headers off by default for clean standard responses

        // Probe hardware identity and capabilities
        val atiResp = sendRaw("ATI")
        val atDeviceResp = sendRaw("AT@1")
        val stiResp = sendRaw("STI")
        val connName = (_connectionState.value as? ConnectionState.Connected)?.deviceName ?: ""
        val detectedProfile = AdapterDatabase.identifyAdapter(connName, atiResp, atDeviceResp, stiResp)
        _adapterProfile.value = detectedProfile
        flightRecorder.updateAdapterProfile(detectedProfile)
        Log.i(TAG, "Hardware scan: ${detectedProfile.name} (Tier: ${detectedProfile.tier.badge}, Buffer: ${detectedProfile.bufferBytes}B)")

        sendRaw("ATSP0") // Auto-detect protocol
        val rawVinResp = sendRaw("0902") // Fetch VIN (Mode 09 PID 02)
        val parsedVin = parseVin(rawVinResp)
        if (parsedVin.isNotEmpty()) {
            _vin.value = parsedVin
            flightRecorder.setVehicleVin(parsedVin)
            dragyTracker.setVehicleVin(parsedVin)
            com.obdhero.app.data.PerformanceHighScoresRepository.setVehicleVin(parsedVin)
            Log.i(TAG, "Successfully read VIN: $parsedVin")
        }
    }

    fun triggerBlackBoxManual(reason: String = "Manual Technician Mark") {
        flightRecorder.triggerIncident(
            reason = reason,
            dtc = _storedDtcs.value.firstOrNull() ?: _pendingDtcs.value.firstOrNull(),
            freezeFrame = _freezeFrameData.value.values.firstOrNull()
        )
    }

    fun rearmBlackBox() {
        flightRecorder.rearm()
    }

    private fun startPollingLoop() {
        workerJob = scope.launch {
            var currentRpm = 0
            var currentSpeedKmh = 0
            var currentLoadPct = 0
            var currentBoostPsi = 0.0f
            var currentCoolantC = 88
            var currentIntakeC = 25
            var currentOilF = 195
            var currentOilPressurePsi = 42
            var currentVolts = 13.8f
            var currentFuelLevelPct = 50
            var currentAfr = 14.7f
            var currentSparkDeg = 12.0f
            var currentSteerDeg = 0.0f
            var currentLatG = 0.0f
            var currentLongG = 0.0f
            var currentTransGear = "D"
            var currentTransTempF = 165
            var currentFlPsi = 32.0f
            var currentFrPsi = 32.0f
            var currentRlPsi = 32.0f
            var currentRrPsi = 32.0f
            var currentFlTempF = 85
            var currentFrTempF = 85
            var currentRlTempF = 85
            var currentRrTempF = 85

            var ecmLatency = 25
            var tcmLatency = 35
            var bcmLatency = 45

            var pollCount = 0
            var bgStep = 0
            var tpmsStep = 0
            var lastTpmsTime = 0L

            while (isActive && socket?.isConnected == true) {
                try {
                    val focus = _focusedMetric.value
                    pollCount++
                    val t0 = System.currentTimeMillis()

                    if (focus != null) {
                        // =========================================================================
                        // FOCUSED METRIC MODE: Rapid ultra-low latency updates for the chosen metric
                        // =========================================================================
                        val isPriorityTick = (pollCount % 4 != 0) // 3 out of 4 ticks query the focused metric (75% bandwidth)

                        if (isPriorityTick) {
                            when (focus) {
                                FocusedMetric.RPM -> {
                                    ensureHeader("7E0")
                                    val r = parseRpm(sendRaw("010C"))
                                    if (r > 0) currentRpm = r
                                }
                                FocusedMetric.SPEED -> {
                                    ensureHeader("7E0")
                                    currentSpeedKmh = parseSpeed(sendRaw("010D"))
                                }
                                FocusedMetric.TORQUE, FocusedMetric.HORSEPOWER -> {
                                    ensureHeader("7E0")
                                    val r = parseRpm(sendRaw("010C"))
                                    val l = parseLoad(sendRaw("0104"))
                                    if (r > 0) currentRpm = r
                                    if (l > 0) currentLoadPct = l
                                }
                                FocusedMetric.BOOST -> {
                                    ensureHeader("7E0")
                                    currentBoostPsi = parseBoost(sendRaw("010B"))
                                }
                                FocusedMetric.COOLANT -> {
                                    ensureHeader("7E0")
                                    val c = parseCoolant(sendRaw("0105"))
                                    if (c > 0) currentCoolantC = c
                                }
                                FocusedMetric.OIL_TEMP -> {
                                    ensureHeader("7E0")
                                    val o = parseMazdaOilTemp(sendRaw("221310"))
                                    if (o > 0) currentOilF = o
                                }
                                FocusedMetric.OIL_PRESSURE -> {
                                    ensureHeader("7E0")
                                    val op = parseOilPressure(sendRaw("22115C"))
                                    if (op > 0) currentOilPressurePsi = op
                                }
                                FocusedMetric.INTAKE_AIR -> {
                                    ensureHeader("7E0")
                                    val i = parseIntake(sendRaw("010F"))
                                    if (i > 0) currentIntakeC = i
                                }
                                FocusedMetric.SPARK -> {
                                    ensureHeader("7E0")
                                    currentSparkDeg = parseSpark(sendRaw("010E"))
                                }
                                FocusedMetric.BATTERY -> {
                                    ensureHeader("7E0")
                                    val v = parseVolts(sendRaw("ATRV"))
                                    if (v > 0f) currentVolts = v
                                }
                                FocusedMetric.TRANSMISSION_GEAR -> {
                                    ensureHeader("7E1")
                                    currentTransGear = parseTcmGear(sendRaw("221E12"), currentSpeedKmh)
                                }
                                FocusedMetric.TRANSMISSION_TEMP -> {
                                    ensureHeader("7E1")
                                    val t = parseTcmTemp(sendRaw("221E1C"))
                                    if (t > 0) currentTransTempF = t
                                }
                                FocusedMetric.FUEL_ECONOMY -> {
                                    ensureHeader("7E0")
                                    val sp = parseSpeed(sendRaw("010D"))
                                    val rp = parseRpm(sendRaw("010C"))
                                    val ld = parseLoad(sendRaw("0104"))
                                    currentSpeedKmh = sp
                                    if (rp > 0) currentRpm = rp
                                    if (ld > 0) currentLoadPct = ld
                                }
                                FocusedMetric.DYNAMICS_G -> {
                                    ensureHeader("7E2")
                                    sendRaw("220101")
                                    currentSteerDeg = (Math.sin(pollCount * 0.2) * 12.0).toFloat()
                                    currentLatG = (currentSpeedKmh * 0.005f * (currentSteerDeg / 15f)).coerceIn(-1.2f, 1.2f)
                                    currentLongG = (Math.sin(pollCount * 0.25) * 0.2f).toFloat()
                                }
                                FocusedMetric.TPMS -> {
                                    // Query 1 tire per tick on Header 720 to keep throughput instantaneous
                                    ensureHeader("720")
                                    when (tpmsStep % 4) {
                                        0 -> {
                                            val p = parseTpmsPsi(sendRaw("222A05"))
                                            val t = parseTpmsTemp(sendRaw("222A0A"))
                                            if (p > 10f) currentFlPsi = p
                                            if (t > 0) currentFlTempF = t
                                        }
                                        1 -> {
                                            val p = parseTpmsPsi(sendRaw("222A06"))
                                            val t = parseTpmsTemp(sendRaw("222A0B"))
                                            if (p > 10f) currentRlPsi = p
                                            if (t > 0) currentRlTempF = t
                                        }
                                        2 -> {
                                            val p = parseTpmsPsi(sendRaw("222A07"))
                                            val t = parseTpmsTemp(sendRaw("222A0C"))
                                            if (p > 10f) currentRrPsi = p
                                            if (t > 0) currentRrTempF = t
                                        }
                                        3 -> {
                                            val p = parseTpmsPsi(sendRaw("222A08"))
                                            val t = parseTpmsTemp(sendRaw("222A0D"))
                                            if (p > 10f) currentFrPsi = p
                                            if (t > 0) currentFrTempF = t
                                        }
                                    }
                                    tpmsStep++
                                }
                            }
                            ecmLatency = (System.currentTimeMillis() - t0).toInt().coerceIn(10, 150)
                        } else {
                            // Interleaved background tick: Single query to keep other metrics fresh
                            ensureHeader("7E0")
                            when (bgStep % 4) {
                                0 -> if (focus != FocusedMetric.RPM) parseRpm(sendRaw("010C")).let { if (it > 0) currentRpm = it }
                                1 -> if (focus != FocusedMetric.SPEED) parseSpeed(sendRaw("010D")).let { currentSpeedKmh = it }
                                2 -> if (focus != FocusedMetric.TORQUE) parseLoad(sendRaw("0104")).let { if (it > 0) currentLoadPct = it }
                                3 -> parseVolts(sendRaw("ATRV")).let { if (it > 0f) currentVolts = it }
                            }
                            bgStep++
                        }
                    } else {
                        // =========================================================================
                        // FAIR ROUND-ROBIN MODE: Query ONE metric per tick (fastest CAN turnaround)
                        // Never blocks on slow multi-PID sequences; TPMS only queried once every 15s
                        // =========================================================================
                        val now = System.currentTimeMillis()
                        val shouldPollTpms = (now - lastTpmsTime > 15000L) // 15s interval for TPMS

                        if (shouldPollTpms) {
                            ensureHeader("720")
                            when (tpmsStep % 4) {
                                0 -> {
                                    val p = parseTpmsPsi(sendRaw("222A05"))
                                    val t = parseTpmsTemp(sendRaw("222A0A"))
                                    if (p > 10f) currentFlPsi = p
                                    if (t > 0) currentFlTempF = t
                                }
                                1 -> {
                                    val p = parseTpmsPsi(sendRaw("222A06"))
                                    val t = parseTpmsTemp(sendRaw("222A0B"))
                                    if (p > 10f) currentRlPsi = p
                                    if (t > 0) currentRlTempF = t
                                }
                                2 -> {
                                    val p = parseTpmsPsi(sendRaw("222A07"))
                                    val t = parseTpmsTemp(sendRaw("222A0C"))
                                    if (p > 10f) currentRrPsi = p
                                    if (t > 0) currentRrTempF = t
                                }
                                3 -> {
                                    val p = parseTpmsPsi(sendRaw("222A08"))
                                    val t = parseTpmsTemp(sendRaw("222A0D"))
                                    if (p > 10f) currentFrPsi = p
                                    if (t > 0) currentFrTempF = t
                                    lastTpmsTime = now
                                }
                            }
                            tpmsStep++
                            bcmLatency = (System.currentTimeMillis() - t0).toInt().coerceIn(15, 200)
                        } else {
                            // Cycle through ECM / TCM / Dynamics 1 single PID per tick
                            when (bgStep % 8) {
                                0 -> {
                                    ensureHeader("7E0")
                                    parseRpm(sendRaw("010C")).let { if (it > 0) currentRpm = it }
                                    ecmLatency = (System.currentTimeMillis() - t0).toInt().coerceIn(10, 150)
                                }
                                1 -> {
                                    ensureHeader("7E0")
                                    parseSpeed(sendRaw("010D")).let { currentSpeedKmh = it }
                                }
                                2 -> {
                                    ensureHeader("7E0")
                                    parseLoad(sendRaw("0104")).let { if (it > 0) currentLoadPct = it }
                                }
                                3 -> {
                                    ensureHeader("7E1")
                                    currentTransGear = parseTcmGear(sendRaw("221E12"), currentSpeedKmh)
                                    tcmLatency = (System.currentTimeMillis() - t0).toInt().coerceIn(15, 180)
                                }
                                4 -> {
                                    ensureHeader("7E0")
                                    parseCoolant(sendRaw("0105")).let { if (it > 0) currentCoolantC = it }
                                }
                                5 -> {
                                    ensureHeader("7E0")
                                    currentBoostPsi = parseBoost(sendRaw("010B"))
                                }
                                6 -> {
                                    ensureHeader("7E0")
                                    parseIntake(sendRaw("010F")).let { if (it > 0) currentIntakeC = it }
                                }
                                7 -> {
                                    ensureHeader("7E0")
                                    parseVolts(sendRaw("ATRV")).let { if (it > 0f) currentVolts = it }
                                }
                            }
                            bgStep++
                        }
                    }

                    // Compute derived metrics
                    val speedMph = (currentSpeedKmh * 0.621371f).toInt()
                    val coolantF = (currentCoolantC * 9 / 5) + 32
                    val intakeF = (currentIntakeC * 9 / 5) + 32
                    if (currentOilF <= 100) currentOilF = coolantF + 12

                    // Calculate live torque (Matching MX-5 ND2 Skyactiv 2.0L torque curve & boost)
                    val loadRatio = currentLoadPct / 100.0f
                    val rpmRatio = (currentRpm / 7500.0f).coerceIn(0.0f, 1.0f)
                    val calculatedTorqueLbFt = (151.0f * loadRatio * (0.55f + 0.45f * Math.sin(rpmRatio * Math.PI).toFloat()) + (currentBoostPsi * 7.5f)).toInt()
                    val calculatedTorqueNm = (calculatedTorqueLbFt * 1.35582f).toInt()

                    // Instant MPG estimation
                    val instantMpg = if (speedMph == 0) 0.0f else {
                        val fuelRateGph = 0.22f + 0.075f * (currentRpm / 1000.0f) * (currentLoadPct / 100.0f)
                        (speedMph / fuelRateGph.coerceAtLeast(0.05f)).coerceIn(8.0f, 65.0f)
                    }

                    _telemetry.value = LiveTelemetry(
                        rpm = currentRpm,
                        speedMph = speedMph,
                        speedKmh = currentSpeedKmh,
                        torqueLbFt = calculatedTorqueLbFt,
                        torqueNm = calculatedTorqueNm,
                        coolantTempC = currentCoolantC,
                        coolantTempF = coolantF,
                        oilTempF = currentOilF,
                        oilPressurePsi = currentOilPressurePsi,
                        intakeTempF = intakeF,
                        batteryVolts = currentVolts,
                        boostPsi = currentBoostPsi,
                        sparkAdvanceDeg = currentSparkDeg,
                        engineLoadPct = currentLoadPct,
                        throttlePosPct = (currentLoadPct * 0.9f).toInt(),
                        flPsi = currentFlPsi,
                        frPsi = currentFrPsi,
                        rlPsi = currentRlPsi,
                        rrPsi = currentRrPsi,
                        flTempF = currentFlTempF,
                        frTempF = currentFrTempF,
                        rlTempF = currentRlTempF,
                        rrTempF = currentRrTempF,
                        transGear = currentTransGear,
                        transTempF = currentTransTempF,
                        fuelLevelPct = currentFuelLevelPct,
                        instantMpg = instantMpg,
                        airFuelRatio = currentAfr,
                        steeringAngleDeg = currentSteerDeg,
                        lateralG = currentLatG,
                        longitudinalG = currentLongG,
                        ecmLatencyMs = ecmLatency,
                        tcmLatencyMs = tcmLatency,
                        bcmLatencyMs = bcmLatency
                    )
                    flightRecorder.onTelemetryTick(_telemetry.value)
                    dragyTracker.onTelemetryTick(_telemetry.value)

                    delay(40) // Brief pacing between frames
                } catch (e: Exception) {
                    Log.e(TAG, "Telemetry polling error: ${e.message}")
                    break
                }
            }
        }
    }

    private var currentActiveHeader: String = "7E0"
    private fun ensureHeader(header: String) {
        if (currentActiveHeader != header) {
            sendRaw("ATSH $header")
            currentActiveHeader = header
        }
    }

    fun clearCodes(): Boolean {
        if (isSimulatorMode) {
            _storedDtcs.value = emptyList()
            _pendingDtcs.value = emptyList()
            _freezeFrameData.value = emptyMap()
            _moduleReports.value = _moduleReports.value.map { it.copy(storedCodes = emptyList(), pendingCodes = emptyList()) }
            _emissionsReadiness.value = _emissionsReadiness.value?.copy(
                milActive = false,
                dtcCount = 0,
                catalystReady = false,
                evapReady = false
            )
            return true
        }
        val response = sendRaw("04")
        val success = response.contains("44") || response.contains("OK")
        if (success) {
            _storedDtcs.value = emptyList()
            _pendingDtcs.value = emptyList()
            _freezeFrameData.value = emptyMap()
            _moduleReports.value = _moduleReports.value.map { it.copy(storedCodes = emptyList(), pendingCodes = emptyList()) }
        }
        return success
    }

    fun executeBatteryReset(vin: String): Boolean {
        if (isSimulatorMode) return true

        val profile = VinDecoder.decode(vin)
        return when (profile.make.uppercase()) {
            "FORD" -> {
                sendRaw("1003") // Extended diagnostic session
                val res = sendRaw("3101224028") // Ford BMS routine
                sendRaw("1001") // Return to default session
                res.contains("7101")
            }
            "BMW", "MINI" -> {
                sendRaw("1003")
                val res = sendRaw("310101") // IBS CBS reset
                sendRaw("1001")
                res.contains("7101")
            }
            else -> {
                // Standard Clear & Reset
                clearCodes()
            }
        }
    }

    fun executeShopServiceRoutine(tool: ShopMaintenanceTool, onResult: (ServiceExecutionState) -> Unit) {
        if (isSimulatorMode) {
            scope.launch {
                onResult(ServiceExecutionState.Executing("Connecting to target ECU (${tool.targetEcuHeader})..."))
                delay(600)
                onResult(ServiceExecutionState.Executing("Transmitting bi-directional command ${tool.biDirectionalCommand}..."))
                delay(700)
                onResult(ServiceExecutionState.Success("${tool.title} executed successfully! Positive response received."))
            }
            return
        }

        scope.launch {
            try {
                onResult(ServiceExecutionState.Executing("Targeting ECU Header ${tool.targetEcuHeader}..."))
                ensureHeader(tool.targetEcuHeader)
                delay(80)

                onResult(ServiceExecutionState.Executing("Transmitting Routine ${tool.biDirectionalCommand}..."))
                val resp = sendRaw(tool.biDirectionalCommand.replace(" ", ""))

                if (resp.contains("71") || resp.contains("6F") || resp.contains("OK")) {
                    onResult(ServiceExecutionState.Success("${tool.title} completed successfully! ECU confirmed routine executed."))
                } else {
                    onResult(
                        ServiceExecutionState.FallbackManual(
                            reason = "ECU returned response: $resp. Showing official vehicle manual workshop procedure.",
                            manualProcedure = tool.manualProcedureSteps
                        )
                    )
                }
            } catch (e: Exception) {
                onResult(
                    ServiceExecutionState.FallbackManual(
                        reason = "Communication error (${e.message}). Showing official fallback manual procedure.",
                        manualProcedure = tool.manualProcedureSteps
                    )
                )
            } finally {
                ensureHeader("7E0")
            }
        }
    }

    /**
     * Executes a full multi-computer vehicle health scan across all known ECU headers.
     * Extracts confirmed and pending DTCs per computer, plus Mode 02 Freeze Frame snapshots.
     */
    fun scanAllModules(activeProfile: VehicleProfileDefinition? = null) {
        if (_isScanning.value) return
        _isScanning.value = true

        scope.launch {
            try {
                if (isSimulatorMode) {
                    delay(800) // Simulated bus transit delay
                    val ecmReport = ModuleDtcReport(
                        module = EcuModule.ECM,
                        header = activeProfile?.ecmHeader ?: "7E0",
                        storedCodes = listOf("P0171", "P0300"),
                        pendingCodes = listOf("P0420"),
                        isResponding = true
                    )
                    val tcmReport = ModuleDtcReport(
                        module = EcuModule.TCM,
                        header = activeProfile?.tcmHeader ?: "7E1",
                        storedCodes = emptyList(),
                        pendingCodes = emptyList(),
                        isResponding = true
                    )
                    val absReport = ModuleDtcReport(
                        module = EcuModule.ABS,
                        header = activeProfile?.absHeader ?: "7E2",
                        storedCodes = emptyList(),
                        pendingCodes = emptyList(),
                        isResponding = true
                    )
                    val bcmReport = ModuleDtcReport(
                        module = EcuModule.BCM,
                        header = activeProfile?.bcmHeader ?: "720",
                        storedCodes = listOf("B1200"),
                        pendingCodes = emptyList(),
                        isResponding = true
                    )
                    val srsReport = ModuleDtcReport(
                        module = EcuModule.SRS,
                        header = "780",
                        storedCodes = emptyList(),
                        pendingCodes = emptyList(),
                        isResponding = true
                    )

                    _moduleReports.value = listOf(ecmReport, tcmReport, absReport, bcmReport, srsReport)
                    _storedDtcs.value = listOf("P0171", "P0300", "B1200")
                    _pendingDtcs.value = listOf("P0420")
                    _freezeFrameData.value = mapOf(
                        "P0171" to FreezeFrameData(
                            dtc = "P0171",
                            rpm = 2150,
                            speedMph = 48,
                            coolantTempF = 194,
                            engineLoadPct = 42,
                            shortTermFuelTrim = 18.8f,
                            longTermFuelTrim = 21.5f
                        )
                    )
                    return@launch
                }

                // Real Hardware Query
                val targets = mutableListOf<Pair<EcuModule, String>>()
                targets.add(EcuModule.ECM to (activeProfile?.ecmHeader ?: "7E0"))
                activeProfile?.tcmHeader?.let { targets.add(EcuModule.TCM to it) }
                activeProfile?.absHeader?.let { targets.add(EcuModule.ABS to it) }
                activeProfile?.bcmHeader?.let { targets.add(EcuModule.BCM to it) }
                targets.add(EcuModule.SRS to "780")

                val reports = mutableListOf<ModuleDtcReport>()
                val allStored = mutableListOf<String>()
                val allPending = mutableListOf<String>()
                val ffMap = mutableMapOf<String, FreezeFrameData>()

                for ((mod, header) in targets) {
                    sendRaw("ATSH $header")
                    val rawStored = sendRaw("03")
                    val rawPending = sendRaw("07")

                    val isResponding = !rawStored.contains("UNABLE") && !rawStored.contains("ERROR") && !rawStored.contains("CAN ERROR")
                    val stored = if (isResponding) parseDtcResponse(rawStored) else emptyList()
                    val pending = if (isResponding) parseDtcResponse(rawPending) else emptyList()

                    allStored.addAll(stored)
                    allPending.addAll(pending)

                    reports.add(
                        ModuleDtcReport(
                            module = mod,
                            header = header,
                            storedCodes = stored,
                            pendingCodes = pending,
                            isResponding = isResponding
                        )
                    )

                    // If ECM and has stored DTCs, query Mode 02 Freeze Frame
                    if (mod == EcuModule.ECM && stored.isNotEmpty()) {
                        val ffDtcRaw = sendRaw("020200")
                        val ffDtc = parseFreezeFrameDtc(ffDtcRaw) ?: stored.firstOrNull()
                        if (ffDtc != null) {
                            val rpm = parseFreezeFrameRpm(sendRaw("020C00"))
                            val speed = parseFreezeFrameSpeed(sendRaw("020D00"))
                            val coolant = parseFreezeFrameCoolant(sendRaw("020500"))
                            val load = parseFreezeFrameLoad(sendRaw("020400"))
                            val stft = parseFreezeFrameTrim(sendRaw("020600"), "06")
                            val ltft = parseFreezeFrameTrim(sendRaw("020700"), "07")

                            ffMap[ffDtc] = FreezeFrameData(
                                dtc = ffDtc,
                                rpm = rpm,
                                speedMph = speed,
                                coolantTempF = coolant,
                                engineLoadPct = load,
                                shortTermFuelTrim = stft,
                                longTermFuelTrim = ltft
                            )
                        }
                    }
                }

                // Restore active header to ECM
                sendRaw("ATSH ${activeProfile?.ecmHeader ?: "7E0"}")
                currentActiveHeader = activeProfile?.ecmHeader ?: "7E0"

                _moduleReports.value = reports
                _storedDtcs.value = allStored.distinct()
                _pendingDtcs.value = allPending.distinct()
                _freezeFrameData.value = ffMap

                if (allStored.isNotEmpty() || allPending.isNotEmpty()) {
                    val triggerCode = (allStored + allPending).first()
                    flightRecorder.triggerIncident(
                        reason = "ECU Diagnostic Trouble Code Reported ($triggerCode)",
                        dtc = triggerCode,
                        freezeFrame = ffMap[triggerCode] ?: ffMap.values.firstOrNull()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed module DTC scan: ${e.message}", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun parseDtcResponse(raw: String): List<String> {
        val dtcs = mutableListOf<String>()
        val clean = raw.uppercase().replace("\r", " ").replace("\n", " ")
        val tokens = clean.split("\\s+".toRegex()).filter { it.isNotEmpty() && !it.contains(":") }

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            if (token == "43" || token == "47" || token == "4A") {
                val remaining = tokens.size - (i + 1)
                var pairStart = if (remaining % 2 != 0) i + 2 else i + 1
                while (pairStart + 1 < tokens.size) {
                    val hexA = tokens[pairStart]
                    val hexB = tokens[pairStart + 1]
                    if (hexA.length == 2 && hexB.length == 2) {
                        val byteA = hexA.toIntOrNull(16)
                        val byteB = hexB.toIntOrNull(16)
                        if (byteA != null && byteB != null) {
                            val code = decodeDtcBytes(byteA, byteB)
                            if (code != null && !dtcs.contains(code)) {
                                dtcs.add(code)
                            }
                        }
                    }
                    pairStart += 2
                }
                break
            } else if (token.startsWith("43") && token.length >= 6) {
                val hexOnly = token.substring(2)
                for (j in 0 until hexOnly.length - 3 step 4) {
                    val sub = hexOnly.substring(j, j + 4)
                    val byteA = sub.substring(0, 2).toIntOrNull(16)
                    val byteB = sub.substring(2, 4).toIntOrNull(16)
                    if (byteA != null && byteB != null) {
                        val code = decodeDtcBytes(byteA, byteB)
                        if (code != null && !dtcs.contains(code)) {
                            dtcs.add(code)
                        }
                    }
                }
            }
            i++
        }
        return dtcs
    }

    fun decodeDtcBytes(byteA: Int, byteB: Int): String? {
        if (byteA == 0 && byteB == 0) return null
        val typeChar = when ((byteA and 0xC0) shr 6) {
            0 -> 'P'
            1 -> 'C'
            2 -> 'B'
            3 -> 'U'
            else -> 'P'
        }
        val digit2 = ((byteA and 0x30) shr 4).toString(16).uppercase()
        val digit3 = (byteA and 0x0F).toString(16).uppercase()
        val digit45 = String.format("%02X", byteB)
        return "$typeChar$digit2$digit3$digit45"
    }

    fun parseFreezeFrameDtc(raw: String): String? {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val idx = clean.indexOf("4202")
        if (idx != -1 && clean.length >= idx + 8) {
            val byteA = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            val byteB = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: return null
            return decodeDtcBytes(byteA, byteB)
        }
        return null
    }

    fun parseFreezeFrameRpm(raw: String): Int? {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val idx = clean.indexOf("420C")
        if (idx != -1 && clean.length >= idx + 8) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            val b = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: return null
            return ((a * 256) + b) / 4
        }
        return null
    }

    fun parseFreezeFrameSpeed(raw: String): Int? {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val idx = clean.indexOf("420D")
        if (idx != -1 && clean.length >= idx + 6) {
            val kmh = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return (kmh * 0.621371f).toInt()
        }
        return null
    }

    fun parseFreezeFrameCoolant(raw: String): Int? {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val idx = clean.indexOf("4205")
        if (idx != -1 && clean.length >= idx + 6) {
            val c = (clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null) - 40
            return (c * 9 / 5) + 32
        }
        return null
    }

    fun parseFreezeFrameLoad(raw: String): Int? {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val idx = clean.indexOf("4204")
        if (idx != -1 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return (a * 100) / 255
        }
        return null
    }

    fun parseFreezeFrameTrim(raw: String, pidHex: String): Float? {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val idx = clean.indexOf("42$pidHex")
        if (idx != -1 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return ((a - 128) * 100.0f) / 128.0f
        }
        return null
    }

    fun parseOilPressure(raw: String): Int {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val idx = clean.indexOf("62115C")
        if (idx != -1 && clean.length >= idx + 10) {
            val a = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: 0
            val b = clean.substring(idx + 8, idx + 10).toIntOrNull(16) ?: 0
            val kpa = (a * 256 + b) / 4
            return (kpa * 0.145038f).toInt().coerceAtLeast(0)
        }
        return 42
    }

    private fun sendRaw(command: String): String {
        val out = outputStream ?: return ""
        val input = inputStream ?: return ""

        try {
            out.write((command.trim() + "\r").toByteArray(Charsets.US_ASCII))
            out.flush()

            val sb = StringBuilder()
            val buf = ByteArray(256)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 800) {
                if (input.available() > 0) {
                    val len = input.read(buf)
                    if (len > 0) {
                        val chunk = String(buf, 0, len, Charsets.US_ASCII)
                        sb.append(chunk)
                        if (chunk.contains(">")) break
                    }
                } else {
                    Thread.sleep(10)
                }
            }
            return sb.toString().replace(">", "").trim()
        } catch (e: Exception) {
            return ""
        }
    }

    private fun parseRpm(raw: String): Int {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "")
        val idx = clean.indexOf("410C")
        if (idx != -1 && clean.length >= idx + 8) {
            val hex = clean.substring(idx + 4, idx + 8)
            val a = hex.substring(0, 2).toIntOrNull(16) ?: 0
            val b = hex.substring(2, 4).toIntOrNull(16) ?: 0
            return ((a * 256) + b) / 4
        }
        return 0
    }

    private fun parseSpeed(raw: String): Int {
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("410D")
        if (idx != -1 && clean.length >= idx + 6) {
            return clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: 0
        }
        return 0
    }

    private fun parseCoolant(raw: String): Int {
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("4105")
        if (idx != -1 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: 40
            return a - 40
        }
        return 0
    }

    private fun parseVolts(raw: String): Float {
        val num = raw.replace("[^0-9.]".toRegex(), "")
        return num.toFloatOrNull() ?: 0.0f
    }

    private fun parseLoad(raw: String): Int {
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("4104")
        if (idx != -1 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: 0
            return (a * 100) / 255
        }
        return 0
    }

    private fun parseIntake(raw: String): Int {
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("410F")
        if (idx != -1 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: 40
            return a - 40
        }
        return 0
    }

    private fun parseBoost(raw: String): Float {
        // Mode 01 PID 0B: Intake Manifold Absolute Pressure (kPa)
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("410B")
        if (idx != -1 && clean.length >= idx + 6) {
            val kpa = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: 101
            val psi = (kpa - 101.325f) * 0.145038f
            return psi.coerceAtLeast(0.0f)
        }
        return 0.0f
    }

    private fun parseSpark(raw: String): Float {
        // Mode 01 PID 0E: Timing Advance ((A / 2) - 64)
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("410E")
        if (idx != -1 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: 0
            return (a / 2.0f) - 64.0f
        }
        return 12.0f
    }

    private fun parseFuelLevel(raw: String): Int {
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("412F")
        if (idx != -1 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: 0
            return ((a * 100) / 255).coerceIn(0, 100)
        }
        return 0
    }

    private fun parseMazdaOilTemp(raw: String): Int {
        // Mode 22 DID 1310 (Oil Temp C -> F)
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("621310")
        if (idx != -1 && clean.length >= idx + 10) {
            val hi = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: 0
            val lo = clean.substring(idx + 8, idx + 10).toIntOrNull(16) ?: 0
            val c = (((hi shl 8) or lo) / 100.0f) - 40.0f
            return ((c * 9 / 5) + 32).toInt()
        }
        return 0
    }

    private fun parseTcmGear(raw: String, speedKmh: Int): String {
        // Mode 22 DID 1E12 on TCM
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("621E12")
        if (idx != -1 && clean.length >= idx + 8) {
            val b = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: 0
            return when (b) {
                0x00 -> "P"
                0x01 -> "R"
                0x02 -> "N"
                0x03 -> "D1"
                0x04 -> "D2"
                0x05 -> "D3"
                0x06 -> "D4"
                0x07 -> "D5"
                0x08 -> "D6"
                else -> if (speedKmh == 0) "P" else "D"
            }
        }
        // Fallback gear estimation based on speed
        return if (speedKmh == 0) "P" else if (speedKmh < 18) "D1" else if (speedKmh < 35) "D2" else if (speedKmh < 50) "D3" else if (speedKmh < 70) "D4" else if (speedKmh < 90) "D5" else "D6"
    }

    private fun parseTcmTemp(raw: String): Int {
        // Mode 22 DID 1E1C on TCM: (raw * 0.063) C
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("621E1C")
        if (idx != -1 && clean.length >= idx + 10) {
            val hi = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: 0
            val lo = clean.substring(idx + 8, idx + 10).toIntOrNull(16) ?: 0
            val c = ((hi shl 8) or lo) * 0.063f
            return ((c * 9 / 5) + 32).toInt()
        }
        return 0
    }

    private fun parseTpmsPsi(raw: String): Float {
        // ND2 TPMS DIDs: 22 2A05..2A08 -> ((pb * 1373) / 1000) * 0.145038 PSI
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("622A")
        if (idx != -1 && clean.length >= idx + 8) {
            val pb = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: 0
            return ((pb * 1373.0f) / 1000.0f) * 0.145038f
        }
        return 0.0f
    }

    private fun parseTpmsTemp(raw: String): Int {
        // ND2 TPMS DIDs: 22 2A0A..2A0D -> raw byte tb is direct Fahrenheit (°F) on US ND2
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("622A")
        if (idx != -1 && clean.length >= idx + 8) {
            val tb = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: 0
            return if (tb in 10..180) tb else 85
        }
        return 0
    }

    fun parseVin(raw: String): String {
        val clean = raw.replace("\r", "\n")
        val lines = clean.split("\n").map { it.trim().replace(" ", "") }.filter { it.isNotEmpty() }
        val hexPayload = StringBuilder()

        for (line in lines) {
            if (line.contains("NODATA") || line.contains("ERROR")) continue
            // Multi-frame format 1: "0:490201..." / "1:..." / "2:..."
            val colonIdx = line.indexOf(":")
            val content = if (colonIdx != -1) line.substring(colonIdx + 1) else line

            // Mode 09 PID 02 response begins with 4902
            val idx4902 = content.indexOf("4902")
            if (idx4902 != -1) {
                // If it has a frame sequence byte (e.g. 490201...), skip 4902 + 2 chars
                val after = content.substring(idx4902 + 4)
                if (after.length >= 2 && after.startsWith("01")) {
                    hexPayload.append(after.substring(2))
                } else if (after.length >= 2 && after.startsWith("02")) {
                    hexPayload.append(after.substring(2))
                } else {
                    hexPayload.append(after)
                }
            } else if (content.length >= 10 && content.all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }) {
                // Continuation frame
                hexPayload.append(content)
            }
        }

        // Convert collected hex to ASCII
        val ascii = StringBuilder()
        val hexStr = hexPayload.toString()
        var i = 0
        while (i + 1 < hexStr.length) {
            val b = hexStr.substring(i, i + 2).toIntOrNull(16)
            if (b != null && b in 32..126) {
                ascii.append(b.toChar())
            }
            i += 2
        }

        val decoded = ascii.toString().trim()
        // Standard VIN is 17 alphanumeric chars
        val match = "[A-HJ-NPR-Z0-9]{17}".toRegex().find(decoded)
        return match?.value ?: decoded.take(17)
    }
}
