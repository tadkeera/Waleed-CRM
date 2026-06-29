package com.waleed.crm.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC LIMIT :limit OFFSET :offset")
    fun observeClientsPage(limit: Int, offset: Int): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR specialization LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY name ASC LIMIT :limit")
    fun searchClients(query: String, limit: Int = 100): Flow<List<ClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(client: ClientEntity): Long
    @Update suspend fun update(client: ClientEntity): Int
    @Delete suspend fun delete(client: ClientEntity): Int
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM message_logs WHERE client_id = :clientId ORDER BY timestamp DESC")
    fun observeLogsForClient(clientId: Long): Flow<List<MessageLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLog(log: MessageLogEntity): Long
}

@Dao
interface FollowUpDao {
    @Query("SELECT * FROM follow_ups WHERE status = 'PENDING' ORDER BY due_at ASC")
    fun observePendingFollowUps(): Flow<List<FollowUpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(followUp: FollowUpEntity): Long
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY created_at ASC")
    fun observeUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(user: UserEntity): Long
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT :limit")
    fun observeAudit(limit: Int = 250): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(log: AuditLogEntity): Long
}

@Dao
interface SegmentDao {
    @Query("SELECT * FROM saved_segments ORDER BY created_at DESC")
    fun observeSegments(): Flow<List<SavedSegmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(segment: SavedSegmentEntity): Long
}
