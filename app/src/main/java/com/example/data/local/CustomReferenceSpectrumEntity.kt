package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ReferenceSpectrum
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumPoint
import com.example.domain.model.SpectrumSourceType
import com.example.domain.model.SpectrumType
import java.util.Locale

@Entity(tableName = "custom_reference_spectra")
data class CustomReferenceSpectrumEntity(
    @PrimaryKey
    val id: String,
    val databaseIdentifier: String,
    val compoundName: String,
    val chemicalFormula: String?,
    val casNumber: String?,
    val materialClass: String,
    val notes: String,
    val measurementMethod: String,
    val source: String,
    val sampleInformation: String,
    val xAxisUnit: String,
    val yAxisType: String,
    val characteristicPeaksStr: String, // "1715.0;1365.0;..."
    val pointsData: String,              // "wn:sig;wn:sig;..."
    val functionalGroupsStr: String,    // "Carbonyl;Ketone"
    val spectralRegionsStr: String,     // "Fingerprint;C-H"
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ReferenceSpectrum {
        val points = if (pointsData.isBlank()) {
            emptyList()
        } else {
            pointsData.split(";").mapNotNull { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val x = parts[0].toDoubleOrNull()
                    val y = parts[1].toDoubleOrNull()
                    if (x != null && y != null) SpectrumPoint(x, y) else null
                } else null
            }
        }

        val peaks = if (characteristicPeaksStr.isBlank()) {
            emptyList()
        } else {
            characteristicPeaksStr.split(";").mapNotNull { it.toDoubleOrNull() }
        }

        val parsedType = try {
            SpectrumType.valueOf(yAxisType)
        } catch (e: Exception) {
            SpectrumType.Transmittance
        }

        val domainSpectrum = Spectrum(
            id = id,
            name = "$compoundName (Reference)",
            sourceType = SpectrumSourceType.FILE_IMPORT,
            spectrumType = parsedType,
            xUnit = xAxisUnit,
            yUnit = if (parsedType == SpectrumType.Transmittance) "%T" else "A.U.",
            points = points,
            rawPoints = points,
            metadata = mapOf(
                "Compound" to compoundName,
                "CAS" to (casNumber ?: "N/A"),
                "Formula" to (chemicalFormula ?: "N/A"),
                "Method" to measurementMethod,
                "Source" to source
            )
        )

        val fgList = if (functionalGroupsStr.isBlank()) emptyList() else functionalGroupsStr.split(";").filter { it.isNotBlank() }
        val regList = if (spectralRegionsStr.isBlank()) emptyList() else spectralRegionsStr.split(";").filter { it.isNotBlank() }

        return ReferenceSpectrum(
            id = id,
            databaseIdentifier = databaseIdentifier,
            compoundName = compoundName,
            chemicalFormula = chemicalFormula,
            casNumber = casNumber,
            materialClass = materialClass,
            description = notes,
            notes = notes,
            measurementMethod = measurementMethod,
            source = source,
            sampleInformation = sampleInformation,
            xAxisUnit = xAxisUnit,
            yAxisType = parsedType,
            functionalGroups = fgList,
            spectralRegions = regList,
            characteristicPeaks = peaks,
            spectrum = domainSpectrum,
            isDemo = false,
            isUserCustom = true
        )
    }

    companion object {
        fun fromDomain(ref: ReferenceSpectrum): CustomReferenceSpectrumEntity {
            val pointsStr = ref.spectrum.points.joinToString(";") {
                "${"%.2f".format(Locale.US, it.wavenumber)}:${"%.4f".format(Locale.US, it.signalValue)}"
            }
            val peaksStr = ref.characteristicPeaks.joinToString(";") {
                "%.1f".format(Locale.US, it)
            }
            return CustomReferenceSpectrumEntity(
                id = ref.id,
                databaseIdentifier = ref.databaseIdentifier,
                compoundName = ref.compoundName,
                chemicalFormula = ref.chemicalFormula,
                casNumber = ref.casNumber,
                materialClass = ref.materialClass,
                notes = ref.notes,
                measurementMethod = ref.measurementMethod,
                source = ref.source,
                sampleInformation = ref.sampleInformation,
                xAxisUnit = ref.xAxisUnit,
                yAxisType = ref.yAxisType.name,
                characteristicPeaksStr = peaksStr,
                pointsData = pointsStr,
                functionalGroupsStr = ref.functionalGroups.joinToString(";"),
                spectralRegionsStr = ref.spectralRegions.joinToString(";"),
                createdAt = System.currentTimeMillis()
            )
        }
    }
}
