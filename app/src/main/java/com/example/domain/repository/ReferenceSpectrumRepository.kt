package com.example.domain.repository

import com.example.data.demo.DemoSpectraProvider
import com.example.data.local.CustomReferenceSpectrumDao
import com.example.data.local.CustomReferenceSpectrumEntity
import com.example.domain.model.ReferenceSpectrum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ReferenceSpectrumRepository {
    fun getAllReferencesFlow(): Flow<List<ReferenceSpectrum>>
    suspend fun getAllReferences(): List<ReferenceSpectrum>
    suspend fun addCustomReference(reference: ReferenceSpectrum)
    suspend fun deleteCustomReference(id: String)
}

class ReferenceSpectrumRepositoryImpl(
    private val dao: CustomReferenceSpectrumDao
) : ReferenceSpectrumRepository {

    private val standardReferences: List<ReferenceSpectrum> = DemoSpectraProvider.getAllReferenceSpectra()

    override fun getAllReferencesFlow(): Flow<List<ReferenceSpectrum>> {
        return dao.getAllCustomReferencesFlow().map { entities ->
            val custom = entities.map { it.toDomain() }
            standardReferences + custom
        }
    }

    override suspend fun getAllReferences(): List<ReferenceSpectrum> {
        val custom = dao.getAllCustomReferences().map { it.toDomain() }
        return standardReferences + custom
    }

    override suspend fun addCustomReference(reference: ReferenceSpectrum) {
        dao.insertReference(CustomReferenceSpectrumEntity.fromDomain(reference))
    }

    override suspend fun deleteCustomReference(id: String) {
        dao.deleteReference(id)
    }
}
