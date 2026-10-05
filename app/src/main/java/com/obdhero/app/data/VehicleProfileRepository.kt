package com.obdhero.app.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.obdhero.app.domain.VehicleProfileDefinition
import java.io.InputStreamReader

object VehicleProfileRepository {
    private const val TAG = "VehicleProfileRepo"
    private val gson = Gson()

    private val profiles = mutableMapOf<String, VehicleProfileDefinition>()
    private var defaultProfile: VehicleProfileDefinition? = null

    fun init(context: Context) {
        try {
            val assetManager = context.assets
            val files = assetManager.list("profiles") ?: emptyArray()
            for (filename in files) {
                if (filename.endsWith(".json")) {
                    assetManager.open("profiles/$filename").use { stream ->
                        val profile = gson.fromJson(InputStreamReader(stream), VehicleProfileDefinition::class.java)
                        profiles[profile.profileId] = profile
                        Log.i(TAG, "Loaded vehicle profile: ${profile.profileId} (${profile.make} ${profile.model})")
                    }
                }
            }
            defaultProfile = profiles["GENERIC_SAE_J1979"]
        } catch (e: Exception) {
            Log.e(TAG, "Error loading vehicle profiles: ${e.message}", e)
        }
    }

    fun getAllProfiles(): List<VehicleProfileDefinition> = profiles.values.toList()

    fun getProfileById(id: String): VehicleProfileDefinition {
        return profiles[id] ?: defaultProfile ?: fallbackGeneric()
    }

    /**
     * Resolves the appropriate vehicle profile from the VIN and decoded parameters.
     */
    fun resolveProfile(vin: String, make: String, model: String, year: Int?): VehicleProfileDefinition {
        val cleanVin = vin.trim().uppercase()

        // 1. Specific Platform & Generation matching
        for (profile in profiles.values) {
            if (profile.make.equals("Mazda", ignoreCase = true)) {
                // Mazda ND (Gen 6)
                if (profile.profileId == "MAZDA_SKYACTIV_ND2" && (cleanVin.contains("ND") || model.contains("MX-5", ignoreCase = true))) {
                    return profile
                }
                // Mazda Gen 7 (BP / DM - 2019+ Mazda 3 / CX-30 / CX-50)
                if (profile.profileId == "MAZDA_GEN7_BP" && (
                    cleanVin.contains("BP") || cleanVin.contains("DM") || cleanVin.contains("VA") ||
                    model.contains("Mazda 3", ignoreCase = true) || model.contains("CX-30", ignoreCase = true) ||
                    (year != null && year >= 2019 && !cleanVin.contains("ND"))
                )) {
                    return profile
                }
            }

            if (profile.make.equals("BMW", ignoreCase = true) && profile.vinWmis.any { cleanVin.startsWith(it) }) {
                return profile
            }

            if (profile.profileId == "CHRYSLER_FCA_CAN" && (
                profile.vinWmis.any { cleanVin.startsWith(it) } ||
                make.contains("Chrysler", ignoreCase = true) ||
                make.contains("Jeep", ignoreCase = true) ||
                make.contains("Dodge", ignoreCase = true) ||
                make.contains("Ram", ignoreCase = true)
            )) {
                return profile
            }

            if (profile.profileId == "FORD_CAN_ECOBOOST" && (
                profile.vinWmis.any { cleanVin.startsWith(it) } ||
                make.contains("Ford", ignoreCase = true) ||
                make.contains("Lincoln", ignoreCase = true)
            )) {
                return profile
            }

            if (profile.profileId == "GM_GLOBAL_A" && (
                profile.vinWmis.any { cleanVin.startsWith(it) } ||
                make.contains("Chevrolet", ignoreCase = true) ||
                make.contains("Chevy", ignoreCase = true) ||
                make.contains("GMC", ignoreCase = true) ||
                make.contains("Cadillac", ignoreCase = true) ||
                make.contains("Buick", ignoreCase = true) ||
                make.equals("GM", ignoreCase = true)
            )) {
                return profile
            }
        }

        // 2. Make match
        for (profile in profiles.values) {
            if (profile.make.equals(make, ignoreCase = true)) {
                return profile
            }
        }

        // 3. Fallback to generic SAE
        return defaultProfile ?: fallbackGeneric()
    }

    private fun fallbackGeneric(): VehicleProfileDefinition {
        return VehicleProfileDefinition(
            profileId = "GENERIC_SAE_J1979",
            make = "Generic",
            model = "Standard OBD-II"
        )
    }
}
