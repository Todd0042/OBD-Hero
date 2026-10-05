package com.obdhero.app.domain

/**
 * Dragy acceleration run states.
 */
enum class DragyRunState(val displayName: String) {
    IDLE("Ready"),
    STAGED_AT_ZERO("STAGED (0 MPH) - HIT GAS!"),
    ACCELERATING("PULLING / RUNNING ACCEL..."),
    FINISHED_60("0-60 MPH COMPLETE!"),
    FINISHED_QUARTER("1/4 MILE TRAP COMPLETE!")
}

/**
 * Persisted High Scores / Personal Bests for an individual vehicle or technician session.
 */
data class VehiclePerformanceHighScores(
    val vin: String = "",
    val best0To60Seconds: Float? = null,
    val best0To30Seconds: Float? = null,
    val bestQuarterMileSeconds: Float? = null,
    val quarterMileTrapMph: Int? = null,
    val peakHorsepower: Int = 0,
    val peakTorqueLbFt: Int = 0,
    val peakSpeedMph: Int = 0,
    val lastUpdatedTimestamp: Long = 0L
)

/**
 * Real-time telemetry snapshot for the Virtual Road Dyno and Dragy acceleration deck.
 */
data class DragyTelemetrySnapshot(
    val runState: DragyRunState = DragyRunState.IDLE,
    val currentSpeedMph: Int = 0,
    val currentRpm: Int = 0,
    val currentHorsepower: Int = 0,
    val currentTorqueLbFt: Int = 0,
    val elapsed0To60Seconds: Float = 0.0f,
    val elapsedQuarterMileSeconds: Float = 0.0f,
    val quarterMileDistanceFeet: Float = 0.0f,
    val currentTrapSpeedMph: Int = 0,
    val sessionPeakHp: Int = 0,
    val sessionPeakTorque: Int = 0,
    val isNewRecord0To60: Boolean = false,
    val isNewRecordQuarter: Boolean = false,
    val isNewRecordHp: Boolean = false,
    val isNewRecordTorque: Boolean = false
)
