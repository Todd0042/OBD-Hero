package com.obdhero.app.domain

import com.obdhero.app.data.AdapterDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class FlightRecorderState {
    STANDBY,
    ARMED_LISTENING,
    TRIGGERED_RECORDING,
    INCIDENT_SAVED
}

/**
 * High-speed Flight Recorder Black Box engine.
 * Maintains an in-memory 5-second circular ring buffer of powertrain telemetry.
 * Upon a DTC trigger or manual technician mark, freezes the pre-trigger buffer,
 * records 30 seconds of post-trigger behavior, captures Mode 02 Freeze Frame,
 * and packages a complete Incident Report.
 */
class BlackBoxFlightRecorder(
    private var adapterProfile: AdapterProfile = AdapterDatabase.PROFILES.first { it.name == "Vgate vLinker MS" }
) {
    private val preTriggerCapacity = 25 // 5.0 seconds at ~5 Hz (200ms per sample)
    private val postTriggerCapacity = 150 // 30.0 seconds at ~5 Hz

    private val preTriggerRingBuffer = ArrayDeque<Pair<Long, LiveTelemetry>>(preTriggerCapacity)
    private val postTriggerSamples = mutableListOf<BlackBoxSample>()

    private val _recorderState = MutableStateFlow(FlightRecorderState.ARMED_LISTENING)
    val recorderState: StateFlow<FlightRecorderState> = _recorderState.asStateFlow()

    private val _activeIncident = MutableStateFlow<BlackBoxIncidentReport?>(null)
    val activeIncident: StateFlow<BlackBoxIncidentReport?> = _activeIncident.asStateFlow()

    private val _incidentHistory = MutableStateFlow<List<BlackBoxIncidentReport>>(emptyList())
    val incidentHistory: StateFlow<List<BlackBoxIncidentReport>> = _incidentHistory.asStateFlow()

    private var triggerTimestamp = 0L
    private var triggerReasonString = ""
    private var triggerDtcCode: String? = null
    private var currentVin = ""

    fun updateAdapterProfile(profile: AdapterProfile) {
        adapterProfile = profile
    }

    fun setVehicleVin(vin: String) {
        currentVin = vin
    }

    /**
     * Called on every live telemetry tick.
     */
    fun onTelemetryTick(telemetry: LiveTelemetry, rawCanHex: List<String> = emptyList()) {
        val now = System.currentTimeMillis()

        when (_recorderState.value) {
            FlightRecorderState.ARMED_LISTENING -> {
                synchronized(preTriggerRingBuffer) {
                    if (preTriggerRingBuffer.size >= preTriggerCapacity) {
                        preTriggerRingBuffer.removeFirst()
                    }
                    preTriggerRingBuffer.addLast(Pair(now, telemetry))
                }
            }
            FlightRecorderState.TRIGGERED_RECORDING -> {
                val offsetMs = now - triggerTimestamp
                postTriggerSamples.add(
                    BlackBoxSample(
                        timestampOffsetMs = offsetMs,
                        rpm = telemetry.rpm,
                        speedMph = telemetry.speedMph,
                        boostPsi = telemetry.boostPsi,
                        coolantTempF = telemetry.coolantTempF,
                        oilTempF = telemetry.oilTempF,
                        oilPressurePsi = telemetry.oilPressurePsi,
                        batteryVolts = telemetry.batteryVolts,
                        sparkAdvanceDeg = telemetry.sparkAdvanceDeg,
                        shortTermFuelTrim = telemetry.shortTermFuelTrim,
                        longTermFuelTrim = telemetry.longTermFuelTrim,
                        throttlePosPct = telemetry.throttlePosPct,
                        rawCanFrames = rawCanHex
                    )
                )

                if (postTriggerSamples.size >= postTriggerCapacity || offsetMs >= 30000L) {
                    finalizeIncident(freezeFrame = null)
                }
            }
            else -> {}
        }
    }

    /**
     * Trigger incident capture either automatically from a DTC firing or manually via technician button.
     */
    fun triggerIncident(reason: String, dtc: String? = null, freezeFrame: FreezeFrameData? = null) {
        if (_recorderState.value == FlightRecorderState.TRIGGERED_RECORDING) return // Already recording an incident

        triggerTimestamp = System.currentTimeMillis()
        triggerReasonString = reason
        triggerDtcCode = dtc
        postTriggerSamples.clear()

        // Check if Cheap Garbage tier
        if (adapterProfile.tier == AdapterHardwareTier.CHEAP_GARBAGE) {
            val jokeReport = BlackBoxIncidentReport(
                incidentId = "INC-CLONE-${System.currentTimeMillis() % 100000}",
                recordedAt = triggerTimestamp,
                triggerReason = reason,
                triggerDtc = dtc,
                vehicleVin = currentVin,
                adapterProfile = adapterProfile,
                preTriggerSamples = emptyList(),
                postTriggerSamples = emptyList(),
                freezeFrame = null,
                booleanAsciiJoke = AdapterDatabase.BOOLEAN_ASCII_JOKE
            )
            _activeIncident.value = jokeReport
            _incidentHistory.value = listOf(jokeReport) + _incidentHistory.value
            _recorderState.value = FlightRecorderState.INCIDENT_SAVED
            return
        }

        _recorderState.value = FlightRecorderState.TRIGGERED_RECORDING
    }

    /**
     * Finalize incident capture and construct BlackBoxIncidentReport.
     */
    fun finalizeIncident(freezeFrame: FreezeFrameData?) {
        val preSamples = mutableListOf<BlackBoxSample>()
        synchronized(preTriggerRingBuffer) {
            preTriggerRingBuffer.forEach { (ts, telem) ->
                val offset = ts - triggerTimestamp
                preSamples.add(
                    BlackBoxSample(
                        timestampOffsetMs = offset,
                        rpm = telem.rpm,
                        speedMph = telem.speedMph,
                        boostPsi = telem.boostPsi,
                        coolantTempF = telem.coolantTempF,
                        oilTempF = telem.oilTempF,
                        oilPressurePsi = telem.oilPressurePsi,
                        batteryVolts = telem.batteryVolts,
                        sparkAdvanceDeg = telem.sparkAdvanceDeg,
                        shortTermFuelTrim = telem.shortTermFuelTrim,
                        longTermFuelTrim = telem.longTermFuelTrim,
                        throttlePosPct = telem.throttlePosPct
                    )
                )
            }
        }

        val dateTag = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(triggerTimestamp))
        val report = BlackBoxIncidentReport(
            incidentId = "INC-$dateTag",
            recordedAt = triggerTimestamp,
            triggerReason = triggerReasonString,
            triggerDtc = triggerDtcCode,
            vehicleVin = currentVin,
            adapterProfile = adapterProfile,
            preTriggerSamples = preSamples,
            postTriggerSamples = postTriggerSamples.toList(),
            freezeFrame = freezeFrame
        )

        _activeIncident.value = report
        _incidentHistory.value = listOf(report) + _incidentHistory.value
        _recorderState.value = FlightRecorderState.INCIDENT_SAVED
    }

    fun rearm() {
        synchronized(preTriggerRingBuffer) {
            preTriggerRingBuffer.clear()
        }
        postTriggerSamples.clear()
        _recorderState.value = FlightRecorderState.ARMED_LISTENING
    }
}
