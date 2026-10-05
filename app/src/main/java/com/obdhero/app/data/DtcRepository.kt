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

    // Master dictionary of 4,700+ official definitions (code -> official title/description)
    private val masterDefinitions = mutableMapOf<String, String>()

    // Manufacturer specific codes (e.g. Mazda P1170, BMW P1014)
    private val manufacturerCodes = mutableMapOf<String, MutableMap<String, DtcInfo>>()

    private var isInitialized = false

    /**
     * Initialize DTC database from assets.
     */
    fun init(context: Context) {
        if (isInitialized) return
        try {
            // 1. Load Master Definitions Dictionary (4,700+ codes)
            try {
                context.assets.open("dtc/dtc_definitions_master.json").use { stream ->
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val map: Map<String, String> = gson.fromJson(InputStreamReader(stream), type)
                    map.forEach { (k, v) -> masterDefinitions[k.uppercase()] = v }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Master definitions file not found or failed to parse: ${e.message}")
            }

            // 2. Load Rich Curated Generic J2012 Codes (with inspection steps and fixes)
            context.assets.open("dtc/dtc_generic.json").use { stream ->
                val type = object : TypeToken<List<DtcInfo>>() {}.type
                val list: List<DtcInfo> = gson.fromJson(InputStreamReader(stream), type)
                list.forEach { genericCodes[it.code.uppercase()] = it }
            }

            // 3. Load Mazda Codes
            loadManufacturerFile(context, "mazda", "dtc/dtc_mazda.json")

            // 4. Load BMW Codes
            loadManufacturerFile(context, "bmw", "dtc/dtc_bmw.json")

            // 5. Load Chrysler / Jeep / Dodge / Ram Codes
            loadManufacturerFile(context, "chrysler", "dtc/dtc_chrysler.json")
            loadManufacturerFile(context, "jeep", "dtc/dtc_chrysler.json")
            loadManufacturerFile(context, "dodge", "dtc/dtc_chrysler.json")
            loadManufacturerFile(context, "ram", "dtc/dtc_chrysler.json")

            // 6. Load Ford & Lincoln Codes
            loadManufacturerFile(context, "ford", "dtc/dtc_ford.json")
            loadManufacturerFile(context, "lincoln", "dtc/dtc_ford.json")

            // 7. Load GM / Chevrolet / GMC / Cadillac Codes
            loadManufacturerFile(context, "chevrolet", "dtc/dtc_gm.json")
            loadManufacturerFile(context, "chevy", "dtc/dtc_gm.json")
            loadManufacturerFile(context, "gmc", "dtc/dtc_gm.json")
            loadManufacturerFile(context, "cadillac", "dtc/dtc_gm.json")
            loadManufacturerFile(context, "buick", "dtc/dtc_gm.json")
            loadManufacturerFile(context, "gm", "dtc/dtc_gm.json")

            isInitialized = true
            Log.i(TAG, "DTC repository initialized with ${masterDefinitions.size} master definitions, ${genericCodes.size} rich generic guides, and ${manufacturerCodes.size} OEM packs.")
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
     * 1. Manufacturer specific rich repair guide (if make matches)
     * 2. Universal Generic rich repair guide (if hand-curated)
     * 3. Master dictionary definition (4,700+ codes) + upfront "data unavailable" disclaimers
     * 4. Unknown code raw fallback + upfront "data unavailable" disclaimers
     */
    fun get(code: String, make: String = ""): DtcInfo {
        val clean = code.trim().uppercase()
        val makeClean = make.trim().lowercase()

        // 1. Check manufacturer rich guide first
        if (makeClean.isNotEmpty()) {
            val oemMap = manufacturerCodes.entries.firstOrNull { makeClean.contains(it.key) }?.value
            if (oemMap != null && oemMap.containsKey(clean)) {
                return oemMap[clean]!!
            }
        }

        // 2. Check universal generic rich guide
        if (genericCodes.containsKey(clean)) {
            return genericCodes[clean]!!
        }

        // 3. Check master definition dictionary (4,700+ codes)
        val officialTitle = masterDefinitions[clean]
        if (officialTitle != null) {
            return buildStandardDtcWithHonestDisclaimer(clean, officialTitle)
        }

        // 4. Absolute fallback for completely unknown/obscure codes
        return buildCompletelyUnknownDtc(clean)
    }

    fun getAllPredefined(): List<DtcInfo> {
        val all = mutableListOf<DtcInfo>()
        all.addAll(genericCodes.values)
        manufacturerCodes.values.forEach { all.addAll(it.values) }
        return all
    }

    private fun getCategoryForCode(code: String): String {
        val prefix = if (code.isNotEmpty()) code.first() else 'P'
        return when (prefix) {
            'P' -> "Powertrain System (Engine & Transmission)"
            'B' -> "Body System (BCM, Lighting, Airbags, Climate)"
            'C' -> "Chassis System (ABS, Traction, Steering, Suspension)"
            'U' -> "Network Communication (CAN Bus Inter-Module)"
            else -> "Vehicle Diagnostic System"
        }
    }

    private fun buildStandardDtcWithHonestDisclaimer(code: String, title: String): DtcInfo {
        return DtcInfo(
            code = code,
            title = title,
            category = getCategoryForCode(code),
            meaning = "Official Definition: $title. The diagnostic controller registered an operational fault on this circuit.",
            symptoms = listOf(
                "Illuminated Check Engine or Module Service Indicator",
                "Detailed symptoms unavailable publicly. Contact an official repair center or OEM service manual for vehicle-specific driveability details."
            ),
            inspectionSteps = listOf(
                "Verify 12V battery health and terminal connections.",
                "Inspect sensor/circuit connector and wiring harness for visible corrosion, oil, or pin damage.",
                "Step-by-step diagnostic procedure unavailable publicly. Contact an official repair center or consult the OEM factory wiring manual for circuit pinouts."
            ),
            verifiedFixes = listOf(
                "Verified fix history unavailable publicly. Contact an official repair center for service bulletins (TSBs) and verified technician repairs."
            ),
            difficulty = "Moderate"
        )
    }

    private fun buildCompletelyUnknownDtc(code: String): DtcInfo {
        return DtcInfo(
            code = code,
            title = "Diagnostic Trouble Code: $code",
            category = getCategoryForCode(code),
            meaning = "Detailed diagnostic definition unavailable publicly. Contact an official repair center or consult factory service documentation for code $code.",
            symptoms = listOf(
                "Illuminated Check Engine or Service Indicator",
                "Symptom details unavailable publicly. Contact an official repair center for details."
            ),
            inspectionSteps = listOf(
                "Check 12V battery state of charge and battery terminals.",
                "Inspection steps unavailable publicly. Contact an official repair center for manufacturer diagnostic flowcharts."
            ),
            verifiedFixes = listOf(
                "Repair guide unavailable publicly. Contact an official repair center for details."
            ),
            difficulty = "Undetermined"
        )
    }
}
