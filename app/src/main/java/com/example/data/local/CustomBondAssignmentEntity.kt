package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.BondAssignment
import com.example.domain.model.PeakIntensity
import com.example.domain.model.SpectralRegion
import com.example.domain.model.SupportingPeakRef
import com.example.domain.model.VibrationType
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "custom_bond_assignments")
data class CustomBondAssignmentEntity(
    @PrimaryKey
    val id: String,
    val bond: String,
    val functionalGroup: String,
    val vibrationType: String,
    val vibrationalMode: String,
    val minWavenumber: Double,
    val maxWavenumber: Double,
    val typicalWavenumber: Double,
    val spectralRegion: String,
    val intensity: String,
    val bandShape: String,
    val possibleCompoundClassesJson: String,
    val supportingPeaksJson: String,
    val notes: String,
    val visualDiagramAscii: String?,
    val userNotes: String?,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): BondAssignment {
        val compoundClasses = mutableListOf<String>()
        try {
            val arr = JSONArray(possibleCompoundClassesJson)
            for (i in 0 until arr.length()) compoundClasses.add(arr.getString(i))
        } catch (_: Exception) {}

        val supporting = mutableListOf<SupportingPeakRef>()
        try {
            val arr = JSONArray(supportingPeaksJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                supporting.add(
                    SupportingPeakRef(
                        description = obj.optString("description", ""),
                        expectedRange = obj.optString("expectedRange", ""),
                        minWavenumber = obj.optDouble("minWavenumber", 0.0),
                        maxWavenumber = obj.optDouble("maxWavenumber", 0.0),
                        diagnosticRole = obj.optString("diagnosticRole", "")
                    )
                )
            }
        } catch (_: Exception) {}

        val vibType = try {
            VibrationType.valueOf(vibrationType)
        } catch (_: Exception) {
            VibrationType.STRETCH
        }

        val region = try {
            SpectralRegion.valueOf(spectralRegion)
        } catch (_: Exception) {
            SpectralRegion.fromWavenumber(typicalWavenumber)
        }

        val peakInt = try {
            PeakIntensity.valueOf(intensity)
        } catch (_: Exception) {
            PeakIntensity.MEDIUM
        }

        return BondAssignment(
            id = id,
            bond = bond,
            functionalGroup = functionalGroup,
            vibrationType = vibType,
            vibrationalMode = vibrationalMode,
            minWavenumber = minWavenumber,
            maxWavenumber = maxWavenumber,
            typicalWavenumber = typicalWavenumber,
            spectralRegion = region,
            intensity = peakInt,
            bandShape = bandShape,
            possibleCompoundClasses = compoundClasses,
            supportingPeaks = supporting,
            notes = notes,
            visualDiagramAscii = visualDiagramAscii,
            isCustom = true,
            userNotes = userNotes
        )
    }

    companion object {
        fun fromDomain(domain: BondAssignment): CustomBondAssignmentEntity {
            val compArr = JSONArray()
            domain.possibleCompoundClasses.forEach { compArr.put(it) }

            val suppArr = JSONArray()
            domain.supportingPeaks.forEach { s ->
                val obj = JSONObject().apply {
                    put("description", s.description)
                    put("expectedRange", s.expectedRange)
                    put("minWavenumber", s.minWavenumber)
                    put("maxWavenumber", s.maxWavenumber)
                    put("diagnosticRole", s.diagnosticRole)
                }
                suppArr.put(obj)
            }

            return CustomBondAssignmentEntity(
                id = domain.id,
                bond = domain.bond,
                functionalGroup = domain.functionalGroup,
                vibrationType = domain.vibrationType.name,
                vibrationalMode = domain.vibrationalMode,
                minWavenumber = domain.minWavenumber,
                maxWavenumber = domain.maxWavenumber,
                typicalWavenumber = domain.typicalWavenumber,
                spectralRegion = domain.spectralRegion.name,
                intensity = domain.intensity.name,
                bandShape = domain.bandShape,
                possibleCompoundClassesJson = compArr.toString(),
                supportingPeaksJson = suppArr.toString(),
                notes = domain.notes,
                visualDiagramAscii = domain.visualDiagramAscii,
                userNotes = domain.userNotes
            )
        }
    }
}
