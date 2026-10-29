package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomReferenceSpectrumDao {
    @Query("SELECT * FROM custom_reference_spectra ORDER BY compoundName ASC")
    fun getAllCustomReferencesFlow(): Flow<List<CustomReferenceSpectrumEntity>>

    @Query("SELECT * FROM custom_reference_spectra ORDER BY compoundName ASC")
    suspend fun getAllCustomReferences(): List<CustomReferenceSpectrumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReference(entity: CustomReferenceSpectrumEntity)

    @Query("DELETE FROM custom_reference_spectra WHERE id = :id")
    suspend fun deleteReference(id: String)

    @Query("SELECT COUNT(*) FROM custom_reference_spectra")
    suspend fun count(): Int
}
