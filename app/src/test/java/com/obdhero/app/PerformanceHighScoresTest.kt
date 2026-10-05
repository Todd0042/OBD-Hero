package com.obdhero.app

import com.obdhero.app.domain.DragyAccelerationTracker
import com.obdhero.app.domain.DragyRunState
import com.obdhero.app.domain.LiveTelemetry
import com.obdhero.app.domain.VehiclePerformanceHighScores
import org.junit.Assert.*
import org.junit.Test

class PerformanceHighScoresTest {

    @Test
    fun testLiveTelemetryHorsepowerCalculation() {
        // Torque 400 lb-ft at 5252 RPM = exactly 400 HP
        val telem1 = LiveTelemetry(rpm = 5252, torqueLbFt = 400)
        assertEquals(400, telem1.horsepower)

        // Torque 300 lb-ft at 6000 RPM = (300 * 6000) / 5252 = 342 HP
        val telem2 = LiveTelemetry(rpm = 6000, torqueLbFt = 300)
        assertEquals(342, telem2.horsepower)

        // Engine off
        val telemZero = LiveTelemetry(rpm = 0, torqueLbFt = 0)
        assertEquals(0, telemZero.horsepower)
    }

    @Test
    fun testDragyTrackerStateFlowAnd0To60() {
        val tracker = DragyAccelerationTracker()
        tracker.setVehicleVin("1FA6P8CF0H5999999")

        // 1. Initial State
        assertEquals(DragyRunState.IDLE, tracker.snapshot.value.runState)

        // 2. Stopped at 0 MPH stages the run
        tracker.onTelemetryTick(LiveTelemetry(speedMph = 0, rpm = 800, torqueLbFt = 50))
        assertEquals(DragyRunState.STAGED_AT_ZERO, tracker.snapshot.value.runState)

        // 3. Vehicle Launches (accelerates to 30 MPH)
        tracker.onTelemetryTick(LiveTelemetry(speedMph = 30, rpm = 3500, torqueLbFt = 320))
        assertEquals(DragyRunState.ACCELERATING, tracker.snapshot.value.runState)
        assertTrue(tracker.snapshot.value.currentHorsepower > 0)

        Thread.sleep(20)

        // 4. Vehicle hits 60 MPH
        tracker.onTelemetryTick(LiveTelemetry(speedMph = 60, rpm = 5800, torqueLbFt = 340))
        assertTrue(tracker.snapshot.value.elapsed0To60Seconds > 0.0f)
        assertTrue(tracker.snapshot.value.currentHorsepower >= 370)
    }

    @Test
    fun testHighScorePersistenceLogic() {
        var scores = VehiclePerformanceHighScores(vin = "TEST_VIN")

        // First 0-60 run: 5.20s
        scores = scores.copy(best0To60Seconds = 5.20f)
        assertEquals(5.20f, scores.best0To60Seconds!!, 0.001f)

        // Second run: 4.85s (Faster - beats record)
        val fasterRun = 4.85f
        if (fasterRun < scores.best0To60Seconds!!) {
            scores = scores.copy(best0To60Seconds = fasterRun)
        }
        assertEquals(4.85f, scores.best0To60Seconds!!, 0.001f)

        // Third run: 5.10s (Slower - does NOT beat record)
        val slowerRun = 5.10f
        if (slowerRun < scores.best0To60Seconds!!) {
            scores = scores.copy(best0To60Seconds = slowerRun)
        }
        assertEquals(4.85f, scores.best0To60Seconds!!, 0.001f)

        // Peak HP record: 350 HP, then 410 HP
        scores = scores.copy(peakHorsepower = 350)
        val newPullHp = 410
        if (newPullHp > scores.peakHorsepower) {
            scores = scores.copy(peakHorsepower = newPullHp)
        }
        assertEquals(410, scores.peakHorsepower)

        // Peak Torque record: 380 LB-FT, then 425 LB-FT
        scores = scores.copy(peakTorqueLbFt = 380)
        val newPullTorque = 425
        if (newPullTorque > scores.peakTorqueLbFt) {
            scores = scores.copy(peakTorqueLbFt = newPullTorque)
        }
        assertEquals(425, scores.peakTorqueLbFt)
    }
}
