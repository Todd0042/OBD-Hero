package com.obdhero.app.data

import com.obdhero.app.domain.DtcInfo

object DtcRepository {

    private val DATABASE = mapOf(
        "P0171" to DtcInfo(
            code = "P0171",
            title = "System Too Lean (Bank 1)",
            category = "Fuel & Air Metering",
            meaning = "ECU detects excess unmetered oxygen in the exhaust; positive fuel trim is maxed out (+20% to +25%).",
            symptoms = listOf(
                "Rough or stumbling engine idle",
                "Sluggish acceleration or hesitation under throttle",
                "Misfire codes under heavy load (P0300)",
                "Decreased fuel economy"
            ),
            inspectionSteps = listOf(
                "Inspect intake accordion bellows boot for tears or hairline cracks between MAF and throttle body.",
                "Check PCV hose and positive crankcase ventilation valve for cracks or oil spray.",
                "Spray electronic cleaner / carb cleaner lightly around intake gasket to identify vacuum leaks at idle.",
                "Inspect Mass Air Flow (MAF) sensor hot-wire for dust, oil, or dirt accumulation.",
                "Monitor live Fuel Rail Pressure under acceleration (confirm fuel pump delivers specified PSI)."
            ),
            verifiedFixes = listOf(
                "Clean MAF sensor with dedicated CRC MAF spray (60% common fix)",
                "Replace cracked intake boot / PCV vacuum hose (25% common fix)",
                "Replace failing Bank 1 Upstream O2/Air-Fuel Sensor (15% common fix)"
            ),
            difficulty = "Beginner"
        ),

        "P0172" to DtcInfo(
            code = "P0172",
            title = "System Too Rich (Bank 1)",
            category = "Fuel & Air Metering",
            meaning = "ECU detects excessive unburnt fuel; negative fuel trim is maxed out (-20% to -25%).",
            symptoms = listOf(
                "Black smoke or strong gasoline smell from exhaust",
                "Engine oil smells strongly of gasoline",
                "Spark plug fouling causing rough idle",
                "High hydrocarbon emissions / failed smog test"
            ),
            inspectionSteps = listOf(
                "Sniff engine oil dipstick for raw fuel aroma (indicates direct-injection HPFP internal seal failure).",
                "Inspect engine air filter for complete blockage or debris.",
                "Check EVAP purge valve solenoid: unplug electrical harness and check if vacuum still pulls fuel vapors.",
                "Inspect spark plugs to identify which specific cylinder is running rich (leaking injector)."
            ),
            verifiedFixes = listOf(
                "Replace stuck-open EVAP purge solenoid valve (45% common fix)",
                "Replace leaking direct injector (30% common fix)",
                "Clean dirty MAF sensor over-reading airflow (20% common fix)",
                "Replace failing high pressure fuel pump seal leaking into crankcase (5% fix)"
            ),
            difficulty = "Moderate"
        ),

        "P0300" to DtcInfo(
            code = "P0300",
            title = "Random / Multiple Cylinder Misfire Detected",
            category = "Ignition & Combustion",
            meaning = "Crankshaft position sensor detects angular acceleration drops across multiple cylinders, indicating incomplete combustion.",
            symptoms = listOf(
                "Flashing Check Engine Light (Active catalytic converter damage risk)",
                "Severe engine shaking, shuddering, or loss of power",
                "Pungent raw exhaust smell",
                "Hesitation during highway passing"
            ),
            inspectionSteps = listOf(
                "Check ignition coils and boots for white carbon tracking or oil soaking from valve cover gaskets.",
                "Remove and inspect spark plug gap and wear on ground electrodes.",
                "Check fuel rail pressure under load to rule out weak low-pressure in-tank pump.",
                "Check for major upstream intake vacuum leak affecting all cylinders simultaneously."
            ),
            verifiedFixes = listOf(
                "Replace worn spark plugs and ignition coils (65% common fix)",
                "Fix vacuum leak at intake manifold runner (20% common fix)",
                "Replace clogged fuel injector or clean carbon buildup on intake valves (15% fix)"
            ),
            difficulty = "Beginner"
        ),

        "P0420" to DtcInfo(
            code = "P0420",
            title = "Catalyst System Efficiency Below Threshold (Bank 1)",
            category = "Emissions Control",
            meaning = "Downstream oxygen sensor oxygen-storage waveform mirrors the upstream sensor, showing catalytic converter is no longer storing and reducing oxygen.",
            symptoms = listOf(
                "Check Engine Light illuminated (no immediate driveability loss in early stages)",
                "Rotten egg / sulfur smell from exhaust",
                "Failed state emissions inspection"
            ),
            inspectionSteps = listOf(
                "Confirm no upstream exhaust manifold leaks or broken flex-pipe studs before replacing catalyst.",
                "Verify no active misfire (P0300) or rich condition (P0172) that melted or contaminated the catalyst.",
                "Compare upstream vs downstream O2 sensor voltages on live scan: downstream should hold steady ~0.6V–0.7V."
            ),
            verifiedFixes = listOf(
                "Replace Bank 1 Catalytic Converter (70% common fix)",
                "Replace faulty Downstream O2 Sensor (20% common fix)",
                "Weld/seal exhaust flange leak ahead of converter (10% fix)"
            ),
            difficulty = "Moderate"
        ),

        "P0128" to DtcInfo(
            code = "P0128",
            title = "Coolant Temp Below Thermostat Regulating Temp",
            category = "Cooling & Thermal",
            meaning = "Engine takes abnormally long to reach closed-loop operating temperature (180°F / 82°C).",
            symptoms = listOf(
                "Cabin heater blows only lukewarm air in cold weather",
                "Temperature gauge stays below center position",
                "Elevated idle RPM and poor fuel economy"
            ),
            inspectionSteps = listOf(
                "Feel upper radiator hose shortly after cold start: if it warms up immediately, thermostat is stuck open.",
                "Inspect coolant expansion tank level.",
                "Read live Engine Coolant Temperature (ECT) sensor telemetry to verify sensor reads smoothly."
            ),
            verifiedFixes = listOf(
                "Replace stuck-open engine thermostat assembly (90% common fix)",
                "Replace faulty Engine Coolant Temperature (ECT) sensor (10% fix)"
            ),
            difficulty = "Beginner"
        ),

        "U0100" to DtcInfo(
            code = "U0100",
            title = "Lost Communication with ECM / PCM 'A'",
            category = "CAN Bus & Network",
            meaning = "Transmission Control Module (TCM), ABS, or BCM stopped receiving expected periodic CAN heartbeat messages from Engine Control Module.",
            symptoms = listOf(
                "Multiple warning lights (ABS, Traction, CEL, Transmission)",
                "Vehicle in transmission limp mode or no-crank / no-start condition",
                "Loss of speedometer or tachometer"
            ),
            inspectionSteps = listOf(
                "Measure 12V battery voltage (low voltage below 11.5V during crank is #1 cause of lost CAN communication).",
                "Inspect main engine bay battery ground strap to chassis and engine block for corrosion.",
                "Inspect PCM wiring harness for rodent chew or water intrusion in plug pins.",
                "Test CAN high (pin 6) and CAN low (pin 14) resistance at OBD port with battery disconnected (should be 60 ohms)."
            ),
            verifiedFixes = listOf(
                "Clean/tighten corroded battery terminals and chassis grounds (50% common fix)",
                "Replace weak or failing 12V battery (35% common fix)",
                "Repair damaged CAN bus wiring harness or damaged PCM plug pins (15% fix)"
            ),
            difficulty = "Moderate"
        )
    )

    fun get(code: String): DtcInfo {
        val clean = code.trim().uppercase()
        return DATABASE[clean] ?: generateGeneric(clean)
    }

    fun getAllPredefined(): List<DtcInfo> = DATABASE.values.toList()

    private fun generateGeneric(code: String): DtcInfo {
        val prefix = if (code.isNotEmpty()) code.first() else 'P'
        val category = when (prefix) {
            'P' -> "Powertrain System (Engine & Transmission)"
            'B' -> "Body System (BCM, Lighting, Airbags, Climate)"
            'C' -> "Chassis System (ABS, Traction, Steering, Suspension)"
            'U' -> "Network Communication (CAN Bus Inter-Module)"
            else -> "Vehicle Diagnostic System"
        }

        return DtcInfo(
            code = code,
            title = "Standard Diagnostic Trouble Code",
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
