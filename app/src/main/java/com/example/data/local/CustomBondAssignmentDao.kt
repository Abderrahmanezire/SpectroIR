package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomBondAssignmentDao {
    @Query("SELECT * FROM custom_bond_assignments ORDER BY typicalWavenumber DESC")
    fun getAllCustomAssignmentsFlow(): Flow<List<CustomBondAssignmentEntity>>

    @Query("SELECT * FROM custom_bond_assignments ORDER BY typicalWavenumber DESC")
    suspend fun getAllCustomAssignments(): List<CustomBondAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: CustomBondAssignmentEntity)

    @Query("DELETE FROM custom_bond_assignments WHERE id = :id")
    suspend fun deleteById(id: String)
}
