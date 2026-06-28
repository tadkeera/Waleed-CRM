package com.pharmacomm.crm.data.repository

import com.pharmacomm.crm.data.local.LookupDao
import com.pharmacomm.crm.domain.model.Region
import com.pharmacomm.crm.domain.model.Specialty
import com.pharmacomm.crm.domain.repository.LookupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LookupRepositoryImpl @Inject constructor(
    private val lookupDao: LookupDao
) : LookupRepository {

    override fun getAllSpecialties(): Flow<List<Specialty>> = lookupDao.getAllSpecialties()

    override fun getAllRegions(): Flow<List<Region>> = lookupDao.getAllRegions()

    override suspend fun insertSpecialty(name: String): Long {
        return lookupDao.insertSpecialty(Specialty(name = name))
    }

    override suspend fun insertRegion(name: String): Long {
        return lookupDao.insertRegion(Region(name = name))
    }

    override suspend fun searchSpecialties(prefix: String): List<Specialty> {
        return lookupDao.searchSpecialties(prefix)
    }

    override suspend fun searchRegions(prefix: String): List<Region> {
        return lookupDao.searchRegions(prefix)
    }
}