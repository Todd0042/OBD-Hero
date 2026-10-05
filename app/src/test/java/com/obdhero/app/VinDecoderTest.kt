package com.obdhero.app

import com.obdhero.app.domain.VinDecoder
import org.junit.Assert.*
import org.junit.Test

class VinDecoderTest {

    @Test
    fun testBmwVinDecodesAndFlagsFrmVulnerability() {
        // 2011 BMW 3-Series (E90 era)
        val profile = VinDecoder.decode("WBA3A5C58BA123456")
        assertEquals("BMW", profile.make)
        assertEquals(2011, profile.year)
        assertTrue("2011 BMW E-series should be flagged as FRM vulnerable", profile.isBmwFrmVulnerable)
        assertFalse("2011 BMW is not SGW locked", profile.isSgwLocked)
    }

    @Test
    fun testModernStellantisVinFlagsSecurityGateway() {
        // 2021 Jeep Wrangler (1C4...)
        val profile = VinDecoder.decode("1C4HJXDG8MW123456")
        assertEquals("Chrysler/Jeep", profile.make)
        assertEquals(2021, profile.year)
        assertTrue("2021 Jeep should be flagged as SGW locked", profile.isSgwLocked)
        assertFalse("Jeep does not have BMW FRM", profile.isBmwFrmVulnerable)
    }

    @Test
    fun testFordF150VinDecodesProperly() {
        // 2018 Ford F-150
        val profile = VinDecoder.decode("1FTFW1ED8JK123456")
        assertEquals("Ford", profile.make)
        assertEquals(2018, profile.year)
        assertEquals("F-150", profile.model)
        assertFalse("Ford does not have BMW FRM", profile.isBmwFrmVulnerable)
    }

    @Test
    fun testMazdaMx5VinDecodesProperly() {
        // 2022 Mazda MX-5 Miata ND2 (JM1ND...)
        val profile = VinDecoder.decode("JM1NDAM76N0123456")
        assertEquals("Mazda", profile.make)
        assertEquals(2022, profile.year)
        assertEquals("MX-5 Miata", profile.model)
        assertFalse("Mazda does not have BMW FRM", profile.isBmwFrmVulnerable)
        assertFalse("Mazda is not SGW locked", profile.isSgwLocked)
    }

    @Test
    fun testMazda3Gen7VinDecodesProperly() {
        // 2021 Mazda 3 BP (JM1BP... or 3MZBP...)
        val profileJapan = VinDecoder.decode("JM1BPABM8M1123456")
        assertEquals("Mazda", profileJapan.make)
        assertEquals(2021, profileJapan.year)
        assertEquals("Mazda 3", profileJapan.model)

        val profileMexico = VinDecoder.decode("3MZBPABM5MM123456")
        assertEquals("Mazda", profileMexico.make)
        assertEquals(2021, profileMexico.year)
        assertEquals("Mazda 3", profileMexico.model)
    }

    @Test
    fun testParseVinMultiFrameResponse() {
        val manager = com.obdhero.app.obd.ObdConnectionManager()
        // ELM327 Mode 09 PID 02 response with multi-frame ISO-TP formatting
        val sampleElmResponse = """
            0: 49 02 01 4A 4D 31
            1: 4E 44 41 4D 37 36 4E
            2: 30 31 32 33 34 35 36
        """.trimIndent()
        val decodedVin = manager.parseVin(sampleElmResponse)
        assertEquals("JM1NDAM76N0123456", decodedVin)
    }
}
