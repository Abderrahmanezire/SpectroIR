package com.example.domain.repository

import com.example.domain.model.Spectrum
import kotlinx.coroutines.flow.Flow

interface SpectrumRepository {
    fun getHistorySpectra(): Flow<List<Spectrum>>
    suspend fun getSpectrumById(id: String): Spectrum?
    suspend fun saveSpectrum(spectrum: Spectrum)
    suspend fun renameSpectrum(id: String, newName: String)
    suspend fun duplicateSpectrum(id: String, newName: String? = null): Spectrum?
    suspend fun deleteSpectrum(id: String)
    suspend fun clearAllHistory()
}
