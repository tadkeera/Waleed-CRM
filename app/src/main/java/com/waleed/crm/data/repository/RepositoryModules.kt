package com.waleed.crm.data.repository

import com.waleed.crm.data.*
import com.waleed.crm.data.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Phase 20 modular repositories.
 * These modules are the clean Room/DAO boundary. The legacy CrmRepository remains as a compatibility facade
 * during migration so current UI screens keep working while new screens can consume Flow APIs safely.
 */
class ClientRepository(private val dao: ClientDao) {
    fun observePage(page: Int, pageSize: Int): Flow<List<Client>> = dao.observeClientsPage(pageSize, page * pageSize).map { list -> list.map { it.toModel() } }
    fun search(query: String, limit: Int = 100): Flow<List<Client>> = dao.searchClients(query, limit).map { list -> list.map { it.toModel() } }
    suspend fun upsert(client: Client): Long = dao.upsert(client.toEntity())
}

class MessagingRepository(private val dao: MessageDao) {
    fun observeClientLogs(clientId: Long): Flow<List<MessageLog>> = dao.observeLogsForClient(clientId).map { list -> list.map { it.toModel() } }
    suspend fun log(log: MessageLog): Long = dao.insertLog(log.toEntity())
}

class FollowUpRepository(private val dao: FollowUpDao) {
    fun observePending(): Flow<List<FollowUp>> = dao.observePendingFollowUps().map { list -> list.map { it.toModel() } }
    suspend fun upsert(followUp: FollowUp): Long = dao.upsert(followUp.toEntity())
}

class UserRepository(private val dao: UserDao) {
    fun observeUsers(): Flow<List<UserAccount>> = dao.observeUsers().map { list -> list.map { it.toModel() } }
    suspend fun insert(user: UserAccount): Long = dao.insert(user.toEntity())
}

class AuditRepository(private val dao: AuditDao) {
    fun observeAudit(limit: Int = 250): Flow<List<AuditLog>> = dao.observeAudit(limit).map { list -> list.map { it.toModel() } }
    suspend fun insert(log: AuditLog): Long = dao.insert(log.toEntity())
}

class SegmentRepository(private val dao: SegmentDao) {
    fun observeSegments(): Flow<List<SavedSegment>> = dao.observeSegments().map { list -> list.map { it.toModel() } }
    suspend fun upsert(segment: SavedSegment): Long = dao.upsert(segment.toEntity())
}

data class RepositoryModuleSet(
    val clients: ClientRepository,
    val messaging: MessagingRepository,
    val followUps: FollowUpRepository,
    val users: UserRepository,
    val audit: AuditRepository,
    val segments: SegmentRepository
) {
    companion object {
        fun fromRoom(db: WaleedRoomDatabase): RepositoryModuleSet = RepositoryModuleSet(
            clients = ClientRepository(db.clientDao()),
            messaging = MessagingRepository(db.messageDao()),
            followUps = FollowUpRepository(db.followUpDao()),
            users = UserRepository(db.userDao()),
            audit = AuditRepository(db.auditDao()),
            segments = SegmentRepository(db.segmentDao())
        )
    }
}
