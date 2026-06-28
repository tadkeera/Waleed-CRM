package com.pharmacomm.crm.data.repository

import com.pharmacomm.crm.data.local.ContactLogDao
import com.pharmacomm.crm.domain.model.ContactLog
import com.pharmacomm.crm.domain.repository.ContactLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ContactLogRepositoryImpl @Inject constructor(
    private val contactLogDao: ContactLogDao
) : ContactLogRepository {

    override fun getLogsByClient(clientId: Long): Flow<List<ContactLog>> = 
        contactLogDao.getLogsByClient(clientId)

    override fun getRecentLogs(): Flow<List<ContactLog>> = contactLogDao.getRecentLogs()

    override suspend fun insertLog(log: ContactLog) {
        contactLogDao.insertLog(log)
    }

    override suspend fun getWeeklyContactCount(clientId: Long, weekStart: Long): Int {
        return contactLogDao.getWeeklyContactCount(clientId, weekStart)
    }

    override fun getTotalWeeklyContacts(weekStart: Long): Flow<Int> {
        return contactLogDao.getTotalWeeklyContacts(weekStart)
    }
}