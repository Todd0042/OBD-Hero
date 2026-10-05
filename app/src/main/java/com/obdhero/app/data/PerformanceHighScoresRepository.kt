package com.obdhero.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.obdhero.app.domain.VehiclePerformanceHighScores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistent repository for High Scores (Personal Bests) across vehicles.
 * Remembers best 0-60 mph, 1/4 mile, peak horsepower, and peak torque.
 */
object PerformanceHighScoresRepository {
    private const val PREFS_NAME = "obd_hero_high_scores"
    private const val KEY_PREFIX = "scores_vin_"
    private val gson = Gson()

    private var prefs: SharedPreferences? = null
    private var activeVin: String = ""

    private val _currentScores = MutableStateFlow(VehiclePerformanceHighScores())
    val currentScores: StateFlow<VehiclePerformanceHighScores> = _currentScores.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadScoresForVin(activeVin)
    }

    fun setVehicleVin(vin: String) {
        activeVin = vin.trim().uppercase()
        loadScoresForVin(activeVin)
    }

    fun loadScoresForVin(vin: String) {
        val cleanVin = vin.trim().uppercase()
        val p = prefs ?: return
        val rawJson = p.getString(KEY_PREFIX + cleanVin, null)
        val loaded = if (rawJson != null) {
            try {
                gson.fromJson(rawJson, VehiclePerformanceHighScores::class.java)
            } catch (_: Exception) {
                VehiclePerformanceHighScores(vin = cleanVin)
            }
        } else {
            VehiclePerformanceHighScores(vin = cleanVin)
        }
        _currentScores.value = loaded
    }

    private fun persist(scores: VehiclePerformanceHighScores) {
        _currentScores.value = scores
        val p = prefs ?: return
        val cleanVin = scores.vin.trim().uppercase()
        val json = gson.toJson(scores)
        p.edit().putString(KEY_PREFIX + cleanVin, json).apply()
    }

    /**
     * Checks if 0-60 time is a new personal best. Returns true if record was beaten.
     */
    fun record0To60(vin: String, seconds: Float): Boolean {
        if (seconds <= 0.5f || seconds >= 30.0f) return false
        val current = _currentScores.value
        val isNewBest = current.best0To60Seconds == null || seconds < current.best0To60Seconds

        if (isNewBest) {
            val updated = current.copy(
                vin = vin,
                best0To60Seconds = seconds,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            persist(updated)
            return true
        }
        return false
    }

    /**
     * Checks if 1/4 mile time is a new personal best.
     */
    fun recordQuarterMile(vin: String, seconds: Float, trapMph: Int): Boolean {
        if (seconds <= 4.0f || seconds >= 60.0f) return false
        val current = _currentScores.value
        val isNewBest = current.bestQuarterMileSeconds == null || seconds < current.bestQuarterMileSeconds

        if (isNewBest) {
            val updated = current.copy(
                vin = vin,
                bestQuarterMileSeconds = seconds,
                quarterMileTrapMph = trapMph,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            persist(updated)
            return true
        }
        return false
    }

    /**
     * Checks if horsepower is a new personal best peak.
     */
    fun recordPeakHp(vin: String, hp: Int): Boolean {
        if (hp <= 0) return false
        val current = _currentScores.value
        if (hp > current.peakHorsepower) {
            val updated = current.copy(
                vin = vin,
                peakHorsepower = hp,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            persist(updated)
            return true
        }
        return false
    }

    /**
     * Checks if torque is a new personal best peak.
     */
    fun recordPeakTorque(vin: String, torque: Int): Boolean {
        if (torque <= 0) return false
        val current = _currentScores.value
        if (torque > current.peakTorqueLbFt) {
            val updated = current.copy(
                vin = vin,
                peakTorqueLbFt = torque,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            persist(updated)
            return true
        }
        return false
    }

    fun resetScores(vin: String) {
        val reset = VehiclePerformanceHighScores(vin = vin.trim().uppercase())
        persist(reset)
    }
}
