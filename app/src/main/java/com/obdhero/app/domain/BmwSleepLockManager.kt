package com.obdhero.app.domain

import android.content.Context
import android.os.SystemClock
import org.json.JSONObject
import java.io.File

/**
 * Anti-Bypass Security Lock for BMW / Mini E-Series & R-Series Footwell Module (FRM3) Protection.
 * Ensures the vehicle stays in deep sleep for 20 minutes before allowing battery disconnection.
 */
class BmwSleepLockManager(private val context: Context) {

    private val lockFile: File
        get() = File(context.filesDir, "security/bmw_sleep_lock.json")

    data class LockState(
        val vin: String,
        val sessionStartEpochMs: Long,
        val elapsedMonotonicBaselineMs: Long,
        val requiredDurationSeconds: Long,
        val isLocked: Boolean,
        val remainingSeconds: Long
    )

    fun startSleepSession(vin: String, durationSeconds: Long = 1200L): LockState {
        val nowEpoch = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        val json = JSONObject().apply {
            put("vin", vin)
            put("session_start_epoch_ms", nowEpoch)
            put("elapsed_monotonic_baseline_ms", nowElapsed)
            put("required_duration_seconds", durationSeconds)
        }

        lockFile.parentFile?.mkdirs()
        lockFile.writeText(json.toString())

        return checkState()
    }

    fun checkState(): LockState {
        if (!lockFile.exists()) {
            return LockState(
                vin = "",
                sessionStartEpochMs = 0L,
                elapsedMonotonicBaselineMs = 0L,
                requiredDurationSeconds = 0L,
                isLocked = false,
                remainingSeconds = 0L
            )
        }

        return try {
            val json = JSONObject(lockFile.readText())
            val vin = json.optString("vin", "")
            val startEpoch = json.optLong("session_start_epoch_ms", 0L)
            val durationSec = json.optLong("required_duration_seconds", 1200L)

            val nowEpoch = System.currentTimeMillis()
            val elapsedSec = (nowEpoch - startEpoch) / 1000L

            if (elapsedSec >= durationSec) {
                // 20 minutes have passed - unlock and remove file
                clearLock()
                LockState(
                    vin = vin,
                    sessionStartEpochMs = startEpoch,
                    elapsedMonotonicBaselineMs = 0L,
                    requiredDurationSeconds = durationSec,
                    isLocked = false,
                    remainingSeconds = 0L
                )
            } else {
                val remaining = durationSec - elapsedSec
                LockState(
                    vin = vin,
                    sessionStartEpochMs = startEpoch,
                    elapsedMonotonicBaselineMs = 0L,
                    requiredDurationSeconds = durationSec,
                    isLocked = true,
                    remainingSeconds = remaining.coerceAtLeast(0L)
                )
            }
        } catch (e: Exception) {
            clearLock()
            LockState("", 0L, 0L, 0L, false, 0L)
        }
    }

    fun clearLock() {
        if (lockFile.exists()) {
            lockFile.delete()
        }
    }
}
