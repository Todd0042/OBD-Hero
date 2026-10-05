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

    @Test
    fun testHyundaiKiaProfileJsonLoadsAndValidates() {
        val file = File("src/main/assets/profiles/hyundai_kia_can.json")
        assertTrue("Hyundai/Kia profile file must exist", file.exists())

        val profile = gson.fromJson(FileReader(file), VehicleProfileDefinition::class.java)
        assertEquals("HYUNDAI_KIA_CAN", profile.profileId)
        assertEquals("Hyundai", profile.make)
        assertEquals("7E0", profile.ecmHeader)
        assertEquals("7E1", profile.tcmHeader)
        assertEquals("7D1", profile.absHeader)
        assertEquals("7A0", profile.bcmHeader)
        assertTrue(profile.vinWmis.contains("KM8"))
        assertTrue(profile.vinWmis.contains("KNA"))
        assertTrue(profile.metrics.containsKey("OIL_TEMP"))
        assertTrue(profile.metrics.containsKey("TRANSMISSION_TEMP"))
        assertTrue(profile.metrics.containsKey("TRANSMISSION_GEAR"))
    }

    @Test
    fun testHyundaiKiaDtcJsonLoadsAndValidates() {
        val file = File("src/main/assets/dtc/dtc_hyundai_kia.json")
        assertTrue("Hyundai/Kia DTC file must exist", file.exists())

        val type = object : TypeToken<List<DtcInfo>>() {}.type
        val dtcs: List<DtcInfo> = gson.fromJson(FileReader(file), type)
        assertTrue("Hyundai/Kia DTC database must have 400+ codes", dtcs.size >= 400)

        val p1326 = dtcs.find { it.code == "P1326" }
        assertNotNull("P1326 KSDS limp mode code must be present", p1326)
        assertTrue(p1326!!.meaning.contains("KSDS") || p1326.title.contains("KSDS") || p1326.meaning.contains("bearing"))

        val c1200 = dtcs.find { it.code == "C1200" }
        assertNotNull("C1200 wheel speed sensor must be present", c1200)

        val c1503 = dtcs.find { it.code == "C1503" }
        assertNotNull("C1503 MDPS torque sensor must be present", c1503)

        val b1346 = dtcs.find { it.code == "B1346" }
        assertNotNull("B1346 driver airbag clockspring code must be present", b1346)
        assertTrue(b1346!!.title.contains("Airbag") || b1346.meaning.contains("Airbag"))
    }

    @Test
    fun testCommunitySourcedDtcsAcrossManufacturers() {
        val type = object : TypeToken<List<DtcInfo>>() {}.type

        // 1. Generic P0300 should NOT be community sourced (SAE Standard)
        val genericFile = File("src/main/assets/dtc/dtc_generic.json")
        val genericDtcs: List<DtcInfo> = gson.fromJson(FileReader(genericFile), type)
        val p0300 = genericDtcs.find { it.code == "P0300" }
        assertNotNull("P0300 must exist", p0300)
        assertFalse("P0300 must not be community sourced", p0300!!.isCommunitySourced)

        // 2. Ford Community Codes
        val fordFile = File("src/main/assets/dtc/dtc_ford.json")
        val fordDtcs: List<DtcInfo> = gson.fromJson(FileReader(fordFile), type)
        val p144c = fordDtcs.find { it.code == "P144C" }
        assertNotNull("Ford P144C must exist", p144c)
        assertTrue("Ford P144C must be marked community sourced", p144c!!.isCommunitySourced)
        val p2196 = fordDtcs.find { it.code == "P2196" }
        assertNotNull("Ford P2196 must exist", p2196)
        assertTrue("Ford P2196 must be marked community sourced", p2196!!.isCommunitySourced)

        // 3. GM Community Codes
        val gmFile = File("src/main/assets/dtc/dtc_gm.json")
        val gmDtcs: List<DtcInfo> = gson.fromJson(FileReader(gmFile), type)
        val p1682 = gmDtcs.find { it.code == "P1682" }
        assertNotNull("GM P1682 must exist", p1682)
        assertTrue("GM P1682 must be marked community sourced", p1682!!.isCommunitySourced)
        val p0741 = gmDtcs.find { it.code == "P0741" }
        assertNotNull("GM P0741 must exist", p0741)
        assertTrue("GM P0741 must be marked community sourced", p0741!!.isCommunitySourced)

        // 4. Chrysler Community Codes
        val chryslerFile = File("src/main/assets/dtc/dtc_chrysler.json")
        val chryslerDtcs: List<DtcInfo> = gson.fromJson(FileReader(chryslerFile), type)
        val p1288 = chryslerDtcs.find { it.code == "P1288" }
        assertNotNull("Chrysler P1288 must exist", p1288)
        assertTrue("Chrysler P1288 must be marked community sourced", p1288!!.isCommunitySourced)
        val c121a = chryslerDtcs.find { it.code == "C121A" }
        assertNotNull("Chrysler C121A must exist", c121a)
        assertTrue("Chrysler C121A must be marked community sourced", c121a!!.isCommunitySourced)

        // 5. Hyundai / Kia Community Codes
        val hkFile = File("src/main/assets/dtc/dtc_hyundai_kia.json")
        val hkDtcs: List<DtcInfo> = gson.fromJson(FileReader(hkFile), type)
        val p1690 = hkDtcs.find { it.code == "P1690" }
        assertNotNull("Hyundai/Kia P1690 must exist", p1690)
        assertTrue("Hyundai/Kia P1690 must be marked community sourced", p1690!!.isCommunitySourced)
        val c1200 = hkDtcs.find { it.code == "C1200" }
        assertNotNull("Hyundai/Kia C1200 must exist", c1200)
        assertTrue("Hyundai/Kia C1200 must be marked community sourced", c1200!!.isCommunitySourced)

        // 6. BMW Community Codes
        val bmwFile = File("src/main/assets/dtc/dtc_bmw.json")
        val bmwDtcs: List<DtcInfo> = gson.fromJson(FileReader(bmwFile), type)
        val p1030 = bmwDtcs.find { it.code == "P1030" }
        assertNotNull("BMW P1030 must exist", p1030)
        assertTrue("BMW P1030 must be marked community sourced", p1030!!.isCommunitySourced)

        // 7. Mazda Community Codes
        val mazdaFile = File("src/main/assets/dtc/dtc_mazda.json")
        val mazdaDtcs: List<DtcInfo> = gson.fromJson(FileReader(mazdaFile), type)
        val p011a = mazdaDtcs.find { it.code == "P011A" }
        assertNotNull("Mazda P011A must exist", p011a)
        assertTrue("Mazda P011A must be marked community sourced", p011a!!.isCommunitySourced)

        // 8. Toyota / Lexus Community Codes
        val toyotaFile = File("src/main/assets/dtc/dtc_toyota.json")
        val toyotaDtcs: List<DtcInfo> = gson.fromJson(FileReader(toyotaFile), type)
        val p0018 = toyotaDtcs.find { it.code == "P0018" }
        assertNotNull("Toyota P0018 must exist", p0018)
        assertTrue("Toyota P0018 must be marked community sourced", p0018!!.isCommunitySourced)
        val p1604 = toyotaDtcs.find { it.code == "P1604" }
        assertNotNull("Toyota P1604 must exist", p1604)
        assertTrue("Toyota P1604 must be marked community sourced", p1604!!.isCommunitySourced)
    }

    @Test
    fun testToyotaLexusProfileAndDtcJson() {
        val profFile = File("src/main/assets/profiles/toyota_lexus_tnga.json")
        assertTrue("Toyota TNGA profile must exist", profFile.exists())

        val profile = gson.fromJson(FileReader(profFile), VehicleProfileDefinition::class.java)
        assertEquals("TOYOTA_LEXUS_TNGA", profile.profileId)
        assertEquals("Toyota", profile.make)
        assertEquals("7E0", profile.ecmHeader)
        assertEquals("7E1", profile.tcmHeader)
        assertEquals("7B0", profile.absHeader)
        assertEquals("750", profile.bcmHeader)
        assertTrue(profile.vinWmis.contains("4T1"))
        assertTrue(profile.vinWmis.contains("JTJ"))
        assertTrue(profile.metrics.containsKey("OIL_TEMP"))
        assertTrue(profile.metrics.containsKey("TRANSMISSION_TEMP"))

        val dtcFile = File("src/main/assets/dtc/dtc_toyota.json")
        assertTrue("Toyota DTC file must exist", dtcFile.exists())

        val type = object : TypeToken<List<DtcInfo>>() {}.type
        val dtcs: List<DtcInfo> = gson.fromJson(FileReader(dtcFile), type)
        assertTrue("Toyota DTC database must have 400+ codes", dtcs.size >= 400)

        val c1201 = dtcs.find { it.code == "C1201" }
        assertNotNull("C1201 VSC disabled code must be present", c1201)
        assertTrue(c1201!!.meaning.contains("VSC") || c1201.meaning.contains("Engine"))

        val p0a80 = dtcs.find { it.code == "P0A80" }
        assertNotNull("P0A80 Hybrid battery code must be present", p0a80)
        assertTrue(p0a80!!.title.contains("Hybrid Battery") || p0a80.meaning.contains("Hybrid"))
    }

    @Test
    fun testHondaAcuraProfileAndDtcJson() {
        val profFile = File("src/main/assets/profiles/honda_acura_can.json")
        assertTrue("Honda CAN profile must exist", profFile.exists())

        val profile = gson.fromJson(FileReader(profFile), VehicleProfileDefinition::class.java)
        assertEquals("HONDA_ACURA_CAN", profile.profileId)
        assertEquals("Honda", profile.make)
        assertEquals("7E0", profile.ecmHeader)
        assertEquals("7E1", profile.tcmHeader)
        assertEquals("7D0", profile.absHeader)
        assertEquals("7A0", profile.bcmHeader)
        assertTrue(profile.vinWmis.contains("1HG"))
        assertTrue(profile.vinWmis.contains("19U"))
        assertTrue(profile.metrics.containsKey("TRANSMISSION_TEMP"))
        assertTrue(profile.metrics.containsKey("TPMS"))

        val dtcFile = File("src/main/assets/dtc/dtc_honda.json")
        assertTrue("Honda DTC file must exist", dtcFile.exists())

        val type = object : TypeToken<List<DtcInfo>>() {}.type
        val dtcs: List<DtcInfo> = gson.fromJson(FileReader(dtcFile), type)
        assertTrue("Honda DTC database must have 400+ codes", dtcs.size >= 400)

        val p1259 = dtcs.find { it.code == "P1259" }
        assertNotNull("P1259 VTEC spool valve code must be present", p1259)
        assertTrue(p1259!!.title.contains("VTEC"))
        assertFalse("P1259 must be factory verified", p1259.isCommunitySourced)

        val p1457 = dtcs.find { it.code == "P1457" }
        assertNotNull("P1457 EVAP canister code must be present", p1457)
        assertTrue(p1457!!.title.contains("EVAP"))

        val p2646 = dtcs.find { it.code == "P2646" }
        assertNotNull("P2646 VTEC rocker arm actuator code must be present", p2646)
        assertTrue(p2646!!.isCommunitySourced)

        val p0847 = dtcs.find { it.code == "P0847" }
        assertNotNull("P0847 3rd clutch pressure switch must be present", p0847)
        assertTrue(p0847!!.title.contains("Pressure"))
    }

    @Test
    fun testSubaruProfileAndDtcJson() {
        val profFile = File("src/main/assets/profiles/subaru_can.json")
        assertTrue("Subaru CAN profile must exist", profFile.exists())

        val profile = gson.fromJson(FileReader(profFile), VehicleProfileDefinition::class.java)
        assertEquals("SUBARU_GLOBAL_CAN", profile.profileId)
        assertEquals("Subaru", profile.make)
        assertEquals("7E0", profile.ecmHeader)
        assertEquals("7E1", profile.tcmHeader)
        assertEquals("7D0", profile.absHeader)
        assertEquals("750", profile.bcmHeader)
        assertTrue(profile.vinWmis.contains("4S3"))
        assertTrue(profile.vinWmis.contains("JF1"))
        assertTrue(profile.metrics.containsKey("OIL_TEMP"))
        assertTrue(profile.metrics.containsKey("TRANSMISSION_TEMP"))
        assertTrue(profile.metrics.containsKey("BOOST"))

        val dtcFile = File("src/main/assets/dtc/dtc_subaru.json")
        assertTrue("Subaru DTC file must exist", dtcFile.exists())

        val type = object : TypeToken<List<DtcInfo>>() {}.type
        val dtcs: List<DtcInfo> = gson.fromJson(FileReader(dtcFile), type)
        assertTrue("Subaru DTC database must have 400+ codes", dtcs.size >= 400)

        val p0011 = dtcs.find { it.code == "P0011" }
        assertNotNull("P0011 AVCS intake cam code must be present", p0011)
        assertTrue(p0011!!.title.contains("Intake Camshaft"))
        assertFalse("P0011 must be factory verified", p0011.isCommunitySourced)

        val p2764 = dtcs.find { it.code == "P2764" }
        assertNotNull("P2764 CVT lockup solenoid code must be present", p2764)
        assertTrue(p2764!!.title.contains("Lock-Up"))
        assertTrue("P2764 must be community sourced", p2764.isCommunitySourced)

        val p0325 = dtcs.find { it.code == "P0325" }
        assertNotNull("P0325 Boxer knock sensor must be present", p0325)
        assertTrue(p0325!!.title.contains("Knock"))

        val c0057 = dtcs.find { it.code == "C0057" }
        assertNotNull("C0057 VDC code must be present", c0057)
        assertTrue(c0057!!.title.contains("VDC"))
    }
}

