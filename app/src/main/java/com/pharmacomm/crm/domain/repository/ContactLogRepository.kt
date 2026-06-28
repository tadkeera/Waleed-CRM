package com.pharmacomm.crm.domain.repository

import com.pharmacomm.crm.domain.model.ContactLog
import kotlinx.coroutines.flow.Flow

interface ContactLogRepository {
    fun getLogsByClient(clientId: Long): Flow<List<ContactLog>>
    fun getRecentLogs(): Flow<List<ContactLog>>
    suspend fun insertLog(log: ContactLog)
    suspend fun getWeeklyContactCount(clientId: Long, weekStart: Long): Int
    fun getTotalWeeklyContacts(weekStart: Long): Flow<Int>
}