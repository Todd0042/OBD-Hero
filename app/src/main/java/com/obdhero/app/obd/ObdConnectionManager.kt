package com.obdhero.app.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
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
                    rrTempF = 86
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

    private fun initElm327() {
        sendRaw("ATZ")   // Reset
        sendRaw("ATE0")  // Echo off
        sendRaw("ATL0")  // Linefeeds off
        sendRaw("ATH1")  // Headers on for multi-ECU identification
        sendRaw("ATSP0") // Auto-detect protocol
        sendRaw("0902")  // Fetch VIN
    }

    private fun startPollingLoop() {
        workerJob = scope.launch {
            while (isActive && socket?.isConnected == true) {
                try {
                    val rpmStr = sendRaw("010C")
                    val speedStr = sendRaw("010D")
                    val tempStr = sendRaw("0105")
                    val voltStr = sendRaw("ATRV")

                    val rpm = parseRpm(rpmStr)
                    val speed = parseSpeed(speedStr)
                    val coolant = parseCoolant(tempStr)
                    val volts = parseVolts(voltStr)

                    _telemetry.value = LiveTelemetry(
                        rpm = rpm,
                        speedKmh = speed,
                        coolantTempC = coolant,
                        batteryVolts = volts
                    )

                    delay(250)
                } catch (e: Exception) {
                    Log.e(TAG, "Telemetry polling error: ${e.message}")
                    break
                }
            }
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
}
