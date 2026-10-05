package com.obdhero.app.data

import com.obdhero.app.domain.AdapterHardwareTier
import com.obdhero.app.domain.AdapterProfile

object AdapterDatabase {

    const val BOOLEAN_ASCII_JOKE = "01011001 01101111 01110101 00100000 01110010 01100101 01100001 01101100 01101100 01111001 00100000 01110100 01101000 01101111 01110101 01100111 01101000 01110100 00100000 01110100 01101000 01100001 01110100 00100000 01100100 01100101 01110110 01101001 01100011 01100101 00100000 01100011 01101111 01110101 01101100 01100100 00100000 01100100 01101111 00100000 01110100 01101000 01101001 01110011 00100000 01100110 01101111 01110010 00100000 00100100 00110001 00110101 00111111"

    const val DECODED_JOKE = "You really thought that device could do this for $15?"

    val PROFILES: List<AdapterProfile> = listOf(
        // 1. Optimal Tier: Vgate vLinker Series (Workshop Standard)
        AdapterProfile(
            name = "Vgate vLinker MS",
            tier = AdapterHardwareTier.OPTIMAL,
            chipArchitecture = "32-bit ARM Cortex-M3 (MIC3312/3322)",
            bufferBytes = 8192,
            supportedProtocols = listOf("ISO 15765-4 (CAN)", "Ford MS-CAN (Pins 3/11)", "SAE J1850 PWM", "SAE J1850 VPW", "ISO 9141-2", "ISO 14230-4"),
            multiBusCapable = true,
            maxBaudRateBps = 2000000
        ),
        AdapterProfile(
            name = "Vgate vLinker MC / MC+",
            tier = AdapterHardwareTier.OPTIMAL,
            chipArchitecture = "32-bit ARM Cortex-M3",
            bufferBytes = 4096,
            supportedProtocols = listOf("ISO 15765-4 (CAN)", "Ford MS-CAN", "GM SW-CAN", "SAE J1850", "ISO 9141-2"),
            multiBusCapable = true,
            maxBaudRateBps = 2000000
        ),
        AdapterProfile(
            name = "Vgate vLinker FD / FD+ / FS",
            tier = AdapterHardwareTier.OPTIMAL,
            chipArchitecture = "32-bit ARM Cortex-M4",
            bufferBytes = 8192,
            supportedProtocols = listOf("ISO 15765-4 (CAN)", "CAN-FD", "Ford MS-CAN", "SAE J1850"),
            multiBusCapable = true,
            maxBaudRateBps = 2000000
        ),
        AdapterProfile(
            name = "OBDLink LX / SX",
            tier = AdapterHardwareTier.OPTIMAL,
            chipArchitecture = "32-bit STN1110 / STN1170",
            bufferBytes = 2048,
            supportedProtocols = listOf("ISO 15765-4 (CAN)", "SAE J1850 PWM", "SAE J1850 VPW", "ISO 9141-2"),
            multiBusCapable = false,
            maxBaudRateBps = 2000000
        ),

        // 2. BLE Filtered Tier: CX-Class (Low-Power BLE Streaming)
        AdapterProfile(
            name = "OBDLink CX",
            tier = AdapterHardwareTier.BLE_FILTERED,
            chipArchitecture = "32-bit OBDLink Proprietary Core (BLE 5.1)",
            bufferBytes = 2048,
            supportedProtocols = listOf("ISO 15765-4 (CAN)", "ISO 14230-4", "ISO 9141-2"),
            multiBusCapable = false,
            maxBaudRateBps = 230400
        ),
        AdapterProfile(
            name = "Vgate iCar Pro BLE",
            tier = AdapterHardwareTier.BLE_FILTERED,
            chipArchitecture = "ARM Cortex BLE 4.0",
            bufferBytes = 1024,
            supportedProtocols = listOf("ISO 15765-4 (CAN)", "ISO 14230-4", "ISO 9141-2"),
            multiBusCapable = false,
            maxBaudRateBps = 115200
        ),
        AdapterProfile(
            name = "Carista BLE",
            tier = AdapterHardwareTier.BLE_FILTERED,
            chipArchitecture = "Nordic Semiconductor BLE Core",
            bufferBytes = 1024,
            supportedProtocols = listOf("ISO 15765-4 (CAN)", "K-Line"),
            multiBusCapable = false,
            maxBaudRateBps = 115200
        ),

        // 3. Exceptional Tier: Pro / Enterprise Hardware
        AdapterProfile(
            name = "OBDLink MX+",
            tier = AdapterHardwareTier.EXCEPTIONAL,
            chipArchitecture = "32-bit STN2120 Core (Dual-Mode MFi/SPP)",
            bufferBytes = 10240,
            supportedProtocols = listOf("ISO 15765-4 (CAN)", "Ford MS-CAN", "GM Single-Wire SW-CAN", "SAE J1850 PWM/VPW", "ISO 9141-2", "ISO 14230-4"),
            multiBusCapable = true,
            maxBaudRateBps = 2000000
        ),
        AdapterProfile(
            name = "Tactrix OpenPort 2.0 / J2534",
            tier = AdapterHardwareTier.EXCEPTIONAL,
            chipArchitecture = "72MHz 32-bit ARM Cortex-M3 Hardware PassThru",
            bufferBytes = 16384,
            supportedProtocols = listOf("ISO 15765-4 (CAN)", "Subaru SSM over CAN", "Mitsubishi MUT-III", "ISO 9141", "ISO 14230"),
            multiBusCapable = true,
            maxBaudRateBps = 2000000
        ),
        AdapterProfile(
            name = "Macchina A0 / CANable Pro",
            tier = AdapterHardwareTier.EXCEPTIONAL,
            chipArchitecture = "STM32F072 Galvanically Isolated USB-CAN",
            bufferBytes = 16384,
            supportedProtocols = listOf("Raw Promiscuous CAN 2.0A/2.0B (10k-1000k bps)", "ISO 15765-4"),
            multiBusCapable = true,
            maxBaudRateBps = 2000000
        ),

        // 4. Budget Clone Tier: Cheap $15 Devices
        AdapterProfile(
            name = "Generic Mini ELM327 Clone",
            tier = AdapterHardwareTier.CHEAP_GARBAGE,
            chipArchitecture = "8-bit Fake PIC18F25K80 / Clone ASIC",
            bufferBytes = 256,
            supportedProtocols = listOf("Simulated ISO 15765-4"),
            multiBusCapable = false,
            maxBaudRateBps = 38400,
            snarkyEasterEgg = BOOLEAN_ASCII_JOKE
        ),
        AdapterProfile(
            name = "Veepeak OBDCheck Basic",
            tier = AdapterHardwareTier.CHEAP_GARBAGE,
            chipArchitecture = "Standard Clone Microcontroller",
            bufferBytes = 256,
            supportedProtocols = listOf("Standard OBD-II"),
            multiBusCapable = false,
            maxBaudRateBps = 38400,
            snarkyEasterEgg = BOOLEAN_ASCII_JOKE
        )
    )

