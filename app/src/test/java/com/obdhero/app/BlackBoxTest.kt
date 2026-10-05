package com.obdhero.app

import com.obdhero.app.data.AdapterDatabase
import com.obdhero.app.domain.AdapterHardwareTier
import com.obdhero.app.domain.BlackBoxFlightRecorder
import com.obdhero.app.domain.FlightRecorderState
import com.obdhero.app.domain.LiveTelemetry
import org.junit.Assert.*
import org.junit.Test

class BlackBoxTest {

    @Test
    fun testAdapterIdentificationTiers() {
        // 1. Optimal: Vgate vLinker MS
        val vlinker = AdapterDatabase.identifyAdapter("vLinker MS-Android", "vLinker MS v2.2", "", "STN2120 v4.3")
        assertEquals(AdapterHardwareTier.OPTIMAL, vlinker.tier)
        assertEquals("Vgate vLinker MS", vlinker.name)
        assertTrue(vlinker.multiBusCapable)
        assertEquals(8192, vlinker.bufferBytes)

        // 2. BLE Filtered: OBDLink CX
        val cx = AdapterDatabase.identifyAdapter("OBDLink CX", "OBDLink CX v5.6", "", "STN1110")
        assertEquals(AdapterHardwareTier.BLE_FILTERED, cx.tier)
        assertEquals("OBDLink CX", cx.name)

        // 3. Enterprise: OBDLink MX+
        val mx = AdapterDatabase.identifyAdapter("OBDLink MX+", "OBDLink MX+ v4.5", "", "STN2120")
        assertEquals(AdapterHardwareTier.EXCEPTIONAL, mx.tier)
        assertEquals(10240, mx.bufferBytes)

        // 4. Budget Clone: Generic ELM327
        val clone = AdapterDatabase.identifyAdapter("OBDII", "ELM327 v1.5", "", "?")
        assertEquals(AdapterHardwareTier.CHEAP_GARBAGE, clone.tier)
        assertEquals(256, clone.bufferBytes)
        assertNotNull(clone.snarkyEasterEgg)
    }

    @Test
    fun testBooleanAsciiJokeAccuracy() {
        val binaryJoke = AdapterDatabase.BOOLEAN_ASCII_JOKE
        val bytes = binaryJoke.split(" ").map { it.toInt(2).toByte() }.toByteArray()
        val decoded = String(bytes, Charsets.US_ASCII)
        assertEquals(AdapterDatabase.DECODED_JOKE, decoded)
        assertEquals("You really thought that device could do this for $15?", decoded)
    }

    @Test
    fun testFlightRecorderRingBufferAndIncidentCapture() {
        val recorder = BlackBoxFlightRecorder(
            adapterProfile = AdapterDatabase.PROFILES.first { it.name == "Vgate vLinker MS" }
        )
        recorder.setVehicleVin("JM1BP234567890123")

        assertEquals(FlightRecorderState.ARMED_LISTENING, recorder.recorderState.value)

        // Feed 40 samples (more than the 25 capacity of the pre-trigger buffer)
        for (i in 1..40) {
            recorder.onTelemetryTick(
                LiveTelemetry(
                    rpm = 2000 + i * 10,
                    speedMph = 45,
                    coolantTempF = 195,
                    oilPressurePsi = 40
                )
            )
            Thread.sleep(5)
        }

        // Trigger incident
        recorder.triggerIncident("High Knock Sensor Spike", dtc = "P0325")
        assertEquals(FlightRecorderState.TRIGGERED_RECORDING, recorder.recorderState.value)

        // Feed 15 post-trigger samples
        for (i in 1..15) {
            recorder.onTelemetryTick(
                LiveTelemetry(
                    rpm = 1800,
                    speedMph = 40,
                    coolantTempF = 200,
                    oilPressurePsi = 38
                )
            )
            Thread.sleep(5)
        }

        recorder.finalizeIncident(freezeFrame = null)
        assertEquals(FlightRecorderState.INCIDENT_SAVED, recorder.recorderState.value)

        val incident = recorder.activeIncident.value
        assertNotNull(incident)
        assertEquals("High Knock Sensor Spike", incident!!.triggerReason)
        assertEquals("P0325", incident.triggerDtc)
        assertEquals("JM1BP234567890123", incident.vehicleVin)

        // Pre-trigger buffer must be capped at 25 samples
        assertEquals(25, incident.preTriggerSamples.size)
        // All pre-trigger samples must have negative or zero timestamp offsets
        assertTrue(incident.preTriggerSamples.all { it.timestampOffsetMs <= 0L })

        // Post-trigger samples must match recorded count
        assertEquals(15, incident.postTriggerSamples.size)
        assertTrue(incident.postTriggerSamples.all { it.timestampOffsetMs >= 0L })
    }

    @Test
    fun testCheapGarbageAdapterInterceptsWithJoke() {
        val recorder = BlackBoxFlightRecorder(
            adapterProfile = AdapterDatabase.PROFILES.first { it.tier == AdapterHardwareTier.CHEAP_GARBAGE }
        )

        recorder.triggerIncident("Overboost detected", dtc = "P0234")
        assertEquals(FlightRecorderState.INCIDENT_SAVED, recorder.recorderState.value)

        val incident = recorder.activeIncident.value
        assertNotNull(incident)
        assertNotNull(incident!!.booleanAsciiJoke)
        assertTrue(incident.booleanAsciiJoke!!.startsWith("01011001 01101111"))
        assertEquals(0, incident.preTriggerSamples.size)
        assertEquals(0, incident.postTriggerSamples.size)
    }
}
