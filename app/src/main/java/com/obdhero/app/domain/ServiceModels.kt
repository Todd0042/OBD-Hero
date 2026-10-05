package com.obdhero.app.domain

/**
 * High-velocity shop maintenance service categories.
 */
enum class ServiceFunctionCategory(val title: String, val badge: String) {
    OIL_MAINTENANCE("Oil & Maintenance", "OIL"),
    BRAKES_EPB("Electronic Parking Brake", "EPB"),
    BATTERY_BMS("Battery & BMS Reset", "BMS"),
    TIRES_TPMS("TPMS & Tire Service", "TPMS")
}

/**
 * Bi-directional execution state of a service tool function.
 */
sealed class ServiceExecutionState {
    data object Idle : ServiceExecutionState()
    data class Executing(val stage: String) : ServiceExecutionState()
    data class Success(val message: String) : ServiceExecutionState()
    data class FallbackManual(val reason: String, val manualProcedure: List<String>) : ServiceExecutionState()
    data class Error(val message: String) : ServiceExecutionState()
}

/**
 * Definition of a shop maintenance tool with both automated UDS bi-directional command
 * and step-by-step manual pedal/button fallback guide.
 */
data class ShopMaintenanceTool(
    val id: String,
    val title: String,
    val category: ServiceFunctionCategory,
    val targetEcuHeader: String,
    val biDirectionalCommand: String,
    val description: String,
    val prerequisites: List<String>,
    val manualProcedureSteps: List<String>,
    val confirmationPrompt: String
)
