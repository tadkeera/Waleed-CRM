package com.waleed.crm.data.room

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class LabelCount(
    val label: String,
    val count: Int
)

data class CampaignTotals(
    @ColumnInfo(name = "total_campaigns") val totalCampaigns: Int,
    @ColumnInfo(name = "total_targets") val totalTargets: Int,
    @ColumnInfo(name = "total_opened") val totalOpened: Int
)

data class ModeCount(
    @ColumnInfo(name = "message_mode") val messageMode: String,
    val count: Int
)

data class ClientWithMessageCount(
    @Embedded val client: ClientEntity,
    @ColumnInfo(name = "message_count") val messageCount: Int
)

@Dao
interface ClientDao {
    @Query("SELECT COUNT(*) FROM clients") suspend fun count(): Int
    @Query("SELECT * FROM clients ORDER BY name ASC") suspend fun getAll(): List<ClientEntity>
    @Query("SELECT * FROM clients ORDER BY name ASC LIMIT :limit OFFSET :offset") suspend fun getPage(limit: Int, offset: Int): List<ClientEntity>
    @Query("SELECT * FROM clients ORDER BY name ASC LIMIT :limit OFFSET :offset") fun observeClientsPage(limit: Int, offset: Int): Flow<List<ClientEntity>>
    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1") suspend fun getById(id: Long): ClientEntity?
    @Query("SELECT * FROM clients WHERE phone LIKE '%' || :phone || '%' OR second_phone LIKE '%' || :phone || '%' ORDER BY id DESC LIMIT 1") suspend fun getByPhone(phone: String): ClientEntity?
    @Query("SELECT * FROM clients WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR second_phone LIKE '%' || :query || '%' OR specialization LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY name ASC LIMIT :limit") fun searchClients(query: String, limit: Int = 100): Flow<List<ClientEntity>>
    @Query("SELECT * FROM clients WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR second_phone LIKE '%' || :query || '%' OR specialization LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY name ASC LIMIT :limit") suspend fun searchClientsList(query: String, limit: Int = 100): List<ClientEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(client: ClientEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(client: ClientEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(clients: List<ClientEntity>): List<Long>
    @Update suspend fun update(client: ClientEntity): Int
    @Delete suspend fun delete(client: ClientEntity): Int
    @Query("DELETE FROM clients WHERE id = :id") suspend fun deleteById(id: Long): Int
    @Query("DELETE FROM clients") suspend fun deleteAll(): Int
}

@Dao
interface CatalogDao {
    @Query("SELECT COUNT(*) FROM specializations") suspend fun countSpecializations(): Int
    @Query("SELECT COUNT(*) FROM locations") suspend fun countLocations(): Int
    @Query("SELECT COUNT(*) FROM pharmacies") suspend fun countPharmacies(): Int
    @Query("SELECT * FROM specializations ORDER BY name ASC") suspend fun getSpecializations(): List<SpecializationEntity>
    @Query("SELECT * FROM specializations WHERE name = :name LIMIT 1") suspend fun getSpecializationByName(name: String): SpecializationEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSpecialization(specialization: SpecializationEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSpecializations(specializations: List<SpecializationEntity>): List<Long>

    @Query("SELECT * FROM locations ORDER BY name ASC") suspend fun getLocations(): List<LocationEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertLocation(location: LocationEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertLocations(locations: List<LocationEntity>): List<Long>

    @Query("SELECT * FROM pharmacies WHERE client_id = :clientId ORDER BY name ASC") suspend fun getPharmaciesByClientId(clientId: Long): List<PharmacyEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPharmacy(pharmacy: PharmacyEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertPharmacies(pharmacies: List<PharmacyEntity>): List<Long>
}

@Dao
interface GalleryDao {
    @Query("SELECT COUNT(*) FROM gallery_files") suspend fun count(): Int
    @Query("SELECT * FROM gallery_files ORDER BY date_added DESC") suspend fun getAll(): List<GalleryFileEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(file: GalleryFileEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(files: List<GalleryFileEntity>): List<Long>
    @Query("DELETE FROM gallery_files WHERE id = :id") suspend fun deleteById(id: Long): Int
}

@Dao
interface MessageDao {
    @Query("SELECT COUNT(*) FROM message_logs") suspend fun countLogs(): Int
    @Query("SELECT COUNT(*) FROM message_templates") suspend fun countTemplates(): Int
    @Query("SELECT COUNT(*) FROM message_campaigns") suspend fun countCampaigns(): Int
    @Query("SELECT * FROM message_logs WHERE client_id = :clientId ORDER BY timestamp DESC") fun observeLogsForClient(clientId: Long): Flow<List<MessageLogEntity>>
    @Query("SELECT * FROM message_logs WHERE client_id = :clientId ORDER BY timestamp DESC") suspend fun getLogsForClient(clientId: Long): List<MessageLogEntity>
    @Query("SELECT * FROM message_logs ORDER BY timestamp DESC") suspend fun getAllLogs(): List<MessageLogEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLog(log: MessageLogEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertLogs(logs: List<MessageLogEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertTemplate(template: MessageTemplateEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTemplates(templates: List<MessageTemplateEntity>): List<Long>
    @Query("DELETE FROM message_templates WHERE id = :id") suspend fun deleteTemplate(id: Long): Int
    @Query("SELECT * FROM message_templates ORDER BY date_added DESC") suspend fun getTemplates(): List<MessageTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCampaign(campaign: MessageCampaignEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCampaigns(campaigns: List<MessageCampaignEntity>): List<Long>
    @Query("UPDATE message_campaigns SET sent_count = :sentCount WHERE id = :campaignId") suspend fun updateCampaignSentCount(campaignId: Long, sentCount: Int): Int
    @Query("SELECT * FROM message_campaigns ORDER BY date_created DESC") suspend fun getCampaigns(): List<MessageCampaignEntity>
}

@Dao
interface FollowUpDao {
    @Query("SELECT COUNT(*) FROM follow_ups") suspend fun count(): Int
    @Query("SELECT * FROM follow_ups WHERE status = 'PENDING' ORDER BY due_at ASC") fun observePendingFollowUps(): Flow<List<FollowUpEntity>>
    @Query("SELECT * FROM follow_ups WHERE status = 'PENDING' ORDER BY due_at ASC") suspend fun getPendingFollowUps(): List<FollowUpEntity>
    @Query("SELECT * FROM follow_ups WHERE client_id = :clientId ORDER BY due_at ASC") suspend fun getByClientId(clientId: Long): List<FollowUpEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(followUp: FollowUpEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(followUps: List<FollowUpEntity>): List<Long>
    @Query("UPDATE follow_ups SET status = :status WHERE id = :id") suspend fun updateStatus(id: Long, status: String): Int
    @Query("DELETE FROM follow_ups WHERE id = :id") suspend fun deleteById(id: Long): Int
}

@Dao
interface UserDao {
    @Query("SELECT COUNT(*) FROM users") suspend fun count(): Int
    @Query("SELECT * FROM users ORDER BY created_at ASC") fun observeUsers(): Flow<List<UserEntity>>
    @Query("SELECT * FROM users ORDER BY created_at ASC") suspend fun getUsers(): List<UserEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(user: UserEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(users: List<UserEntity>): List<Long>
    @Query("UPDATE users SET role = :role, is_active = :active WHERE id = :id") suspend fun updateRole(id: Long, role: String, active: Boolean): Int
    @Query("DELETE FROM users WHERE id = :id") suspend fun deleteById(id: Long): Int
}

@Dao
interface AuditDao {
    @Query("SELECT COUNT(*) FROM audit_logs") suspend fun count(): Int
    @Query("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT :limit") fun observeAudit(limit: Int = 250): Flow<List<AuditLogEntity>>
    @Query("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT :limit") suspend fun getAudit(limit: Int = 250): List<AuditLogEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(log: AuditLogEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(logs: List<AuditLogEntity>): List<Long>
}

@Dao
interface SegmentDao {
    @Query("SELECT COUNT(*) FROM saved_segments") suspend fun count(): Int
    @Query("SELECT * FROM saved_segments ORDER BY created_at DESC") fun observeSegments(): Flow<List<SavedSegmentEntity>>
    @Query("SELECT * FROM saved_segments ORDER BY created_at DESC") suspend fun getSegments(): List<SavedSegmentEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(segment: SavedSegmentEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(segments: List<SavedSegmentEntity>): List<Long>
    @Query("DELETE FROM saved_segments WHERE id = :id") suspend fun deleteById(id: Long): Int
}

@Dao
interface DashboardDao {
    @Query("SELECT COUNT(*) FROM clients") suspend fun totalClients(): Int
    @Query("SELECT COUNT(*) FROM clients WHERE client_type = 'طبيب'") suspend fun totalDoctors(): Int
    @Query("SELECT COUNT(*) FROM clients WHERE client_type = 'طبيب' AND is_classified = 1 AND specialization != '' AND location != ''") suspend fun totalClassifiedDoctors(): Int
    @Query("SELECT COUNT(*) FROM message_logs WHERE timestamp >= :since") suspend fun countMessagesSince(since: Long): Int
    @Query("SELECT COUNT(*) FROM follow_ups WHERE status = 'PENDING'") suspend fun pendingFollowUpsCount(): Int

    @Query("SELECT * FROM clients WHERE client_type = 'طبيب' AND (is_classified = 0 OR specialization = '' OR location = '') ORDER BY name ASC LIMIT :limit") suspend fun unclassifiedDoctors(limit: Int): List<ClientEntity>

    @Query("""
        SELECT c.*, COUNT(m.id) AS message_count FROM clients c
        INNER JOIN message_logs m ON c.id = m.client_id
        WHERE c.client_type = 'طبيب' AND m.timestamp >= :since
        GROUP BY c.id
        ORDER BY message_count DESC
    """)
    suspend fun contactedDoctorsSince(since: Long): List<ClientWithMessageCount>

    @Query("""
        SELECT * FROM clients
        WHERE client_type = 'طبيب' AND id NOT IN (
            SELECT DISTINCT client_id FROM message_logs WHERE timestamp >= :since
        )
        ORDER BY name ASC
        LIMIT :limit
    """)
    suspend fun uncontactedDoctorsSince(since: Long, limit: Int): List<ClientEntity>

    @Query("""
        SELECT c.*, COUNT(m.id) AS message_count FROM clients c
        INNER JOIN message_logs m ON c.id = m.client_id
        WHERE c.client_type = 'طبيب'
        GROUP BY c.id
        ORDER BY message_count DESC
        LIMIT :limit
    """)
    suspend fun topContactedDoctors(limit: Int): List<ClientWithMessageCount>

    @Query("""
        SELECT * FROM clients
        WHERE client_type = 'طبيب' AND id NOT IN (
            SELECT DISTINCT client_id FROM message_logs WHERE timestamp >= :since
        )
        ORDER BY name ASC
        LIMIT :limit
    """)
    suspend fun overdueDoctors(since: Long, limit: Int): List<ClientEntity>

    @Query("SELECT specialization AS label, COUNT(*) AS count FROM clients WHERE specialization != '' GROUP BY specialization ORDER BY count DESC LIMIT :limit") suspend fun specializationStats(limit: Int): List<LabelCount>
    @Query("SELECT location AS label, COUNT(*) AS count FROM clients WHERE location != '' GROUP BY location ORDER BY count DESC LIMIT :limit") suspend fun locationStats(limit: Int): List<LabelCount>
    @Query("SELECT COUNT(*) AS total_campaigns, COALESCE(SUM(target_count), 0) AS total_targets, COALESCE(SUM(sent_count), 0) AS total_opened FROM message_campaigns") suspend fun campaignTotals(): CampaignTotals
    @Query("SELECT message_mode, COUNT(*) AS count FROM message_campaigns GROUP BY message_mode") suspend fun campaignModeCounts(): List<ModeCount>
    @Query("SELECT * FROM message_campaigns ORDER BY date_created DESC LIMIT :limit") suspend fun recentCampaigns(limit: Int): List<MessageCampaignEntity>
}
