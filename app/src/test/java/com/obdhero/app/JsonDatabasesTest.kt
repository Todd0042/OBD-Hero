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
        assertTrue("P0300 title must contain Cylinder Misfire Detected", p0300!!.title.contains("Cylinder Misfire Detected"))
        assertTrue("Must have inspection steps", p0300.inspectionSteps.isNotEmpty())
        assertTrue("Must have verified fixes", p0300.verifiedFixes.isNotEmpty())
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

    @Test
    fun testFordProfileJsonLoadsAndValidates() {
        val file = File("src/main/assets/profiles/ford_can_ecoboost.json")
        assertTrue("Ford profile file must exist", file.exists())

        val profile = gson.fromJson(FileReader(file), VehicleProfileDefinition::class.java)
        assertEquals("FORD_CAN_ECOBOOST", profile.profileId)
        assertEquals("Ford", profile.make)
        assertEquals("7E0", profile.ecmHeader)
        assertEquals("7E1", profile.tcmHeader)
        assertEquals("726", profile.bcmHeader)
        assertTrue(profile.vinWmis.contains("1FT"))
        assertTrue(profile.metrics.containsKey("OIL_TEMP"))
        assertTrue(profile.metrics.containsKey("TRANSMISSION_TEMP"))
    }

    @Test
    fun testFordDtcJsonLoadsAndValidates() {
        val file = File("src/main/assets/dtc/dtc_ford.json")
        assertTrue("Ford DTC file must exist", file.exists())

        val type = object : TypeToken<List<DtcInfo>>() {}.type
        val dtcs: List<DtcInfo> = gson.fromJson(FileReader(file), type)
        assertTrue("Ford DTC database must have 400+ codes", dtcs.size >= 400)

        val p1450 = dtcs.find { it.code == "P1450" }
        assertNotNull("P1450 EVAP canister purge valve must be present", p1450)
        assertTrue(p1450!!.title.contains("Evaporative") || p1450.title.contains("Fuel Tank"))

        val p1247 = dtcs.find { it.code == "P1247" }
        assertNotNull("P1247 Ford Turbo Boost Pressure Low must be present", p1247)
        assertTrue(p1247!!.title.contains("Turbo Boost"))
    }

    @Test
    fun testGmProfileJsonLoadsAndValidates() {
        val file = File("src/main/assets/profiles/gm_global_a.json")
        assertTrue("GM profile file must exist", file.exists())

        val profile = gson.fromJson(FileReader(file), VehicleProfileDefinition::class.java)
        assertEquals("GM_GLOBAL_A", profile.profileId)
        assertEquals("Chevrolet", profile.make)
        assertEquals("7E0", profile.ecmHeader)
        assertEquals("7E2", profile.tcmHeader)
        assertTrue(profile.vinWmis.contains("1GC"))
        assertTrue(profile.metrics.containsKey("OIL_PRESSURE"))
        assertTrue(profile.metrics.containsKey("TRANSMISSION_TEMP"))
    }

    @Test
    fun testGmDtcJsonLoadsAndValidates() {
        val file = File("src/main/assets/dtc/dtc_gm.json")
        assertTrue("GM DTC file must exist", file.exists())

        val type = object : TypeToken<List<DtcInfo>>() {}.type
        val dtcs: List<DtcInfo> = gson.fromJson(FileReader(file), type)
        assertTrue("GM DTC database must have 400+ codes", dtcs.size >= 400)

        val p1189 = dtcs.find { it.code == "P1189" }
        assertNotNull("P1189 LS/LT engine oil pressure switch must be present", p1189)
        assertTrue(p1189!!.title.contains("Engine Oil Pressure") || p1189.meaning.contains("Oil Pressure"))

        val p1185 = dtcs.find { it.code == "P1185" }
        assertNotNull("P1185 oil temp sensor must be present", p1185)
    }
}
