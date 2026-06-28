package com.pharmacomm.crm.data.local

import androidx.room.*
import com.pharmacomm.crm.domain.model.ContactLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactLogDao {
    @Query("SELECT * FROM contact_logs WHERE clientId = :clientId ORDER BY timestamp DESC")
    fun getLogsByClient(clientId: Long): Flow<List<ContactLog>>

    @Query("SELECT * FROM contact_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<ContactLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ContactLog)

    @Query("SELECT COUNT(*) FROM contact_logs WHERE clientId = :clientId AND timestamp >= :weekStart")
    suspend fun getWeeklyContactCount(clientId: Long, weekStart: Long): Int

    @Query("SELECT COUNT(*) FROM contact_logs WHERE timestamp >= :weekStart")
    fun getTotalWeeklyContacts(weekStart: Long): Flow<Int>
}