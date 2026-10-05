package com.obdhero.app

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.obdhero.app.domain.DtcInfo
import com.obdhero.app.domain.VehicleProfileDefinition
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileReader

class JsonDatabasesTest {

    private val gson = Gson()

    @Test
    fun testGenericDtcJsonLoadsAndValidates() {
        val file = File("src/main/assets/dtc/dtc_generic.json")
        assertTrue("Generic DTC file must exist", file.exists())

        val type = object : TypeToken<List<DtcInfo>>() {}.type
        val dtcs: List<DtcInfo> = gson.fromJson(FileReader(file), type)
        assertTrue("Generic DTC list must have entries", dtcs.isNotEmpty())

        val p0300 = dtcs.find { it.code == "P0300" }
        assertNotNull("P0300 must be present", p0300)
        assertEquals("Random / Multiple Cylinder Misfire Detected", p0300?.title)
    }

    @Test
    fun testMazdaDtcJsonLoadsAndValidates() {
        val file = File("src/main/assets/dtc/dtc_mazda.json")
        assertTrue("Mazda DTC file must exist", file.exists())

        val type = object : TypeToken<List<DtcInfo>>() {}.type
        val dtcs: List<DtcInfo> = gson.fromJson(FileReader(file), type)
        assertTrue(dtcs.isNotEmpty())

        val p1170 = dtcs.find { it.code == "P1170" }
        assertNotNull("P1170 must be present in Mazda pack", p1170)
        assertEquals("Front O2 Sensor Inversion / Slow Response", p1170?.title)
    }

    @Test
    fun testMazdaProfileJsonLoadsAndValidates() {
        val file = File("src/main/assets/profiles/mazda_skyactiv_nd.json")
        assertTrue("Mazda profile file must exist", file.exists())

        val profile = gson.fromJson(FileReader(file), VehicleProfileDefinition::class.java)
        assertEquals("MAZDA_SKYACTIV_ND2", profile.profileId)
        assertEquals("Mazda", profile.make)
        assertEquals("7E0", profile.ecmHeader)
        assertEquals("720", profile.bcmHeader)
        assertTrue(profile.metrics.containsKey("RPM"))
        assertTrue(profile.metrics.containsKey("TPMS"))
        assertTrue(profile.metrics.containsKey("OIL_TEMP"))
    }

    @Test
    fun testGenericProfileJsonLoadsAndValidates() {
        val file = File("src/main/assets/profiles/generic_obd2.json")
        assertTrue("Generic profile file must exist", file.exists())

        val profile = gson.fromJson(FileReader(file), VehicleProfileDefinition::class.java)
        assertEquals("GENERIC_SAE_J1979", profile.profileId)
        assertEquals("7DF", profile.ecmHeader)
        assertNull("Generic profile shouldn't have BCM header", profile.bcmHeader)
        assertTrue(profile.metrics.containsKey("RPM"))
        assertTrue(profile.metrics.containsKey("SPEED"))
    }
}
