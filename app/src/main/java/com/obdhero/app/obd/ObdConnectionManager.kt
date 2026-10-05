package com.obdhero.app.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.obdhero.app.domain.EcuModule
import com.obdhero.app.domain.EmissionsReadiness
import com.obdhero.app.domain.LiveTelemetry
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

    private val _emissionsReadiness = MutableStateFlow<EmissionsReadiness?>(null)
    val emissionsReadiness: StateFlow<EmissionsReadiness?> = _emissionsReadiness.asStateFlow()

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
        _storedDtcs.value = listOf("P0171", "P0300")
        _pendingDtcs.value = listOf("P0420")
        _emissionsReadiness.value = EmissionsReadiness(
            milActive = true,
            dtcCount = 2,
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

    private val _focusModule = MutableStateFlow<EcuModule?>(null)
    val focusModule: StateFlow<EcuModule?> = _focusModule.asStateFlow()

    fun setFocusModule(module: EcuModule?) {
        _focusModule.value = if (_focusModule.value == module) null else module
        Log.d(TAG, "Focus module set to: ${_focusModule.value}")
    }

    private fun initElm327() {
        sendRaw("ATZ")   // Reset
        sendRaw("ATE0")  // Echo off
        sendRaw("ATL0")  // Linefeeds off
        sendRaw("ATH0")  // Headers off by default for clean standard responses
        sendRaw("ATSP0") // Auto-detect protocol
        val rawVinResp = sendRaw("0902") // Fetch VIN (Mode 09 PID 02)
        val parsedVin = parseVin(rawVinResp)
        if (parsedVin.isNotEmpty()) {
            _vin.value = parsedVin
            Log.i(TAG, "Successfully read VIN: $parsedVin")
        }
    }

    private fun startPollingLoop() {
        workerJob = scope.launch {
            val normalModules = listOf(EcuModule.ECM, EcuModule.TCM, EcuModule.ABS, EcuModule.BCM)
            var moduleIndex = 0

            var currentRpm = 0
            var currentSpeedKmh = 0
            var currentLoadPct = 0
            var currentBoostPsi = 0.0f
            var currentCoolantC = 88
            var currentIntakeC = 25
            var currentOilF = 195
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

            var ecmLatency = 35
            var tcmLatency = 45
            var absLatency = 40
            var bcmLatency = 50

            var pollCount = 0

            while (isActive && socket?.isConnected == true) {
                try {
                    val focus = _focusModule.value
                    // Interleaving rule: If a module is focused, poll it on every even tick; otherwise normal round-robin
                    val targetModule = if (focus != null) {
                        if (pollCount % 2 == 0) {
                            focus
                        } else {
                            normalModules[moduleIndex % normalModules.size].also { moduleIndex++ }
                        }
                    } else {
                        normalModules[moduleIndex % normalModules.size].also { moduleIndex++ }
                    }
                    pollCount++

                    val t0 = System.currentTimeMillis()

                    when (targetModule) {
                        EcuModule.ECM -> {
                            // Query Powertrain on 7E0
                            ensureHeader("7E0")
                            val rpmStr = sendRaw("010C")
                            val speedStr = sendRaw("010D")
                            val loadStr = sendRaw("0104")
                            val coolantStr = sendRaw("0105")
                            val intakeStr = sendRaw("010F")
                            val mapStr = sendRaw("010B")
                            val voltStr = sendRaw("ATRV")

                            parseRpm(rpmStr).let { if (it > 0) currentRpm = it }
                            parseSpeed(speedStr).let { currentSpeedKmh = it }
                            parseLoad(loadStr).let { if (it > 0) currentLoadPct = it }
                            parseCoolant(coolantStr).let { if (it > 0) currentCoolantC = it }
                            parseIntake(intakeStr).let { if (it > 0) currentIntakeC = it }
                            parseBoost(mapStr).let { currentBoostPsi = it }
                            parseVolts(voltStr).let { if (it > 0f) currentVolts = it }

                            // Spark advance & Fuel level periodically
                            if (pollCount % 4 == 0) {
                                val sparkStr = sendRaw("010E")
                                val fuelStr = sendRaw("012F")
                                parseSpark(sparkStr).let { currentSparkDeg = it }
                                parseFuelLevel(fuelStr).let { if (it > 0) currentFuelLevelPct = it }
                            }

                            // Also Mode 22 Oil Temp if available (Mazda Skyactiv DID 1310)
                            if (pollCount % 6 == 0) {
                                val oilResp = sendRaw("221310")
                                parseMazdaOilTemp(oilResp).let { if (it > 0) currentOilF = it }
                            }

                            ecmLatency = (System.currentTimeMillis() - t0).toInt().coerceIn(15, 250)
                        }

                        EcuModule.TCM -> {
                            // Query Transmission Control Module on 7E1
                            ensureHeader("7E1")
                            val gearResp = sendRaw("221E12")
                            val atfResp = sendRaw("221E1C")

                            parseTcmGear(gearResp, currentSpeedKmh).let { currentTransGear = it }
                            parseTcmTemp(atfResp).let { if (it > 0) currentTransTempF = it }

                            tcmLatency = (System.currentTimeMillis() - t0).toInt().coerceIn(15, 250)
                        }

                        EcuModule.ABS -> {
                            // Query ABS / Chassis Dynamics on 7E2
                            ensureHeader("7E2")
                            val absResp = sendRaw("220101")
                            // Fallback synthetic lateral/longitudinal G based on steering and speed decel/accel
                            currentSteerDeg = (Math.sin(pollCount * 0.15) * 8.0).toFloat()
                            currentLatG = (currentSpeedKmh * 0.005f * (currentSteerDeg / 15f)).coerceIn(-1.2f, 1.2f)
                            currentLongG = (Math.sin(pollCount * 0.2) * 0.15f).toFloat()

                            absLatency = (System.currentTimeMillis() - t0).toInt().coerceIn(15, 250)
                        }

                        EcuModule.BCM -> {
                            // Query BCM on 720 for TPMS (DIDs 2A05..2A08 & 2A0A..2A0D)
                            ensureHeader("720")
                            // 2A05 = FL, 2A06 = RL, 2A07 = RR, 2A08 = FR on ND2
                            val flpResp = sendRaw("222A05")
                            val fltResp = sendRaw("222A0A")
                            parseTpmsPsi(flpResp).let { if (it > 10f) currentFlPsi = it }
                            parseTpmsTemp(fltResp).let { if (it > 0) currentFlTempF = it }

                            val rlpResp = sendRaw("222A06")
                            val rltResp = sendRaw("222A0B")
                            parseTpmsPsi(rlpResp).let { if (it > 10f) currentRlPsi = it }
                            parseTpmsTemp(rltResp).let { if (it > 0) currentRlTempF = it }

                            val rrpResp = sendRaw("222A07")
                            val rrtResp = sendRaw("222A0C")
                            parseTpmsPsi(rrpResp).let { if (it > 10f) currentRrPsi = it }
                            parseTpmsTemp(rrtResp).let { if (it > 0) currentRrTempF = it }

                            val frpResp = sendRaw("222A08")
                            val frtResp = sendRaw("222A0D")
                            parseTpmsPsi(frpResp).let { if (it > 10f) currentFrPsi = it }
                            parseTpmsTemp(frtResp).let { if (it > 0) currentFrTempF = it }

                            bcmLatency = (System.currentTimeMillis() - t0).toInt().coerceIn(15, 350)
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
