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

    @Test
    fun testMazdaGen7ProfileJsonLoadsAndValidates() {
        val file = File("src/main/assets/profiles/mazda_gen7_bp.json")
        assertTrue("Mazda Gen 7 profile file must exist", file.exists())

        val profile = gson.fromJson(FileReader(file), VehicleProfileDefinition::class.java)
        assertEquals("MAZDA_GEN7_BP", profile.profileId)
        assertEquals("Mazda", profile.make)
        assertEquals("Mazda 3 (Gen 7)", profile.model)
        assertEquals("7E0", profile.ecmHeader)
        assertTrue(profile.generations.contains("BP"))
        assertTrue(profile.metrics.containsKey("RPM"))
        assertTrue(profile.metrics.containsKey("OIL_TEMP"))
        assertTrue(profile.metrics.containsKey("TORQUE"))
        assertTrue(profile.metrics.containsKey("TRANSMISSION_GEAR"))
    }

    @Test
    fun testChryslerProfileJsonLoadsAndValidates() {
        val file = File("src/main/assets/profiles/chrysler_fca_can.json")
        assertTrue("Chrysler profile file must exist", file.exists())

        val profile = gson.fromJson(FileReader(file), VehicleProfileDefinition::class.java)
        assertEquals("CHRYSLER_FCA_CAN", profile.profileId)
        assertEquals("Chrysler", profile.make)
        assertEquals("7E0", profile.ecmHeader)
        assertEquals("7E1", profile.tcmHeader)
        assertTrue(profile.vinWmis.contains("1C4"))
        assertTrue(profile.metrics.containsKey("OIL_TEMP"))
        assertTrue(profile.metrics.containsKey("TRANSMISSION_TEMP"))
        assertTrue(profile.metrics.containsKey("TRANSMISSION_GEAR"))
    }

    @Test
    fun testChryslerDtcJsonLoadsAndValidates() {
        val file = File("src/main/assets/dtc/dtc_chrysler.json")
        assertTrue("Chrysler DTC file must exist", file.exists())

        val type = object : TypeToken<List<DtcInfo>>() {}.type
        val dtcs: List<DtcInfo> = gson.fromJson(FileReader(file), type)
        assertTrue(dtcs.isNotEmpty())

        val p0520 = dtcs.find { it.code == "P0520" }
        assertNotNull("P0520 oil pressure switch code must be present", p0520)
        assertTrue(p0520!!.meaning.contains("Pentastar"))

        val p0456 = dtcs.find { it.code == "P0456" }
        assertNotNull("P0456 EVAP ESIM code must be present", p0456)
        assertTrue(p0456!!.meaning.contains("ESIM"))
    }

    @Test
    fun testMasterDtcDefinitionsDictionaryLoadsAndValidates() {
        val file = File("src/main/assets/dtc/dtc_definitions_master.json")
        assertTrue("Master definitions file must exist", file.exists())

        val type = object : TypeToken<Map<String, String>>() {}.type
        val masterDict: Map<String, String> = gson.fromJson(FileReader(file), type)
        assertTrue("Master dictionary must have over 4000 entries", masterDict.size >= 4000)

        // Test known standardized codes from various sub-systems
        assertTrue("P0135 must be defined in master", masterDict.containsKey("P0135"))
        assertTrue("P0505 must be defined in master", masterDict.containsKey("P0505"))
        assertTrue("B1200 must be defined in master", masterDict.containsKey("B1200"))
        assertTrue("C0035 must be defined in master", masterDict.containsKey("C0035"))
        assertTrue("U0155 must be defined in master", masterDict.containsKey("U0155"))
    }
}
