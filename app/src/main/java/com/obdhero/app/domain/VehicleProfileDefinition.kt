package com.obdhero.app.domain

/**
 * Mathematical formula types for decoding raw hexadecimal PID/DID payloads.
 */
enum class FormulaType {
    RAW_RPM,             // ((A * 256) + B) / 4
    RAW_SPEED_KMH,       // A
    RAW_TEMP_C,          // A - 40
    RAW_MAP_KPA,         // A (or (A*256+B)/100 for high precision boost)
    RAW_SPARK_DEG,       // (A / 2) - 64
    RAW_MODULE_VOLTS,    // ((A * 256) + B) / 1000
    RAW_MAF_GPS,         // ((A * 256) + B) / 100
    MAZDA_TORQUE,        // ((A * 256) + B) - 500 (Nm)
    MAZDA_GEAR,          // Bitwise byte decode
    MAZDA_STEERING_G,    // Steering angle & lateral accelerometer
    MAZDA_TPMS_PSI       // ((A * 256) + B) * 0.145038 / 4
}

/**
 * Definition of an individual PID / DID query.
 */
data class PidDefinition(
    val id: String,
    val displayName: String,
    val ecuHeader: String = "7E0",
    val command: String,
    val formulaType: FormulaType,
    val unit: String
)

/**
 * Complete vehicle profile defining CAN architecture, addressing, and supported telemetry PIDs.
 */
data class VehicleProfileDefinition(
    val profileId: String,
    val make: String,
    val model: String,
    val generations: List<String> = emptyList(),
    val yearMin: Int = 1996,
    val yearMax: Int = 2030,
    val vinWmis: List<String> = emptyList(),
    val preferredProtocol: String = "ATSP0",
    val ecmHeader: String = "7E0",
    val tcmHeader: String? = null,
    val bcmHeader: String? = null,
    val absHeader: String? = null,
    val supportedFeatures: List<String> = emptyList(),
    val metrics: Map<String, PidDefinition> = emptyMap()
)