    /**
     * Identify adapter hardware and assign tier based on Bluetooth name and AT/ST probe queries.
     */
    fun identifyAdapter(
        deviceName: String,
        atiResp: String = "",
        atDeviceResp: String = "",
        stiResp: String = ""
    ): AdapterProfile {
        val nameClean = deviceName.trim().uppercase()
        val atiClean = atiResp.trim().uppercase()
        val stiClean = stiResp.trim().uppercase()

        // 1. Check for vLinker MS (Priority Workshop Standard)
        if (nameClean.contains("VLINKER MS") || atiClean.contains("VLINKER MS")) {
            return PROFILES.first { it.name == "Vgate vLinker MS" }
        }

        // 2. Check for other vLinker models (MC, FD, FS)
        if (nameClean.contains("VLINKER MC") || atiClean.contains("VLINKER MC")) {
            return PROFILES.first { it.name.startsWith("Vgate vLinker MC") }
        }
        if (nameClean.contains("VLINKER FD") || nameClean.contains("VLINKER FS") || atiClean.contains("VLINKER FD") || atiClean.contains("VLINKER FS")) {
            return PROFILES.first { it.name.startsWith("Vgate vLinker FD") }
        }

        // 3. Check for OBDLink MX+ (Enterprise)
        if (nameClean.contains("OBDLINK MX") || atiClean.contains("OBDLINK MX") || stiClean.contains("STN2120")) {
            return PROFILES.first { it.name == "OBDLink MX+" }
        }

        // 4. Check for Tactrix / J2534 / Macchina
        if (nameClean.contains("TACTRIX") || nameClean.contains("OPENPORT") || nameClean.contains("CANABLE") || nameClean.contains("MACCHINA")) {
            return PROFILES.first { it.tier == AdapterHardwareTier.EXCEPTIONAL }
        }

        // 5. Check for OBDLink CX (BLE Pro)
        if (nameClean.contains("OBDLINK CX") || atiClean.contains("OBDLINK CX")) {
            return PROFILES.first { it.name == "OBDLink CX" }
        }

        // 6. Check for OBDLink LX / SX
        if (nameClean.contains("OBDLINK LX") || nameClean.contains("OBDLINK SX") || stiClean.contains("STN1110") || stiClean.contains("STN1170")) {
            return PROFILES.first { it.name == "OBDLink LX / SX" }
        }

        // 7. Check for iCar Pro or Carista
        if (nameClean.contains("ICAR") || atiClean.contains("ICAR")) {
            return PROFILES.first { it.name == "Vgate iCar Pro BLE" }
        }
        if (nameClean.contains("CARISTA")) {
            return PROFILES.first { it.name == "Carista BLE" }
        }

        // 8. If device has valid STN response but unrecognized model -> Optimal tier STN
        if (stiClean.isNotEmpty() && !stiClean.contains("?") && !stiClean.contains("ERROR")) {
            return AdapterProfile(
                name = if (deviceName.isNotBlank()) deviceName else "Custom STN-Powered Adapter",
                tier = AdapterHardwareTier.OPTIMAL,
                chipArchitecture = "32-bit STN Architecture ($stiClean)",
                bufferBytes = 4096,
                supportedProtocols = listOf("ISO 15765-4 (CAN)", "Multi-Protocol"),
                multiBusCapable = true,
                maxBaudRateBps = 2000000
            )
        }

        // 9. Generic / Budget Clone Fallback:
        // Names like "OBDII", "ELM327", "V-GATE" (fake clone), "VIEPEAK", "SCANTOOL" without STN
        return PROFILES.first { it.tier == AdapterHardwareTier.CHEAP_GARBAGE }.copy(
            name = if (deviceName.isNotBlank()) deviceName else "Budget Clone Adapter"
        )
    }
}
