package com.obdhero.app

import com.obdhero.app.domain.EcuModule
import com.obdhero.app.domain.ModuleDtcReport
import com.obdhero.app.obd.ObdConnectionManager
import org.junit.Assert.*
import org.junit.Test

class DiagnosticsAndFreezeFrameTest {

    private val obdManager = ObdConnectionManager()

    @Test
    fun testDecodeDtcBytesForStandardCategories() {
        // Powertrain P-codes
        assertEquals("P0171", obdManager.decodeDtcBytes(0x01, 0x71))
        assertEquals("P0300", obdManager.decodeDtcBytes(0x03, 0x00))
        assertEquals("P1450", obdManager.decodeDtcBytes(0x14, 0x50))

        // Chassis C-codes (first 2 bits: 01 -> 0x40)
        assertEquals("C0133", obdManager.decodeDtcBytes(0x41, 0x33))
        assertEquals("C0035", obdManager.decodeDtcBytes(0x40, 0x35))

        // Body B-codes (first 2 bits: 10 -> 0x80)
        assertEquals("B1200", obdManager.decodeDtcBytes(0x92, 0x00))
        assertEquals("B0120", obdManager.decodeDtcBytes(0x81, 0x20))

        // Network U-codes (first 2 bits: 11 -> 0xC0)
        assertEquals("U0140", obdManager.decodeDtcBytes(0xC1, 0x40))
        assertEquals("U0155", obdManager.decodeDtcBytes(0xC1, 0x55))

        // Empty / Null
        assertNull(obdManager.decodeDtcBytes(0x00, 0x00))
    }

    @Test
    fun testParseMode03DtcResponses() {
        val standardResp = "43 01 71 03 00 00 00"
        val dtcs = obdManager.parseDtcResponse(standardResp)
        assertEquals(2, dtcs.size)
        assertTrue(dtcs.contains("P0171"))
        assertTrue(dtcs.contains("P0300"))

        val multilineResp = "0: 43 03 01 71 03 00 \r 1: 04 20 00 00 00 00"
        val multilineDtcs = obdManager.parseDtcResponse(multilineResp)
        assertTrue(multilineDtcs.contains("P0171"))
        assertTrue(multilineDtcs.contains("P0300"))
    }

    @Test
    fun testParseMode02FreezeFrameParameters() {
        // DTC that triggered freeze frame: 0x01, 0x71 -> P0171
        val dtcResp = "42 02 01 71"
        assertEquals("P0171", obdManager.parseFreezeFrameDtc(dtcResp))

        // RPM: (0x21 * 256 + 0x98) / 4 = (33 * 256 + 152) / 4 = 8600 / 4 = 2150 RPM
        val rpmResp = "42 0C 21 98"
        assertEquals(Integer.valueOf(2150), obdManager.parseFreezeFrameRpm(rpmResp))

        // Speed: 0x4E = 78 km/h -> 48 mph
        val speedResp = "42 0D 4E"
        assertEquals(Integer.valueOf(48), obdManager.parseFreezeFrameSpeed(speedResp))

        // Coolant: 0x82 = 130 -> 130 - 40 = 90C -> 194F
        val coolantResp = "42 05 82"
        assertEquals(Integer.valueOf(194), obdManager.parseFreezeFrameCoolant(coolantResp))

        // Load: 0x6B = 107 -> (107 * 100) / 255 = 41.96% -> 41%
        val loadResp = "42 04 6B"
        assertEquals(Integer.valueOf(41), obdManager.parseFreezeFrameLoad(loadResp))

        // Trims
        val trimResp = "42 06 A0" // 0xA0 = 160 -> (160 - 128)*100/128 = 25.0%
        val trim = obdManager.parseFreezeFrameTrim(trimResp, "06")
        assertNotNull(trim)
        assertEquals(25.0f, trim!!, 0.1f)
    }

    @Test
    fun testModuleDtcReportHealthCalculation() {
        val healthyTcm = ModuleDtcReport(
            module = EcuModule.TCM,
            header = "7E1",
            storedCodes = emptyList(),
            pendingCodes = emptyList(),
            isResponding = true
        )
        assertTrue(healthyTcm.isHealthy)
        assertEquals(0, healthyTcm.totalFaults)

        val faultEcm = ModuleDtcReport(
            module = EcuModule.ECM,
            header = "7E0",
            storedCodes = listOf("P0171", "P0300"),
            pendingCodes = listOf("P0420"),
            isResponding = true
        )
        assertFalse(faultEcm.isHealthy)
        assertEquals(3, faultEcm.totalFaults)
    }
}
