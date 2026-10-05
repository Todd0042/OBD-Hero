package com.obdhero.app.domain

/**
 * Categorization tiers for OBD-II scan tool hardware.
 */
enum class AdapterHardwareTier(
    val title: String,
    val badge: String,
    val description: String,
    val blackBoxCapability: String
) {
    CHEAP_GARBAGE(
        title = "Budget Clone Tier",
        badge = "CLONE",
        description = "Generic 8-bit ELM327 clone PIC microcontroller. Small 256B buffer, prone to buffer overruns and crashes under bus load.",
        blackBoxCapability = "UNSUPPORTED"
    ),
    BLE_FILTERED(
        title = "BLE Filtered Tier (CX-Class)",
        badge = "BLE PRO",
        description = "32-bit STN/OBDLink architecture over BLE 5.x. Hardware CAN ID pass filtering for clean, lossless BLE streaming.",
        blackBoxCapability = "FILTERED_CAN_STREAMS"
    ),
    OPTIMAL(
        title = "Optimal Hardware Tier (vLinker MS-Class)",
        badge = "OPTIMAL",
        description = "32-bit ARM Cortex with 4KB-8KB RAM buffer. Dual-bus HS-CAN & MS-CAN multiplexer, up to 2Mbps SPP throughput, zero packet drop.",
        blackBoxCapability = "FULL_MULTI_BUS_BLACKBOX"
    ),
    EXCEPTIONAL(
        title = "Exceptional Enterprise Tier",
        badge = "ENTERPRISE",
        description = "Pro-grade OBDLink MX+ / J2534 PassThru / STN2120. Promiscuous full-bus dump with microsecond hardware timestamping.",
        blackBoxCapability = "PROMISCUOUS_ENTERPRISE_BLACKBOX"
    )
}

/**
 * Specifications for a recognized adapter hardware profile.
 */
data class AdapterProfile(
    val name: String,
    val tier: AdapterHardwareTier,
    val chipArchitecture: String,
    val bufferBytes: Int,
    val supportedProtocols: List<String>,
    val multiBusCapable: Boolean = false,
    val maxBaudRateBps: Int = 115200,
    val snarkyEasterEgg: String? = null
)

/**
 * An individual snapshot sample captured by the Black Box Flight Recorder.
 */
data class BlackBoxSample(
    val timestampOffsetMs: Long, // Negative for pre-trigger (-5000 to 0), positive for post-trigger (0 to 30000)
    val rpm: Int,
    val speedMph: Int,
    val boostPsi: Float,
    val coolantTempF: Int,
    val oilTempF: Int,
    val oilPressurePsi: Int,
    val batteryVolts: Float,
    val sparkAdvanceDeg: Float,
    val shortTermFuelTrim: Float,
    val longTermFuelTrim: Float,
    val throttlePosPct: Int,
    val rawCanFrames: List<String> = emptyList()
)

/**
 * Complete serialized Incident Report captured by the Flight Recorder.
 */
data class BlackBoxIncidentReport(
    val incidentId: String,
    val recordedAt: Long,
    val triggerReason: String,
    val triggerDtc: String?,
    val vehicleVin: String,
    val adapterProfile: AdapterProfile,
    val preTriggerSamples: List<BlackBoxSample>,
    val postTriggerSamples: List<BlackBoxSample>,
    val freezeFrame: FreezeFrameData?,
    val booleanAsciiJoke: String? = null
) {
    val totalSamples: Int get() = preTriggerSamples.size + postTriggerSamples.size
    val durationSeconds: Float get() = if (totalSamples == 0) 0f else (postTriggerSamples.lastOrNull()?.timestampOffsetMs ?: 0L - (preTriggerSamples.firstOrNull()?.timestampOffsetMs ?: 0L)) / 1000f
}
