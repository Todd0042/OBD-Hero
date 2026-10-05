package com.obdhero.app.domain

import com.obdhero.app.data.PerformanceHighScoresRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real-time Virtual Road Dyno & Acceleration Dragy Tracker engine.
 * Computes live horsepower from torque and RPM, tracks acceleration runs (0-60, 1/4 mile),
 * and checks against persistent High Scores.
 */
class DragyAccelerationTracker {

    private val _snapshot = MutableStateFlow(DragyTelemetrySnapshot())
    val snapshot: StateFlow<DragyTelemetrySnapshot> = _snapshot.asStateFlow()

    private var activeVin: String = ""
    private var launchTimestampMs: Long = 0L
    private var lastTickTimestampMs: Long = 0L
    private var accumulatedDistanceFeet: Float = 0.0f
    private var captured0To60 = false
    private var capturedQuarter = false
    private var runState: DragyRunState = DragyRunState.IDLE

    private var sessionMaxHp = 0
    private var sessionMaxTorque = 0

    fun setVehicleVin(vin: String) {
        activeVin = vin
    }

    fun resetRun() {
        runState = DragyRunState.IDLE
        launchTimestampMs = 0L
        lastTickTimestampMs = 0L
        accumulatedDistanceFeet = 0.0f
        captured0To60 = false
        capturedQuarter = false
        _snapshot.value = _snapshot.value.copy(
            runState = DragyRunState.IDLE,
            elapsed0To60Seconds = 0f,
            elapsedQuarterMileSeconds = 0f,
            quarterMileDistanceFeet = 0f,
            currentTrapSpeedMph = 0,
            isNewRecord0To60 = false,
            isNewRecordQuarter = false
        )
    }

    fun onTelemetryTick(telemetry: LiveTelemetry) {
        val now = System.currentTimeMillis()
        val speedMph = telemetry.speedMph
        val rpm = telemetry.rpm
        val torque = telemetry.torqueLbFt

        // 1. Dyno Calculations: Horsepower = (Torque * RPM) / 5252
        val calculatedHp = if (rpm > 0 && torque > 0) ((torque * rpm) / 5252).coerceAtLeast(0) else 0

        var isNewHp = false
        var isNewTorque = false

        if (calculatedHp > sessionMaxHp) {
            sessionMaxHp = calculatedHp
            isNewHp = PerformanceHighScoresRepository.recordPeakHp(activeVin, calculatedHp)
        }

        if (torque > sessionMaxTorque) {
            sessionMaxTorque = torque
            isNewTorque = PerformanceHighScoresRepository.recordPeakTorque(activeVin, torque)
        }

        // 2. Dragy Acceleration State Machine
        var elapsed60 = _snapshot.value.elapsed0To60Seconds
        var elapsedQuarter = _snapshot.value.elapsedQuarterMileSeconds
        var trapSpeed = _snapshot.value.currentTrapSpeedMph
        var isNew0To60 = _snapshot.value.isNewRecord0To60
        var isNewQtr = _snapshot.value.isNewRecordQuarter

        when (runState) {
            DragyRunState.IDLE -> {
                if (speedMph == 0) {
                    runState = DragyRunState.STAGED_AT_ZERO
                }
            }

            DragyRunState.STAGED_AT_ZERO -> {
                if (speedMph > 0) {
                    // Vehicle launched!
                    runState = DragyRunState.ACCELERATING
                    launchTimestampMs = now
                    lastTickTimestampMs = now
                    accumulatedDistanceFeet = 0f
                    captured0To60 = false
                    capturedQuarter = false
                    isNew0To60 = false
                    isNewQtr = false
                }
            }

            DragyRunState.ACCELERATING -> {
                if (speedMph == 0) {
                    // Aborted or stopped before finishing
                    runState = DragyRunState.STAGED_AT_ZERO
                } else {
                    val dtSeconds = if (lastTickTimestampMs > 0) ((now - lastTickTimestampMs).toFloat() / 1000f).coerceIn(0.01f, 1.0f) else 0.1f
                    lastTickTimestampMs = now

                    // Distance integration: 1 mph = 1.46667 feet/sec
                    accumulatedDistanceFeet += speedMph * 1.46667f * dtSeconds

                    val elapsedFromLaunch = (now - launchTimestampMs).toFloat() / 1000f

                    // 0-60 MPH check
                    if (!captured0To60) {
                        if (speedMph >= 60) {
                            captured0To60 = true
                            elapsed60 = elapsedFromLaunch
                            isNew0To60 = PerformanceHighScoresRepository.record0To60(activeVin, elapsed60)
                        } else {
                            elapsed60 = elapsedFromLaunch
                        }
                    }

                    // 1/4 Mile check (1320 feet)
                    if (!capturedQuarter) {
                        if (accumulatedDistanceFeet >= 1320f) {
                            capturedQuarter = true
                            elapsedQuarter = elapsedFromLaunch
                            trapSpeed = speedMph
                            isNewQtr = PerformanceHighScoresRepository.recordQuarterMile(activeVin, elapsedQuarter, trapSpeed)
                            runState = DragyRunState.FINISHED_QUARTER
                        } else {
                            elapsedQuarter = elapsedFromLaunch
                        }
                    }
                }
            }

            DragyRunState.FINISHED_60, DragyRunState.FINISHED_QUARTER -> {
                if (speedMph == 0) {
                    runState = DragyRunState.STAGED_AT_ZERO
                }
            }
        }

        _snapshot.value = DragyTelemetrySnapshot(
            runState = runState,
            currentSpeedMph = speedMph,
            currentRpm = rpm,
            currentHorsepower = calculatedHp,
            currentTorqueLbFt = torque,
            elapsed0To60Seconds = elapsed60,
            elapsedQuarterMileSeconds = elapsedQuarter,
            quarterMileDistanceFeet = accumulatedDistanceFeet.coerceAtMost(1320f),
            currentTrapSpeedMph = trapSpeed,
            sessionPeakHp = sessionMaxHp,
            sessionPeakTorque = sessionMaxTorque,
            isNewRecord0To60 = isNew0To60,
            isNewRecordQuarter = isNewQtr,
            isNewRecordHp = isNewHp,
            isNewRecordTorque = isNewTorque
        )
    }
}
