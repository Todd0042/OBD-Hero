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
}
