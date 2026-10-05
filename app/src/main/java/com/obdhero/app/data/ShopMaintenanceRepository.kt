package com.obdhero.app.data

import com.obdhero.app.domain.ServiceFunctionCategory
import com.obdhero.app.domain.ShopMaintenanceTool

object ShopMaintenanceRepository {

    fun getToolsForVehicle(make: String): List<ShopMaintenanceTool> {
        val makeClean = make.uppercase()

        val oilTool = when {
            makeClean.contains("FORD") || makeClean.contains("LINCOLN") -> ShopMaintenanceTool(
                id = "OIL_RESET_FORD",
                title = "Engine Oil Life Reset (Ford / Lincoln)",
                category = ServiceFunctionCategory.OIL_MAINTENANCE,
                targetEcuHeader = "7E0",
                biDirectionalCommand = "31 01 02 01",
                description = "Resets Ford Intelligent Oil-Life Monitor (IOLM) to 100% on PCM (7E0).",
                prerequisites = listOf("Ignition ON (Engine OFF)", "Transmission in Park", "Driver door closed"),
                manualProcedureSteps = listOf(
                    "Turn ignition ON (Engine OFF).",
                    "Simultaneously press and hold both the ACCELERATOR and BRAKE pedals fully to the floor.",
                    "Hold both pedals down for 20 to 25 seconds.",
                    "The instrument cluster will display 'Oil reset in prog.' followed by 'Oil reset complete'.",
                    "Release both pedals and turn ignition OFF."
                ),
                confirmationPrompt = "Confirm Oil Change Complete: Reset Ford Engine Oil Life counter to 100%?"
            )

            makeClean.contains("CHEVROLET") || makeClean.contains("CHEVY") || makeClean.contains("GMC") || makeClean.contains("CADILLAC") || makeClean.contains("BUICK") || makeClean.contains("GM") -> ShopMaintenanceTool(
                id = "OIL_RESET_GM",
                title = "Engine Oil Life Reset (GM / Chevrolet / GMC)",
                category = ServiceFunctionCategory.OIL_MAINTENANCE,
                targetEcuHeader = "760",
                biDirectionalCommand = "2F 11 52 03 64",
                description = "Commands Instrument Panel Cluster (IPC 760) / BCM to calibrate engine oil life to 100%.",
                prerequisites = listOf("Ignition ON (Engine OFF)", "Transmission in Park"),
                manualProcedureSteps = listOf(
                    "Turn ignition ON (Engine OFF - press Start button for 5-8 seconds without touching brake pedal to enter Service Mode).",
                    "Fully depress and release the accelerator pedal 3 times within 5 seconds.",
                    "The Oil Life display on the cluster will flash and reset to 100%.",
                    "Turn ignition OFF."
                ),
                confirmationPrompt = "Confirm Oil Change Complete: Reset GM Oil Life percentage to 100%?"
            )

            makeClean.contains("HONDA") || makeClean.contains("ACURA") -> ShopMaintenanceTool(
                id = "OIL_RESET_HONDA",
                title = "Maintenance Minder Reset (Honda / Acura)",
                category = ServiceFunctionCategory.OIL_MAINTENANCE,
                targetEcuHeader = "7A0",
                biDirectionalCommand = "31 01 A0 01",
                description = "Clears Honda Maintenance Minder sub-codes (A/B, 1, 2, 3, 4) and resets engine oil to 100%.",
                prerequisites = listOf("Ignition ON (Engine OFF)", "Driver door closed"),
                manualProcedureSteps = listOf(
                    "Turn ignition ON (Engine OFF).",
                    "Press the TRIP/RESET stem or use steering wheel scroll wheel until 'Oil Life' percentage is displayed.",
                    "Press and hold the button / scroll wheel for approximately 10 seconds until the Oil Life % and maintenance codes begin blinking.",
                    "Release the button.",
                    "Press and hold the button again for 5 more seconds until the Oil Life resets to 100% and codes clear."
                ),
                confirmationPrompt = "Confirm Service Complete: Reset Honda Maintenance Minder and Oil Life to 100%?"
            )

            makeClean.contains("TOYOTA") || makeClean.contains("LEXUS") || makeClean.contains("SCION") -> ShopMaintenanceTool(
                id = "OIL_RESET_TOYOTA",
                title = "Maintenance Required Reset (Toyota / Lexus)",
                category = ServiceFunctionCategory.OIL_MAINTENANCE,
                targetEcuHeader = "7C0",
                biDirectionalCommand = "31 01 10 01",
                description = "Zeros the 5,000-mile Maintenance Required counter and turns off the dash indicator light.",
                prerequisites = listOf("Ignition ON (Engine OFF)"),
                manualProcedureSteps = listOf(
                    "Turn ignition ON and set the odometer display to 'TRIP A' (or 'ODO' on older models).",
                    "Turn ignition OFF.",
                    "Press and hold the ODOMETER / TRIP reset stem on the cluster.",
                    "While continuing to hold the stem, turn ignition ON (Engine OFF).",
                    "Continue holding until the dashes on the display count down (- - - - -) and display '000000', followed by 'COMPLETE'.",
                    "Release the button."
                ),
                confirmationPrompt = "Confirm Service Complete: Clear Toyota Maintenance Required indicator?"
            )

            makeClean.contains("CHRYSLER") || makeClean.contains("JEEP") || makeClean.contains("DODGE") || makeClean.contains("RAM") -> ShopMaintenanceTool(
                id = "OIL_RESET_CHRYSLER",
                title = "Oil Change Indicator Reset (Chrysler / Jeep / Ram)",
                category = ServiceFunctionCategory.OIL_MAINTENANCE,
                targetEcuHeader = "760",
                biDirectionalCommand = "31 01 03 01",
                description = "Resets Oil Change Required message in Instrument Cluster (IPC 760).",
                prerequisites = listOf("Ignition ON (Engine OFF)", "Transmission in Park"),
                manualProcedureSteps = listOf(
                    "Turn ignition to ON/RUN (Engine OFF - do not touch brake).",
                    "Slowly press the accelerator pedal fully to the floor and release 3 times within 10 seconds.",
                    "Turn ignition OFF, wait 5 seconds, then start engine to verify message is cleared."
                ),
                confirmationPrompt = "Confirm Oil Change Complete: Reset Chrysler/Jeep/Ram Oil Change Indicator?"
            )

            makeClean.contains("BMW") || makeClean.contains("MINI") -> ShopMaintenanceTool(
                id = "OIL_RESET_BMW",
                title = "CBS Engine Oil Service Reset (BMW / Mini)",
                category = ServiceFunctionCategory.OIL_MAINTENANCE,
                targetEcuHeader = "60",
                biDirectionalCommand = "31 01 00 01",
                description = "Executes Condition Based Service (CBS) Oil Service reset routine on Instrument Cluster (60).",
                prerequisites = listOf("Ignition ON (Engine OFF)", "Doors closed", "No warning chimes active"),
                manualProcedureSteps = listOf(
                    "Turn ignition ON (Engine OFF).",
                    "Press and hold the odometer reset button for approximately 10 seconds until the CBS service menu appears.",
                    "Toggle the button to highlight 'Engine Oil'.",
                    "Press and hold the button for 3 seconds until 'Reset?' appears.",
                    "Release and hold again for 3 seconds until the progress bar completes and displays a green checkmark."
                ),
                confirmationPrompt = "Confirm CBS Oil Service Reset: Calibrate BMW Oil Interval to new period?"
            )

            makeClean.contains("SUBARU") -> ShopMaintenanceTool(
                id = "OIL_RESET_SUBARU",
                title = "Maintenance Reminder Reset (Subaru)",
                category = ServiceFunctionCategory.OIL_MAINTENANCE,
                targetEcuHeader = "7C0",
                biDirectionalCommand = "31 01 12 01",
                description = "Calibrates multi-function display engine oil maintenance schedule.",
                prerequisites = listOf("Ignition ON (Engine OFF)", "Vehicle stopped"),
                manualProcedureSteps = listOf(
                    "Turn ignition ON.",
                    "On steering wheel or lower dash, pull and hold the 'i / SET' control switch to enter settings.",
                    "Scroll to 'Maintenance' and select with the SET switch.",
                    "Select 'Engine Oil', update the interval or select 'Reset', and confirm 'Set'."
                ),
                confirmationPrompt = "Confirm Oil Service Complete: Reset Subaru Engine Oil Maintenance schedule?"
            )

            else -> ShopMaintenanceTool(
                id = "OIL_RESET_GENERIC",
                title = "Engine Oil Life / Maintenance Reset",
                category = ServiceFunctionCategory.OIL_MAINTENANCE,
                targetEcuHeader = "7E0",
                biDirectionalCommand = "31 01 02 01",
                description = "Universal UDS Routine 0x31 Oil Degradation counter reset.",
                prerequisites = listOf("Ignition ON (Engine OFF)", "Transmission in Park"),
                manualProcedureSteps = listOf(
                    "Turn ignition ON (Engine OFF).",
                    "Fully depress accelerator pedal 3 times within 5 seconds, or press both brake and gas pedals for 20 seconds.",
                    "Turn ignition OFF."
                ),
                confirmationPrompt = "Confirm Oil Change Complete: Execute Oil Life Reset routine?"
            )
        }

        val epbEnterTool = ShopMaintenanceTool(
            id = "EPB_ENTER_SERVICE",
            title = "Enter EPB Brake Pad Replacement Mode",
            category = ServiceFunctionCategory.BRAKES_EPB,
            targetEcuHeader = "7D0",
            biDirectionalCommand = "31 01 05 01",
            description = "Winds back rear caliper electric parking brake servo motors into service retraction mode so pistons can be pushed straight back without damaging internal gear actuators.",
            prerequisites = listOf("Ignition ON (Engine OFF)", "Transmission in Park", "Parking brake DISENGAGED (released)", "Foot OFF the brake pedal"),
            manualProcedureSteps = listOf(
                "Ensure parking brake is released.",
                "Turn ignition ON (Engine OFF).",
                "Press and hold the Electronic Parking Brake (EPB) switch DOWN (release position).",
                "While holding EPB switch DOWN, press accelerator pedal fully to the floor.",
                "Cycle ignition OFF then immediately back ON within 5 seconds while continuing to hold EPB switch and gas pedal.",
                "Listen for caliper motors whirring as they wind back fully into service mode (takes ~5 seconds)."
            ),
            confirmationPrompt = "WARNING: Keep fingers clear of rear calipers! Wind back rear electronic parking brake motors into Pad Service Mode?"
        )

        val epbExitTool = ShopMaintenanceTool(
            id = "EPB_EXIT_SERVICE",
            title = "Exit EPB Mode & Calibrate Calipers",
            category = ServiceFunctionCategory.BRAKES_EPB,
            targetEcuHeader = "7D0",
            biDirectionalCommand = "31 01 05 02",
            description = "Ratchets rear caliper servo motors forward until pad contact resistance is measured, re-calibrating clamp clearance after installing new brake pads.",
            prerequisites = listOf("New brake pads installed and calipers bolted tight", "Brake pedal pumped 3-4 times to seat pads against rotors", "Ignition ON (Engine OFF)"),
            manualProcedureSteps = listOf(
                "Pump brake pedal firmly 3 times until pedal is hard.",
                "Pull and hold the Electronic Parking Brake switch UP (engage position).",
                "Press accelerator pedal to floor and cycle ignition OFF then ON.",
                "Motors will whir forward, clamp the rotor, and store zero point."
            ),
            confirmationPrompt = "Have new pads been installed and brake pedal pumped? Advance parking brake motors and calibrate clamp clearance?"
        )

        val bmsTool = when {
            makeClean.contains("FORD") || makeClean.contains("LINCOLN") -> ShopMaintenanceTool(
                id = "BMS_RESET_FORD",
                title = "12V Battery Monitoring System (BMS) Reset (Ford)",
                category = ServiceFunctionCategory.BATTERY_BMS,
                targetEcuHeader = "726",
                biDirectionalCommand = "31 01 02 02",
                description = "Resets Battery Age days counter to 0 in BCM (726) so alternator charging profile matches a brand new battery.",
                prerequisites = listOf("New 12V battery installed", "Ignition ON (Engine OFF)"),
                manualProcedureSteps = listOf(
                    "Turn ignition ON (Engine OFF).",
                    "Flash high-beam headlights 5 times.",
                    "Press and release brake pedal 3 times.",
                    "Within 5-10 seconds, the battery warning light on the cluster will flash 3 times confirming BMS reset is complete."
                ),
                confirmationPrompt = "Confirm New 12V Battery Installed: Reset Ford BMS Battery Age counter to 0 days?"
            )

            makeClean.contains("CHEVROLET") || makeClean.contains("GMC") || makeClean.contains("GM") -> ShopMaintenanceTool(
                id = "BMS_RESET_GM",
                title = "Battery Current Sensor Zeroing & Reset (GM)",
                category = ServiceFunctionCategory.BATTERY_BMS,
                targetEcuHeader = "720",
                biDirectionalCommand = "31 01 14 01",
                description = "Zeroes the Hall-effect Intelligent Battery Sensor on the negative terminal cable.",
                prerequisites = listOf("New battery installed", "Ignition ON (Engine OFF)"),
                manualProcedureSteps = listOf(
                    "Turn ignition ON (Engine OFF).",
                    "Ensure all electrical accessories (lights, radio, HVAC blower) are turned OFF.",
                    "Allow vehicle to sit locked for 4 hours undisturbed to self-calibrate sleep quiescent current."
                ),
                confirmationPrompt = "Confirm New 12V Battery: Zero GM Battery Current Sensor?"
            )

            makeClean.contains("BMW") || makeClean.contains("MINI") -> ShopMaintenanceTool(
                id = "BMS_RESET_BMW",
                title = "12V Battery Registration (BMW / Mini)",
                category = ServiceFunctionCategory.BATTERY_BMS,
                targetEcuHeader = "60",
                biDirectionalCommand = "31 01 01 00",
                description = "Registers new battery capacity/chemistry in DME/DDE and resets aging histogram (FRM-Safe sleep lock protected).",
                prerequisites = listOf("New battery installed", "Ignition ON (Engine OFF)"),
                manualProcedureSteps = listOf(
                    "Ensure vehicle has completed 16-minute deep sleep countdown (preventing FRM microcontroller corruptions).",
                    "Execute battery registration command via OBD-Hero."
                ),
                confirmationPrompt = "Confirm New Battery Installed: Register battery swap in BMW DME/DDE?"
            )

            else -> ShopMaintenanceTool(
                id = "BMS_RESET_GENERIC",
                title = "12V Intelligent Battery Sensor (IBS) Reset",
                category = ServiceFunctionCategory.BATTERY_BMS,
                targetEcuHeader = "7E0",
                biDirectionalCommand = "31 01 02 02",
                description = "Resets 12V battery aging counter and sensor baseline for optimal alternator charging voltage.",
                prerequisites = listOf("New 12V battery installed", "Ignition ON (Engine OFF)"),
                manualProcedureSteps = listOf(
                    "Turn ignition ON (Engine OFF).",
                    "Leave vehicle locked and parked undisturbed for 4 hours to calibrate open-circuit resting voltage."
                ),
                confirmationPrompt = "Confirm New Battery: Reset Battery Sensor baseline?"
            )
        }

        val tpmsTool = ShopMaintenanceTool(
            id = "TPMS_RELEARN_TRIGGER",
            title = "Stationary TPMS Sensor Relearn Mode",
            category = ServiceFunctionCategory.TIRES_TPMS,
            targetEcuHeader = "720",
            biDirectionalCommand = "31 01 08 01",
            description = "Triggers BCM / TPMS module into Stationary Learn Mode to register new sensor IDs or corner positions after tire rotation.",
            prerequisites = listOf("All 4 tires inflated to recommended door jamb placard PSI", "Vehicle in Park", "Ignition ON (Engine OFF)"),
            manualProcedureSteps = listOf(
                "Trigger Learn Mode (vehicle horn chirps twice and left front turn signal illuminates).",
                "Wheel 1 (Left Front): Ping sensor with TPMS trigger tool or lower pressure by 5 PSI until horn chirps once.",
                "Wheel 2 (Right Front): Proceed to front passenger wheel; repeat until horn chirps.",
                "Wheel 3 (Right Rear): Proceed to rear passenger wheel; repeat until horn chirps.",
                "Wheel 4 (Left Rear): Proceed to rear driver wheel; repeat until double horn chirp confirms complete relearn."
            ),
            confirmationPrompt = "Are all 4 tires set to cold door placard pressure? Trigger BCM Stationary TPMS Relearn Mode?"
        )

        return listOf(oilTool, epbEnterTool, epbExitTool, bmsTool, tpmsTool)
    }
}
