package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpectrumDao {
    @Query("SELECT * FROM spectrum_history ORDER BY createdAt DESC")
    fun getAllSpectra(): Flow<List<SpectrumEntity>>

    @Query("SELECT * FROM spectrum_history WHERE id = :id LIMIT 1")
    suspend fun getSpectrumById(id: String): SpectrumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpectrum(spectrum: SpectrumEntity)

    @Query("UPDATE spectrum_history SET name = :newName WHERE id = :id")
    suspend fun renameSpectrum(id: String, newName: String)

    @Query("DELETE FROM spectrum_history WHERE id = :id")
    suspend fun deleteSpectrumById(id: String)

    @Query("DELETE FROM spectrum_history")
    suspend fun clearHistory()
}
