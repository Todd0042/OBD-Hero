package com.obdhero.app.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.obdhero.app.domain.BlackBoxIncidentReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Persistent repository for Black Box Flight Recorder incident dumps.
 * Associates captured recordings with specific vehicle VINs, allowing technicians
 * to review historic powertrain failure logs across customer visits.
 */
object BlackBoxRepository {
    private const val TAG = "BlackBoxRepository"
    private const val FILE_NAME = "blackbox_incidents.json"
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var storageFile: File? = null

    private val _incidents = MutableStateFlow<List<BlackBoxIncidentReport>>(emptyList())
    val incidents: StateFlow<List<BlackBoxIncidentReport>> = _incidents.asStateFlow()

    fun init(context: Context) {
        storageFile = File(context.filesDir, FILE_NAME)
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val file = storageFile ?: return
        if (!file.exists()) return

        try {
            val json = file.readText()
            val listType = object : TypeToken<List<BlackBoxIncidentReport>>() {}.type
            val loaded: List<BlackBoxIncidentReport>? = gson.fromJson(json, listType)
            if (loaded != null) {
                _incidents.value = loaded
                Log.i(TAG, "Loaded ${loaded.size} Black Box incidents from disk")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading incidents from disk: ${e.message}", e)
        }
    }

    private fun persistToDisk() {
        val file = storageFile ?: return
        scope.launch {
            try {
                val json = gson.toJson(_incidents.value)
                file.writeText(json)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist incidents to disk: ${e.message}", e)
            }
        }
    }

    fun saveIncident(report: BlackBoxIncidentReport) {
        val current = _incidents.value.toMutableList()
        val index = current.indexOfFirst { it.incidentId == report.incidentId }
        if (index >= 0) {
            current[index] = report
        } else {
            current.add(0, report) // Most recent first
        }
        _incidents.value = current
        persistToDisk()
    }

    fun deleteIncident(incidentId: String) {
        _incidents.value = _incidents.value.filter { it.incidentId != incidentId }
        persistToDisk()
    }

    fun getIncidentsForVin(vin: String): List<BlackBoxIncidentReport> {
        val clean = vin.trim().uppercase()
        if (clean.isEmpty()) return _incidents.value
        return _incidents.value.filter { it.vehicleVin.trim().uppercase() == clean }
    }

    fun clearAll() {
        _incidents.value = emptyList()
        persistToDisk()
    }
}
