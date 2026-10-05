package com.obdhero.app.domain

/**
 * Diagnostic Trouble Code information with actionable repair guides.
 */
data class DtcInfo(
    val code: String,
    val title: String,
    val category: String,
    val meaning: String,
    val symptoms: List<String>,
    val inspectionSteps: List<String>,
    val verifiedFixes: List<String>,
    val difficulty: String = "Moderate" // Beginner, Moderate, Advanced
)

/**
 * Standard continuous & non-continuous emissions readiness monitor flags (SAE J1979 Mode 01 PID 01).
 */
data class EmissionsReadiness(
    val milActive: Boolean,
    val dtcCount: Int,
    val misfireReady: Boolean,
    val fuelSystemReady: Boolean,
    val compComponentsReady: Boolean,
    val catalystReady: Boolean,
    val evapReady: Boolean,
    val o2SensorReady: Boolean,
    val egrReady: Boolean
) {
    val isOverallPass: Boolean
        get() = !milActive && dtcCount == 0 && misfireReady && fuelSystemReady && compComponentsReady && catalystReady && evapReady && o2SensorReady
}

/**
 * Vehicle Profile decoded from Mode 09 PID 02 VIN.
 */
data class VehicleProfile(
    val vin: String,
    val year: Int?,
    val make: String,
    val model: String,
    val engine: String = "",
    val fuelType: String = "Gasoline",
    val isBmwFrmVulnerable: Boolean = false,
    val isSgwLocked: Boolean = false
)

/**
 * Live vehicle telemetry snapshot.
 */
data class LiveTelemetry(
    val rpm: Int = 0,
    val speedKmh: Int = 0,
    val coolantTempC: Int = 0,
    val batteryVolts: Float = 0.0f,
    val engineLoadPct: Int = 0,
    val shortTermFuelTrim: Float = 0.0f,
    val longTermFuelTrim: Float = 0.0f
)
