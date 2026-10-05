package com.obdhero.app.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.obdhero.app.domain.DtcInfo
import java.io.InputStreamReader

object DtcRepository {
    private const val TAG = "DtcRepository"
    private val gson = Gson()

    // Generic SAE standard codes (shared across all vehicles)
    private val genericCodes = mutableMapOf<String, DtcInfo>()

    // Manufacturer specific codes (e.g. Mazda P1170, BMW P1014)
    private val manufacturerCodes = mutableMapOf<String, MutableMap<String, DtcInfo>>()

    private var isInitialized = false

    /**
     * Initialize DTC database from assets.
     */
    fun init(context: Context) {
        if (isInitialized) return
        try {
            // 1. Load Generic J2012 Codes
            context.assets.open("dtc/dtc_generic.json").use { stream ->
                val type = object : TypeToken<List<DtcInfo>>() {}.type
                val list: List<DtcInfo> = gson.fromJson(InputStreamReader(stream), type)
                list.forEach { genericCodes[it.code.uppercase()] = it }
            }

            // 2. Load Mazda Codes
            loadManufacturerFile(context, "mazda", "dtc/dtc_mazda.json")

            // 3. Load BMW Codes
            loadManufacturerFile(context, "bmw", "dtc/dtc_bmw.json")

            isInitialized = true
            Log.i(TAG, "DTC repository initialized with ${genericCodes.size} generic codes and ${manufacturerCodes.size} OEM packs.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading DTC database from assets: ${e.message}", e)
        }
    }

    private fun loadManufacturerFile(context: Context, makeKey: String, path: String) {
        try {
            context.assets.open(path).use { stream ->
                val type = object : TypeToken<List<DtcInfo>>() {}.type
                val list: List<DtcInfo> = gson.fromJson(InputStreamReader(stream), type)
                val map = manufacturerCodes.getOrPut(makeKey.lowercase()) { mutableMapOf() }
                list.forEach { map[it.code.uppercase()] = it }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load OEM DTC file $path: ${e.message}")
        }
    }

    /**
     * Lookup DTC definition with fallback chain:
     * 1. Manufacturer specific lookup (if make provided and code is 1xxx/2xxx)
     * 2. Universal Generic SAE lookup (0xxx)
     * 3. Prefix decomposition fallback
     */
    fun get(code: String, make: String = ""): DtcInfo {
        val clean = code.trim().uppercase()
        val makeClean = make.trim().lowercase()

        // Check manufacturer overlay first
        if (makeClean.isNotEmpty()) {
            val oemMap = manufacturerCodes.entries.firstOrNull { makeClean.contains(it.key) }?.value
            if (oemMap != null && oemMap.containsKey(clean)) {
                return oemMap[clean]!!
            }
        }

        // Check universal generic standard codes
        if (genericCodes.containsKey(clean)) {
            return genericCodes[clean]!!
        }

        // Fallback: rule-based standard breakdown
        return generateGeneric(clean)
    }

    fun getAllPredefined(): List<DtcInfo> {
        val all = mutableListOf<DtcInfo>()
        all.addAll(genericCodes.values)
        manufacturerCodes.values.forEach { all.addAll(it.values) }
        return all
    }

    private fun generateGeneric(code: String): DtcInfo {
        val prefix = if (code.isNotEmpty()) code.first() else 'P'
        val category = when (prefix) {
            'P' -> "Powertrain System (Engine & Transmission)"
            'B' -> "Body System (BCM, Lighting, Airbags, Climate)"
            'C' -> "Chassis System (ABS, Traction, Steering, Suspension)"
            'U' -> "Network Communication (CAN Bus Inter-Module)"
            else -> "Vehicle Diagnostic System"
        }

        val isManufacturerControlled = code.length >= 2 && code[1] in listOf('1', '2', '3')
        val typeDesc = if (isManufacturerControlled) "Manufacturer-Controlled Diagnostic Trouble Code" else "Standard SAE J2012 Diagnostic Trouble Code"

        return DtcInfo(
            code = code,
            title = typeDesc,
            category = category,
            meaning = "The onboard diagnostic computer logged a detected operational variance adhering to SAE J2012 classification for code $code.",
            symptoms = listOf(
                "Illuminated Check Engine or Module Service Indicator",
                "Potential driveability or performance variance depending on driving cycle"
            ),
            inspectionSteps = listOf(
                "Check 12V battery health and terminal connections.",
                "Inspect sensor connectors and wiring harness for visible damage, oil intrusion, or loose pins.",
                "Query freeze frame data to inspect engine load, RPM, and temperature at moment code was tripped."
            ),
            verifiedFixes = listOf(
                "Consult OEM factory service manual for detailed circuit pinout diagnostics for code $code.",
                "Verify sensor signal ground and 5V reference voltage."
            ),
            difficulty = "Moderate"
        )
    }
}
