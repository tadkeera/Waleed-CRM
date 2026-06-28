package com.pharmacomm.crm.data.local

import androidx.room.*
import com.pharmacomm.crm.domain.model.Region
import com.pharmacomm.crm.domain.model.Specialty
import kotlinx.coroutines.flow.Flow

@Dao
interface LookupDao {
    @Query("SELECT * FROM specialties ORDER BY name ASC")
    fun getAllSpecialties(): Flow<List<Specialty>>

    @Query("SELECT * FROM regions ORDER BY name ASC")
    fun getAllRegions(): Flow<List<Region>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSpecialty(specialty: Specialty): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRegion(region: Region): Long

    @Query("SELECT * FROM specialties WHERE name LIKE '%' || :prefix || '%' LIMIT 10")
    suspend fun searchSpecialties(prefix: String): List<Specialty>

    @Query("SELECT * FROM regions WHERE name LIKE '%' || :prefix || '%' LIMIT 10")
    suspend fun searchRegions(prefix: String): List<Region>
}