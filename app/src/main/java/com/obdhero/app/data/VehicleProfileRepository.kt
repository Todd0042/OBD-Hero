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

        // 1. Direct WMI match (e.g. JM1 for Mazda ND)
        for (profile in profiles.values) {
            if (profile.vinWmis.any { wmi -> cleanVin.startsWith(wmi) }) {
                // If it's Mazda, check if it's ND
                if (profile.make.equals("Mazda", ignoreCase = true) && cleanVin.contains("ND")) {
                    return profile
                }
                if (profile.make.equals("BMW", ignoreCase = true)) {
                    return profile
                }
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
