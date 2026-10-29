package com.example.data.repository

import com.example.data.local.SpectrumDao
import com.example.data.local.SpectrumEntity
import com.example.domain.model.Spectrum
import com.example.domain.repository.SpectrumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SpectrumRepositoryImpl(
    private val spectrumDao: SpectrumDao
) : SpectrumRepository {

    override fun getHistorySpectra(): Flow<List<Spectrum>> {
        return spectrumDao.getAllSpectra().map { entities ->
            entities.map { it.toDomainSpectrum() }
        }
    }

    override suspend fun getSpectrumById(id: String): Spectrum? = withContext(Dispatchers.IO) {
        spectrumDao.getSpectrumById(id)?.toDomainSpectrum()
    }

    override suspend fun saveSpectrum(spectrum: Spectrum) = withContext(Dispatchers.IO) {
        val entity = SpectrumEntity.fromDomainSpectrum(spectrum)
        spectrumDao.insertSpectrum(entity)
    }

    override suspend fun renameSpectrum(id: String, newName: String) = withContext(Dispatchers.IO) {
        spectrumDao.renameSpectrum(id, newName)
    }

    override suspend fun duplicateSpectrum(id: String, newName: String?): Spectrum? = withContext(Dispatchers.IO) {
        val original = spectrumDao.getSpectrumById(id)?.toDomainSpectrum() ?: return@withContext null
        val clonedName = newName ?: "${original.name} (Copy)"
        val cloned = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = clonedName,
            createdAt = System.currentTimeMillis()
        )
        val entity = SpectrumEntity.fromDomainSpectrum(cloned)
        spectrumDao.insertSpectrum(entity)
        cloned
    }

    override suspend fun deleteSpectrum(id: String) = withContext(Dispatchers.IO) {
        spectrumDao.deleteSpectrumById(id)
    }

    override suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        spectrumDao.clearHistory()
    }
}
