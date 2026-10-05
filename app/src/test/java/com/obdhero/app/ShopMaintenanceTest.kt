package com.obdhero.app

import com.obdhero.app.data.ShopMaintenanceRepository
import com.obdhero.app.domain.ServiceFunctionCategory
import org.junit.Assert.*
import org.junit.Test

class ShopMaintenanceTest {

    @Test
    fun testFordMaintenanceToolsContainOilEpbBatteryTpms() {
        val tools = ShopMaintenanceRepository.getToolsForVehicle("Ford")
        assertEquals(5, tools.size)

        val oil = tools.find { it.category == ServiceFunctionCategory.OIL_MAINTENANCE }
        assertNotNull(oil)
        assertTrue(oil!!.manualProcedureSteps.any { it.contains("ACCELERATOR and BRAKE pedals", ignoreCase = true) })
        assertEquals("OIL_RESET_FORD", oil.id)

        val epb = tools.find { it.category == ServiceFunctionCategory.BRAKES_EPB && it.id == "EPB_ENTER_SERVICE" }
        assertNotNull(epb)
        assertEquals("EPB_ENTER_SERVICE", epb!!.id)

        val bms = tools.find { it.category == ServiceFunctionCategory.BATTERY_BMS }
        assertNotNull(bms)
        assertTrue(bms!!.manualProcedureSteps.any { it.contains("high-beam", ignoreCase = true) })

        val tpms = tools.find { it.category == ServiceFunctionCategory.TIRES_TPMS }
        assertNotNull(tpms)
        assertEquals("TPMS_RELEARN_TRIGGER", tpms!!.id)
    }

    @Test
    fun testGmMaintenanceToolsContainOilAndPedalPumps() {
        val tools = ShopMaintenanceRepository.getToolsForVehicle("Chevrolet")
        assertEquals(5, tools.size)
        val oil = tools.find { it.category == ServiceFunctionCategory.OIL_MAINTENANCE }
        assertNotNull(oil)
        assertTrue(oil!!.manualProcedureSteps.any { it.contains("accelerator pedal 3 times", ignoreCase = true) })
        assertEquals("OIL_RESET_GM", oil.id)

        val epb = tools.find { it.category == ServiceFunctionCategory.BRAKES_EPB && it.id == "EPB_ENTER_SERVICE" }
        assertNotNull(epb)
    }

    @Test
    fun testBmwMaintenanceToolsContainOilAndEpb() {
        val tools = ShopMaintenanceRepository.getToolsForVehicle("BMW")
        assertEquals(5, tools.size)
        val oil = tools.find { it.category == ServiceFunctionCategory.OIL_MAINTENANCE }
        assertNotNull(oil)
        assertTrue(oil!!.manualProcedureSteps.any { it.contains("odometer", ignoreCase = true) })

        val bms = tools.find { it.category == ServiceFunctionCategory.BATTERY_BMS }
        assertNotNull(bms)
        assertTrue(bms!!.biDirectionalCommand.contains("31 01 01 00"))
    }

    @Test
    fun testGenericFallbackProvidesComprehensiveProcedures() {
        val tools = ShopMaintenanceRepository.getToolsForVehicle("UnknownMaker")
        assertEquals(5, tools.size)
        assertTrue(tools.all { it.manualProcedureSteps.isNotEmpty() })
    }
}
