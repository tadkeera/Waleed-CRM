package com.pharmacomm.crm.domain.repository

import com.pharmacomm.crm.domain.model.Region
import com.pharmacomm.crm.domain.model.Specialty
import kotlinx.coroutines.flow.Flow

interface LookupRepository {
    fun getAllSpecialties(): Flow<List<Specialty>>
    fun getAllRegions(): Flow<List<Region>>
    suspend fun insertSpecialty(name: String): Long
    suspend fun insertRegion(name: String): Long
    suspend fun searchSpecialties(prefix: String): List<Specialty>
    suspend fun searchRegions(prefix: String): List<Region>
}