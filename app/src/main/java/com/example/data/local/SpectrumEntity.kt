package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Peak
import com.example.domain.model.PeakIntensity
import com.example.domain.model.PreprocessingStep
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumPoint
import com.example.domain.model.SpectrumSourceType
import com.example.domain.model.SpectrumType
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@Entity(tableName = "spectrum_history")
data class SpectrumEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val sourceType: String,
    val spectrumType: String,
    val xUnit: String,
    val yUnit: String,
    val minWavenumber: Double,
    val maxWavenumber: Double,
    val minSignal: Double,
    val maxSignal: Double,
    val pointCount: Int,
    val pointsData: String, // serialized points "wn:sig;wn:sig;..."
    val rawPointsData: String? = null,
    val detectedPeaksJson: String? = null,
    val preprocessingHistoryJson: String? = null,
    val metadataJson: String? = null,
    val peaksSummary: String,
    val createdAt: Long
) {
    fun toDomainSpectrum(): Spectrum {
        val points = deserializePoints(pointsData)
        val rawPoints = if (!rawPointsData.isNullOrBlank()) {
            deserializePoints(rawPointsData)
        } else points

        val parsedType = try {
            SpectrumType.valueOf(spectrumType)
        } catch (_: Exception) {
            SpectrumType.Transmittance
        }

        val parsedSource = try {
            SpectrumSourceType.valueOf(sourceType)
        } catch (_: Exception) {
            SpectrumSourceType.FILE_IMPORT
        }

        val peaks = deserializePeaks(detectedPeaksJson)
        val history = deserializePreprocessing(preprocessingHistoryJson)
        val meta = deserializeMetadata(metadataJson)

        return Spectrum(
            id = id,
            name = name,
            sourceType = parsedSource,
            spectrumType = parsedType,
            xUnit = xUnit,
            yUnit = yUnit,
            points = points,
            rawPoints = rawPoints,
            metadata = meta,
            preprocessingHistory = history,
            detectedPeaks = peaks,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomainSpectrum(spectrum: Spectrum): SpectrumEntity {
            val serializedPoints = serializePoints(spectrum.points)
            val serializedRawPoints = serializePoints(spectrum.rawPoints)
            val serializedPeaks = serializePeaks(spectrum.detectedPeaks)
            val serializedHistory = serializePreprocessing(spectrum.preprocessingHistory)
            val serializedMetadata = serializeMetadata(spectrum.metadata)

            val peakSummary = spectrum.detectedPeaks.take(6).joinToString(", ") {
                "${"%.0f".format(Locale.US, it.wavenumber)} cm⁻¹"
            }

            val preferredName = spectrum.metadata["user_sample_name"]?.takeIf { it.isNotBlank() } ?: spectrum.name

            return SpectrumEntity(
                id = spectrum.id,
                name = preferredName,
                sourceType = spectrum.sourceType.name,
                spectrumType = spectrum.spectrumType.name,
                xUnit = spectrum.xUnit,
                yUnit = spectrum.yUnit,
                minWavenumber = spectrum.minWavenumber,
                maxWavenumber = spectrum.maxWavenumber,
                minSignal = spectrum.minSignal,
                maxSignal = spectrum.maxSignal,
                pointCount = spectrum.pointCount,
                pointsData = serializedPoints,
                rawPointsData = serializedRawPoints,
                detectedPeaksJson = serializedPeaks,
                preprocessingHistoryJson = serializedHistory,
                metadataJson = serializedMetadata,
                peaksSummary = peakSummary,
                createdAt = spectrum.createdAt
            )
        }

        private fun serializePoints(points: List<SpectrumPoint>): String {
            return points.joinToString(";") {
                "${"%.2f".format(Locale.US, it.wavenumber)}:${"%.3f".format(Locale.US, it.signalValue)}"
            }
        }

        private fun deserializePoints(data: String): List<SpectrumPoint> {
            if (data.isBlank()) return emptyList()
            return data.split(";").mapNotNull { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val x = parts[0].toDoubleOrNull()
                    val y = parts[1].toDoubleOrNull()
                    if (x != null && y != null) SpectrumPoint(x, y) else null
                } else null
            }
        }

        private fun serializePeaks(peaks: List<Peak>): String {
            if (peaks.isEmpty()) return "[]"
            val array = JSONArray()
            peaks.forEach { p ->
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("index", p.index)
                    put("wn", p.wavenumber)
                    put("sig", p.signalValue)
                    put("prom", p.prominence)
                    put("width", p.width)
                    put("fwhm", p.fwhm)
                    put("relInt", p.relativeIntensity)
                    put("area", p.area)
                    put("intensity", p.intensity.name)
                    p.matchedFunctionalGroup?.let { put("fg", it) }
                    p.notes?.let { put("notes", it) }
                }
                array.put(obj)
            }
            return array.toString()
        }

        private fun deserializePeaks(json: String?): List<Peak> {
            if (json.isNullOrBlank() || json == "[]") return emptyList()
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<Peak>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val intensity = try {
                        PeakIntensity.valueOf(obj.optString("intensity", "MEDIUM"))
                    } catch (_: Exception) {
                        PeakIntensity.MEDIUM
                    }
                    list.add(
                        Peak(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            index = obj.optInt("index", i + 1),
                            wavenumber = obj.optDouble("wn", 0.0),
                            signalValue = obj.optDouble("sig", 0.0),
                            prominence = obj.optDouble("prom", 0.0),
                            width = obj.optDouble("width", 0.0),
                            fwhm = obj.optDouble("fwhm", 0.0),
                            relativeIntensity = obj.optDouble("relInt", 0.0),
                            area = obj.optDouble("area", 0.0),
                            intensity = intensity,
                            matchedFunctionalGroup = obj.optString("fg").takeIf { it.isNotBlank() },
                            notes = obj.optString("notes").takeIf { it.isNotBlank() }
                        )
                    )
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun serializePreprocessing(steps: List<PreprocessingStep>): String {
            if (steps.isEmpty()) return "[]"
            val array = JSONArray()
            steps.forEach { s ->
                val obj = JSONObject().apply {
                    put("type", s.stepType)
                    put("desc", s.description)
                    put("time", s.timestamp)
                    val paramsObj = JSONObject()
                    s.parameters.forEach { (k, v) -> paramsObj.put(k, v) }
                    put("params", paramsObj)
                }
                array.put(obj)
            }
            return array.toString()
        }

        private fun deserializePreprocessing(json: String?): List<PreprocessingStep> {
            if (json.isNullOrBlank() || json == "[]") return emptyList()
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<PreprocessingStep>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val paramsMap = mutableMapOf<String, String>()
                    val paramsObj = obj.optJSONObject("params")
                    paramsObj?.keys()?.forEach { k ->
                        paramsMap[k] = paramsObj.optString(k)
                    }
                    list.add(
                        PreprocessingStep(
                            stepType = obj.optString("type", "TRANSFORMATION"),
                            description = obj.optString("desc", ""),
                            timestamp = obj.optLong("time", System.currentTimeMillis()),
                            parameters = paramsMap
                        )
                    )
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun serializeMetadata(metadata: Map<String, String>): String {
            if (metadata.isEmpty()) return "{}"
            val obj = JSONObject()
            metadata.forEach { (k, v) -> obj.put(k, v) }
            return obj.toString()
        }

        private fun deserializeMetadata(json: String?): Map<String, String> {
            if (json.isNullOrBlank() || json == "{}") return emptyMap()
            return try {
                val obj = JSONObject(json)
                val map = mutableMapOf<String, String>()
                obj.keys().forEach { k ->
                    map[k] = obj.optString(k)
                }
                map
            } catch (_: Exception) {
                emptyMap()
            }
        }
    }
}
