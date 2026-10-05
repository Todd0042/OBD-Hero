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
 * ECU Module identifiers for multi-computer vehicle polling & focus targeting.
 */
enum class EcuModule(val header: String, val displayName: String, val badge: String) {
    ECM("7E0", "Engine Control Module", "ECM 7E0"),
    TCM("7E1", "Transmission Control Module", "TCM 7E1"),
    ABS("7E2", "Anti-Lock Braking / Dynamics", "ABS 7E2"),
    BCM("720", "Body Control Module / TPMS", "BCM 720")
}

/**
 * Specific individual telemetry metrics that can be tapped to focus and poll at maximum frequency.
 */
enum class FocusedMetric(val module: EcuModule, val displayName: String) {
    RPM(EcuModule.ECM, "Engine RPM"),
    SPEED(EcuModule.ECM, "Ground Speed"),
    TORQUE(EcuModule.ECM, "Torque"),
    COOLANT(EcuModule.ECM, "Coolant Temp"),
    OIL_TEMP(EcuModule.ECM, "Oil Temp"),
    INTAKE_AIR(EcuModule.ECM, "Intake Air"),
    BOOST(EcuModule.ECM, "Boost PSI"),
    SPARK(EcuModule.ECM, "Spark Advance"),
    BATTERY(EcuModule.ECM, "Battery Voltage"),
    TRANSMISSION_GEAR(EcuModule.TCM, "Trans Gear"),
    TRANSMISSION_TEMP(EcuModule.TCM, "ATF Temp"),
    FUEL_ECONOMY(EcuModule.ECM, "Fuel & AFR"),
    DYNAMICS_G(EcuModule.ABS, "G-Force & Steer"),
    TPMS(EcuModule.BCM, "Tire Pressures & Temps")
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
 * Live vehicle telemetry snapshot with enthusiast telemetry & TPMS.
 */
data class LiveTelemetry(
    val rpm: Int = 0,
    val speedMph: Int = 0,
    val speedKmh: Int = 0,
    val torqueLbFt: Int = 0,
    val torqueNm: Int = 0,
    val coolantTempC: Int = 90,
    val coolantTempF: Int = 195,
    val oilTempF: Int = 210,
    val intakeTempF: Int = 78,
    val batteryVolts: Float = 0.0f,
    val boostPsi: Float = 0.0f,
    val sparkAdvanceDeg: Float = 14.5f,
    val engineLoadPct: Int = 0,
    val throttlePosPct: Int = 0,
    val shortTermFuelTrim: Float = 0.0f,
    val longTermFuelTrim: Float = 0.0f,
    // Live TPMS (PSI & Temp)
    val flPsi: Float = 32.5f,
    val frPsi: Float = 32.8f,
    val rlPsi: Float = 33.1f,
    val rrPsi: Float = 33.0f,
    val flTempF: Int = 85,
    val frTempF: Int = 86,
    val rlTempF: Int = 88,
    val rrTempF: Int = 87,
    // Multi-ECU Powertrain & Transmission Metrics (TCM - 0x7E1 / Mode 22)
    val transGear: String = "D4",
    val transTempF: Int = 168,
    // Fuel System & Hybrid/Electrical (ECM - 0x7E0 / Mode 01)
    val fuelLevelPct: Int = 68,
    val instantMpg: Float = 27.4f,
    val airFuelRatio: Float = 14.7f,
    // Chassis & Dynamics / Steering (ABS/ESP - 0x7E2)
    val steeringAngleDeg: Float = 0.0f,
    val lateralG: Float = 0.02f,
    val longitudinalG: Float = 0.05f,
    // ECU Polling & Bus Latency Performance
    val ecmLatencyMs: Int = 38,
    val tcmLatencyMs: Int = 45,
    val bcmLatencyMs: Int = 52
)

